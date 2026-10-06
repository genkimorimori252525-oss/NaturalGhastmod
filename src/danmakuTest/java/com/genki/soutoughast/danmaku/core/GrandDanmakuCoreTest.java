package com.genki.soutoughast.danmaku.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GrandDanmakuCoreTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("score JSON path required");
        String json = Files.readString(Path.of(args[0]));
        GrandDanmakuScore.Config score = GrandDanmakuScoreJson.read(json);

        truth(score.tracks().size() == 4, "score track count");
        truth(score.tracks().stream().allMatch(t -> t.frame() == GrandDanmakuScore.Frame.PLAYER_VIEW),
            "all initial tracks use player-view frame");

        var haloBurst = GrandDanmakuScore.emitAt(score, 20);
        equal(haloBurst.size(), 24, "halo burst count");
        near(haloBurst.get(0).x(), 0, "spawn x");
        near(haloBurst.get(0).y(), GrandDanmakuScore.EMITTER_Y, "spawn y");
        near(haloBurst.get(0).z(), 0, "spawn z");
        near(haloBurst.get(0).vx(), 0, "player-view top vx");
        near(haloBurst.get(0).vy(), 0.16, "player-view Pattern Z becomes screen up");
        near(haloBurst.get(0).vz(), 0.11, "forwardSpeed advances toward player");

        truth(GrandDanmakuScore.at(score, 60).equals(GrandDanmakuScore.at(score, 60)),
            "analytic score is deterministic");
        truth(GrandDanmakuScore.at(score, 100).size() > 0, "score has live bullets");
        truth(GrandDanmakuScore.at(score, 100).size() <= GrandDanmakuScore.MAX_LIVE_BULLETS,
            "score respects live budget");

        var session = new GrandDanmakuSession(score);
        for (int i = 0; i <= 20; i++) session.tick();
        equal(session.activeCount(), 24, "session emits halo at tick 20");
        var batches = session.drainBurstBatch();
        equal(batches.size(), 1, "one burst descriptor replaces 24 per-bullet spawn packets");
        truth(GrandDanmakuScore.expandBurst(score, batches.get(0)).equals(haloBurst),
            "burst descriptor reconstructs deterministic family");
        session.tick();
        var first = session.bullets().get(0);
        near(first.y(), GrandDanmakuScore.EMITTER_Y + 0.16, "virtual bullet advances without Entity");
        near(first.z(), 0.11, "virtual forward movement");

        // Explicit spawn-during-iteration staging, inspired by Youkai Homecoming's temp spawn list.
        var tinyPattern = new DanmakuPattern.Config(DanmakuPattern.Kind.FAN, 1, 0, 2, 0, 0, 0, 1, 2);
        var tinyTrack = new GrandDanmakuScore.Track("tiny", 0, 2, tinyPattern,
            GrandDanmakuScore.Frame.WORLD, 0, 0, 0.1);
        var tinyScore = new GrandDanmakuScore.Config(2, List.of(tinyTrack));
        final boolean[] expiryCalled = {false};
        var staging = new GrandDanmakuSession(tinyScore, (expired, owner) -> {
            expiryCalled[0] = true;
            owner.spawn(new GrandDanmakuScore.SpawnSpec(
                9999, 0, owner.tickIndex(), 0, 2, 0,
                0.25, 0, 0, 2, 120, 0.1));
            truth(owner.pendingCountForTest() == 1, "spawn during iteration is staged");
        });
        staging.tick(); // tick 0: score parent appears at origin
        equal(staging.activeCount(), 1, "tiny parent emitted");
        staging.tick(); // parent expires; child is staged then merged
        truth(expiryCalled[0], "expiry handler ran");
        equal(staging.pendingCountForTest(), 0, "pending list merged after iteration");
        equal(staging.activeCount(), 1, "staged child became active");
        var child = staging.bullets().get(0);
        equal(child.id(), 9999, "child identity");
        equal(child.ageTicks(), 0, "same-tick child does not advance during parent iteration");

        rejects(() -> new GrandDanmakuScore.Config(100, List.of(
            track("a", 0, 100, 100, 1, 20),
            track("b", 0, 100, 100, 1, 20)
        )), "combined score budget");

        System.out.println("PASS: " + checks + " Grand Danmaku core assertions");
    }

    private static GrandDanmakuScore.Track track(String name, int start, int end,
                                                  int bullets, int interval, int lifetime) {
        return new GrandDanmakuScore.Track(name, start, end,
            new DanmakuPattern.Config(DanmakuPattern.Kind.RING, bullets, 1, interval,
                360, 0, 0, lifetime, end - start),
            GrandDanmakuScore.Frame.WORLD, 0, 0, 0.1);
    }

    private static void truth(boolean value, String message) {
        checks++; if (!value) throw new AssertionError(message);
    }
    private static void equal(long got, long want, String message) {
        truth(got == want, message + ": " + got + " != " + want);
    }
    private static void near(double got, double want, String message) {
        truth(Math.abs(got - want) < 1e-9, message + ": " + got + " != " + want);
    }
    private static void rejects(Runnable action, String message) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("must reject: " + message);
    }
}
