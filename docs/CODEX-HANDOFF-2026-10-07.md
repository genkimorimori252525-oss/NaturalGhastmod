# NaturalGhastmod — Codex implementation handoff

Date: 2026-10-07  
Target: Minecraft 1.20.1 / Forge 47.x / Java 17

## 1. Project purpose

NaturalGhastmod is being redesigned around **Soutou Ghast**, a unique boss-class Ghast that does not naturally spawn and does not use a boss bar.

The redesign goal is not simply to make the old v0.02 AI stronger. The intended boss should feel:

- deliberate rather than random;
- smooth and inertia-heavy rather than snapping between points;
- deceptive but readable;
- visually easy to keep track of in ordinary combat;
- different in open air, confined terrain and ground-forced situations;
- replayable without collapsing into one fixed attack loop.

Armor is visual-only. Existing voice / sound / particle / texture / animation assets may be reused.

The authoritative design is:

`docs/design/NATURAL-GHAST-REDESIGN-v0.1.md`

## 2. Important repository state

### main

`main` currently contains:

- the imported v0.02 source snapshot;
- its static audit / failure record;
- the current redesign document;
- the recovered 1.0.0 reference metadata;
- no claim that the legacy gameplay code is already the redesign.

The old v0.02 source is **not** the target implementation. It is historical/reference material and has known compile/runtime design defects.

See:

- `README.md`
- `docs/SOUTOU-GHAST-AUDIT-2026-10-06.md`
- `docs/reference/NATURAL-GHAST-1.0.0-REFERENCE.md`

### Draft PR #1

Branch:

`jolly/grand-danmaku-runtime-2026-10-06`

Draft PR #1 contains an isolated **Grand Danmaku runtime scaffold**:

- pure-Java Score / Track geometry;
- PLAYER_VIEW coordinate frame;
- phase rotation;
- owner-managed VirtualBullet swarm;
- spawn-during-iteration staging;
- deterministic BurstDescriptor reconstruction;
- Java 17 isolated CI.

This code is intentionally not connected to the obsolete v0.02 combat AI.

## 3. Critical status: danmaku is NOT designed yet

**The actual Grand Danmaku attack pattern has not been created or approved yet.**

Any current Halo / Twin Spiral / petal / weave / finale JSON or JavaFX preset is only:

- an engineering test;
- a visualization example;
- a schema/runtime validation asset.

It must **not** be treated as final gameplay content.

The final danmaku will be designed later, probably using:

- the TECH-HUB JavaFX danmaku authoring tool;
- TECH-HUB Youkai Homecoming danmaku research;
- edited projectile visuals using red / orange / dark-red / black assets.

Implementation should therefore preserve the Score/runtime abstraction so the final Score can be swapped in later without rewriting the engine.

## 4. Confirmed normal-combat design

### Combat Anchor Volume

User clarification,2026-10-08: normal air combat uses a broad boss-owned world-space region retained during the fight and reselected only when necessary. It does not follow Player position/look every tick. Initial proposed open-space size40×16×40; a roughly10-block region is too small. The center is not an exact return point. This supersedes v0.5's smoothed-facing frontal anchor.

`OBSERVED COMBAT GEOMETRY -> BOSS REGION SELECTION -> RETAINED WORLD-SPACE REGION`

Temporary actions leave the anchor, then normally return:

`ANCHOR -> MANEUVER -> BRAKE -> RETURN -> ANCHOR`

### Preferred range band

Use a comfortable range band, not an exact radius.

Range is a soft tactical preference inside the retained region, not a hard reason for continuous anchor relocation. Readability must not cause camera-chasing steering.

- too close -> open distance;
- comfortable -> hold / drift / attack / feint;
- too far -> close distance.

### Flight feel

Core rule:

`acceleration authority > braking authority`

Acceleration should feel stronger than braking. High speed should create broad turns and visible inertia.

Architecture:

