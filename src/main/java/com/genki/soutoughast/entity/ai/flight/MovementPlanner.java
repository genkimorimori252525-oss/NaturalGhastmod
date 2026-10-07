package com.genki.soutoughast.entity.ai.flight;

/** First movement unit: quiet intervals, compact feasible drift and frontal correction. */
public final class MovementPlanner {
    public record Plan(MovementPrimitive primitive, FlightController.Intent intent, FlightVector waypoint,
                       CombatAnchor.Range range, boolean inRegion) { }
    private int remaining;
    private FlightVector waypoint;
    public void reset(){remaining=0;waypoint=null;}
    public Plan step(CombatAnchor anchor,FlightVector target,FlightVector boss,FlightVector facing,
                     MobilityContext.Kind context,MobilityContext.Sample sample,double variation){
        if(!Double.isFinite(variation)||variation<0||variation>=1)throw new IllegalArgumentException("Invalid movement variation");
        var region=anchor.evaluate(target,facing,boss);
        if(context==MobilityContext.Kind.GROUND_FORCED){reset();return stopped(MovementPrimitive.BRAKE,region);}
        if(!region.inRegion()){
            reset();
            MovementPrimitive primitive=switch(region.range()){
                case TOO_CLOSE->MovementPrimitive.WITHDRAW;case TOO_FAR->MovementPrimitive.APPROACH;case COMFORTABLE->MovementPrimitive.RETURN;};
            return sample.allows(region.intent().direction())?new Plan(primitive,region.intent(),region.point(),region.range(),false)
                    :stopped(MovementPrimitive.BRAKE,region);
        }
        if(remaining>0){
            remaining--;
            if(waypoint==null)return stopped(MovementPrimitive.HOLD,region);
            FlightVector error=waypoint.subtract(boss);
            if(!anchor.evaluate(target,facing,waypoint).inRegion()||!sample.allows(error)||error.length()<.3){waypoint=null;remaining=24;return stopped(MovementPrimitive.BRAKE,region);}
            return moving(error,region);
        }
        if(waypoint!=null){waypoint=null;remaining=24;return stopped(MovementPrimitive.BRAKE,region);}
        if(variation<.6){remaining=24+(int)(variation*20);return stopped(MovementPrimitive.HOLD,region);}
        double radius=context==MobilityContext.Kind.OPEN_AIR?4:context==MobilityContext.Kind.SEMI_OPEN?3:1.5;
        int start=Math.min(9,(int)((variation-.6)/.4*10));
        for(int offset=0;offset<10;offset++){
            int direction=(start+offset)%10;
            if(!sample.clear(direction))continue;
            FlightVector candidate=boss.add(MobilityContext.direction(direction).scale(radius));
            if(!anchor.evaluate(target,facing,candidate).inRegion())continue;
            waypoint=candidate;remaining=8;return moving(candidate.subtract(boss),region);
        }
        remaining=24;return stopped(MovementPrimitive.HOLD,region);
    }
    private Plan moving(FlightVector error,CombatAnchor.Evaluation region){
        return new Plan(MovementPrimitive.DRIFT,FlightController.Intent.move(error,.16),waypoint,region.range(),true);
    }
    private static Plan stopped(MovementPrimitive primitive,CombatAnchor.Evaluation region){
        return new Plan(primitive,primitive==MovementPrimitive.BRAKE?FlightController.Intent.brake():FlightController.Intent.hold(),
                region.point(),region.range(),region.inRegion());
    }
}
