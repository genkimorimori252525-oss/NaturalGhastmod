package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import com.genki.soutoughast.entity.ai.flight.TacticalBrain;
import com.genki.soutoughast.entity.ai.flight.OverheadReanchor;
import com.genki.soutoughast.entity.ai.flight.MobilityContext;
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
    private final OverheadReanchor relocation = new OverheadReanchor();
    private final SoutouGhastProjectileDodge dodge;
    private UUID relocationSubject;
    private long relocationGeneration;

    public SoutouGhastAnchorGoal(SoutouGhast ghast) {
        this.ghast = ghast;
        this.dodge = new SoutouGhastProjectileDodge(ghast);
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
        if(relocation.active()){control().sampleMobility();applyRelocation(target,visible);return;}
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
        boolean groundBusy=dodge.active()||ghast.getStandardAttack().engaged()||brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT;
        if(ghast.getDomainAttack().tryBegin(target,visible,anchor.region(),groundBusy)){
            brain.reset();planner.reset();applyDomain(target,visible,sample);return;
        }
        if(ghast.getGroundCombat().tryBegin(sample,groundBusy)){
            brain.reset();planner.reset();ghast.getStandardAttack().reset();applyGround(target,visible,sample);return;
        }
        if(!com.genki.soutoughast.entity.ai.domain.DomainRuntime.released())ghast.getOverheadAttack().ordinaryTick(visible);
        boolean busy=dodge.active()||ghast.getStandardAttack().engaged()||brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT;
        if(ghast.getOverheadAttack().tryBegin(target,visible,anchor.region(),busy)){
            brain.reset();planner.reset();ghast.getStandardAttack().reset();applyMajor(target,visible);return;
        }
        if(tryRelocation(target,visible,busy,boss)){applyRelocation(target,visible);return;}
        var dodgePlan=dodge.tick(target,visible,anchor.region(),brain.state().action()!=com.genki.soutoughast.entity.ai.flight.TacticalEvaluator.Action.DRIFT);
        if(dodgePlan!=null){
            brain.reset();planner.reset();control().setTacticalState(TacticalBrain.State.idle());control().setMovementPlan(dodgePlan);
            if(visible)((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(target.getEyePosition().subtract(ghast.getEyePosition()));
            ghast.getStandardAttack().tick(target,visible,true);return;
        }
        // No live geometry or velocity is read after LOS loss. Committed recipes use locked waypoints.
        FlightVector observedVelocity=visible?SoutouGhastInertialMoveControl.from(target.getDeltaMovement()):null;
        double variation=ghast.getStandardAttack().engaged()?0:ghast.getRandom().nextDouble();
        var plan = brain.step(anchor,visible?lastObservedPosition:null,observedVelocity,boss,visible,
                control().getMobilityContext(),sample,variation,control()::hasDirectionalClearance,control()::hasManeuverClearance,
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

    private boolean tryRelocation(LivingEntity target,boolean visible,boolean busy,FlightVector boss){
        var director=ghast.getMajorDirector();
        // Do not consume extra variation during ordinary ineligible swimming/attack periods.
        if(!visible||target==null||busy||anchor.region()==null||!anchor.region().contains(boss)
                ||control().isClearanceBlocked()||control().getMobilityContext()!=MobilityContext.Kind.OPEN_AIR
                ||director.active()||director.quietTicks()>0||director.recentTicks()>0)return false;
        var delta=lastObservedPosition.subtract(boss);double distance=Math.hypot(delta.x(),delta.z());
        if(distance<8||distance>16)return false;
        if(!director.shouldBeginRelocation(control().getMobilityContext(),true,busy,distance,ghast.getRandom().nextDouble()))return false;
        if(!relocation.begin(target.getUUID(),lastObservedPosition,target.getBoundingBox().maxY,boss,anchor.region(),this::relocationRoute)){
            director.rejected();return false;
        }
        relocationSubject=target.getUUID();relocationGeneration=anchor.region().generation();director.began();
        brain.reset();planner.reset();ghast.getStandardAttack().reset();return true;
    }

    private boolean relocationRoute(FlightVector from,FlightVector to){
        double height=ghast.getBbHeight();
        return Math.min(from.y(),to.y())>=ghast.level().getMinBuildHeight()
                &&Math.max(from.y(),to.y())+height<ghast.level().getMaxBuildHeight()
                &&control().hasManeuverClearance(from,to);
    }

    private void applyRelocation(LivingEntity target,boolean visible){
        ghast.getMajorDirector().tick(false);
        if(control().getMobilityContext()!=MobilityContext.Kind.OPEN_AIR)relocation.abort();
        var boss=SoutouGhastInertialMoveControl.from(ghast.position());
        UUID identity=target!=null&&target.isAlive()?target.getUUID():null;
        var observedLook=visible&&identity!=null&&identity.equals(relocationSubject)?SoutouGhastInertialMoveControl.from(target.getEyePosition().subtract(ghast.getEyePosition())):null;
        var state=relocation.step(identity,visible,anchor.region(),boss,SoutouGhastInertialMoveControl.from(ghast.getDeltaMovement()),ghast.getYRot(),ghast.getXRot(),observedLook,this::relocationRoute);
        if(state.commit())anchor.commitRelocation(relocationSubject,relocationGeneration,relocation.candidateCenter());
        control().setCombatRegion(anchor.region());control().setTacticalState(TacticalBrain.State.idle());
        control().setMajorIntent(state.intent(),switch(state.phase()){
            case TELL,CLIMB -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.RISE;
            case CROSS -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.OVERSHOOT;
            case DESCEND,HANDOFF -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.DROP;
            default -> com.genki.soutoughast.entity.ai.flight.MovementPrimitive.BRAKE;
        });
        if(state.look()!=null)((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(SoutouGhastInertialMoveControl.to(state.look()));
        else ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
        if(!relocation.active()){ghast.getMajorDirector().finished();relocationSubject=null;planner.reset();brain.reset();}
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
        relocation.reset();relocationSubject=null;
        dodge.reset();
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
