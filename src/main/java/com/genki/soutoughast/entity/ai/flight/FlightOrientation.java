package com.genki.soutoughast.entity.ai.flight;

/** Bounded body rotation, independent from the player's smoothed combat-facing region. */
public final class FlightOrientation {
    public static final float YAW_STEP=7, PITCH_STEP=5;
    private FlightOrientation() { }

    public static float yawLimit(boolean attackTracking){return attackTracking?180:YAW_STEP;}
    public static float pitchLimit(boolean attackTracking){return attackTracking?180:PITCH_STEP;}
    public static boolean validDirection(double x,double y,double z){
        return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)&&Math.hypot(Math.hypot(x,y),z)>1e-6;
    }
    public static float targetYaw(double x,double z,float previous){
        return Math.hypot(x,z)<1e-6?previous:(float)Math.toDegrees(-Math.atan2(x,z));
    }
    public static float targetPitch(double x,double y,double z){
        return (float)Math.max(-90,Math.min(90,Math.toDegrees(-Math.atan2(y,Math.hypot(x,z)))));
    }
    public static float attackRenderPitch(float current,float observed,boolean active){
        return approachDegrees(current,Math.max(-90,Math.min(90,active?observed:0)),active?180:PITCH_STEP);
    }
    public static float renderPitch(float major,float majorTarget,float attack){
        return Math.abs(major)>1e-4||Math.abs(majorTarget)>1e-4?major:attack;
    }

    public static float approachDegrees(float current, float desired, float limit) {
        if (!Float.isFinite(current) || !Float.isFinite(desired) || !Float.isFinite(limit) || limit < 0) {
            throw new IllegalArgumentException("Invalid flight orientation");
        }
        float difference = (desired - current) % 360;
        if (difference >= 180) difference -= 360;
        if (difference < -180) difference += 360;
        return current + Math.max(-limit, Math.min(limit, difference));
    }
}
