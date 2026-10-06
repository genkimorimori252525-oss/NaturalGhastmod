package com.genki.soutoughast.entity;

import com.genki.soutoughast.entity.ai.SoutouGhastCombatMoveGoal;
import com.genki.soutoughast.entity.ai.SoutouGhastLookGoal;
import com.genki.soutoughast.entity.ai.SoutouGhastMoveControl;
import com.genki.soutoughast.entity.ai.SoutouGhastRandomFloatGoal;
import com.genki.soutoughast.entity.ai.SoutouGhastShootGoal;
import com.genki.soutoughast.sound.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class SoutouGhast extends Ghast {
    public enum CombatPhase {
        SCOUTING,
        CHASING,
        BOMBARDMENT,
        ENRAGED,
        RETREATING
    }

    public enum Temperament {
        COWARDLY,
        TENACIOUS,
        IRASCIBLE,
        BALANCED;

        public static Temperament byName(String name) {
            for (Temperament value : values()) {
                if (value.name().equalsIgnoreCase(name)) {
                    return value;
                }
            }
            return BALANCED;
        }
    }

    private static final int LAST_SEEN_MEMORY_TICKS = 80;

    private CombatPhase combatPhase = CombatPhase.SCOUTING;
    private Temperament temperament = Temperament.BALANCED;
    private Vec3 lastSeenPos;
    private int lastSeenTicks;
    private int orbitDirection = 1;
    private int orbitChangeCooldown;
    private int feintCooldown;
    private int terrainRefreshCooldown;
    private float orbitAngleOffset;
    private double preferredAltitudeOffset = 6.0D;
    private double cachedOpenness = 0.6D;
    private boolean cachedNearLava;

    public SoutouGhast(EntityType<? extends Ghast> type, Level level) {
        super(type, level);
        this.moveControl = new SoutouGhastMoveControl(this);
        this.randomizeBehaviorProfile();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ghast.createAttributes();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new SoutouGhastCombatMoveGoal(this));
        this.goalSelector.addGoal(5, new SoutouGhastRandomFloatGoal(this));
        this.goalSelector.addGoal(7, new SoutouGhastLookGoal(this));
        this.goalSelector.addGoal(7, new SoutouGhastShootGoal(this));

        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                entity -> Math.abs(entity.getY() - this.getY()) <= 24.0D));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        this.randomizeBehaviorProfile();
        return data;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        if (this.terrainRefreshCooldown-- <= 0) {
            this.terrainRefreshCooldown = 20;
            this.refreshTerrainCache();
        }

        if (this.orbitChangeCooldown > 0) {
            --this.orbitChangeCooldown;
        }
        if (this.feintCooldown > 0) {
            --this.feintCooldown;
        }

        if (this.horizontalCollision && this.orbitChangeCooldown <= 0) {
            this.flipOrbitDirection();
            this.orbitChangeCooldown = 30;
        }

        if (this.random.nextInt(240) == 0 && this.orbitChangeCooldown <= 0) {
            this.flipOrbitDirection();
            this.orbitChangeCooldown = 80;
        }

        if (this.lastSeenTicks > 0) {
            --this.lastSeenTicks;
            if (this.lastSeenTicks <= 0) {
                this.lastSeenPos = null;
            }
        }

        if (this.getTarget() != null) {
            if (this.hasLineOfSight(this.getTarget())) {
                this.rememberTargetPosition(this.getTarget().position());
            }
            this.updateCombatPhase();
        } else {
            this.combatPhase = CombatPhase.SCOUTING;
        }
    }

    private void updateCombatPhase() {
        if (this.getTarget() == null) {
            this.combatPhase = CombatPhase.SCOUTING;
            return;
        }

        double distance = Math.sqrt(this.distanceToSqr(this.getTarget()));
        boolean hasSight = this.hasLineOfSight(this.getTarget());
        double retreatDistance = this.getRetreatDistance();
        double bombardDistance = this.getBombardDistance();
        double healthRatio = this.getHealth() / this.getMaxHealth();

        boolean enraged = healthRatio <= this.getEnrageThreshold();
        if (!enraged && this.temperament == Temperament.IRASCIBLE) {
            enraged = healthRatio <= 0.72F;
        }
        if (!enraged && this.cachedNearLava && hasSight && distance <= bombardDistance + 6.0D) {
            enraged = this.random.nextInt(8) == 0;
        }

        if (distance < retreatDistance) {
            this.combatPhase = CombatPhase.RETREATING;
        } else if (enraged) {
            this.combatPhase = CombatPhase.ENRAGED;
        } else if (hasSight && distance <= bombardDistance + 8.0D) {
            this.combatPhase = CombatPhase.BOMBARDMENT;
        } else {
            this.combatPhase = CombatPhase.CHASING;
        }
    }

    private void refreshTerrainCache() {
        int clearSamples = 0;
        int totalSamples = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    ++totalSamples;
                    if (this.level().isEmptyBlock(this.blockPosition().offset(dx * 6, dy * 4, dz * 6))) {
                        ++clearSamples;
                    }
                }
            }
        }

        this.cachedOpenness = totalSamples == 0 ? 0.5D : (double) clearSamples / (double) totalSamples;
        this.cachedNearLava = false;

        for (int dx = -6; dx <= 6 && !this.cachedNearLava; dx += 2) {
            for (int dy = -3; dy <= 3 && !this.cachedNearLava; dy++) {
                for (int dz = -6; dz <= 6; dz += 2) {
                    if (this.level().getFluidState(this.blockPosition().offset(dx, dy, dz)).is(FluidTags.LAVA)) {
                        this.cachedNearLava = true;
                        break;
                    }
                }
            }
        }
    }

    private void randomizeBehaviorProfile() {
        Temperament[] values = Temperament.values();
        this.temperament = values[this.random.nextInt(values.length)];
        this.preferredAltitudeOffset = 5.0D + this.random.nextDouble() * 4.0D;
        this.orbitDirection = this.random.nextBoolean() ? 1 : -1;
        this.orbitAngleOffset = this.random.nextFloat() * Mth.TWO_PI;
        this.cachedOpenness = 0.6D;
        this.cachedNearLava = false;
    }

    public CombatPhase getCombatPhase() {
        return this.combatPhase;
    }

    public Temperament getTemperament() {
        return this.temperament;
    }

    public boolean isEnraged() {
        return this.combatPhase == CombatPhase.ENRAGED;
    }

    public boolean hasFreshLastSeenPos() {
        return this.lastSeenPos != null && this.lastSeenTicks > 0;
    }

    @Nullable
    public Vec3 getLastSeenPos() {
        return this.lastSeenPos;
    }

    public void rememberTargetPosition(Vec3 pos) {
        this.lastSeenPos = pos;
        this.lastSeenTicks = this.getMemoryTicks();
    }

    public int getMemoryTicks() {
        return switch (this.temperament) {
            case TENACIOUS -> LAST_SEEN_MEMORY_TICKS + 60;
            case COWARDLY -> LAST_SEEN_MEMORY_TICKS - 20;
            default -> LAST_SEEN_MEMORY_TICKS;
        };
    }

    public double getPreferredCombatDistance() {
        double base = 22.0D + this.cachedOpenness * 10.0D;
        base += switch (this.temperament) {
            case COWARDLY -> 6.0D;
            case TENACIOUS -> -1.5D;
            case IRASCIBLE -> -3.0D;
            case BALANCED -> 0.0D;
        };
        if (this.cachedNearLava) {
            base -= 2.0D;
        }
        return Mth.clamp(base, 14.0D, 34.0D);
    }

    public double getRetreatDistance() {
        double base = switch (this.temperament) {
            case COWARDLY -> 13.5D;
            case TENACIOUS -> 8.5D;
            case IRASCIBLE -> 7.5D;
            case BALANCED -> 10.0D;
        };
        return this.cachedNearLava ? base - 1.0D : base;
    }

    public double getBombardDistance() {
        return this.getPreferredCombatDistance() + 3.0D;
    }

    public double getPreferredAltitudeOffset() {
        double base = this.preferredAltitudeOffset;
        if (this.cachedOpenness > 0.65D) {
            base += 2.5D;
        } else if (this.cachedOpenness < 0.35D) {
            base -= 2.5D;
        }
        if (this.cachedNearLava) {
            base += 1.0D;
        }
        return Mth.clamp(base, 2.5D, 12.0D);
    }

    public double getOrbitRadius() {
        double radius = this.getPreferredCombatDistance() - 4.0D;
        if (this.combatPhase == CombatPhase.ENRAGED) {
            radius -= 2.0D;
        }
        return Mth.clamp(radius, 10.0D, 26.0D);
    }

    public double getMoveSpeedForPhase() {
        return switch (this.combatPhase) {
            case SCOUTING -> 0.85D;
            case CHASING -> 1.15D;
            case BOMBARDMENT -> 1.05D;
            case ENRAGED -> 1.35D;
            case RETREATING -> 1.28D;
        };
    }

    public double getEnrageThreshold() {
        return switch (this.temperament) {
            case COWARDLY -> 0.38D;
            case TENACIOUS -> 0.26D;
            case IRASCIBLE -> 0.55D;
            case BALANCED -> 0.33D;
        };
    }

    public boolean shouldTryFeint() {
        if (this.feintCooldown > 0) {
            return false;
        }
        int roll = switch (this.combatPhase) {
            case ENRAGED -> 18;
            case BOMBARDMENT -> 36;
            case CHASING -> 64;
            default -> 120;
        };
        if (this.temperament == Temperament.IRASCIBLE) {
            roll = Math.max(10, roll - 8);
        }
        return this.random.nextInt(roll) == 0;
    }

    public void markFeintUsed() {
        this.feintCooldown = this.combatPhase == CombatPhase.ENRAGED ? 16 : 30;
    }

    public int getOrbitDirection() {
        return this.orbitDirection;
    }

    public float getOrbitAngleOffset() {
        return this.orbitAngleOffset;
    }

    public void flipOrbitDirection() {
        this.orbitDirection *= -1;
        this.orbitAngleOffset += (float) (Math.PI / 3.0D + this.random.nextDouble() * Math.PI / 3.0D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Temperament", this.temperament.name());
        tag.putInt("OrbitDirection", this.orbitDirection);
        tag.putFloat("OrbitAngleOffset", this.orbitAngleOffset);
        tag.putDouble("PreferredAltitudeOffset", this.preferredAltitudeOffset);
        if (this.lastSeenPos != null) {
            tag.putDouble("LastSeenX", this.lastSeenPos.x);
            tag.putDouble("LastSeenY", this.lastSeenPos.y);
            tag.putDouble("LastSeenZ", this.lastSeenPos.z);
            tag.putInt("LastSeenTicks", this.lastSeenTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.temperament = Temperament.byName(tag.getString("Temperament"));
        this.orbitDirection = tag.contains("OrbitDirection") ? tag.getInt("OrbitDirection") : 1;
        this.orbitAngleOffset = tag.contains("OrbitAngleOffset") ? tag.getFloat("OrbitAngleOffset") : 0.0F;
        this.preferredAltitudeOffset = tag.contains("PreferredAltitudeOffset") ? tag.getDouble("PreferredAltitudeOffset") : 6.0D;
        if (tag.contains("LastSeenX") && tag.contains("LastSeenY") && tag.contains("LastSeenZ")) {
            this.lastSeenPos = new Vec3(tag.getDouble("LastSeenX"), tag.getDouble("LastSeenY"), tag.getDouble("LastSeenZ"));
            this.lastSeenTicks = tag.getInt("LastSeenTicks");
        } else {
            this.lastSeenPos = null;
            this.lastSeenTicks = 0;
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SOUTOU_GHAST_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.SOUTOU_GHAST_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SOUTOU_GHAST_DEATH.get();
    }
}
