package com.genki.soutoughast.entity.ai.flight;

import java.util.ArrayList;
import java.util.List;

/** Actual core/controller integration; native movement and human readability remain separate. */
public final class CompleteFeintsTest {
    private static int checks;
    private static final FlightVector START=new FlightVector(0,6,28),TARGET=new FlightVector(0,6,18);
    private static final CombatAnchor.Region REGION=new CombatAnchor.Region(START,CombatAnchor.RADII,1,CombatAnchor.Reason.ACQUIRED);
    private static final MobilityContext.Sample OPEN=new MobilityContext.Sample(1023);
    public static void main(String[] args){
        for(String name:new String[]{"PASS_BY_FAKE","DOUBLE_FAKE","ABORT_FAKE"}){
            if(java.util.Arrays.stream(TacticalEvaluator.Action.values()).noneMatch(x->x.name().equals(name)))
                throw new AssertionError("MISSING_PLANNED_FEINT_"+name);
        }
        geometry();simulation(TacticalEvaluator.Action.PASS_BY_FAKE);simulation(TacticalEvaluator.Action.DOUBLE_FAKE);
        simulation(TacticalEvaluator.Action.ABORT_FAKE);selection();timeout();cancellation();reuse();
        System.out.println("PASS: "+checks+" complete feint/controller assertions");
    }
    private static ManeuverComposer composer(TacticalEvaluator.Action action){
        var c=new ManeuverComposer();check(c.start(action,TARGET,START,REGION,OPEN,.99,(from,to)->true),"actual admitted recipe "+action);return c;
    }
    private static MovementPlanner.Plan ordinary(FlightVector boss){
        return new MovementPlanner.Plan(MovementPrimitive.DRIFT,FlightController.Intent.hold(),boss,CombatAnchor.Range.COMFORTABLE,true);
    }
    private static void geometry(){
        var c=new ManeuverComposer();check(!c.start(TacticalEvaluator.Action.PASS_BY_FAKE,TARGET,START,REGION,OPEN,.9),"coarse sample cannot substitute for full body preflight");
        check(!c.start(TacticalEvaluator.Action.PASS_BY_FAKE,new FlightVector(0,6,0),START,REGION,OPEN,.9,(a,b)->true),"cannot rename infeasible out-of-region pass as a sidestep");
        check(!c.start(TacticalEvaluator.Action.PASS_BY_FAKE,new FlightVector(0,6,27),START,REGION,OPEN,.9,(a,b)->true),"overlapping target geometry is not a pass-by");
        var calls=new int[]{0};check(!c.start(TacticalEvaluator.Action.DOUBLE_FAKE,TARGET,START,REGION,OPEN,.9,(a,b)->++calls[0]<2)&&calls[0]==2,"reject blocked second segment before start");
        check(!c.start(TacticalEvaluator.Action.DOUBLE_FAKE,TARGET,START,REGION,new MobilityContext.Sample(0),.9,(a,b)->true),"coarse forbidden directions remain authoritative");
        var started=composer(TacticalEvaluator.Action.PASS_BY_FAKE);check(!started.start(TacticalEvaluator.Action.ABORT_FAKE,TARGET,START,REGION,OPEN,.9,(a,b)->true),"cannot replace an active committed sequence");
        var changing=new boolean[]{true};var dynamic=new ManeuverComposer();dynamic.start(TacticalEvaluator.Action.DOUBLE_FAKE,TARGET,START,REGION,OPEN,.9,(a,b)->changing[0]);changing[0]=false;
        check(dynamic.step(START,CombatAnchor.Range.COMFORTABLE,d->true,()->ordinary(START)).intent().mode()==FlightController.Mode.BRAKE&&!dynamic.active(),"dynamic obstruction cancels and brakes without silently choosing another route");
    }
    private static void simulation(TacticalEvaluator.Action action){
        var c=composer(action);var controller=new FlightController();FlightVector position=START,velocity=FlightVector.ZERO;
        var phases=new ArrayList<ManeuverComposer.Phase>();var revealPoints=new ArrayList<FlightVector>();FlightVector first=null;
        double minX=0,maxX=0,minZ=START.z();int ticks=0;
        while(c.active()&&ticks<200){
            final var boss=position;var plan=c.step(boss,CombatAnchor.Range.COMFORTABLE,d->true,()->ordinary(boss));var state=c.state();
            boolean newPhase=phases.isEmpty()||phases.get(phases.size()-1)!=state.phase();if(newPhase)phases.add(state.phase());
            if(state.phase()==ManeuverComposer.Phase.TELEGRAPH){if(first==null)first=plan.waypoint();check(plan.waypoint().equals(first),"tell remains locked");check(!state.committed(),"24tick tell is pre-commit");}
            if(state.phase()==ManeuverComposer.Phase.REVEAL&&newPhase)revealPoints.add(plan.waypoint());
            if(action==TacticalEvaluator.Action.ABORT_FAKE)check(!state.committed()&&!c.committed(),"abort never reports commitment");
            var next=controller.step(velocity,plan.intent());check(next.subtract(velocity).length()<=Math.hypot(FlightController.ACCELERATION,FlightController.LATERAL_ACCELERATION)+1e-9,"same controller limits acceleration");
            velocity=next;position=position.add(velocity);minX=Math.min(minX,position.x());maxX=Math.max(maxX,position.x());minZ=Math.min(minZ,position.z());
            check(REGION.contains(position),"physical sequence retains broad region");check(velocity.length()<=FlightController.MAX_SPEED+1e-9,"sole-controller speed cap");ticks++;
        }
        check(!c.active()&&ticks<=160,"whole recipe is bounded");check(phases.get(phases.size()-1)==ManeuverComposer.Phase.RETURN,"actual recovery stage");
        if(action==TacticalEvaluator.Action.PASS_BY_FAKE){
            check(first.subtract(TARGET).dot(TARGET.subtract(START))>0,"locked pass waypoint lies beyond target plane");
            check(minZ<TARGET.z()-2&&revealPoints.size()==1&&position.x()>8,"actual motion crosses target plane then bends");
            check(phases.equals(List.of(ManeuverComposer.Phase.TELEGRAPH,ManeuverComposer.Phase.COMMIT,ManeuverComposer.Phase.BRAKE,ManeuverComposer.Phase.REVEAL,ManeuverComposer.Phase.BRAKE,ManeuverComposer.Phase.RETURN)),"pass sequence has distinct brake and reveal");
        }else if(action==TacticalEvaluator.Action.DOUBLE_FAKE){
            check(revealPoints.size()==2&&!revealPoints.get(0).equals(revealPoints.get(1))&&minX<-3&&maxX>3,"double has two physical separately-braked reversals");
            check(phases.stream().filter(x->x==ManeuverComposer.Phase.BRAKE).count()==3,"double includes three distinct brakes");
        }else check(!phases.contains(ManeuverComposer.Phase.COMMIT)&&!phases.contains(ManeuverComposer.Phase.REVEAL)&&ticks==52,"abort returns before any betrayal commitment");
    }
    private static void selection(){
        var evaluator=new TacticalEvaluator();var memory=new TacticalMemory();int drift=0,doubles=0,aborts=0,passes=0;
        for(int i=0;i<10000;i++){
            double variation=i/10000.0;var action=evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.COMFORTABLE,FlightVector.ZERO,variation,memory);
            if(action==TacticalEvaluator.Action.DRIFT)drift++;if(action==TacticalEvaluator.Action.DOUBLE_FAKE)doubles++;if(action==TacticalEvaluator.Action.ABORT_FAKE)aborts++;
            if(evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.TOO_CLOSE,FlightVector.ZERO,variation,memory)==TacticalEvaluator.Action.PASS_BY_FAKE)passes++;
            check(evaluator.choose(MobilityContext.Kind.CONFINED,CombatAnchor.Range.COMFORTABLE,FlightVector.ZERO,variation,memory)!=TacticalEvaluator.Action.DOUBLE_FAKE,"confinement suppresses broad double sequence");
        }
        check(drift==7500&&doubles>0&&doubles<=150&&aborts>0&&passes>0,"ordinary weight unchanged and extra recipes are rare/selectable");
        memory.record(TacticalEvaluator.Action.DOUBLE_FAKE);
        check(evaluator.choose(MobilityContext.Kind.OPEN_AIR,CombatAnchor.Range.COMFORTABLE,FlightVector.ZERO,.945,memory)!=TacticalEvaluator.Action.DOUBLE_FAKE,"600tick repetition penalty covers extra recipes");
    }
    private static void timeout(){
        var c=composer(TacticalEvaluator.Action.PASS_BY_FAKE);int ticks=0;boolean returning=false;
        while(c.active()&&ticks<200){c.step(START,CombatAnchor.Range.COMFORTABLE,d->true,()->ordinary(START));returning|=c.state().phase()==ManeuverComposer.Phase.RETURN;ticks++;}
        check(!c.active()&&ticks<=160&&returning,"native nonarrival cannot wait indefinitely; timeout brakes and returns");
    }
    private static void cancellation(){
        for(boolean committed:new boolean[]{false,true}){
            var anchor=new CombatAnchor();anchor.evaluate(FlightVector.ZERO,FlightVector.ZERO,START);var original=anchor.region();var brain=new TacticalBrain();
            for(int i=0;i<241+(committed?24:0);i++)brain.step(anchor,TARGET,FlightVector.ZERO,START,true,MobilityContext.Kind.OPEN_AIR,OPEN,.895,d->true,(a,b)->true,()->ordinary(START));
            check(brain.state().action()==TacticalEvaluator.Action.PASS_BY_FAKE,"real brain selects extended recipe");
            brain.step(anchor,null,null,START,false,MobilityContext.Kind.OPEN_AIR,OPEN,.895,d->true,(a,b)->true,()->ordinary(START));
            check((brain.state().phase()!=ManeuverComposer.Phase.IDLE)==committed,"LOS loss cancels before commit, preserves frozen geometry after");
            check(original.equals(anchor.region()),"no camera/target-driven region transfer");
            brain.step(anchor,null,null,START,false,MobilityContext.Kind.GROUND_FORCED,OPEN,.895,d->true,(a,b)->true,()->ordinary(START));
            check(brain.state().phase()==ManeuverComposer.Phase.IDLE,"safety context overrides extra commitment");
        }
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void reuse(){
        var c=composer(TacticalEvaluator.Action.ABORT_FAKE);
        for(int i=0;i<52;i++)c.step(START,CombatAnchor.Range.COMFORTABLE,d->true,()->ordinary(START));
        check(c.start(TacticalEvaluator.Action.LATERAL_FAKE,TARGET,START,REGION,OPEN,.99),"completed extra recipe can restart original recipe");
        c.step(START,CombatAnchor.Range.COMFORTABLE,d->true,()->ordinary(START));
        check(c.active()&&c.state().action()==TacticalEvaluator.Action.LATERAL_FAKE&&c.state().phase()==ManeuverComposer.Phase.TELEGRAPH,"original recipe must not delegate a stale completed extension");
    }
}
