package com.genki.soutoughast.entity.ai.flight;

import java.util.function.Predicate;

/** Sustained feasible swimming inside a retained region; no physical velocity writes. */
public final class MovementPlanner {
    public record Plan(MovementPrimitive primitive, FlightController.Intent intent, FlightVector waypoint,
                       CombatAnchor.Range range, boolean inRegion) { }
    private int remaining;
    private FlightVector waypoint;
    private FlightVector eased=FlightVector.ZERO;
    private double swimSpeed;
    private long generation=-1;
    private MovementPrimitive swimmingPrimitive=MovementPrimitive.DRIFT;
    public void reset(){remaining=0;waypoint=null;eased=FlightVector.ZERO;generation=-1;}
    public Plan step(CombatAnchor anchor,FlightVector target,FlightVector boss,FlightVector facing,
                     MobilityContext.Kind context,MobilityContext.Sample sample,double variation){
        return step(anchor,target,boss,facing,context,sample,variation,sample::allows);
    }
    public Plan step(CombatAnchor anchor,FlightVector target,FlightVector boss,FlightVector facing,
                     MobilityContext.Kind context,MobilityContext.Sample sample,double variation,
                     Predicate<FlightVector> clearance){
        if(!Double.isFinite(variation)||variation<0||variation>=1)throw new IllegalArgumentException("Invalid movement variation");
        var region=anchor.evaluate(target,facing,boss);
        if(generation!=anchor.region().generation()){reset();generation=anchor.region().generation();}
        if(context==MobilityContext.Kind.GROUND_FORCED){reset();return stopped(MovementPrimitive.BRAKE,region);}
        if(!region.inRegion()){
            waypoint=null;remaining=0;eased=FlightVector.ZERO;
            return sample.allows(region.intent().direction())&&clearance.test(region.point().subtract(boss))
                    ?new Plan(MovementPrimitive.RETURN,region.intent(),region.point(),region.range(),false):stopped(MovementPrimitive.BRAKE,region);
        }
        int queries=0;
        if(waypoint!=null&&remaining>0&&waypoint.subtract(boss).length()>1){
            FlightVector delta=waypoint.subtract(boss);
            if(sample.allows(delta)&&clearance.test(delta)){remaining--;return moving(delta,region);}
            queries++;waypoint=null;remaining=0;
        }
        double best=Double.POSITIVE_INFINITY;FlightVector selected=null;
        for(int i=0;i<10&&queries<2;i++){
            double angle=(variation+i*.137)*Math.PI*2;
            double vertical=Math.sin(angle*1.7)*.5;
            FlightVector candidate;
            if(context==MobilityContext.Kind.CONFINED){
                candidate=boss.add(new FlightVector(Math.cos(angle)*3,vertical*2,Math.sin(angle)*3));
            }else{
                // Interior headroom for eased turns; the retained region itself stays20/8/20.
                double scale=context==MobilityContext.Kind.OPEN_AIR?.7:.55;
                double horizontal=20*scale*Math.sqrt(1-vertical*vertical);
                candidate=anchor.region().center().add(new FlightVector(Math.cos(angle)*horizontal,vertical*8,Math.sin(angle)*horizontal));
            }
            FlightVector delta=candidate.subtract(boss);
            if(!anchor.region().contains(candidate)||delta.length()<2||!sample.allows(delta))continue;
            queries++;if(!clearance.test(delta))continue;
            double score=Math.abs(candidate.subtract(target).length()-28);
            if(score<best){best=score;selected=candidate;}
        }
        if(selected==null){waypoint=null;remaining=0;eased=FlightVector.ZERO;return stopped(MovementPrimitive.BRAKE,region);}
        waypoint=selected;remaining=40+(int)(variation*80);swimSpeed=.1+.08*variation;
        swimmingPrimitive=variation<.2?MovementPrimitive.HOLD:MovementPrimitive.DRIFT;
        return moving(waypoint.subtract(boss),region);
    }
    private Plan moving(FlightVector error,CombatAnchor.Evaluation region){
        FlightVector desired=error.normalized().scale(swimSpeed);
        eased=eased.add(desired.subtract(eased).limited(.004));
        return new Plan(swimmingPrimitive,FlightController.Intent.move(eased,eased.length()),waypoint,region.range(),true);
    }
    private static Plan stopped(MovementPrimitive primitive,CombatAnchor.Evaluation region){
        return new Plan(primitive,primitive==MovementPrimitive.BRAKE?FlightController.Intent.brake():FlightController.Intent.hold(),
                region.point(),region.range(),region.inRegion());
    }
}
