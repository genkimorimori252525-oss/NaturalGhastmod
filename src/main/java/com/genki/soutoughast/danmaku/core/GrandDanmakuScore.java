package com.genki.soutoughast.danmaku.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic Grand Danmaku score.
 * PLAYER_VIEW maps the pattern plane to screen-right/screen-up while forwardSpeed advances toward the player.
 */
public final class GrandDanmakuScore {
    public static final int MAX_TRACKS = 32;
    public static final int MAX_LIVE_BULLETS = 3000;
    public static final int MAX_DURATION_TICKS = 1200;
    public static final double EMITTER_Y = 2.0;

    public enum Frame { WORLD, PLAYER_VIEW }

    public record Track(String name, int startTick, int endTick, DanmakuPattern.Config pattern,
                        Frame frame, double forwardSpeed, double phaseDeg,
                        double hue, double radius) {
        public Track {
            if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}"))
                throw new IllegalArgumentException("track name must be 1..32 safe ASCII chars");
            range(startTick, 0, MAX_DURATION_TICKS, "startTick");
            range(endTick, 1, MAX_DURATION_TICKS, "endTick");
            if (endTick <= startTick) throw new IllegalArgumentException("endTick must be > startTick");
            if (pattern == null) throw new IllegalArgumentException("pattern is required");
            if (pattern.durationTicks() != endTick - startTick)
                throw new IllegalArgumentException("pattern duration must equal endTick-startTick");
            if (frame == null) throw new IllegalArgumentException("frame is required");
            range(forwardSpeed, 0, 4, "forwardSpeed");
            range(phaseDeg, -360, 360, "phaseDeg");
            range(hue, 0, 360, "hue");
            range(radius, 0.04, 1.0, "radius");
        }
    }

    public record Config(int durationTicks, List<Track> tracks) {
        public Config {
            range(durationTicks, 1, MAX_DURATION_TICKS, "durationTicks");
            if (tracks == null || tracks.isEmpty()) throw new IllegalArgumentException("at least one track is required");
            if (tracks.size() > MAX_TRACKS) throw new IllegalArgumentException("too many tracks");
            tracks = List.copyOf(tracks);
            for (Track track : tracks) {
                if (track.endTick() > durationTicks)
                    throw new IllegalArgumentException("track exceeds score duration: " + track.name());
            }
            for (int tick = 0; tick <= durationTicks; tick++) {
                int live = 0;
                for (Track track : tracks) live += liveCount(track, tick);
                if (live > MAX_LIVE_BULLETS)
                    throw new IllegalArgumentException("combined live bullets exceed " + MAX_LIVE_BULLETS + " at tick " + tick);
            }
        }
    }

    public record SpawnSpec(long id, int trackIndex, int bornTick,
                            double x, double y, double z,
                            double vx, double vy, double vz,
                            int lifetimeTicks, double hue, double radius) {}

    public record BurstDescriptor(int trackIndex, int bornTick) {}

    public record BulletState(long id, int trackIndex, int bornTick,
                              double x, double y, double z,
                              double hue, double radius) {}

    public static List<BurstDescriptor> burstsAt(Config score, int tick) {
        if (tick < 0 || tick > score.durationTicks()) throw new IllegalArgumentException("tick out of score range");
        var out = new ArrayList<BurstDescriptor>();
        for (int ti = 0; ti < score.tracks().size(); ti++) {
            Track track = score.tracks().get(ti);
            int local = tick - track.startTick();
            if (local >= 0 && local < track.pattern().durationTicks() &&
                local % track.pattern().intervalTicks() == 0)
                out.add(new BurstDescriptor(ti, tick));
        }
        return List.copyOf(out);
    }

    public static List<SpawnSpec> emitAt(Config score, int tick) {
        var out = new ArrayList<SpawnSpec>();
        for (BurstDescriptor burst : burstsAt(score, tick))
            out.addAll(expandBurst(score, burst));
        return List.copyOf(out);
    }

    public static List<SpawnSpec> expandBurst(Config score, BurstDescriptor burst) {
        if (burst.trackIndex() < 0 || burst.trackIndex() >= score.tracks().size())
            throw new IllegalArgumentException("invalid track index");
        Track track = score.tracks().get(burst.trackIndex());
        int localBorn = burst.bornTick() - track.startTick();
        var vectors = DanmakuPattern.burst(track.pattern(), localBorn);
        var out = new ArrayList<SpawnSpec>(vectors.size());
        int burstNumber = localBorn / track.pattern().intervalTicks();
        double phase = Math.toRadians(track.phaseDeg());
        double cos = Math.cos(phase), sin = Math.sin(phase);
        for (DanmakuPattern.Velocity v : vectors) {
            double rotatedX = v.x() * cos + v.z() * sin;
            double rotatedZ = -v.x() * sin + v.z() * cos;
            double vx, vy, vz;
            if (track.frame() == Frame.PLAYER_VIEW) {
                vx = rotatedX;
                vy = rotatedZ;
                vz = track.forwardSpeed() + v.y();
            } else {
                vx = rotatedX;
                vy = v.y();
                vz = rotatedZ;
            }
            long id = deterministicId(burst.trackIndex(), burstNumber, v.bulletIndex());
            out.add(new SpawnSpec(id, burst.trackIndex(), burst.bornTick(),
                0, EMITTER_Y, 0, vx, vy, vz,
                track.pattern().lifetimeTicks(), track.hue(), track.radius()));
        }
        return List.copyOf(out);
    }

    public static List<BulletState> at(Config score, double tick) {
        range(tick, 0, score.durationTicks(), "tick");
        var out = new ArrayList<BulletState>();
        for (int ti = 0; ti < score.tracks().size(); ti++) {
            Track track = score.tracks().get(ti);
            int firstTick = track.startTick();
            int lastBorn = Math.min(track.endTick() - 1, (int) Math.floor(tick));
            for (int born = firstTick; born <= lastBorn; born++) {
                int local = born - track.startTick();
                if (local % track.pattern().intervalTicks() != 0) continue;
                double age = tick - born;
                if (age < 0 || age >= track.pattern().lifetimeTicks()) continue;
                for (SpawnSpec spawn : expandBurst(score, new BurstDescriptor(ti, born))) {
                    out.add(new BulletState(spawn.id(), ti, born,
                        spawn.x() + spawn.vx() * age,
                        spawn.y() + spawn.vy() * age,
                        spawn.z() + spawn.vz() * age,
                        spawn.hue(), spawn.radius()));
                }
            }
        }
        if (out.size() > MAX_LIVE_BULLETS)
            throw new IllegalStateException("validated score exceeded live bullet budget");
        return List.copyOf(out);
    }

    public static int liveCount(Track track, double tick) {
        return DanmakuPattern.liveCount(track.pattern(), tick - track.startTick());
    }

    private static long deterministicId(int trackIndex, int burstNumber, int bulletIndex) {
        return ((long) trackIndex << 48) | ((long) burstNumber << 20) | (bulletIndex & 0xfffffL);
    }

    private static void range(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be " + min + ".." + max);
    }
}
