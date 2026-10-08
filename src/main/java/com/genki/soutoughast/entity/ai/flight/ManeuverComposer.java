package com.genki.soutoughast.entity.ai.flight;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.BiPredicate;

/** Finite motion recipe. Locked geometry, timing and physical integration stay separate. */
public final class ManeuverComposer {
    public enum Phase { IDLE, TELEGRAPH, COMMIT, REVEAL, BRAKE, RETURN }
    public enum TimingSlot { BEFORE_TELEGRAPH, BEFORE_COMMIT, ON_COMMIT, AFTER_REVEAL, ON_RETURN, AT_ANCHOR }
    private TacticalEvaluator.Action action=TacticalEvaluator.Action.DRIFT;
    private FlightVector first,reveal,eased=FlightVector.ZERO;
    private int age;
    private boolean active;
    private ExtendedFeintRecipe extended;
    private TacticalBrain.State state=TacticalBrain.State.idle();
    public boolean active(){return active;}
    public boolean committed(){return extended!=null?extended.committed():active&&age>=24;}
    public TacticalBrain.State state(){return state;}
    public void cancel(){active=false;extended=null;action=TacticalEvaluator.Action.DRIFT;state=TacticalBrain.State.idle();}
    public boolean start(TacticalEvaluator.Action chosen,FlightVector target,FlightVector boss,
                         CombatAnchor.Region region,MobilityContext.Sample sample,double variation){
        return start(chosen,target,boss,region,sample,variation,null);
    }
    public boolean start(TacticalEvaluator.Action chosen,FlightVector target,FlightVector boss,
                         CombatAnchor.Region region,MobilityContext.Sample sample,double variation,
                         BiPredicate<FlightVector,FlightVector> route){
        if(active)return false;
        if(ExtendedFeintRecipe.handles(chosen)){
            extended=ExtendedFeintRecipe.prepare(chosen,target,boss,region,sample,variation,route);
            if(extended==null)return false;
            action=chosen;active=true;state=extended.state();return true;
        }
        FlightVector toward=target.subtract(boss);toward=new FlightVector(toward.x(),0,toward.z()).normalized();
        if(toward.length()<1e-9||chosen==TacticalEvaluator.Action.DRIFT)return false;
        FlightVector direction=switch(chosen){
            case FALSE_APPROACH -> toward;
            case FALSE_RETREAT -> toward.scale(-1);
            case LATERAL_FAKE -> new FlightVector(-toward.z(),0,toward.x()).scale(variation<.875?-1:1);
            case VERTICAL_FAKE -> new FlightVector(0,variation<.875?-1:1,0);
            default -> FlightVector.ZERO;
        };
        first=boss.add(direction.scale(4));reveal=boss.subtract(direction.scale(4));
        if(!region.contains(first)||!region.contains(reveal)||!sample.allows(direction)||!sample.allows(direction.scale(-1)))return false;
        extended=null;action=chosen;active=true;age=0;eased=FlightVector.ZERO;state=new TacticalBrain.State(action,Phase.TELEGRAPH,TimingSlot.BEFORE_COMMIT,false);
        return true;
    }
    public MovementPlanner.Plan step(FlightVector boss,CombatAnchor.Range range,Predicate<FlightVector> clearance,
                                     Supplier<MovementPlanner.Plan> ordinary){
        if(!active)return ordinary.get();
        if(extended!=null){var plan=extended.step(boss,range,ordinary);state=extended.state();active=extended.active();return plan;}
        Phase phase=age<24?Phase.TELEGRAPH:age<32?Phase.COMMIT:age<56?Phase.REVEAL:age<68?Phase.BRAKE:Phase.RETURN;
        state=new TacticalBrain.State(action,phase,switch(phase){case TELEGRAPH->TimingSlot.BEFORE_COMMIT;case COMMIT->TimingSlot.ON_COMMIT;case RETURN->TimingSlot.ON_RETURN;default->TimingSlot.AFTER_REVEAL;},age>=24);
        MovementPlanner.Plan plan;
        if(phase==Phase.RETURN){plan=ordinary.get();}
        else if(phase==Phase.BRAKE){plan=new MovementPlanner.Plan(MovementPrimitive.BRAKE,FlightController.Intent.brake(),boss,range,true);}
        else{
            FlightVector waypoint=phase==Phase.REVEAL?reveal:first,error=waypoint.subtract(boss);
            if(!clearance.test(error)){cancel();return new MovementPlanner.Plan(MovementPrimitive.BRAKE,FlightController.Intent.brake(),boss,range,true);}
            double speed=Math.min(phase==Phase.REVEAL?.32:.14,Math.sqrt(2*FlightController.BRAKING*Math.max(0,error.length()-.4)));
            FlightVector desired=error.normalized().scale(speed);eased=eased.add(desired.subtract(eased).limited(.008));
            MovementPrimitive primitive=switch(action){
                case FALSE_APPROACH -> phase==Phase.REVEAL?MovementPrimitive.WITHDRAW:MovementPrimitive.APPROACH;
                case FALSE_RETREAT -> phase==Phase.REVEAL?MovementPrimitive.APPROACH:MovementPrimitive.WITHDRAW;
                case VERTICAL_FAKE -> waypoint.y()>boss.y()?MovementPrimitive.RISE:MovementPrimitive.DROP;
                default -> phase==Phase.REVEAL?MovementPrimitive.CURVE:MovementPrimitive.STRAFE;
            };
            plan=new MovementPlanner.Plan(primitive,FlightController.Intent.move(eased,eased.length()),waypoint,range,true);
        }
        if(++age>=84)active=false;
        return plan;
    }
}
