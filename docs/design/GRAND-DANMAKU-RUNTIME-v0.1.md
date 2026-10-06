# Grand Danmaku runtime v0.1

Date: 2026-10-06

## Purpose

This is the first runtime foundation for Soutou Ghast's **Grand Danmaku** major art.

It intentionally does **not** connect to the existing v0.02 combat AI yet. The current AI/projectile source is a historical/intermediate generation and is scheduled for replacement by the new Combat Anchor / Maneuver / Flight Controller design.

The runtime is therefore isolated under:

`com.genki.soutoughast.danmaku.core`

and is Minecraft/Forge-independent.

## Authoring pipeline

The current intended pipeline is:

```text
KNEEKURA-TECH-HUB JavaFX Score Mode
        |
        | Score JSON v2
        v
NaturalGhast GrandDanmakuScoreJson
        |
        v
GrandDanmakuScore
        |
        v
GrandDanmakuSession
        |
        +--> future server collision adapter
        +--> future burst network adapter
        +--> future client virtual-bullet cache
        +--> future batched renderer
```

The same initial score is retained in both projects.

NaturalGhast resource:

`data/soutou_ghast/danmaku/grand_danmaku_v1.json`

The first score contains:
- Halo
- counter-rotating Twin Spiral
- Finale ring

This is an engineering preset, not a final visual composition.

## Player-view coordinate frame

Grand Danmaku is authored for the player's combat view, not for an arbitrary world plane.

For `PLAYER_VIEW` tracks:

- pattern X -> screen-right
- pattern radial Z -> screen-up
- `forwardSpeed` -> Soutou Ghast toward player
- emitter origin -> Soutou Ghast firing source

This lets a RING or SPIRAL visually bloom around the Ghast while the bullets also advance toward the player.

The current pure core uses a canonical local frame. A future Minecraft adapter must construct the real world basis from:

```text
Forward = normalized(Ghast -> player)
Right   = stable perpendicular
Up      = perpendicular to Forward/Right
```

and transform the local score into world coordinates.

## Youkai Homecoming techniques adopted

The design is source-informed by the TECH-HUB Youkai's Homecoming 2.7.0 analysis.

### 1. Owner-managed virtual projectile swarm

Adopted concept:

> dense ephemeral bullets do not need to become ordinary Level-managed Minecraft entities.

`GrandDanmakuSession` owns plain-Java `VirtualBullet` objects.

Benefits:
- no Entity manager entry per bullet;
- no generic Entity lifecycle work per bullet;
- predictable bounded ownership;
- easier deterministic score playback.

### 2. Spawn-during-iteration staging

Adopted directly as an architectural rule.

Session collections:

```text
active
pendingSpawn
newBurstBatch
```

If an expiry/transform action spawns a child while `active` is being iterated, the child is placed in `pendingSpawn`.

After the pass:

`active.addAll(pendingSpawn)`

This prevents mutation-during-iteration bugs and defines clear same-tick semantics.

### 3. Batch-oriented spawn synchronization

Youkai Homecoming moved from projectile-per-packet synchronization to batched projectile synchronization.

Grand Danmaku can compress further because its score is deterministic.

The current runtime exposes:

`BurstDescriptor(trackIndex, bornTick)`

A client that already owns the same score can reconstruct the whole burst with:

`GrandDanmakuScore.expandBurst(score, descriptor)`

Example:

one Halo descriptor -> 24 virtual projectile spawns.

This is the intended network boundary, rather than one packet per bullet.

### 4. Separate visual and gameplay geometry

The current pure core stores visual radius only.

The next collision adapter should keep the Youkai Homecoming lesson:

```text
rendered bullet radius
!=
damage hit radius
```

Grand Danmaku should be visually large and beautiful while using a smaller, fair damage core.

### 5. Future per-owner collision cache

Not yet implemented.

The intended server adapter should:
- collect candidate living entities once per relevant area/tick;
- reuse candidates across Soutou Ghast's entire swarm;
- avoid one Level nearby-entity query per bullet;
- account for target movement in broadphase/narrowphase for fast targets.

This follows the reusable part of Youkai Homecoming's entity-section and shooter-local cache design.

### 6. Future render batching

Not yet implemented.

The client should not create one renderer submission path per bullet.

Planned shape:

```text
ClientDanmakuCache
  -> group by visual type
  -> prepare compact instances
  -> batch buffer submission
```

Render state should be memoized per visual type.

## Techniques deliberately not copied into v0.1

### Ordinary Minecraft Entity subclass per bullet

Rejected for Grand Danmaku density.

### Direct BufferBuilder internal writes

Not part of the first implementation.

Youkai Homecoming's history demonstrates that unsafe internal buffer fast paths need explicit compatibility fallback (e.g. ImmediatelyFast). NaturalGhast should first implement a public-contract batched renderer, then profile before adding an optional unsafe fast path.

### Child/moving emitters

Not used in initial Grand Danmaku.

The design rule is that damaging projectiles visibly originate from Soutou Ghast. Child emitters can be reconsidered later only if they preserve that visual source integrity.

### Continuous hidden homing

Not part of initial Grand Danmaku.

The first score is deterministic and readable.

### Graze economy

Not needed for Soutou Ghast v0.1.

The useful geometric lesson (separate direct-hit envelope) remains relevant.

## Runtime lifecycle

Current session tick order:

```text
1. advance existing active bullets
2. expire bullets
3. expiry handler may spawn -> pendingSpawn
4. finish active iteration
5. merge pendingSpawn
6. emit Score bursts for this tick
7. store BurstDescriptor(s)
8. increment session tick
```

Newly emitted or expiry-created bullets remain at age 0 until the next tick.

## Budgets

Current hard design bounds:

- score duration <= 1200 ticks / 60 seconds
- track count <= 32
- combined live virtual bullets <= 3000
- individual Pattern contract remains bounded as well

These are safety/contracts, not performance targets.

No Minecraft FPS/MSPT benchmark is claimed yet.

## Verification

Isolated CI:

`Grand Danmaku pure core`

GitHub Actions run:

`37442123806`

Result:
- Java 17 isolated compile: SUCCESS
- GrandDanmakuCoreTest: SUCCESS

Assertions cover:
- Score JSON v2 load;
- PLAYER_VIEW launch mapping;
- deterministic score evaluation;
- 3000 live-bullet budget;
- owner-managed bullet movement;
- BurstDescriptor reconstruction;
- expiry spawn staging;
- same-tick child age semantics.

## Next adapter boundary

The next implementation stage should add **adapters**, not rewrite this core:

1. `SoutouGhastGrandDanmakuController`
   - starts/stops a Session;
   - builds world basis from Ghast/player;
   - freezes/Relative-Locks the boss during the major art.

2. server collision adapter
   - small damage core;
   - shared candidate cache;
   - swept target-motion checks.

3. network adapter
   - session start: score ID + basis/seed/session ID;
   - active use: BurstDescriptor batches;
   - correction/erase messages only when required.

4. client cache + renderer
   - reconstruct same deterministic bursts;
   - virtual bullet tick;
   - render-type batching.

5. LAB instrumentation
   - live virtual bullet count;
   - bursts/tick;
   - packet bytes/tick;
   - collision candidates;
   - frame time / server MSPT contribution;
   - safe-space and hit metrics.

Only after these are stable should the major art be wired into the redesigned combat AI.
