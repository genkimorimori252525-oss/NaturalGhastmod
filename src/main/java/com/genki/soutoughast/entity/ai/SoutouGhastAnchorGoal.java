package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.CombatAnchor;
import com.genki.soutoughast.entity.ai.flight.CombatFacing;
import com.genki.soutoughast.entity.ai.flight.FlightController;
import net.minecraft.util.Mth;
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
            facing.reset(SoutouGhastInertialMoveControl.from(target.getLookAngle()));
            observedSubject = subject;
        } else {
            facing.update(SoutouGhastInertialMoveControl.from(target.getLookAngle()));
        }
        CombatAnchor.Evaluation evaluation = anchor.evaluate(SoutouGhastInertialMoveControl.from(target.position()),
                facing.direction(), SoutouGhastInertialMoveControl.from(ghast.position()));
        control().setIntent(evaluation.intent());
        Vec3 look = target.getEyePosition().subtract(ghast.getEyePosition());
        float yaw = (float)(-Mth.atan2(look.x, look.z) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
        ghast.setYRot(Mth.rotateIfNecessary(ghast.getYRot(), yaw, 4));
        ghast.setXRot(Mth.rotateIfNecessary(ghast.getXRot(), pitch, 3));
        ghast.yBodyRot = ghast.getYRot();
        ghast.yHeadRot = ghast.getYRot();
    }

    @Override
    public void stop() {
        observedSubject = null;
        facing.reset(com.genki.soutoughast.entity.ai.flight.FlightVector.ZERO);
        control().setIntent(FlightController.Intent.hold());
    }

    private SoutouGhastInertialMoveControl control() {
        return (SoutouGhastInertialMoveControl)ghast.getMoveControl();
    }
}
