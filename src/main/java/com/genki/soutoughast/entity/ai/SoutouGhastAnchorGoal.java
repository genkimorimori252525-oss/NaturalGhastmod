package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.CombatFacing;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import com.genki.soutoughast.entity.ai.flight.MovementPlanner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/** Observable target -> smoothed frontal region -> intent. No attacks or hidden pursuit. */
public final class SoutouGhastAnchorGoal extends Goal {
    private final SoutouGhast ghast;
    private final CombatFacing facing = new CombatFacing();
    private final CombatAnchor anchor = new CombatAnchor();
    private UUID observedSubject;
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
        if (target == null || !target.isAlive() || !ghast.getSensing().hasLineOfSight(target)) {
            stop();
            return;
        }
        UUID subject = target.getUUID();
        if (!subject.equals(observedSubject)) {
            planner.reset();
            control().resetMobility();
            facing.reset(SoutouGhastInertialMoveControl.from(target.getLookAngle()));
            observedSubject = subject;
        } else {
            facing.update(SoutouGhastInertialMoveControl.from(target.getLookAngle()));
        }
        var sample = control().sampleMobility();
        var plan = planner.step(anchor, SoutouGhastInertialMoveControl.from(target.position()),
                SoutouGhastInertialMoveControl.from(ghast.position()), facing.direction(),
                control().getMobilityContext(), sample, ghast.getRandom().nextDouble(),control()::hasDirectionalClearance);
        control().setMovementPlan(plan);
        Vec3 look = target.getEyePosition().subtract(ghast.getEyePosition());
        ((SoutouGhastFlightLookControl)ghast.getLookControl()).setIntent(look);
    }

    @Override
    public void stop() {
        observedSubject = null;
        planner.reset();
        control().resetMobility();
        facing.reset(com.genki.soutoughast.entity.ai.flight.FlightVector.ZERO);
        control().setIntent(FlightController.Intent.hold());
        ((SoutouGhastFlightLookControl)ghast.getLookControl()).clearIntent();
    }

    private SoutouGhastInertialMoveControl control() {
        return (SoutouGhastInertialMoveControl)ghast.getMoveControl();
    }
}
