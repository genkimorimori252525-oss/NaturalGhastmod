package com.genki.soutoughast.danmaku.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Owner-managed virtual bullet swarm.
 * No bullet is a Minecraft Entity; Forge/network/render/collision adapters sit outside this class.
 */
public final class GrandDanmakuSession {
    @FunctionalInterface
    public interface ExpiryHandler {
        void onExpiry(VirtualBullet bullet, GrandDanmakuSession session);
    }

    public record BulletView(long id, int trackIndex, int bornTick, int ageTicks,
                             double x, double y, double z,
                             double hue, double radius) {}

    public static final class VirtualBullet {
        private final GrandDanmakuScore.SpawnSpec spec;
        private int ageTicks;
        private double x, y, z;

        VirtualBullet(GrandDanmakuScore.SpawnSpec spec) {
            this.spec = spec;
            this.x = spec.x(); this.y = spec.y(); this.z = spec.z();
        }

        void advance() {
            x += spec.vx(); y += spec.vy(); z += spec.vz(); ageTicks++;
        }

        boolean expired() { return ageTicks >= spec.lifetimeTicks(); }

        public long id() { return spec.id(); }
        public int trackIndex() { return spec.trackIndex(); }
        public int bornTick() { return spec.bornTick(); }
        public int ageTicks() { return ageTicks; }
        public double x() { return x; }
        public double y() { return y; }
        public double z() { return z; }
        public double vx() { return spec.vx(); }
        public double vy() { return spec.vy(); }
        public double vz() { return spec.vz(); }
        public int lifetimeTicks() { return spec.lifetimeTicks(); }
        public double hue() { return spec.hue(); }
        public double radius() { return spec.radius(); }

        BulletView view() {
            return new BulletView(id(), trackIndex(), bornTick(), ageTicks, x, y, z, hue(), radius());
        }
    }

    private final GrandDanmakuScore.Config score;
    private final ExpiryHandler expiryHandler;
    private final ArrayList<VirtualBullet> active = new ArrayList<>();
    private final ArrayList<VirtualBullet> pendingSpawn = new ArrayList<>();
    private final ArrayList<GrandDanmakuScore.BurstDescriptor> newBurstBatch = new ArrayList<>();
    private boolean iterating;
    private int tick;

    public GrandDanmakuSession(GrandDanmakuScore.Config score) {
        this(score, (bullet, session) -> {});
    }

    public GrandDanmakuSession(GrandDanmakuScore.Config score, ExpiryHandler expiryHandler) {
        if (score == null) throw new IllegalArgumentException("score is required");
        if (expiryHandler == null) throw new IllegalArgumentException("expiryHandler is required");
        this.score = score;
        this.expiryHandler = expiryHandler;
    }

    /**
     * Advances existing bullets, stages expiry-created bullets safely, then emits this score tick.
     * New bullets remain at their source position until the following tick.
     */
    public void tick() {
        iterating = true;
        for (Iterator<VirtualBullet> it = active.iterator(); it.hasNext();) {
            VirtualBullet bullet = it.next();
            bullet.advance();
            if (bullet.expired()) {
                it.remove();
                expiryHandler.onExpiry(bullet, this);
            }
        }
        iterating = false;

        if (!pendingSpawn.isEmpty()) {
            active.addAll(pendingSpawn);
            pendingSpawn.clear();
        }

        if (tick <= score.durationTicks()) {
            for (GrandDanmakuScore.BurstDescriptor burst : GrandDanmakuScore.burstsAt(score, tick)) {
                newBurstBatch.add(burst);
                for (GrandDanmakuScore.SpawnSpec spec : GrandDanmakuScore.expandBurst(score, burst))
                    spawn(spec);
            }
        }
        tick++;
    }

    /**
     * Public extension point for future TrailAction-like transforms.
     * During iteration this is staged and becomes active only after the pass.
     */
    public void spawn(GrandDanmakuScore.SpawnSpec spec) {
        if (spec == null) throw new IllegalArgumentException("spawn spec is required");
        if (active.size() + pendingSpawn.size() >= GrandDanmakuScore.MAX_LIVE_BULLETS)
            throw new IllegalStateException("virtual bullet live budget exceeded");
        VirtualBullet bullet = new VirtualBullet(spec);
        if (iterating) pendingSpawn.add(bullet);
        else active.add(bullet);
    }

    /**
     * One descriptor represents the deterministic bullet family emitted for a track at one tick.
     * A client with the same score can expand it without receiving one packet per bullet.
     */
    public List<GrandDanmakuScore.BurstDescriptor> drainBurstBatch() {
        List<GrandDanmakuScore.BurstDescriptor> result = List.copyOf(newBurstBatch);
        newBurstBatch.clear();
        return result;
    }

    public List<BulletView> bullets() {
        return active.stream().map(VirtualBullet::view).toList();
    }

    public int tickIndex() { return tick; }
    public int activeCount() { return active.size(); }
    public int pendingCountForTest() { return pendingSpawn.size(); }
    public boolean scoreEmissionFinished() { return tick > score.durationTicks(); }
}
