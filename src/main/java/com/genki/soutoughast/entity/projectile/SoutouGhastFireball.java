package com.genki.soutoughast.entity.projectile;

import com.genki.soutoughast.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SoutouGhastFireball extends LargeFireball {
    public enum Variant {
        FAST_SMALL,
        HEAVY_SLOW,
        GROUND_FIRE,
        SPLITTER,
        WALL_BURST;

        public static Variant byName(String name) {
            for (Variant value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return FAST_SMALL;
        }
    }

    private Variant variant = Variant.FAST_SMALL;
    private Vec3 splitTarget = Vec3.ZERO;
    private int lifeTicks;
    private boolean hasSplit;
    // Mirror LargeFireball's private power through its constructor/NBT contract.
    private int childExplosionPower = 1;

    public SoutouGhastFireball(EntityType<? extends LargeFireball> type, Level level) {
        super(type, level);
    }

    public SoutouGhastFireball(Level level, LivingEntity owner, double dx, double dy, double dz, int explosionPower) {
        super(level, owner, dx, dy, dz, explosionPower);
        this.childExplosionPower = explosionPower;
    }

    public SoutouGhastFireball configure(Variant variant, Vec3 targetPos) {
        this.variant = variant;
        this.splitTarget = targetPos;
        return this;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || !this.isAlive()) {
            return;
        }

        ++this.lifeTicks;
        this.applyMotionProfile();
        if (this.variant == Variant.SPLITTER && !this.hasSplit && this.shouldSplitNow()) {
            this.splitMidAir();
        }
    }

    private void applyMotionProfile() {
        Vec3 motion = this.getDeltaMovement();
        double speed = motion.length();
        if (speed < 1.0E-5D) {
            return;
        }

        double targetSpeed = switch (this.variant) {
            case FAST_SMALL -> 1.45D;
            case HEAVY_SLOW -> 0.55D;
            case GROUND_FIRE -> 0.85D;
            case SPLITTER -> 0.92D;
            case WALL_BURST -> 0.95D;
        };

        double adjustment = switch (this.variant) {
            case FAST_SMALL -> 0.18D;
            case HEAVY_SLOW -> 0.10D;
            case GROUND_FIRE -> 0.12D;
            case SPLITTER -> 0.10D;
            case WALL_BURST -> 0.10D;
        };

        double newSpeed = Mth.lerp(adjustment, speed, targetSpeed);
        this.setDeltaMovement(motion.normalize().scale(newSpeed));
    }

    private boolean shouldSplitNow() {
        if (this.lifeTicks >= 18) {
            return true;
        }
        return this.splitTarget != null && this.position().distanceToSqr(this.splitTarget) <= 36.0D;
    }

    private void splitMidAir() {
        if (!(this.getOwner() instanceof LivingEntity owner)) {
            return;
        }

        this.hasSplit = true;
        Vec3 baseDir = this.splitTarget != null && this.splitTarget.lengthSqr() > 0.0D
                ? this.splitTarget.subtract(this.position()).normalize()
                : this.getDeltaMovement().normalize();
        if (baseDir.lengthSqr() < 1.0E-6D) {
            baseDir = new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 side = new Vec3(-baseDir.z, 0.0D, baseDir.x);
        if (side.lengthSqr() < 1.0E-6D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        side = side.normalize();

        for (int i = -1; i <= 1; ++i) {
            Vec3 dir = baseDir.add(side.scale(i * 0.35D)).add(0.0D, i == 0 ? 0.08D : 0.02D, 0.0D).normalize();
            SoutouGhastFireball child = new SoutouGhastFireball(this.level(), owner, dir.x, dir.y, dir.z, Math.max(1, this.childExplosionPower - 1))
                    .configure(Variant.FAST_SMALL, this.splitTarget);
            child.setPos(this.getX(), this.getY(), this.getZ());
            child.setDeltaMovement(dir.scale(0.9D));
            this.level().addFreshEntity(child);
        }

        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    ModSounds.SOUTOU_GHAST_FIREBALL_IMPACT.get(), SoundSource.HOSTILE,
                    2.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);

            if (this.variant == Variant.GROUND_FIRE) {
                this.igniteAround(BlockPos.containing(result.getLocation()));
            }
            if (this.variant == Variant.WALL_BURST && result instanceof BlockHitResult blockHitResult) {
                this.spawnWallBurst(blockHitResult);
            }
        }
        super.onHit(result);
    }

    private void spawnWallBurst(BlockHitResult hitResult) {
        if (!(this.getOwner() instanceof LivingEntity owner)) {
            return;
        }

        Direction face = hitResult.getDirection();
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 tangent = Math.abs(normal.y) > 0.8D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(-normal.z, 0.0D, normal.x).normalize();
        Vec3 bitangent = normal.cross(tangent).normalize();
        Vec3 origin = hitResult.getLocation().add(normal.scale(0.35D));

        Vec3[] dirs = new Vec3[] {
                normal.scale(0.55D).add(tangent.scale(0.45D)),
                normal.scale(0.55D).add(tangent.scale(-0.45D)),
                normal.scale(0.55D).add(bitangent.scale(0.45D)),
                normal.scale(0.55D).add(bitangent.scale(-0.45D))
        };

        for (Vec3 dir : dirs) {
            Vec3 n = dir.normalize();
            SoutouGhastFireball child = new SoutouGhastFireball(this.level(), owner, n.x, n.y, n.z, Math.max(1, this.childExplosionPower - 1))
                    .configure(Variant.FAST_SMALL, origin.add(n.scale(8.0D)));
            child.setPos(origin.x, origin.y, origin.z);
            child.setDeltaMovement(n.scale(0.85D));
            this.level().addFreshEntity(child);
        }
    }

    private void igniteAround(BlockPos center) {
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                BlockPos firePos = center.offset(dx, 0, dz);
                BlockState state = this.level().getBlockState(firePos);
                if (state.isAir() && this.level().getBlockState(firePos.below()).isFaceSturdy(this.level(), firePos.below(), Direction.UP)) {
                    this.level().setBlockAndUpdate(firePos, BaseFireBlock.getState(this.level(), firePos));
                }
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.variant.name());
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putBoolean("HasSplit", this.hasSplit);
        tag.putDouble("SplitTargetX", this.splitTarget.x);
        tag.putDouble("SplitTargetY", this.splitTarget.y);
        tag.putDouble("SplitTargetZ", this.splitTarget.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ExplosionPower", 99)) {
            this.childExplosionPower = tag.getByte("ExplosionPower");
        }
        this.variant = Variant.byName(tag.getString("Variant"));
        this.lifeTicks = tag.getInt("LifeTicks");
        this.hasSplit = tag.getBoolean("HasSplit");
        this.splitTarget = new Vec3(tag.getDouble("SplitTargetX"), tag.getDouble("SplitTargetY"), tag.getDouble("SplitTargetZ"));
    }
}
