package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.FlightVector;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/** Observed encounter -> retained region -> swimming intent. No attacks or hidden pursuit. */
public final class SoutouGhastAnchorGoal extends Goal {
    private final SoutouGhast ghast;
    private final CombatAnchor anchor = new CombatAnchor();
    private UUID observedSubject;
    private FlightVector lastObservedPosition;
    private final MovementPlanner planner = new MovementPlanner();

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
        if(visible){
            UUID subject=target.getUUID();
            if(!subject.equals(observedSubject)){planner.reset();control().resetMobility();observedSubject=subject;}
            lastObservedPosition=SoutouGhastInertialMoveControl.from(target.position());
            anchor.observe(subject,lastObservedPosition,boss,control().isClearanceBlocked()
                    ||control().getPrimitive()==com.genki.soutoughast.entity.ai.flight.MovementPrimitive.BRAKE);
        }else{
            anchor.unobserved(present);
            ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
        }
        control().setCombatRegion(anchor.region());
        if(anchor.region()==null||lastObservedPosition==null){
            planner.reset();control().setIntent(FlightController.Intent.hold());return;
        }
        var sample = control().sampleMobility();
        var plan = planner.step(anchor,lastObservedPosition,boss,FlightVector.ZERO,
                control().getMobilityContext(), sample, ghast.getRandom().nextDouble(),control()::hasDirectionalClearance);
        control().setMovementPlan(plan);
        if(visible){
            Vec3 look=target.getEyePosition().subtract(ghast.getEyePosition());
            ((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(look);
        }
    }

    @Override
    public void stop() {
        observedSubject = null;
        lastObservedPosition=null;
        anchor.clear();control().setCombatRegion(null);
        planner.reset();
        control().resetMobility();
        control().setIntent(FlightController.Intent.hold());
        ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
    }

    private SoutouGhastInertialMoveControl control() {
        return (SoutouGhastInertialMoveControl)ghast.getMoveControl();
    }
}
