package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import com.genki.soutoughast.entity.ai.flight.TacticalBrain;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/** Observed encounter -> retained region -> motion composition and explicit major exceptions. */
public final class SoutouGhastAnchorGoal extends Goal {
    private final SoutouGhast ghast;
    private final CombatAnchor anchor = new CombatAnchor();
    private UUID observedSubject;
    private FlightVector lastObservedPosition;
    private final MovementPlanner planner = new MovementPlanner();
    private final TacticalBrain brain = new TacticalBrain();

    public SoutouGhastAnchorGoal(SoutouGhast ghast) {
        this.ghast = ghast;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() { return true; }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity target = ghast.getTarget();
        boolean present=target!=null&&target.isAlive();
        boolean visible=present&&ghast.getSensing().hasLineOfSight(target);
        FlightVector boss=SoutouGhastInertialMoveControl.from(ghast.position());
        if(ghast.getDomainAttack().active()){applyDomain(target,visible,control().sampleMobility());return;}
        if(ghast.getGroundCombat().active()){
            if(com.genki.soutoughast.entity.ai.domain.DomainRuntime.released())ghast.getMajorDirector().tick(visible);
            if(ghast.getDomainAttack().tryBegin(target,visible,anchor.region(),ghast.getGroundCombat().offensiveBusy())){applyDomain(target,visible,control().sampleMobility());return;}
            applyGround(target,visible,control().sampleMobility());return;
        }
        if(ghast.getOverheadAttack().active()){
            control().sampleMobility();applyMajor(target,visible);return;
        }
        if(visible){
            UUID subject=target.getUUID();
            if(!subject.equals(observedSubject)){planner.reset();brain.reset();control().resetMobility();observedSubject=subject;}
            lastObservedPosition=SoutouGhastInertialMoveControl.from(target.position());
            anchor.observe(subject,lastObservedPosition,boss,control().isClearanceBlocked()
                    ||control().getPrimitive()==com.genki.soutoughast.entity.ai.flight.MovementPrimitive.BRAKE);
        }else{
            anchor.unobserved(present);
            ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
        }
        control().setCombatRegion(anchor.region());
        if(anchor.region()==null||lastObservedPosition==null){
            planner.reset();brain.reset();ghast.getStandardAttack().reset();control().setTacticalState(brain.state());control().setIntent(FlightController.Intent.hold());return;
        }
        var sample = control().sampleMobility();
        if(com.genki.soutoughast.entity.ai.domain.DomainRuntime.released())ghast.getOverheadAttack().ordinaryTick(visible);
        boolean groundBusy=ghast.getStandardAttack().engaged()||brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT;
        if(ghast.getDomainAttack().tryBegin(target,visible,anchor.region(),groundBusy)){
            brain.reset();planner.reset();applyDomain(target,visible,sample);return;
        }
        if(ghast.getGroundCombat().tryBegin(sample,groundBusy)){
            brain.reset();planner.reset();ghast.getStandardAttack().reset();applyGround(target,visible,sample);return;
        }
        if(!com.genki.soutoughast.entity.ai.domain.DomainRuntime.released())ghast.getOverheadAttack().ordinaryTick(visible);
        boolean busy=ghast.getStandardAttack().engaged()||brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT;
        if(ghast.getOverheadAttack().tryBegin(target,visible,anchor.region(),busy)){
            brain.reset();planner.reset();ghast.getStandardAttack().reset();applyMajor(target,visible);return;
        }
        // No live geometry or velocity is read after LOS loss. Committed recipes use locked waypoints.
        FlightVector observedVelocity=visible?SoutouGhastInertialMoveControl.from(target.getDeltaMovement()):null;
        double variation=ghast.getStandardAttack().engaged()?0:ghast.getRandom().nextDouble();
        var plan = brain.step(anchor,visible?lastObservedPosition:null,observedVelocity,boss,visible,
                control().getMobilityContext(),sample,variation,control()::hasDirectionalClearance,
                ()->planner.step(anchor,lastObservedPosition,boss,FlightVector.ZERO,
                        control().getMobilityContext(),sample,variation,control()::hasDirectionalClearance));
        control().setTacticalState(brain.state());
        control().setMovementPlan(plan);
        if(visible){
            Vec3 look=target.getEyePosition().subtract(ghast.getEyePosition());
            ((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(look);
        }
        Vec3 attackLook=ghast.getStandardAttack().tick(target,visible,brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT);
        if(attackLook!=null)((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(attackLook);
    }

    private void applyDomain(LivingEntity target,boolean visible,com.genki.soutoughast.entity.ai.flight.MobilityContext.Sample sample){
        var state=ghast.getDomainAttack().tick(target,visible,anchor.region(),sample);
        control().setCombatRegion(anchor.region());control().setTacticalState(TacticalBrain.State.idle());
        control().setMajorIntent(state.intent(),state.intent().mode()==FlightController.Mode.MOVE?
                com.genki.soutoughast.entity.ai.flight.MovementPrimitive.APPROACH:com.genki.soutoughast.entity.ai.flight.MovementPrimitive.BRAKE);
        if(target!=null&&visible)((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(target.getEyePosition().subtract(ghast.getEyePosition()));
        else ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
    }

    private void applyGround(LivingEntity target,boolean visible,com.genki.soutoughast.entity.ai.flight.MobilityContext.Sample sample){
        var state=ghast.getGroundCombat().tick(target,visible,anchor.region(),sample);
        control().setCombatRegion(anchor.region());control().setTacticalState(TacticalBrain.State.idle());
        control().setMajorIntent(state.intent(),state.intent().mode()==FlightController.Mode.MOVE?
                com.genki.soutoughast.entity.ai.flight.MovementPrimitive.APPROACH:com.genki.soutoughast.entity.ai.flight.MovementPrimitive.BRAKE);
    }

    private void applyMajor(LivingEntity target,boolean visible){
        var state=ghast.getOverheadAttack().tick(target,visible);
        control().setCombatRegion(anchor.region());control().setTacticalState(TacticalBrain.State.idle());
        var primitive=switch(state.phase()){
            case WITHDRAW -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.WITHDRAW;
            case RETURN -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.APPROACH;
            case RECOVER -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.RETURN;
            default -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.HOLD;
        };
        control().setMajorIntent(state.intent(),primitive);
        var look=ghast.getOverheadAttack().look(target,visible);
        if(look==null)((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
        else ((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(look);
    }

    @Override
    public void stop() {
        observedSubject = null;
        lastObservedPosition=null;
        anchor.clear();control().setCombatRegion(null);
        planner.reset();
        brain.reset();control().setTacticalState(brain.state());
        ghast.getStandardAttack().reset();
        ghast.getOverheadAttack().reset();
        ghast.getDomainAttack().abortForStop();
        if(!ghast.getDomainAttack().active()){ghast.getGroundCombat().reset();ghast.getMajorDirector().reset();}
        control().resetMobility();
        control().setIntent(FlightController.Intent.hold());
        ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
    }

    private SoutouGhastInertialMoveControl control() {
        return (SoutouGhastInertialMoveControl)ghast.getMoveControl();
    }
}
