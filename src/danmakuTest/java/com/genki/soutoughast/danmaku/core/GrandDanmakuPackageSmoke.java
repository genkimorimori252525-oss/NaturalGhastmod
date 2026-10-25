package com.genki.soutoughast.danmaku.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Porting smoke for an existing NaturalGhastmod pure-Java core checkout.
 * Not a Forge in-game test. Requires Java 17 and the four classes from Draft PR #1.
 * Usage: GrandDanmakuPackageSmoke <score1.json> <score2.json> <score3.json>
 */
public final class GrandDanmakuPackageSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("expected three Score v2 JSON files");
        int[] expectedCounts = {22, 25, 31};
        int[] expectedDuration = {540, 540, 680};
        for (int n = 0; n < 3; n++) {
            GrandDanmakuScore.Config score = GrandDanmakuScoreJson.read(Files.readString(Path.of(args[n])));
            ensure(score.tracks().size() == expectedCounts[n], "track count: " + args[n]);
            ensure(score.durationTicks() == expectedDuration[n], "duration: " + args[n]);
            ensure(score.tracks().stream().allMatch(t -> t.frame() == GrandDanmakuScore.Frame.PLAYER_VIEW),
                   "PLAYER_VIEW: " + args[n]);
            int earliest = score.tracks().stream().mapToInt(GrandDanmakuScore.Track::startTick).min().orElseThrow();
            ensure(GrandDanmakuScore.emitAt(score, 0).isEmpty(), "no off-source prelude bullets at tick 0");
            var burst = GrandDanmakuScore.emitAt(score, earliest);
            ensure(!burst.isEmpty(), "first burst exists");
            for (var spawn : burst) {
                ensure(spawn.x() == 0 && spawn.y() == GrandDanmakuScore.EMITTER_Y && spawn.z() == 0,
                       "projectiles start at local emitter origin");
            }
            var deterministicA = GrandDanmakuScore.at(score, earliest + 2.0);
            var deterministicB = GrandDanmakuScore.at(score, earliest + 2.0);
            ensure(deterministicA.equals(deterministicB), "deterministic reconstruction");
            int peak = 0;
            for (int tick = 0; tick <= score.durationTicks(); tick++) {
                int live = 0;
                for (var tr : score.tracks()) live += GrandDanmakuScore.liveCount(tr, tick);
                if (live > peak) peak = live;
                ensure(live <= GrandDanmakuScore.MAX_LIVE_BULLETS, "runtime live cap");
            }
            GrandDanmakuSession session = new GrandDanmakuSession(score);
            for (int tick = 0; tick <= earliest; tick++) session.tick();
            ensure(session.activeCount() == burst.size(), "server-side session first emission");
            List<GrandDanmakuScore.BurstDescriptor> descriptors = session.drainBurstBatch();
            ensure(!descriptors.isEmpty(), "descriptors emitted");
            System.out.println("JAVA CORE PASS score=" + args[n] + ", tracks=" + score.tracks().size() + ", peak=" + peak);
        }
    }
    private static void ensure(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
