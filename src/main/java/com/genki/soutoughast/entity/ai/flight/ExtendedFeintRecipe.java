package com.genki.soutoughast.entity.ai.flight;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import static com.genki.soutoughast.entity.ai.flight.ManeuverComposer.Phase.*;

/** Frozen, preflighted extra recipes. The normal controller owns acceleration and braking. */
final class ExtendedFeintRecipe {
    private record Leg(ManeuverComposer.Phase phase,FlightVector point,int budget,double speed,boolean arrival) {}
    private final TacticalEvaluator.Action action;
    private final CombatAnchor.Region region;
    private final BiPredicate<FlightVector,FlightVector> route;
    private final List<Leg> legs;
    private int index,ticks,age;
    private boolean active=true,committed,timeout;
    private TacticalBrain.State state;

    private ExtendedFeintRecipe(TacticalEvaluator.Action action,CombatAnchor.Region region,
            BiPredicate<FlightVector,FlightVector> route,List<Leg> legs){
        this.action=action;this.region=region;this.route=route;this.legs=legs;
        state=new TacticalBrain.State(action,TELEGRAPH,ManeuverComposer.TimingSlot.BEFORE_COMMIT,false);
    }
    static boolean handles(TacticalEvaluator.Action action){return action==TacticalEvaluator.Action.PASS_BY_FAKE
            ||action==TacticalEvaluator.Action.DOUBLE_FAKE||action==TacticalEvaluator.Action.ABORT_FAKE;}
    static ExtendedFeintRecipe prepare(TacticalEvaluator.Action action,FlightVector target,FlightVector boss,
            CombatAnchor.Region region,MobilityContext.Sample sample,double variation,
            BiPredicate<FlightVector,FlightVector> route){
        if(route==null||target==null||region==null||!region.contains(boss))return null;
        var offset=target.subtract(boss);var toward=new FlightVector(offset.x(),0,offset.z()).normalized();
        if(toward.length()<1e-9||!Double.isFinite(variation)||variation<0||variation>=1)return null;
        var side=new FlightVector(-toward.z(),0,toward.x()).scale(variation<.5?-1:1);
        FlightVector first,second;List<Leg> legs;
        if(action==TacticalEvaluator.Action.PASS_BY_FAKE){
            if(Math.hypot(offset.x(),offset.z())<6||Math.abs(offset.y())>8)return null;
            var plane=new FlightVector(target.x(),boss.y(),target.z());
            first=plane.add(toward.scale(4)).add(side.scale(6));second=first.add(side.scale(4));
            legs=List.of(new Leg(TELEGRAPH,first,24,.14,false),new Leg(COMMIT,first,travel(boss,first),.32,true),
                    brake(),new Leg(REVEAL,second,travel(first,second),.32,true),brake(),returning());
        }else{
            first=boss.add(side.scale(4));second=boss.subtract(side.scale(4));
            legs=action==TacticalEvaluator.Action.ABORT_FAKE?List.of(new Leg(TELEGRAPH,first,24,.14,false),brake(),returning()):
                    List.of(new Leg(TELEGRAPH,first,24,.14,false),new Leg(COMMIT,first,8,.14,false),brake(),
                            new Leg(REVEAL,second,travel(first,second),.32,true),brake(),
                            new Leg(REVEAL,first,travel(second,first),.32,true),brake(),returning());
        }
        if(legs.stream().mapToInt(Leg::budget).sum()>160)return null;
        var from=boss;
        for(var leg:legs){
            if(leg.point()==null||leg.point().equals(from))continue;
            if(region.radius(leg.point())>.9||!sample.allows(leg.point().subtract(from))||!route.test(from,leg.point()))return null;
            from=leg.point();
        }
        return new ExtendedFeintRecipe(action,region,route,legs);
    }
    private static int travel(FlightVector from,FlightVector to){
        // Full travel distance plus acceleration/arrival allowance, no fixed short deadline.
        return (int)Math.ceil(to.subtract(from).length()/.32)+12;
    }
    private static Leg brake(){return new Leg(BRAKE,null,12,0,false);}
    private static Leg returning(){return new Leg(RETURN,null,16,0,false);}
    boolean active(){return active;}
    boolean committed(){return active&&committed;}
    TacticalBrain.State state(){return state;}
    MovementPlanner.Plan step(FlightVector boss,CombatAnchor.Range range,Supplier<MovementPlanner.Plan> ordinary){
        var leg=timeout?(index==0?brake():returning()):legs.get(index);
        if(!timeout&&leg.phase()==COMMIT)committed=true;
        if(leg.arrival()&&boss.subtract(leg.point()).length()<=.7){advance();leg=legs.get(index);}
        if(leg.point()!=null&&(!region.contains(boss)||!route.test(boss,leg.point()))){
            active=false;state=new TacticalBrain.State(action,BRAKE,ManeuverComposer.TimingSlot.AFTER_REVEAL,committed);
            return stopped(boss,range);
        }
        state=new TacticalBrain.State(action,leg.phase(),leg.phase()==TELEGRAPH?ManeuverComposer.TimingSlot.BEFORE_COMMIT:
                leg.phase()==COMMIT?ManeuverComposer.TimingSlot.ON_COMMIT:leg.phase()==RETURN?
                        ManeuverComposer.TimingSlot.ON_RETURN:ManeuverComposer.TimingSlot.AFTER_REVEAL,committed);
        MovementPlanner.Plan plan;
        if(leg.phase()==RETURN)plan=ordinary.get();
        else if(leg.phase()==BRAKE)plan=stopped(boss,range);
        else{
            var delta=leg.point().subtract(boss);
            var primitive=action==TacticalEvaluator.Action.PASS_BY_FAKE?(leg.phase()==REVEAL?MovementPrimitive.CURVE:MovementPrimitive.APPROACH):MovementPrimitive.STRAFE;
            double speed=Math.min(leg.speed(),Math.sqrt(2*FlightController.BRAKING*Math.max(0,delta.length()-.4)));
            plan=new MovementPlanner.Plan(primitive,FlightController.Intent.move(delta,speed),leg.point(),range,region.contains(boss));
        }
        age++;ticks++;
        if(ticks>=leg.budget()){
            if(!timeout&&leg.arrival()&&boss.subtract(leg.point()).length()>.7){timeout=true;index=0;ticks=0;}
            else advance();
        }
        if(age>=160)active=false;
        return plan;
    }
    private void advance(){ticks=0;if(timeout){if(++index>=2)active=false;}else if(++index>=legs.size())active=false;}
    private static MovementPlanner.Plan stopped(FlightVector boss,CombatAnchor.Range range){
        return new MovementPlanner.Plan(MovementPrimitive.BRAKE,FlightController.Intent.brake(),boss,range,true);
    }
}
