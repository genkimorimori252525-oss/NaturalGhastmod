package com.genki.soutoughast.entity.ai;

import com.genki.soutoughast.entity.SoutouGhast;
import com.genki.soutoughast.entity.ai.flight.FlightOrientation;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.phys.Vec3;

/** Goal supplies observed look intent; the later LookControl tick owns body rotation. */
public final class SoutouGhastFlightLookControl extends LookControl {
    private final SoutouGhast ghast;
    private Vec3 direction;
    private boolean attackTracking;

    public SoutouGhastFlightLookControl(SoutouGhast ghast) { super(ghast); this.ghast = ghast; }
    public void setIntent(Vec3 direction) { this.direction = direction; attackTracking=false; }
    public void setAttackIntent(Vec3 observedDirection) {
        if(observedDirection==null||!FlightOrientation.validDirection(observedDirection.x,observedDirection.y,observedDirection.z)){clearIntent();return;}
        direction=observedDirection;attackTracking=true;
    }
    public void clearIntent() { direction = null; attackTracking=false; }

    @Override
    public void tick() {
        if (ghast.level().isClientSide || direction == null) return;
        if(!FlightOrientation.validDirection(direction.x,direction.y,direction.z)){clearIntent();return;}
        float yaw=FlightOrientation.targetYaw(direction.x,direction.z,ghast.getYRot());
        float pitch=FlightOrientation.targetPitch(direction.x,direction.y,direction.z);
        ghast.setYRot(FlightOrientation.approachDegrees(ghast.getYRot(), yaw, FlightOrientation.yawLimit(attackTracking)));
        ghast.setXRot(FlightOrientation.approachDegrees(ghast.getXRot(), pitch, FlightOrientation.pitchLimit(attackTracking)));
        ghast.yBodyRot = ghast.getYRot();
        ghast.yHeadRot = ghast.getYRot();
    }
}
