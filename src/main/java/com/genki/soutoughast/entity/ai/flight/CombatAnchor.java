package com.genki.soutoughast.entity.ai.flight;

/** A soft frontal region, not an orbit or mandatory exact-radius waypoint. */
public final class CombatAnchor {
    public static final double MIN_RANGE = 22;
    public static final double MAX_RANGE = 34;
    private static final double PREFERRED_RANGE = 28;
    private static final double ALTITUDE = 6;
    private static final double FRONT_COSINE = Math.cos(Math.toRadians(35));

    public enum Range { TOO_CLOSE, COMFORTABLE, TOO_FAR }
    public record Evaluation(Range range, boolean inRegion, FlightVector point, FlightController.Intent intent) { }

    public Evaluation evaluate(FlightVector playerPosition, FlightVector facing, FlightVector bossPosition) {
        FlightVector offset = bossPosition.subtract(playerPosition);
        double distance = offset.length();
        Range range = distance < MIN_RANGE ? Range.TOO_CLOSE : distance > MAX_RANGE ? Range.TOO_FAR : Range.COMFORTABLE;
        FlightVector horizontal = new FlightVector(offset.x(), 0, offset.z()).normalized();
        boolean inRegion = range == Range.COMFORTABLE && offset.y() >= 4 && offset.y() <= 10
                && horizontal.dot(facing) >= FRONT_COSINE;
        FlightVector point = playerPosition.add(facing.scale(Math.sqrt(PREFERRED_RANGE * PREFERRED_RANGE - ALTITUDE * ALTITUDE)))
                .add(new FlightVector(0, ALTITUDE, 0));
        FlightVector error = point.subtract(bossPosition);
        double arrivalDistance = Math.max(0, error.length() - 1.5);
        FlightController.Intent intent = inRegion || arrivalDistance == 0 ? FlightController.Intent.hold()
                : FlightController.Intent.move(error, Math.min(FlightController.MAX_SPEED,
                Math.sqrt(2 * FlightController.BRAKING * arrivalDistance)));
        return new Evaluation(range, inRegion, point, intent);
    }
}