`Brain -> Movement Intent -> Maneuver -> Flight Controller -> Actual Motion`

The Flight Controller owns momentum, acceleration, braking, turn authority, collision avoidance and arrival correction.

Normal quiet/HOLD intervals include gentle actual horizontal/vertical swimming with sustained commitments; avoid repeated brief bursts followed by complete stops. Safety braking remains authoritative. The approved [correction design](superpowers/specs/2026-10-08-combat-region-swimming-design.md) is implemented on the local flight branch; [static-player physical acceptance](COMBAT-REGION-SWIMMING-VERIFICATION.md) is separate from pending user visual acceptance.

### Feints

Feints must communicate an intention first, then betray it.

`TELEGRAPH -> COMMIT -> BETRAYAL -> ACTION -> BRAKE -> RETURN`

Initial recipes include:

- False Approach;
- False Retreat;
- Left-Right / Right-Left;
- Vertical Fake;
- Pass-By Fake;
- rare Double Fake;
- Abort Fake.

Do not implement every behavior as a monolithic Goal. Movement, attack and timing should stay separable.

## 5. Confirmed projectile design

Baseline:

### Standard Soutou Fireball

- visually larger than vanilla;
- slightly longer charge;
- firing face marks charge;
- firing face remains briefly after launch;
- modest predictive aim;
- gameplay explosion roughly 1.5x vanilla baseline;
- deflection interaction area larger than vanilla.

### Special profiles

- Delayed Burst — timing pressure;
- Curve — lateral pressure, non-homing;
- Lob — cover / ground-position pressure.

On valid player deflection, special fireballs can normalize into a common readable rally state.

### Fireball Rally

Returned fireballs are an explicit mechanic.

Soutou Ghast may show the firing face and return the reflected projectile.

Rarely it may feint the return and dodge instead.

## 6. Environment-aware behavior

Maintain a smoothed Mobility Context:

- OPEN_AIR
- SEMI_OPEN
- CONFINED
- GROUND_FORCED

Open air favors large arcs, long feints, pass-bys and rare camera-disruption moves.

Confined space must have its own positive behavior set rather than merely disabling air maneuvers.

If ceiling/clearance makes flight unsuitable, Soutou Ghast may intentionally enter Ground Combat Mode.

Ground identity:

- fast scuttling;
- abrupt lateral movement;
- lower per-hit damage;
- higher projectile frequency;
- short-range feints;
- occasional intentionally irritating / taunting passes.

## 7. Three major arts

### 1. Overhead Bombing

Confirmed concept:

- withdraw far enough to look like retreat;
- rapidly return;
- acquire the player's exact overhead X/Z axis;
- rotate/facing downward;
- maintain a tight overhead lock;
- release bombs at intervals;
- bombs commit to the release-time impact point rather than homing forever;
- after the sequence, release lock and return to the normal Combat Anchor.

The player looking straight up should consistently see Soutou Ghast above them and looking downward.

### 2. Grand Danmaku

Confirmed system concept:

- Soutou Ghast becomes a stable visual center at a player-relative distance;
- ordinary dodge behavior is suspended during the performance;
- damaging projectiles visibly originate from Soutou Ghast;
- patterns become beautiful through travel rather than appearing fully formed in empty space;
- the player receives a ranged-attack opportunity while also dodging;
- visual radius and damage hit radius should be separate;
- final pattern content is **not designed yet**.

### 3. Domain Expansion

Domain Expansion remains the third major art.

It changes the arena / locomotion rules instead of merely adding more projectiles.

State concept:

`AIR -> DOMAIN_START -> LANDING -> GROUND -> DOMAIN_COMBAT -> DOMAIN_END -> TAKEOFF -> AIR`

The standalone domain should use the reusable JujutsuCraft-inspired architecture already researched in KNEEKURA-TECH-HUB:

- controller / owner / center / radius;
- lifecycle;
- reversible world overlay;
- restoration ledger;
- barrier/interior roles;
- participant tracking;
- clash / domain-attack policy.

