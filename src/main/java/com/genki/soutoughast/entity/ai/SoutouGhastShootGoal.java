package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.projectile.SoutouGhastFireball;
import com.genki.soutoughast.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Stage-based bombardment logic with predictive aiming and projectile variants.
 */
public class SoutouGhastShootGoal extends Goal {
    private final SoutouGhast ghast;
    private int chargeTime;
    private int burstShotsRemaining;

    public SoutouGhastShootGoal(SoutouGhast ghast) {
        this.ghast = ghast;
    }

    @Override
    public boolean canUse() {
        return this.ghast.getTarget() != null;
    }

    @Override
    public void start() {
        this.chargeTime = 0;
        this.burstShotsRemaining = 0;
    }

    @Override
    public void stop() {
        this.ghast.setCharging(false);
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

        Level level = this.ghast.level();
        double distanceSqr = this.ghast.distanceToSqr(target);
        boolean hasSight = this.ghast.hasLineOfSight(target);
        double maxRange = this.ghast.getBombardDistance() + 18.0D;

        if (distanceSqr > maxRange * maxRange) {
            this.chargeTime = Math.max(0, this.chargeTime - 2);
            this.ghast.setCharging(false);
            return;
        }

        if (!hasSight) {
            this.chargeTime = Math.max(0, this.chargeTime - 1);
            this.ghast.setCharging(this.chargeTime > 10);
            return;
        }

        if (!this.shouldFireNow(Math.sqrt(distanceSqr))) {
            this.chargeTime = Math.max(0, this.chargeTime - 1);
            this.ghast.setCharging(this.chargeTime > 10);
            return;
        }

        ++this.chargeTime;

        if (this.chargeTime == 10 && !this.ghast.isSilent()) {
            level.playSound(null, this.ghast.getX(), this.ghast.getY(), this.ghast.getZ(),
                    ModSounds.SOUTOU_GHAST_ATTACK.get(), SoundSource.HOSTILE,
                    10.0F, (this.ghast.getRandom().nextFloat() - this.ghast.getRandom().nextFloat()) * 0.2F + 1.0F);
        }

        int releaseTime = this.getReleaseTime();
        if (this.chargeTime >= releaseTime) {
            this.fireProjectile(target);
            this.chargeTime = this.getPostFireCooldown();
        }

        this.ghast.setCharging(this.chargeTime > 10);
    }

    private boolean shouldFireNow(double distance) {
        return switch (this.ghast.getCombatPhase()) {
            case SCOUTING -> false;
            case CHASING -> distance <= this.ghast.getBombardDistance() + 4.0D;
            case BOMBARDMENT, ENRAGED, RETREATING -> true;
        };
    }

    private int getReleaseTime() {
        return switch (this.ghast.getCombatPhase()) {
            case ENRAGED -> 14;
            case RETREATING -> 16;
            case BOMBARDMENT -> 18;
            case CHASING -> 20;
            case SCOUTING -> 24;
        };
    }

    private int getPostFireCooldown() {
        if (this.burstShotsRemaining > 0) {
            --this.burstShotsRemaining;
            return -8;
        }
        return switch (this.ghast.getCombatPhase()) {
            case ENRAGED -> {
                if (this.ghast.getRandom().nextInt(3) == 0) {
                    this.burstShotsRemaining = 1 + this.ghast.getRandom().nextInt(2);
                    yield -10;
                }
                yield -22;
            }
            case RETREATING -> -18;
            case BOMBARDMENT -> -26;
            case CHASING -> -30;
            case SCOUTING -> -40;
        };
    }

