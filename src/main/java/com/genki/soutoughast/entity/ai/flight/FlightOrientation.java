package com.genki.soutoughast.entity.ai.flight;

/** Bounded body rotation, independent from the player's smoothed combat-facing region. */
public final class FlightOrientation {
    private FlightOrientation() { }

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
