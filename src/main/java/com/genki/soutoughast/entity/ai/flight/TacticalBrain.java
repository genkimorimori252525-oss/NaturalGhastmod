package com.genki.soutoughast.entity.ai.flight;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.BiPredicate;

/** Quiet swimming is authoritative; observed-only feints never own or relocate the region. */
public final class TacticalBrain {
    public record State(TacticalEvaluator.Action action,ManeuverComposer.Phase phase,
                        ManeuverComposer.TimingSlot timingSlot,boolean committed){
        public static State idle(){return new State(TacticalEvaluator.Action.DRIFT,ManeuverComposer.Phase.IDLE,ManeuverComposer.TimingSlot.AT_ANCHOR,false);}
    }
    private final TacticalMemory memory=new TacticalMemory();
    private final TacticalEvaluator evaluator=new TacticalEvaluator();
    private final ManeuverComposer composer=new ManeuverComposer();
    private int quiet=240;
    private long generation=-1;
    public State state(){return composer.active()||composer.state().phase()!=ManeuverComposer.Phase.IDLE?composer.state():State.idle();}
    public void reset(){memory.clear();composer.cancel();quiet=240;generation=-1;}
    public MovementPlanner.Plan step(CombatAnchor anchor,FlightVector observedPosition,FlightVector observedVelocity,
            FlightVector boss,boolean visible,MobilityContext.Kind context,MobilityContext.Sample sample,double variation,
            Predicate<FlightVector> clearance,Supplier<MovementPlanner.Plan> ordinary){
        return step(anchor,observedPosition,observedVelocity,boss,visible,context,sample,variation,clearance,null,ordinary);
    }
    public MovementPlanner.Plan step(CombatAnchor anchor,FlightVector observedPosition,FlightVector observedVelocity,
            FlightVector boss,boolean visible,MobilityContext.Kind context,MobilityContext.Sample sample,double variation,
            Predicate<FlightVector> clearance,BiPredicate<FlightVector,FlightVector> route,Supplier<MovementPlanner.Plan> ordinary){
        memory.tick();if(quiet>0)quiet--;
        var region=anchor.region();
        if(region==null||region.generation()!=generation){composer.cancel();generation=region==null?-1:region.generation();quiet=240;}
        if(context==MobilityContext.Kind.GROUND_FORCED||region==null||!region.contains(boss)||
                composer.active()&&!visible&&!composer.committed()){
            composer.cancel();quiet=Math.max(quiet,120);return ordinary.get();
        }
        if(!composer.active()){
            composer.cancel();
            if(quiet>0||!visible)return ordinary.get();
            var range=anchor.evaluate(observedPosition,FlightVector.ZERO,boss).range();
            var choice=evaluator.choose(context,range,observedVelocity,variation,memory);
            quiet=40;
            if(!composer.start(choice,observedPosition,boss,region,sample,variation,route))return ordinary.get();
            memory.record(choice);
        }
        var range=visible?anchor.evaluate(observedPosition,FlightVector.ZERO,boss).range():CombatAnchor.Range.COMFORTABLE;
        var result=composer.step(boss,range,clearance,ordinary);
        if(!composer.active())quiet=240;
        return result;
    }
}
