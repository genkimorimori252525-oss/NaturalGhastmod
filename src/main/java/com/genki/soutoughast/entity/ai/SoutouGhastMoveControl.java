package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SoutouGhastMoveControl extends MoveControl {
    private final SoutouGhast ghast;
    private int floatDuration;

    public SoutouGhastMoveControl(SoutouGhast ghast) {
        super(ghast);
        this.ghast = ghast;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            return;
        }

        if (this.floatDuration-- > 0) {
            return;
        }

        this.floatDuration = this.ghast.getRandom().nextInt(3) + 2;
        Vec3 move = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
        double distance = move.length();
        if (distance < 0.5D) {
            this.operation = Operation.WAIT;
            this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().scale(0.7D));
            return;
        }

        if (!this.canReach(move, Mth.ceil(distance))) {
            this.operation = Operation.WAIT;
            return;
        }

        Vec3 direction = move.normalize();
        double acceleration = 0.075D * this.speedModifier;
        this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().add(direction.scale(acceleration)));
    }

    private boolean canReach(Vec3 move, int steps) {
        AABB box = this.ghast.getBoundingBox();
        Vec3 step = move.scale(1.0D / (double) steps);

        for (int i = 1; i <= steps; ++i) {
            box = box.move(step);
            if (!this.ghast.level().noCollision(this.ghast, box)) {
                return false;
            }
        }

        return true;
    }
}
