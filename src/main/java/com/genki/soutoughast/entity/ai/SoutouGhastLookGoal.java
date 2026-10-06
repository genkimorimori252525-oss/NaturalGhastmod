package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Faces along current motion while idle and slightly leads the target while fighting.
 */
public class SoutouGhastLookGoal extends Goal {
    private final SoutouGhast ghast;

    public SoutouGhastLookGoal(SoutouGhast ghast) {
        this.ghast = ghast;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Vec3 facing;
        if (this.ghast.getTarget() == null) {
            facing = this.ghast.getDeltaMovement();
            if (facing.lengthSqr() < 1.0E-4D) {
                facing = new Vec3(
                        this.ghast.getMoveControl().getWantedX() - this.ghast.getX(),
                        0.0D,
                        this.ghast.getMoveControl().getWantedZ() - this.ghast.getZ()
                );
            }
        } else {
            Vec3 predicted = this.ghast.getTarget().position().add(this.ghast.getTarget().getDeltaMovement().scale(6.0D));
            facing = predicted.subtract(this.ghast.position());
        }

        if (facing.lengthSqr() > 1.0E-6D) {
            this.ghast.setYRot(-((float) Math.atan2(facing.x, facing.z)) * (180.0F / (float) Math.PI));
            this.ghast.yBodyRot = this.ghast.getYRot();
        }
    }
}
