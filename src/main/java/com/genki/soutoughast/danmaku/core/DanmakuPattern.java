package com.genki.soutoughast.danmaku.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft-independent launch geometry.
 * 1 unit = 1 block, 20 ticks = 1 second, +Y up, +Z forward.
 */
public final class DanmakuPattern {
    public enum Kind { FAN, RING, SPIRAL }

    public record Config(Kind pattern, int bullets, double speed, int intervalTicks,
                         double fanAngleDeg, double rotationDegPerSecond, double elevationDeg,
                         int lifetimeTicks, int durationTicks) {
        public Config {
            if (pattern == null) throw new IllegalArgumentException("pattern is required");
            range(bullets, 1, 1000, "bullets");
            range(speed, 0, 4, "speed");
            range(intervalTicks, 1, 1200, "intervalTicks");
            range(fanAngleDeg, 0, 360, "fanAngleDeg");
            range(rotationDegPerSecond, -720, 720, "rotationDegPerSecond");
            range(elevationDeg, -90, 90, "elevationDeg");
            range(lifetimeTicks, 1, 1200, "lifetimeTicks");
            range(durationTicks, 1, 1200, "durationTicks");
            int bursts = (Math.min(lifetimeTicks, durationTicks) + intervalTicks - 1) / intervalTicks;
            if ((long) bullets * bursts > 3000)
                throw new IllegalArgumentException("single track live bullet budget exceeds 3000");
        }
    }

    public record Velocity(int bulletIndex, double x, double y, double z) {}

    public static List<Velocity> burst(Config config, int bornTick) {
        if (bornTick < 0 || bornTick >= config.durationTicks() || bornTick % config.intervalTicks() != 0)
            throw new IllegalArgumentException("bornTick is not a valid burst tick");
        var result = new ArrayList<Velocity>(config.bullets());
        double elevation = Math.toRadians(config.elevationDeg());
        double cosElevation = Math.cos(elevation);
        for (int index = 0; index < config.bullets(); index++) {
            double heading = switch (config.pattern()) {
                case FAN -> config.bullets() == 1 ? 0 :
                    -config.fanAngleDeg() / 2.0 + config.fanAngleDeg() * index / (config.bullets() - 1.0);
                case RING, SPIRAL -> 360.0 * index / config.bullets();
            };
            if (config.pattern() == Kind.SPIRAL)
                heading += config.rotationDegPerSecond() * bornTick / 20.0;
            double yaw = Math.toRadians(heading);
            result.add(new Velocity(index,
                Math.sin(yaw) * cosElevation * config.speed(),
                Math.sin(elevation) * config.speed(),
                Math.cos(yaw) * cosElevation * config.speed()));
        }
        return List.copyOf(result);
    }

    static int liveCount(Config config, double localTick) {
        if (localTick < 0 || localTick > config.durationTicks()) return 0;
        int last = Math.min((int) Math.floor(localTick / config.intervalTicks()),
            (config.durationTicks() - 1) / config.intervalTicks());
        int first = Math.max(0,
            (int) Math.floor((localTick - config.lifetimeTicks()) / config.intervalTicks()) + 1);
        return last < first ? 0 : (last - first + 1) * config.bullets();
    }

    private static void range(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException(name + " must be " + min + ".." + max);
    }
}
