package com.genki.soutoughast.entity.ai.flight;

/** Deterministic core scenarios; native timing and visual readability remain separate. */
public final class ObservedTacticsTest {
    private static int checks;
    private static final MobilityContext.Sample OPEN=new MobilityContext.Sample(1023);
    private static final FlightVector TARGET=FlightVector.ZERO,START=new FlightVector(0,6,28);
    public static void main(String[] args){
        selection();composition();brain();repeatAcrossRecovery();
        System.out.println("PASS: "+checks+" observed tactics assertions");
    }
    private static void selection(){
        var memory=new TacticalMemory();var evaluator=new TacticalEvaluator();
        check(evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.COMFORTABLE,FlightVector.ZERO,.1,memory)==TacticalEvaluator.Action.DRIFT,"quiet/default swimming remains common");
        check(evaluator.choose(MobilityContext.Kind.GROUND_FORCED,CombatAnchor.Range.TOO_FAR,FlightVector.ZERO,.99,memory)==TacticalEvaluator.Action.DRIFT,"no air feint in ground-forced context");
        var far=evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.TOO_FAR,FlightVector.ZERO,.99,memory);
        check(far==TacticalEvaluator.Action.FALSE_APPROACH,"range informs a soft tactical preference");
        memory.record(far);
        check(evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.TOO_FAR,FlightVector.ZERO,.99,memory)!=far,"recent maneuver cannot dominate again immediately");
        for(int i=0;i<600;i++)memory.tick();
        check(evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.TOO_FAR,FlightVector.ZERO,.99,memory)==far,"bounded memory decays");
    }
    private static void composition(){
        var anchor=new CombatAnchor();anchor.evaluate(TARGET,FlightVector.ZERO,START);var region=anchor.region();
        var normal=new MovementPlanner.Plan(MovementPrimitive.DRIFT,FlightController.Intent.move(new FlightVector(1,0,0),.1),START,CombatAnchor.Range.COMFORTABLE,true);
        for(var action:TacticalEvaluator.Action.values()){
            if(action==TacticalEvaluator.Action.DRIFT)continue;
            var composer=new ManeuverComposer();
            check(composer.start(action,TARGET,START,region,OPEN,.99),"recipe has feasible region endpoints: "+action);
            FlightVector locked=null;
            for(int tick=0;tick<84;tick++){
                var plan=composer.step(START,CombatAnchor.Range.COMFORTABLE,d->true,()->normal);
                var state=composer.state();
                if(tick==0)locked=plan.waypoint();
                if(tick<24)check(plan.waypoint().equals(locked),"telegraph is committed over multiple ticks");
                check(region.contains(plan.waypoint()),"initial recipes retain the broad region");
                check(state.committed()==(tick>=24),"commit point has explicit timing");
                if(tick==32)check(!plan.waypoint().equals(locked),"reveal contradicts telegraphed movement");
                check(anchor.region().equals(region),"composer never relocates region");
            }
            check(!composer.active(),"finite recipe terminates");
        }
        var denied=new ManeuverComposer();denied.start(TacticalEvaluator.Action.FALSE_RETREAT,TARGET,START,region,OPEN,.99);
        check(denied.step(START,CombatAnchor.Range.COMFORTABLE,d->false,()->normal).intent().mode()==FlightController.Mode.BRAKE,"unsafe committed route is braked");
        check(!denied.active(),"unsafe recipe stops instead of retrying indefinitely");
    }
    private static void brain(){
        var anchor=new CombatAnchor();anchor.evaluate(TARGET,FlightVector.ZERO,START);var original=anchor.region();
        var planner=new MovementPlanner();var brain=new TacticalBrain();var controller=new FlightController();
        FlightVector position=START,velocity=FlightVector.ZERO;int ordinary=0,maneuver=0;
        for(int tick=0;tick<6000;tick++){
            final FlightVector boss=position;
            var plan=brain.step(anchor,TARGET,FlightVector.ZERO,boss,true,MobilityContext.Kind.OPEN_AIR,OPEN,.99,
                d->true,()->planner.step(anchor,TARGET,boss,FlightVector.ZERO,MobilityContext.Kind.OPEN_AIR,OPEN,.42,d->true));
            if(brain.state().phase()==ManeuverComposer.Phase.IDLE)ordinary++;else maneuver++;
            velocity=controller.step(velocity,plan.intent());position=position.add(velocity);
            check(original.equals(anchor.region()),"tactics never reseat region");
            check(original.contains(position),"physical maneuver remains in region");
            check(velocity.length()<=FlightController.MAX_SPEED+1e-9,"single controller retains cap");
        }
        check(ordinary>maneuver*2&&maneuver>100,"swimming dominates while actual feints occur");
        cancellation(false);cancellation(true);
    }
    private static void cancellation(boolean committed){
        var anchor=new CombatAnchor();anchor.evaluate(TARGET,FlightVector.ZERO,START);var brain=new TacticalBrain();
        var normal=new MovementPlanner.Plan(MovementPrimitive.DRIFT,FlightController.Intent.move(new FlightVector(1,0,0),.1),START,CombatAnchor.Range.COMFORTABLE,true);
        for(int i=0;i<241+(committed?24:0);i++)brain.step(anchor,TARGET,FlightVector.ZERO,START,true,MobilityContext.Kind.OPEN_AIR,OPEN,.99,d->true,()->normal);
        check(brain.state().phase()!=ManeuverComposer.Phase.IDLE,"test starts a real recipe");
        brain.step(anchor,null,null,START,false,MobilityContext.Kind.OPEN_AIR,OPEN,.99,d->true,()->normal);
        check((brain.state().phase()!=ManeuverComposer.Phase.IDLE)==committed,"LOS loss cancels before commit, preserves locked movement afterward");
        brain.step(anchor,null,null,START,false,MobilityContext.Kind.GROUND_FORCED,OPEN,.99,d->true,()->normal);
        check(brain.state().phase()==ManeuverComposer.Phase.IDLE,"safety context overrides committed action");
    }
    private static void repeatAcrossRecovery(){
        var anchor=new CombatAnchor();anchor.evaluate(TARGET,FlightVector.ZERO,START);var brain=new TacticalBrain();
        var ordinary=new MovementPlanner.Plan(MovementPrimitive.DRIFT,FlightController.Intent.move(new FlightVector(1,0,0),.1),START,CombatAnchor.Range.COMFORTABLE,true);
        var actions=new java.util.ArrayList<TacticalEvaluator.Action>();
        var previous=ManeuverComposer.Phase.IDLE;
        for(int tick=0;tick<1000&&actions.size()<2;tick++){
            brain.step(anchor,TARGET,FlightVector.ZERO,START,true,MobilityContext.Kind.OPEN_AIR,OPEN,.99,d->true,()->ordinary);
            var state=brain.state();
            if(state.phase()==ManeuverComposer.Phase.TELEGRAPH&&previous==ManeuverComposer.Phase.IDLE)actions.add(state.action());
            previous=state.phase();
        }
        check(actions.size()==2,"integration reaches two completed-selection cycles");
        check(actions.get(0)!=actions.get(1),"repetition penalty survives recipe plus quiet recovery");
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
