package com.genki.soutoughast.entity.ai.flight;

/** Development tuning in blocks/tick; reads actual velocity, never integrates position. */
public final class FlightController {
    public static final double MAX_SPEED = 0.65;
    public static final double ACCELERATION = 0.11;
    public static final double BRAKING = 0.035;
    public static final double LATERAL_ACCELERATION = 0.045;

    public enum Mode { MOVE, BRAKE, HOLD }
    public FlightVector clearanceSweep(FlightVector actualVelocity,FlightVector next){
        double horizon=Math.min(MAX_SPEED/BRAKING+2,actualVelocity.length()/BRAKING+2);
        return next.scale(horizon*.5+1);
    }

    public record Intent(Mode mode, FlightVector direction, double speed) {
        public Intent {
            if (mode == null || direction == null || !Double.isFinite(speed) || speed < 0 || speed > MAX_SPEED) {
                throw new IllegalArgumentException("Invalid flight intent");
            }
        }
        public static Intent move(FlightVector direction, double speed) { return new Intent(Mode.MOVE, direction.normalized(), speed); }
        public static Intent brake() { return new Intent(Mode.BRAKE, FlightVector.ZERO, 0); }
        public static Intent hold() { return new Intent(Mode.HOLD, FlightVector.ZERO, 0); }
    }

    public FlightVector step(FlightVector actualVelocity, Intent intent) {
        double speed = actualVelocity.length();
        if (intent.mode() != Mode.MOVE || intent.direction().length() < 1e-9) {
            return speed <= BRAKING ? FlightVector.ZERO : actualVelocity.scale((speed - BRAKING) / speed);
        }
        FlightVector desired = intent.direction().scale(intent.speed());
        if (speed < 1e-9) return desired.limited(ACCELERATION);
        FlightVector forward = actualVelocity.normalized();
        FlightVector correction = desired.subtract(actualVelocity);
        double longitudinal = correction.dot(forward);
        FlightVector lateral = correction.subtract(forward.scale(longitudinal)).limited(LATERAL_ACCELERATION);
        double thrust = Math.max(-BRAKING, Math.min(ACCELERATION, longitudinal));
        // External impulses can exceed the cap; shed them gradually rather than snapping.
        return actualVelocity.add(forward.scale(thrust)).add(lateral).limited(Math.max(MAX_SPEED, speed));
    }
}
