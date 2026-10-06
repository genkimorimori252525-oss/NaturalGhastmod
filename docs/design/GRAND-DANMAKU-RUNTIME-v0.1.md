# Grand Danmaku runtime v0.1

Date: 2026-10-06

## Purpose

Soutou Ghast の **Grand Danmaku** 大技用runtime foundation。

既存v0.02戦闘AIにはまだ接続しない。
新Combat Anchor / Maneuver / Flight Controller設計へ後で接続するため、runtimeを独立させる。

Package:

`com.genki.soutoughast.danmaku.core`

Minecraft/Forge非依存。

## Authoring pipeline

```text
KNEEKURA-TECH-HUB JavaFX Score Mode
        |
        | Score JSON v2
        v
GrandDanmakuScoreJson
        |
        v
GrandDanmakuScore
        |
        v
GrandDanmakuSession
        |
        +--> future collision adapter
        +--> future network adapter
        +--> future client virtual cache
        +--> future batched renderer
```

NaturalGhast resource:

`data/soutou_ghast/danmaku/grand_danmaku_v1.json`

## Current first score

Current score is approximately 14 seconds / 280 ticks:

1. Halo Gold
2. counter-rotating blue/magenta Twin Spiral
3. Six Petal Bloom
4. cyan/violet Weave
5. Double Finale

The six petals use six FAN Tracks with 60° phase separation.
Each petal is 28° wide, leaving roughly 32° between adjacent petal centers as intentional negative space.

This is already more than an engineering placeholder: it is the first concrete visual grammar for Grand Danmaku.
It remains tunable in the Techhub JavaFX Score Mode.

## Player-view coordinate frame

PLAYER_VIEW:
- pattern X -> screen-right
- pattern radial Z -> screen-up
- forwardSpeed -> Ghast toward player
- phaseDeg -> rotation around the Ghast->player firing axis
- emitter origin -> Soutou Ghast

A future Minecraft adapter constructs:

```text
Forward = normalized(Ghast -> player)
Right   = stable perpendicular
Up      = perpendicular to Forward/Right
```

and maps the local Score frame to world space.

`phaseDeg` is optional in Score JSON v2 for backward compatibility; absent values mean 0°.

## Youkai Homecoming techniques adopted

### Owner-managed virtual swarm

GrandDanmakuSession owns plain-Java VirtualBullet objects.
Dense bullets are not ordinary Level-managed entities.

### Spawn-during-iteration staging

```text
active
pendingSpawn
newBurstBatch
```

Expiry-created bullets during iteration enter pendingSpawn and are merged after the active pass.

### Deterministic batch synchronization boundary

`BurstDescriptor(trackIndex, bornTick)`

is enough to reconstruct a full deterministic family when server/client share the same Score.

One descriptor can represent dozens of bullet spawns.

### Separate render/damage geometry

Visual radius is not intended to equal future damage hit radius.
The collision adapter should use a smaller gameplay core.

### Future owner-local collision cache

Future server adapter should query candidate targets once per owner/area/tick rather than once per bullet, and should account for target motion.

### Future render batching

Future client:

```text
ClientDanmakuCache
 -> group by visual type
 -> compact instances
 -> batched buffer submission
```

## Deliberately not copied yet

- one Minecraft Entity per bullet
- direct BufferBuilder internals
- child/moving emitters
- graze economy
- hidden continuous homing

All damaging bullets in the initial Grand Danmaku visibly originate from Soutou Ghast.

## Runtime lifecycle

```text
1. advance active bullets
2. expire bullets
3. expiry callback may spawn -> pendingSpawn
4. finish iteration
5. merge pendingSpawn
6. emit Score bursts
7. retain BurstDescriptor batch
8. increment session tick
```

New bullets remain age 0 until the following tick.

## Budgets

- duration <= 1200 tick / 60s
- tracks <= 32
- combined live virtual bullets <= 3000

These are safety bounds, not performance claims.

## Verification

Current isolated CI validates:
- Java 17 compile
- Score JSON v2
- PLAYER_VIEW
- phaseDeg rotation
- deterministic score evaluation
- dense multi-motif score under the live budget
- owner-managed movement
- BurstDescriptor reconstruction
- expiry spawn staging
- same-tick child age semantics

Latest phase/floral-score core run: `37447456366` — SUCCESS.

No Minecraft FPS/MSPT claim yet.

## Next adapter boundary

1. SoutouGhastGrandDanmakuController
   - starts/stops Session
   - Relative Stationary Lock
   - world basis construction
   - major-art lifecycle

2. server collision adapter
   - smaller damage core
   - shared candidate cache
   - swept target motion

3. network adapter
   - session start = score ID + session/basis data
   - BurstDescriptor batches
   - correction/erase only when required

4. client cache + renderer
   - reconstruct deterministic bursts
   - virtual bullet tick
   - render batching by visual family

5. LAB instrumentation
   - live bullets
   - spawn/burst rate
   - packet bytes
   - collision candidates
   - frame time / MSPT contribution
   - safe-space / hit metrics

Only after these adapters are stable should Grand Danmaku be connected to the redesigned Major Action Director.
