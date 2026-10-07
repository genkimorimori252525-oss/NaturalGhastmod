package com.genki.soutoughast.entity.ai.flight;

/** Dependency-free grouped regression checks; run with tools/test-flight-foundation.ps1. */
public final class FlightFoundationTest {
    private static final FlightVector FORWARD = new FlightVector(0, 0, 1);

    public static void main(String[] args) {
        FlightController controller = new FlightController();
        FlightVector accelerated = controller.step(FlightVector.ZERO, FlightController.Intent.move(FORWARD, 0.65));
        FlightVector braking = controller.step(FORWARD.scale(0.65), FlightController.Intent.brake());
        check(accelerated.length() > 0.65 - braking.length(), "acceleration exceeds braking");
        check(controller.step(FlightVector.ZERO, FlightController.Intent.brake()).equals(FlightVector.ZERO), "zero stays finite");
        FlightVector turn = controller.step(FORWARD.scale(0.65), FlightController.Intent.move(new FlightVector(1, 0, 0), 0.65));
        check(turn.z() > 0.60 && turn.x() <= FlightController.LATERAL_ACCELERATION, "high speed turns stay broad");
        FlightVector reverse = controller.step(FORWARD.scale(0.65), FlightController.Intent.move(FORWARD.scale(-1), 0.65));
        check(reverse.z() > 0.60, "opposite intent cannot snap velocity");
        FlightVector v = FlightVector.ZERO;
        for (int i = 0; i < 200; i++) v = controller.step(v, FlightController.Intent.move(FORWARD, 0.65));
        check(v.length() <= 0.650001, "bounded acceleration");
        for (int i = 0; i < 30; i++) v = controller.step(v, FlightController.Intent.hold());
        check(v.length() < 1e-9, "hold arrives at rest");
        CombatFacing facing = new CombatFacing();
        facing.reset(FORWARD);
        for (int i = 0; i < 100; i++) facing.update(new FlightVector(i % 2 == 0 ? 0.03 : -0.03, 0, 1));
        check(facing.direction().dot(FORWARD) > 0.9999, "camera jitter does not drag anchor");
        facing.update(new FlightVector(0, 1, 0));
        check(facing.direction().dot(FORWARD) > 0.9999, "vertical look preserves horizontal facing");
        for (int i = 0; i < 140; i++) facing.update(FORWARD.scale(-1));
        check(facing.direction().dot(FORWARD) < -0.99, "sustained 180 degree turn converges");
        facing.reset(new FlightVector(1, 0, 0));
        check(facing.direction().x() == 1, "subject change resets facing");
        CombatAnchor anchor = new CombatAnchor();
        FlightVector player = new FlightVector(10, 20, 30);
        CombatAnchor.Evaluation e = anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, 28)));
        check(e.range() == CombatAnchor.Range.COMFORTABLE && e.inRegion(), "frontal range and altitude band");
        check(anchor.evaluate(player, FORWARD, player.add(FORWARD.scale(21))).range() == CombatAnchor.Range.TOO_CLOSE, "near boundary");
        check(anchor.evaluate(player, FORWARD, player.add(FORWARD.scale(35))).range() == CombatAnchor.Range.TOO_FAR, "far boundary");
        check(!anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, -28))).inRegion(), "rear requires recovery");
        check(anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, 28))).intent().mode() == FlightController.Mode.HOLD, "comfortable region avoids exact point chasing");
        FlightVector shift = new FlightVector(5, -2, 7);
        check(anchor.evaluate(player.add(shift), FORWARD, player).point().subtract(e.point()).subtract(shift).length() < 1e-9, "anchor follows world translation");
        check(anchor.evaluate(player, FORWARD, player.add(FORWARD.scale(70))).point().equals(e.point()), "maneuver position cannot replace anchor");
        FlightVector far = player.add(FORWARD.scale(70));
        for (int i = 0; i < 350; i++) {
            CombatAnchor.Evaluation recovery = anchor.evaluate(player, FORWARD, far);
            v = controller.step(v, recovery.intent());
            far = far.add(v);
        }
        check(anchor.evaluate(player, FORWARD, far).inRegion() && v.length() < 0.01, "soft arrival settles in region");
        try { new FlightVector(Double.NaN, 0, 0); throw new AssertionError("nonfinite accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("PASS: 19 flight/facing/anchor regression assertions");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
