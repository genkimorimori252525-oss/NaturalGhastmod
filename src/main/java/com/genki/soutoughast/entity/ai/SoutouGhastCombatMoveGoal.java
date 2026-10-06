package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class SoutouGhastCombatMoveGoal extends Goal {
    private final SoutouGhast ghast;
    private int repositionCooldown;
    private Vec3 cachedDestination;

    public SoutouGhastCombatMoveGoal(SoutouGhast ghast) {
        this.ghast = ghast;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.ghast.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.ghast.getTarget() != null;
    }

    @Override
    public void start() {
        this.repositionCooldown = 0;
        this.cachedDestination = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.ghast.getTarget();
        if (target == null) {
            return;
        }

        if (this.repositionCooldown-- > 0 && this.cachedDestination != null) {
            this.moveTo(this.cachedDestination);
            return;
        }

        Vec3 desired = this.computeDesiredPosition(target);
        desired = this.findBestVisibleOffset(desired, target);
        this.cachedDestination = desired;
        this.repositionCooldown = this.ghast.isEnraged() ? 3 : 6;
        this.moveTo(desired);
    }

    private void moveTo(Vec3 pos) {
        this.ghast.getMoveControl().setWantedPosition(pos.x, pos.y, pos.z, this.ghast.getMoveSpeedForPhase());
    }

    private Vec3 computeDesiredPosition(LivingEntity target) {
        Vec3 targetPos = target.position();
        double distance = Math.sqrt(this.ghast.distanceToSqr(target));
        double radius = this.ghast.getOrbitRadius();
        double verticalOffset = this.ghast.getPreferredAltitudeOffset();
        double time = (this.ghast.tickCount * 0.085D) + this.ghast.getOrbitAngleOffset();
        double angle = time * this.ghast.getOrbitDirection();

        Vec3 toGhast = this.ghast.position().subtract(targetPos);
        Vec3 radial = toGhast.horizontalDistanceSqr() < 1.0E-4D
                ? new Vec3(1.0D, 0.0D, 0.0D)
                : new Vec3(toGhast.x, 0.0D, toGhast.z).normalize();
        Vec3 tangential = new Vec3(-radial.z, 0.0D, radial.x).scale(this.ghast.getOrbitDirection());

        Vec3 orbitOffset = radial.scale(Math.cos(angle) * radius * 0.55D)
                .add(tangential.scale(Math.sin(angle) * radius));

        if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.CHASING) {
            orbitOffset = this.computeFlankOffset(target, radius, verticalOffset);
        } else if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.RETREATING) {
            orbitOffset = radial.scale(radius + 8.0D).add(tangential.scale(radius * 0.3D));
            verticalOffset += 2.0D;
        } else if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.ENRAGED) {
            orbitOffset = orbitOffset.add(tangential.scale(2.0D));
        }

        if (!this.ghast.hasLineOfSight(target) && this.ghast.hasFreshLastSeenPos() && this.ghast.getLastSeenPos() != null) {
            Vec3 seen = this.ghast.getLastSeenPos();
            return seen.add(orbitOffset.scale(0.35D)).add(0.0D, verticalOffset * 0.5D, 0.0D);
        }

        if (distance > this.ghast.getPreferredCombatDistance() + 10.0D) {
            orbitOffset = orbitOffset.scale(0.55D);
        }

        if (distance < this.ghast.getRetreatDistance()) {
            orbitOffset = radial.scale(radius + 10.0D).add(0.0D, 2.0D, 0.0D);
        }

        Vec3 desired = targetPos.add(orbitOffset).add(0.0D, verticalOffset, 0.0D);
        if (this.ghast.shouldTryFeint()) {
            desired = applyFeint(desired, tangential, radial);
            this.ghast.markFeintUsed();
        }
        return desired;
    }

    private Vec3 computeFlankOffset(LivingEntity target, double radius, double verticalOffset) {
        Vec3 look = target.getLookAngle();
        Vec3 behind = look.horizontalDistanceSqr() < 1.0E-4D
                ? new Vec3(0.0D, 0.0D, -1.0D)
                : new Vec3(-look.x, 0.0D, -look.z).normalize();
        Vec3 side = new Vec3(-behind.z, 0.0D, behind.x).scale(this.ghast.getOrbitDirection());
        return behind.scale(radius * 0.8D).add(side.scale(radius * 0.45D)).add(0.0D, verticalOffset * 0.15D, 0.0D);
    }

    private Vec3 applyFeint(Vec3 desired, Vec3 tangential, Vec3 radial) {
        double sideAmount = this.ghast.isEnraged() ? 8.0D : 5.0D;
        double rise = this.ghast.isEnraged() ? 3.5D : 1.5D;
        return desired.add(tangential.normalize().scale(sideAmount)).add(radial.normalize().scale(-2.0D)).add(0.0D, rise, 0.0D);
    }

    private Vec3 findBestVisibleOffset(Vec3 desired, LivingEntity target) {
        Vec3 source = this.ghast.position();
        if (this.isLineMostlyClear(source, desired)) {
            return desired;
        }

        double radius = this.ghast.getOrbitRadius();
        for (int i = 0; i < 6; ++i) {
            double angle = this.ghast.getOrbitAngleOffset() + (Math.PI / 3.0D) * i * this.ghast.getOrbitDirection();
            Vec3 offset = new Vec3(Mth.cos((float) angle) * radius, this.ghast.getPreferredAltitudeOffset() + (i % 2 == 0 ? 2.0D : -1.5D), Mth.sin((float) angle) * radius);
            Vec3 candidate = target.position().add(offset);
            if (this.isLineMostlyClear(source, candidate)) {
                return candidate;
            }
        }

        if (this.ghast.hasFreshLastSeenPos() && this.ghast.getLastSeenPos() != null) {
            return this.ghast.getLastSeenPos().add(0.0D, this.ghast.getPreferredAltitudeOffset() * 0.35D, 0.0D);
        }
        return desired;
    }

    private boolean isLineMostlyClear(Vec3 from, Vec3 to) {
        HitResult hit = this.ghast.level().clip(new net.minecraft.world.level.ClipContext(
                from,
                to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                this.ghast));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) <= 4.0D;
    }
}
