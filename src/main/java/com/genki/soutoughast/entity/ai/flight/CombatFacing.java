package com.genki.soutoughast.entity.ai.flight;

/** Observable horizontal facing, with a dead band and sustained bounded turning. */
public final class CombatFacing {
    private static final double DEAD_BAND = Math.toRadians(4);
    private static final double TURN_RATE = Math.toRadians(1.5);
    private double yaw;
    private int turnSign;
    private int sustainedTicks;

    public void reset(FlightVector observedLook) {
        FlightVector horizontal = horizontal(observedLook);
        yaw = horizontal.length() < 1e-9 ? 0 : Math.atan2(horizontal.x(), horizontal.z());
        turnSign = 0;
        sustainedTicks = 0;
    }

    public void update(FlightVector observedLook) {
        FlightVector horizontal = horizontal(observedLook);
        if (horizontal.length() < 1e-6) return;
        double difference = wrap(Math.atan2(horizontal.x(), horizontal.z()) - yaw);
        if (Math.abs(difference) <= DEAD_BAND) {
            sustainedTicks = 0;
            turnSign = 0;
            return;
        }
        // At the antipode choose the current turn side; tiny camera noise cannot flip it.
        int sign = Math.abs(difference) > Math.PI - DEAD_BAND && turnSign != 0
                ? turnSign : difference < 0 ? -1 : 1;
        sustainedTicks = sign == turnSign ? sustainedTicks + 1 : 1;
        turnSign = sign;
        if (sustainedTicks >= 3) yaw = wrap(yaw + sign * Math.min(TURN_RATE, Math.abs(difference)));
    }

    public FlightVector direction() { return new FlightVector(Math.sin(yaw), 0, Math.cos(yaw)); }
    private static FlightVector horizontal(FlightVector v) { return new FlightVector(v.x(), 0, v.z()); }
    private static double wrap(double angle) { return Math.atan2(Math.sin(angle), Math.cos(angle)); }
}