Future JujutsuCraft compatibility should be an adapter, not a hard dependency.

## 8. Major-action scheduling

Major arts should be handled by a dedicated Major Action Director.

Do not chain them continuously.

Expected rhythm:

`NORMAL COMBAT -> MAJOR TELEGRAPH -> MAJOR ACTION -> RECOVERY -> NORMAL COMBAT`

Recent major actions should receive strong repetition penalties.

## 9. Implementation priorities for Codex

Recommended order:

1. **Do not extend the old v0.02 combat phases.**
2. Establish the new entity/combat state boundary and Flight Controller.
3. Preserve observable-only Perception; Player facing cannot move the anchor.
4. Correct Combat Anchor Volume to a persistent broad world-space region; preferred range is soft.
5. Correct normal swimming/commitments, then extend movement primitives and environment / Mobility Context.
6. Implement Tactical Evaluator + short-term repetition memory + Commit Points.
7. Add Standard Fireball and rally behavior.
8. Add Delayed Burst / Curve / Lob through reusable projectile profiles.
9. Add feint recipes by composing movement + timing + attack slots.
10. Add Overhead Bombing major-art controller.
11. Keep Grand Danmaku runtime isolated; build renderer/network/collision adapters, but **do not invent final danmaku content**.
12. Rebuild Domain Expansion around the new controller / reversible overlay architecture.
13. Only after static/unit checks are stable, perform Minecraft runtime / LAB verification.

## 10. Grand Danmaku runtime implementation boundary

Draft PR #1 is useful infrastructure, not finished gameplay.

Next technical adapters may include:

- `SoutouGhastGrandDanmakuController`
- server collision adapter
- BurstDescriptor network adapter
- client VirtualBullet cache
- batched renderer
- LAB telemetry

But the actual pattern Score remains pending user design.

## 11. Verification policy

Follow the KNEEKURA / TECH-HUB evidence discipline:

- source / bytecode / exact project history before community claims;
- Minecraft 1.20.1 / Forge target only;
- static/unit verification before expensive runtime work;
- record failures and repairs rather than deleting history;
- do not claim runtime behavior from pure-Java tests;
- do not silently treat test presets as approved design.

## 12. Current completion summary

### Designed

- overall boss identity;
- new air-AI philosophy;
- Combat Anchor Volume;
- preferred range / readable telegraphs without camera-following region ownership;
- inertia-heavy Flight Controller architecture;
- feint grammar;
- environment adaptation;
- Ground Combat identity;
- Standard / Delayed / Curve / Lob projectile concepts;
- Fireball Rally;
- Overhead Bombing major art;
- Grand Danmaku system requirements;
- Domain Expansion direction;
- major-action scheduling.

### Implemented as reusable scaffold

- legacy v0.02 snapshot + audit;
- isolated Grand Danmaku pure-Java runtime scaffold in Draft PR #1;
- JavaFX Score authoring technology in TECH-HUB.

Local flight-foundation branch contains a non-attacking inertia/clearance scaffold and the2026-10-08 persistent-region/swimming correction (`COMBAT-REGION-SWIMMING-VERIFICATION.md`). The earlier camera-relative static-player receipt stays historical. User visual acceptance precedes further tactics. NaturalGhast Tank preparation excludes unintended seed Reimu; explicit subject experiments remain separate.

### Not implemented / not finished

- the redesigned normal combat AI;
- final Flight Controller behavior and native smooth-swimming acceptance;
- new movement/feint/tactical system;
- new fireball family;
- rally gameplay;
- Overhead Bombing;
- Minecraft-connected Grand Danmaku renderer/network/collision;
- **final Grand Danmaku pattern/content**;
- rebuilt Domain Expansion;
- final runtime balancing / performance / multiplayer verification.

The project is therefore **design-rich but implementation-early**. Codex should treat the redesign document and this handoff as the target, not the legacy v0.02 behavior.
