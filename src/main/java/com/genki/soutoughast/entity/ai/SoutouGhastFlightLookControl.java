package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightOrientation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.phys.Vec3;

/** Goal supplies observed look intent; the later LookControl tick owns body rotation. */
public final class SoutouGhastFlightLookControl extends LookControl {
    private final SoutouGhast ghast;
    private Vec3 direction;

    public SoutouGhastFlightLookControl(SoutouGhast ghast) { super(ghast); this.ghast = ghast; }
    public void setIntent(Vec3 direction) { this.direction = direction; }
    public void clearIntent() { direction = null; }

    @Override
    public void tick() {
        if (ghast.level().isClientSide || direction == null || direction.lengthSqr() < 1e-9) return;
        float yaw = (float)(-Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
        float pitch = (float)(-Mth.atan2(direction.y, direction.horizontalDistance()) * Mth.RAD_TO_DEG);
        ghast.setYRot(FlightOrientation.approachDegrees(ghast.getYRot(), yaw, 4));
        ghast.setXRot(FlightOrientation.approachDegrees(ghast.getXRot(), pitch, 3));
        ghast.yBodyRot = ghast.getYRot();
        ghast.yHeadRot = ghast.getYRot();
    }
}
