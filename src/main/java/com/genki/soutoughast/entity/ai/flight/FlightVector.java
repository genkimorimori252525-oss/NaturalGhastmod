package com.genki.soutoughast.entity.ai.flight;

public record FlightVector(double x, double y, double z) {
    public static final FlightVector ZERO = new FlightVector(0, 0, 0);

    public FlightVector {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Flight vectors must be finite");
        }
    }

    public FlightVector add(FlightVector other) { return new FlightVector(x + other.x, y + other.y, z + other.z); }
    public FlightVector subtract(FlightVector other) { return new FlightVector(x - other.x, y - other.y, z - other.z); }
    public FlightVector scale(double factor) { return new FlightVector(x * factor, y * factor, z * factor); }
    public double dot(FlightVector other) { return x * other.x + y * other.y + z * other.z; }
    public double length() { return Math.hypot(Math.hypot(x, y), z); }
    public FlightVector normalized() { double length = length(); return length < 1e-9 ? ZERO : scale(1 / length); }
    public FlightVector limited(double limit) { double length = length(); return length > limit ? scale(limit / length) : this; }
}
