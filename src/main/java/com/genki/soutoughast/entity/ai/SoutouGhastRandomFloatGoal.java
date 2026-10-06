package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Idle scouting drift. Picks roomy hover points and occasionally investigates the
 * last position where a target was seen.
 */
public class SoutouGhastRandomFloatGoal extends Goal {
    private final SoutouGhast ghast;

    public SoutouGhastRandomFloatGoal(SoutouGhast ghast) {
        this.ghast = ghast;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.ghast.getTarget() != null) {
            return false;
        }

        var moveControl = this.ghast.getMoveControl();
        if (!moveControl.hasWanted()) {
            return true;
        }

        double dx = moveControl.getWantedX() - this.ghast.getX();
        double dy = moveControl.getWantedY() - this.ghast.getY();
        double dz = moveControl.getWantedZ() - this.ghast.getZ();
        double distSqr = dx * dx + dy * dy + dz * dz;
        return distSqr < 4.0D || distSqr > 4096.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        RandomSource random = this.ghast.getRandom();
        Vec3 anchor = this.ghast.hasFreshLastSeenPos() && this.ghast.getLastSeenPos() != null
                ? this.ghast.getLastSeenPos()
                : this.ghast.position();

        double opennessBoost = this.ghast.getPreferredAltitudeOffset() > 7.0D ? 20.0D : 14.0D;
        double x = anchor.x + (random.nextDouble() * 2.0D - 1.0D) * opennessBoost;
        double y = anchor.y + (random.nextDouble() * 2.0D - 1.0D) * (opennessBoost * 0.55D);
        double z = anchor.z + (random.nextDouble() * 2.0D - 1.0D) * opennessBoost;
        this.ghast.getMoveControl().setWantedPosition(x, y, z, 0.85D);
    }
}
