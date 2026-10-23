package com.genki.soutoughast.entity.ai.flight;

/** Freeze observed target geometry for the tell; derive the return ray at the actual ball. */
public final class RallyAim {
    private RallyAim() {}
    public static FlightVector lock(FlightVector ball, FlightVector targetCenter, FlightVector observedVelocity) {
        double leadTicks = Math.min(12, 6 + targetCenter.subtract(ball).length() / 1.15);
        return targetCenter.add(observedVelocity.scale(leadTicks).limited(2));
    }
    public static FlightVector direction(FlightVector ballAtReturn, FlightVector lockedTarget) {
        return lockedTarget.subtract(ballAtReturn).normalized();
    }
}
