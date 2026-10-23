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
        check(turn.z() >= .65-FlightController.BRAKING-1e-9 && turn.x() <= FlightController.LATERAL_ACCELERATION, "high speed turns stay broad");
        FlightVector reverse = controller.step(FORWARD.scale(0.65), FlightController.Intent.move(FORWARD.scale(-1), 0.65));
        check(reverse.z() >= .65-FlightController.BRAKING-1e-9, "opposite intent cannot snap velocity");
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
        check(Math.abs(FlightOrientation.approachDegrees(0, 180, 4)) <= 4, "body cannot snap through a 180 degree turn");
        check(Math.abs(FlightOrientation.approachDegrees(179, -179, 1) - 180) < 1e-6, "body turns across yaw seam by shortest path");
        check(FlightOrientation.approachDegrees(0, 2, 4) == 2, "small body turns finish rather than stopping within the limit");
        CombatAnchor anchor = new CombatAnchor();
        FlightVector player = new FlightVector(10, 20, 30);
        CombatAnchor.Evaluation e = anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, 28)));
        check(e.range() == CombatAnchor.Range.COMFORTABLE && e.inRegion(), "initial observed combat geometry");
        check(anchor.evaluate(player, FORWARD, player.add(FORWARD.scale(21))).range() == CombatAnchor.Range.TOO_CLOSE, "near boundary");
        check(anchor.evaluate(player, FORWARD, player.add(FORWARD.scale(35))).range() == CombatAnchor.Range.TOO_FAR, "far boundary");
        check(!anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, -28))).inRegion(), "outside retained region requires recovery");
        check(anchor.evaluate(player, FORWARD, player.add(new FlightVector(0, 6, 28))).intent().mode() == FlightController.Mode.HOLD, "comfortable region avoids exact point chasing");
        FlightVector shift = new FlightVector(5, -2, 7);
        check(anchor.evaluate(player.add(shift), FORWARD, player.add(new FlightVector(0,6,28))).point().equals(e.point()), "ordinary Player translation preserves anchor");
        check(anchor.region().center().equals(e.point()), "maneuver position cannot replace anchor");
        FlightVector far = player.add(FORWARD.scale(70));
        for (int i = 0; i < 350; i++) {
            CombatAnchor.Evaluation recovery = anchor.evaluate(player, FORWARD, far);
            v = controller.step(v, recovery.intent());
            far = far.add(v);
        }
        check(anchor.evaluate(player, FORWARD, far).inRegion() && v.length() < 0.01, "soft arrival settles in region");
        try { new FlightVector(Double.NaN, 0, 0); throw new AssertionError("nonfinite accepted"); }
        catch (IllegalArgumentException expected) { }
        highSpeedFlight(controller);
        System.out.println("PASS: 22 flight/facing/anchor/orientation regression assertions");
    }

    private static void highSpeedFlight(FlightController controller) {
        check(FlightController.MAX_SPEED == 1.625, "optional ceiling is 2.5 times the previous maximum");
        var intent=FlightController.Intent.move(FORWARD,1.625);
        var velocity=FlightVector.ZERO;
        for(int i=0;i<60;i++){
            var next=controller.step(velocity,intent);
            check(next.length()<=1.625+1e-9,"high speed stays capped");
            check(next.subtract(velocity).length()<=FlightController.ACCELERATION+1e-9,"no instant high speed");
            var sweep=controller.clearanceSweep(velocity,next);
            var stopping=next;double travel=next.length();
            for(int j=0;j<60&&stopping.length()>1e-9;j++){
                stopping=controller.step(stopping,FlightController.Intent.brake());travel+=stopping.length();
            }
            check(sweep.length()+1e-9>=travel,"sweep covers complete next-step braking travel");
            velocity=next;
        }
        check(Math.abs(velocity.length()-1.625)<1e-9,"controller actually reaches requested ceiling");
        var turn=controller.step(velocity,FlightController.Intent.move(new FlightVector(1,0,0),1.625));
        check(turn.z()>=1.625-FlightController.BRAKING-1e-9&&turn.x()<=FlightController.LATERAL_ACCELERATION,"fast turn remains broad");
        var reverse=controller.step(velocity,FlightController.Intent.move(FORWARD.scale(-1),1.625));
        check(reverse.z()>=1.625-FlightController.BRAKING-1e-9,"fast reversal cannot snap");
        for(int i=0;i<60;i++)velocity=controller.step(velocity,FlightController.Intent.hold());
        check(velocity.length()<1e-9,"high speed can settle without snapping");
        CombatAnchor anchor=new CombatAnchor();
        FlightVector player=FlightVector.ZERO,boss=new FlightVector(0,6,120);
        anchor.evaluate(player,FORWARD,new FlightVector(0,6,28));
        double peak=0;velocity=FlightVector.ZERO;
        for(int i=0;i<450;i++){
            var recovery=anchor.evaluate(player,FORWARD,boss);
            velocity=controller.step(velocity,recovery.intent());boss=boss.add(velocity);peak=Math.max(peak,velocity.length());
        }
        check(peak>1.624,"production anchor recovery chooses the higher ceiling for a long journey");
        check(anchor.region().contains(boss)&&velocity.length()<.01,"fast recovery hands off inside retained region");
        int[] queries={0};
        check(FlightChunkCoverage.loaded(-1,2,-1,2,(x,z)->{queries[0]++;return true;}),"16-chunk stopping sweep admitted");
        check(queries[0]==16,"bounded coverage queries");queries[0]=0;
        check(!FlightChunkCoverage.loaded(-1,3,-1,2,(x,z)->{queries[0]++;return true;}),"oversized sweep brakes");
        check(queries[0]==0,"over-budget sweep makes no chunk queries");
        check(!FlightChunkCoverage.loaded(0,3,0,0,(x,z)->x!=2),"missing forward chunk rejects sweep");
        check(!FlightChunkCoverage.loaded(Integer.MIN_VALUE,Integer.MAX_VALUE,0,0,(x,z)->{throw new AssertionError("unbounded chunk query");}),"external impulse coverage cannot overflow budget");
        check(!FlightChunkCoverage.loaded(1,0,0,0,(x,z)->true),"invalid sweep rejects");
        var impulse=new FlightVector(0,0,2.5);
        var impulseNext=controller.step(impulse,FlightController.Intent.brake());
        double stoppingTravel=impulseNext.length();var residual=impulseNext;
        for(int i=0;i<100&&residual.length()>1e-9;i++){residual=controller.step(residual,FlightController.Intent.brake());stoppingTravel+=residual.length();}
        check(controller.clearanceSweep(impulse,impulseNext).length()>=stoppingTravel,"above-cap impulse retains full stopping horizon");
        float yaw=0;
        for(int i=0;i<26;i++){
            float nextYaw=FlightOrientation.approachDegrees(yaw,180,FlightOrientation.YAW_STEP);
            check(Math.abs(nextYaw-yaw)<=7,"body yaw remains bounded during faster turn");yaw=nextYaw;
        }
        check(Math.abs(yaw)==180,"half-turn completes within26ticks instead of45");
        try { FlightController.Intent.move(FORWARD,1.626);throw new AssertionError("overspeed intent accepted"); }
        catch(IllegalArgumentException expected) { }
        System.out.println("PASS: optional high-speed acceleration/turn/braking/sweep/production recovery checks");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