    private void fireProjectile(LivingEntity target) {
        Vec3 viewVec = this.ghast.getViewVector(1.0F);
        Vec3 spawnPos = new Vec3(
                this.ghast.getX() + viewVec.x * 4.0D,
                this.ghast.getY(0.5D) + 0.5D,
                this.ghast.getZ() + viewVec.z * 4.0D
        );

        SoutouGhastFireball.Variant variant = this.selectVariant(target);
        double speedHint = this.getProjectileSpeedHint(variant);
        Vec3 aimPoint = this.predictAimPoint(target, spawnPos, speedHint, variant);
        Vec3 delta = aimPoint.subtract(spawnPos);
        if (delta.lengthSqr() < 1.0E-6D) {
            delta = target.position().subtract(spawnPos);
        }

        if (!this.ghast.isSilent()) {
            this.ghast.level().playSound(null, this.ghast.getX(), this.ghast.getY(), this.ghast.getZ(),
                    ModSounds.SOUTOU_GHAST_SHOOT.get(), SoundSource.HOSTILE,
                    10.0F, (this.ghast.getRandom().nextFloat() - this.ghast.getRandom().nextFloat()) * 0.2F + 1.0F);
        }

        SoutouGhastFireball fireball = new SoutouGhastFireball(
                this.ghast.level(),
                this.ghast,
                delta.x,
                delta.y,
                delta.z,
                this.getExplosionPower(variant)
        ).configure(variant, target.position());
        fireball.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.ghast.level().addFreshEntity(fireball);
    }

    private SoutouGhastFireball.Variant selectVariant(LivingEntity target) {
        double distance = Math.sqrt(this.ghast.distanceToSqr(target));
        if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.RETREATING) {
            return SoutouGhastFireball.Variant.FAST_SMALL;
        }
        if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.CHASING) {
            return this.ghast.getRandom().nextBoolean()
                    ? SoutouGhastFireball.Variant.FAST_SMALL
                    : SoutouGhastFireball.Variant.WALL_BURST;
        }
        if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.ENRAGED) {
            int roll = this.ghast.getRandom().nextInt(100);
            if (roll < 35) {
                return SoutouGhastFireball.Variant.FAST_SMALL;
            }
            if (roll < 60) {
                return SoutouGhastFireball.Variant.SPLITTER;
            }
            return SoutouGhastFireball.Variant.WALL_BURST;
        }
        if (target.onGround() && distance > 16.0D && this.ghast.getRandom().nextInt(4) == 0) {
            return SoutouGhastFireball.Variant.GROUND_FIRE;
        }
        if (distance > 24.0D && this.ghast.getRandom().nextInt(3) == 0) {
            return SoutouGhastFireball.Variant.SPLITTER;
        }
        return this.ghast.getRandom().nextInt(4) == 0
                ? SoutouGhastFireball.Variant.HEAVY_SLOW
                : SoutouGhastFireball.Variant.FAST_SMALL;
    }

    private int getExplosionPower(SoutouGhastFireball.Variant variant) {
        int base = this.ghast.getExplosionPower();
        if (this.ghast.isEnraged()) {
            base += 1;
        }
        return switch (variant) {
            case FAST_SMALL -> Math.max(1, base);
            case HEAVY_SLOW -> Math.max(2, base + 1);
            case GROUND_FIRE -> Math.max(1, base);
            case SPLITTER -> Math.max(1, base);
            case WALL_BURST -> Math.max(1, base);
        };
    }

    private double getProjectileSpeedHint(SoutouGhastFireball.Variant variant) {
        return switch (variant) {
            case FAST_SMALL -> 1.35D;
            case HEAVY_SLOW -> 0.65D;
            case GROUND_FIRE -> 0.8D;
            case SPLITTER -> 0.95D;
            case WALL_BURST -> 0.9D;
        };
    }

    private Vec3 predictAimPoint(LivingEntity target, Vec3 spawnPos, double projectileSpeed, SoutouGhastFireball.Variant variant) {
        Vec3 targetPos = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        if (variant == SoutouGhastFireball.Variant.GROUND_FIRE) {
            targetPos = target.position().add(0.0D, 0.15D, 0.0D);
        }

        Vec3 targetMotion = target.getDeltaMovement();
        double distance = spawnPos.distanceTo(targetPos);
        double travelTime = distance / Math.max(0.2D, projectileSpeed);
        travelTime = Mth.clamp(travelTime, 4.0D, 28.0D);

        if (this.ghast.getCombatPhase() == SoutouGhast.CombatPhase.ENRAGED) {
            travelTime *= 1.1D;
        }
        if (this.ghast.getTemperament() == SoutouGhast.Temperament.TENACIOUS) {
            travelTime *= 1.08D;
        }

        return targetPos.add(targetMotion.scale(travelTime));
    }
}
