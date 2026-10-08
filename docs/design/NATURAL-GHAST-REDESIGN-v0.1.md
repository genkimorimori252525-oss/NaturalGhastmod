# Natural Ghast redesign — current design

Revision: v0.6
Date: 2026-10-08
Updated: 2026-10-09 (natural-flow relocation, observed Dodge, remembered-cover Lob and bounded terminal subdivision)

The user's2026-10-08 clarification supersedes v0.5's player-camera-relative anchor. A combat anchor is a broad, boss-owned world-space region, retained during combat and reselected only when necessary. The user has explicitly accepted its current swimming and authorized further non-danmaku development. Separate controlled camera/moving-Player/LOS scenarios remain limited as recorded. See [the correction design](../superpowers/specs/2026-10-08-combat-region-swimming-design.md), [verification receipt](../COMBAT-REGION-SWIMMING-VERIFICATION.md) and [next tactical unit](../superpowers/plans/2026-10-08-observed-tactics.md). Grand Danmaku implementation is excluded by the current user instruction, including runtime adapters.

## Core identity

- Non-natural-spawning boss-class special Ghast; no boss bar.
- External armor is visual-only and has no combat stats.
- Air-combat AI may be rewritten from scratch.
- Legacy attacks are retired except Domain Expansion; existing voice, sound, particle, texture and animation assets may be reused.
- The goal is not a perfect machine. Soutou Ghast should look intentional, deceptive, smooth, readable enough to learn, but variable enough to avoid becoming a fixed routine.

## Air-combat foundation

Normal combat uses a persistent **Combat Anchor Volume** instead of constant orbiting.

Soutou Ghast chooses a broad three-dimensional region in world space. It retains that region during combat, with freedom to swim horizontally and vertically. The center is a reference for the region, not a mandatory return point. Player yaw/pitch must never translate or rotate it; ordinary Player movement must not drag it every tick.

Initial development size in open space: approximately 40 blocks wide, 16 high and 40 deep (ellipsoid radii20/8/20). This is tuning, not final balance. Do not substitute a roughly10-block home region. Body clearance and terrain constrain feasible routes, while confined-space adaptation is an explicit context change.

Reselection requires an explicit reason, such as sustained loss of usable space/LOS, substantial disengagement, target replacement or a committed tactical relocation. Use observation, persistence and cooldowns to avoid oscillation; a Player turning away is not a reason.

Temporary maneuvers such as dodge, feint, pass-by, attack reposition and emergency movement normally preserve the same anchor volume and return to it afterward.

Typical lifecycle:

`ANCHOR VOLUME -> TEMPORARY MANEUVER -> BRAKE -> RETURN -> ANCHOR VOLUME`

After avoiding an attack, Soutou Ghast should often resume swimming within the same retained region as if nothing happened.

First observed Dodge implementation uses bounded visible projectile motion, fallible same-region lateral evasion and ordinary recovery through the sole controller. Initial damaging-class coverage/tuning and controlled native evidence are recorded in [the Dodge receipt](../OBSERVED-DODGE-VERIFICATION.md); human readability and full class coverage remain open. Do not replace normal swimming with constant perfect interception or use the boss's own fireball family to bypass Rally rules.

## Combat-region ownership and readability

`OBSERVED COMBAT GEOMETRY -> BOSS REGION SELECTION -> RETAINED WORLD-SPACE REGION`

Readable telegraphs remain important, but they must not make the boss chase the Player's camera. Player-facing direction can inform observable tactical choices or telegraphs; it cannot define the retained region, its return destination or continuous idle steering.

Temporary maneuvers may leave the region and recover into it. Normal swimming can occupy a broad part of it rather than circling an exact point or repeatedly correcting to the screen center. Deliberate region transfers are separate committed actions.

## Preferred range band

Soutou Ghast should prefer a **comfortable combat-distance band**, not one exact distance. The initial22–34-block band is a soft tactical preference within the retained region; crossing it must not force immediate region relocation or cancel ordinary swimming.

Reason:
- if it stays too close, ranged attacks and reflected fireballs leave too little reaction time;
- if it stays too far away, the fight becomes slow and disconnected;
- a range band gives the Flight Controller space to dodge, brake, curve and recover without forcing constant retreat.

The target is therefore:

`TOO CLOSE -> open distance`

`COMFORTABLE BAND -> hold / drift / attack / feint`

`TOO FAR -> close distance`

The preferred distance must contain controlled variation. Soutou Ghast should not mechanically snap back to an exact radius every time.

The range band may shift temporarily for:
- a chosen maneuver;
- terrain or line-of-sight problems;
- Domain setup;
- a specific future attack;
- recovery after a high-speed action.

Once the temporary reason ends, the AI should generally recover into its retained Combat Anchor Volume, considering range without forcing a fixed radius.

## Flight feel

Motion should be smooth and inertia-heavy.

Normal HOLD/quiet periods allow gentle physical floating and swimming. Use sustained, smoothly changing low-speed horizontal and vertical intent through the same Flight Controller; repeated short bursts followed by full stops must not be the default idle rhythm. Emergency collision braking remains authoritative. Visual-only bobbing cannot substitute for actual flight motion.

Soutou Ghast can intentionally accelerate, brake, turn, dodge and return to position. Braking is possible, but weaker/slower than acceleration.

Core principle:

`acceleration authority > braking authority`

High-speed turns require broader arcs. Tactical intent must not directly overwrite velocity.

Architecture:

`Brain -> Movement Intent -> Maneuver -> Flight Controller -> Actual Motion`

The Flight Controller owns:
- acceleration;
- braking;
- turning authority;
- momentum;
- collision avoidance;
- arrival correction.

## Movement primitives

Complex actions should be composed from reusable primitives rather than one-off hard-coded goals.

- HOLD — remain within the current Combat Anchor Volume
- DRIFT — low-acceleration movement within or around the volume
- APPROACH — close distance
- WITHDRAW — open distance
- STRAFE — lateral movement
- RISE — gain altitude
- DROP — lose altitude
- BRAKE — intentionally reduce velocity
- BURST — short strong acceleration
- CURVE — inertia-preserving turn
- RETURN — recover into the retained Combat Anchor Volume, not its exact center
- OVERSHOOT — intentionally or physically pass the desired position

## Feint system

A feint is not random movement. Soutou Ghast first makes a readable intention visible, gives the player time to believe it, then betrays that expectation.

Generic structure:

`TELEGRAPH -> COMMIT POINT -> BETRAYAL/REVEAL -> MAIN ACTION -> BRAKE -> RETURN`

Initial Feint Recipes:

- False Approach — slow approach, then quick withdrawal
- False Retreat — retreat far enough to look disengaged, then burst back in
- Left-Right Fake — show rightward movement, brake, then redirect left
- Right-Left Fake — mirrored version
- Vertical Fake — rise then drop, or drop then rise
- Pass-By Fake — appear committed to passing the player, then alter the route
- Double Fake — e.g. right -> left -> right; rare
- Abort Fake — begin something that resembles a feint, then simply return to normal behavior

Normal HOLD/DRIFT/reposition movement must remain common so that not every movement automatically signals a feint.

Most feints should use readable motion and telegraphs within the combat space. The player should be deceived by the movement rather than repeated unexplained region relocation; this does not authorize camera-following steering.

## Movement, attack and timing are separate

Do not implement monolithic actions such as `FalseRetreatAttack`.

Use:

`Movement Pattern + Attack Pattern + Timing Slot`

Possible timing slots:
- BEFORE_TELEGRAPH
- BEFORE_COMMIT
- ON_COMMIT
- AFTER_REVEAL
- WHILE_PASSING
- WHILE_RETREATING
- ON_RETURN
- AT_ANCHOR

This allows future attacks to be reused across many maneuvers.

## Standard Soutou Fireball

The normal fireball is the baseline attack and should still feel recognizably Ghast-like.

Design:
- visually larger than a vanilla Ghast fireball, roughly 1.4-1.6x as a starting point;
- a slightly longer charge/telegraph than vanilla;
- “charge” begins when the Ghast face changes to its firing expression;
- after the projectile is fired, the firing expression remains for about 1 second before returning to normal;
- basic trajectory remains mostly straight, with modest predictive aim allowed;
- explosion strength is approximately 1.5x the vanilla Ghast baseline in gameplay terms.

Do not force the 1.5x concept into vanilla integer `explosionPower`. The final implementation should be free to separate entity damage, knockback, fire creation and terrain damage if needed.

## Deflection hit area

Vanilla Ghast fireballs are too unforgiving to hit back reliably.

Soutou Ghast fireballs should therefore have a larger **deflection/interaction hit area**.

Prefer separating:
- ordinary collision / impact volume;
- visual size;
- melee deflection interaction radius.

This avoids making the projectile collide with walls too early simply because it is easier to hit with a weapon.

Initial tuning target for deflection radius: around 1.5-1.75x the vanilla interaction scale, subject to runtime testing.

## Fireball Rally

Returned fireballs are an explicit combat mechanic.

Flow:

`SOUTOU SHOT -> PLAYER DEFLECT -> INCOMING RETURN -> SOUTOU REACTION -> SOUTOU DEFLECT -> PLAYER`

When Soutou Ghast decides to return a reflected fireball:
- it detects the incoming reflected projectile;
- its face changes to the same firing expression used for normal attacks;
- that expression acts as a readable telegraph that it intends to return the shot;
- the rally-return telegraph is much shorter than a normal firing charge because the projectile is already incoming;
- if timing succeeds, Soutou Ghast reflects the projectile back.

The firing face therefore means more broadly **“actively applying attack force / interacting offensively with a fireball”**, not only “creating a new fireball”.

Rally reaction should not be perfect. Soutou Ghast may fail to return a shot and be hit.

A rally may gradually become faster, with a sensible cap, so repeated successful returns create escalating pressure without becoming impossible.

## Rally feint

The firing expression is usually a trustworthy tell during a rally — but not always.

Rarely:

`PLAYER DEFLECT -> SOUTOU FIRING FACE -> PLAYER EXPECTS RETURN -> SOUTOU DODGES INSTEAD`

The incoming fireball then passes by or impacts elsewhere.

This works only if ordinary rally returns are common enough for the player to learn the firing face as a meaningful signal first.


## Special Ghast-fireball profiles

The first special projectile set should remain recognizably derived from Ghast fireballs. These are not three stronger versions of the Standard Soutou Fireball. Each one exists to force a different kind of defensive decision.

Shared rule:
- Standard Fireball remains the damage/presentation baseline.
- Special profiles should generally stay near the Standard Fireball's damage and explosion budget unless a later design explicitly changes it.
- Their strength comes from timing, trajectory and space control rather than raw damage escalation.
- The firing face remains the common attack telegraph.
- Projectile-specific cues appear after or around launch so the player can learn the difference.
- A valid melee deflection may transition a special projectile into a common `RALLY_NORMALIZED` state for clear rally play: enlarged deflection interaction, straightened return behavior and shared rally-speed cap. This prevents every special projectile from requiring a completely different rally rule.

### Delayed Burst Fireball

**Combat role:** break the player's dodge timing.

This projectile begins slower than the Standard Fireball and then performs a short, visible acceleration burst.

It should create the thought:

> "I already started dodging this."

and then punish an early or lazy dodge without becoming an unavoidable instant-speed projectile.

#### Flight phases

`SLOW TRAVEL -> PRE-BURST CUE -> ACCELERATION BURST -> FAST TRAVEL`

Candidate tuning direction:
- initial speed: roughly 55-75% of Standard Fireball;
- slow phase: roughly 0.6-1.2 seconds with controlled variation;
- pre-burst warning: brief, roughly 0.15-0.30 seconds;
- post-burst speed: roughly 1.5-1.9x Standard Fireball;
- acceleration should occur across several ticks rather than one velocity teleport.

Exact values remain runtime-tuning parameters.

#### Telegraph

The launch uses the normal firing face and normal charge grammar.

The projectile itself communicates the coming burst:
- glow/intensity increases;
- particles visually compress or pull inward;
- a short rising or tightening sound cue may be reused/created;
- then the projectile accelerates.

The burst cue must remain readable enough that an attentive player can perform a late dodge.

#### Aim

Delayed Burst should not home after launch.

Its launch vector may use the same modest predictive aiming available to the Standard Fireball, but once fired the projectile commits to its path.

This keeps the challenge about **timing**, not invisible steering.

#### Range rules

Do not prefer Delayed Burst at point-blank range.

The AI should require enough travel distance for:
- the slow phase;
- the warning cue;
- a meaningful reaction window after the burst.

If Soutou Ghast is too close, Standard Fireball, withdrawal or another action should score higher.

#### Maneuver connections

Good pairings:
- HOLD + Delayed Burst;
- DRIFT + Delayed Burst;
- False Approach + Delayed Burst;
- False Retreat + Delayed Burst.

A strong example:

`SLOW APPROACH -> FIRE -> WITHDRAW -> PROJECTILE SLOW PHASE -> BURST -> RETURN`

The body appears to disengage while the projectile becomes more dangerous.

Avoid stacking this with another major deception at the same moment. The burst itself should usually be the primary surprise.

The committed Lob recipe can add one interpolated point to its last line segment before full-height floor contact, preserving every original point and the exact endpoint. This adds one trajectory tick; it preserves the spatial arc rather than identical timing. The unchanged full-path/native-ray validator, speed bound,97-point cap and four exact terminal colliders still apply. Unrefinable paths remain subject to rejection. One finite integer-endpoint trial proves the declared stone impact, native explosion and END removal; production Goal/human/readability/native-negative scope remains open. [Terminal plan](../superpowers/plans/2026-10-09-lob-terminal-subdivision.md)/[receipt](../LOB-TERMINAL-VERIFICATION.md).

#### Deflection

If the player deflects before the burst, the return should be rewarded rather than invalidated.

Preferred first-pass rule:
- the projectile enters `RALLY_NORMALIZED` on valid deflection;
- its special burst steering/timer is cancelled;
- the reflected ball travels as a readable rally projectile under the shared rally speed cap.

This keeps Fireball Rally consistent and prevents a hidden post-deflection burst from becoming unfair.

---

### Curve Fireball

**Combat role:** attack lateral movement and alter the expected line of approach.

Curve Fireball is not homing.

It follows a precommitted curved trajectory whose lateral direction is chosen at launch.

The player should be able to learn:

> "This one is coming around from the side."

rather than:

> "This projectile magically followed me."

#### Flight shape

Conceptually:

`LAUNCH -> OUTWARD / LATERAL ARC -> INWARD SWEEP -> COMMITTED EXIT`

The curve should be smooth and continuous.

It must not:
- snap direction;
- reverse itself multiple times;
- U-turn behind the player;
- retarget every tick.

The projectile's path should remain physically legible.

#### Curve strength

Use multiple soft profiles rather than arbitrary steering:
- SHALLOW — small lateral bend;
- NORMAL — default combat curve;
- DEEP — wider arc used only when space allows.

The AI selects a profile based on Mobility Context and available clearance.

#### Telegraph

The firing face remains normal.

After launch, the player should be able to identify the intended curve side through one or more of:
- asymmetric particle trail;
- slight projectile roll/spin cue;
- curved ember trail;
- initial launch angle that visibly opens toward the arc.

The left/right version should be learnable before the projectile reaches the player.

#### Tactical selection

Curve direction should consider:
- free space;
- walls;
- the player's recent strafe tendency;
- current Combat Anchor side;
- other active projectiles.

However, it should not perfectly counter the player every time.

The AI may deliberately choose the less-optimal side sometimes to retain uncertainty and avoid looking omniscient.

#### Maneuver connections

Good pairings:
- STRAFE + Curve;
- HOLD + Curve;
- Left-Right Fake + Curve;
- Right-Left Fake + Curve.

A particularly useful grammar is:

`BODY MOVES RIGHT -> CURVE PROJECTILE ENTERS FROM LEFT`

but this should be used sparingly because it contains two simultaneous directional reads.

Often the clearer pairing is simply:

`LIGHT STRAFE -> CURVE FIREBALL -> RETURN/HOLD`

#### Environment rules

OPEN_AIR:
- NORMAL and occasional DEEP curves available.

SEMI_OPEN:
- Curve is especially valuable because it can use gaps and obstacle edges.
- Candidate arcs must be collision-tested before selection.

CONFINED:
- only SHALLOW or corridor-compatible arcs;
- never choose a curve profile whose required lateral space is unavailable.

GROUND_FORCED:
- not part of the default ground barrage set unless a future ground-specific adaptation is designed.

#### Deflection

On valid melee deflection, Curve Fireball enters `RALLY_NORMALIZED`.

The return becomes a straight, readable rally ball rather than preserving hidden lateral steering.

---

### Lob Fireball

**Combat role:** attack cover, stationary positions and the player's ground space.

Lob Fireball changes the vertical geometry of Ghast combat.

Instead of flying directly at the player, it climbs and then descends toward a locked landing/impact point.

This is a pseudo-ballistic projectile profile; it does not need to use vanilla gravity internally as long as the visible path behaves like a coherent arc.

#### Flight phases

`ASCEND -> APEX -> DESCEND -> IMPACT`

The target impact point is selected and locked when fired.

The projectile does not continuously retarget the player after launch.

A modest predictive impact point may use the player's current velocity at launch.

#### Why target locking matters

The counterplay should be:

> "Move away from where it is going to land."

not:

> "Keep running because the falling projectile is secretly following me."

This also lets Lob act as a setup tool that pushes the player out of a preferred position.

#### Arc selection

The arc height depends on available vertical clearance.

OPEN_AIR:
- high, visually dramatic lob permitted.

SEMI_OPEN:
- lower arc chosen to clear known obstacles when a safe route exists.

CONFINED:
- use only when a verified vertical corridor and landing path exist;
- otherwise suppress Lob entirely.

GROUND_FORCED / low ceiling:
- do not select the normal Lob profile.

The AI should test the intended arc before firing rather than allowing the projectile to immediately hit the ceiling.

#### Telegraph

The attack should be obvious once launched:
- visibly upward launch angle;
- persistent ember/smoke trail that exposes the arc;
- a distinct apex/descending audio cue if useful;
- stronger glow while descending may help the player relocate the threat.

A game-like ground marker is not required for the first design. The projectile's visible arc should provide the primary information.

#### Damage

Lob should initially use approximately the Standard Fireball explosion budget.

Its advantage is trajectory and area displacement, not greater raw explosion damage.

#### Maneuver connections

Good pairings:
- HOLD + Lob;
- slow DRIFT + Lob;
- light reposition after launch.

The body should usually remain relatively readable while the projectile performs the large motion.

Example:

`HOLD -> LOB LAUNCH -> PROJECTILE ASCENDS -> SOUTOU SMALL STRAFE -> PLAYER MOVES FROM IMPACT AREA -> NEXT DECISION`

Lob can later become a setup attack for another maneuver, but the follow-up should not be guaranteed or immediate every time.

Sometimes Soutou Ghast should simply watch the player move and return to normal combat.

#### Cover behavior

Lob receives extra tactical value when:
- direct line of fire is blocked but a valid overhead arc exists;
- the player remains near one piece of cover;
- the player repeatedly holds a stationary firing position.

It should not magically pass through roofs. If there is no viable ballistic corridor, the attack is invalid.

Initial remembered-cover implementation uses only the same target's last visible eye/landing snapshot. It permits one attempt per LOS-loss episode within10actual game ticks, requires loaded terrain obstruction toward that stored eye and a fully validated Lob arc, and expires after40ticks including launch. A lost visible charge restarts the complete cover tell; freeze the remembered endpoint/recipe at admission, emit the distinct cue at19 and launch at30 from the actual muzzle after full revalidation. Regained LOS, target/clock/context/ownership discontinuity or invalid geometry cancels; rejection never fires hidden Standard or refreshes hidden target coordinates. Existing variation and600tick Lob repetition penalty remain authoritative. [Implementation plan](../superpowers/plans/2026-10-09-remembered-cover-lob.md)/[scoped native receipt](../COVER-LOB-VERIFICATION.md) distinguish actual native perception with declared fresh fixture geometry from production Goal/human hiding/readability acceptance.

#### Deflection

A player may still deflect a Lob Fireball if physically reachable.

On valid deflection, it enters `RALLY_NORMALIZED` and leaves the lob arc, becoming a straight rally projectile.

---

## Projectile selection grammar

The Tactical Evaluator should select projectile profiles by **combat problem**, not by random rotation.

| Combat problem | Preferred profile |
| --- | --- |
| Neutral ranged pressure | Standard Fireball |
| Player dodges too early / predictable timing | Delayed Burst |
| Player relies on lateral strafing / side lanes | Curve |
| Player hides behind cover / holds one area | Lob |
| Player returns a fireball | Rally state |

These are preference signals, not deterministic counters.

Short-term memory must reduce repeated use of the same profile.

A player who early-dodges once should not cause every future projectile to become Delayed Burst.

## Projectile cognitive-load rule

One action should normally contain only one dominant projectile trick.

Avoid combinations such as:

`DEEP CURVE + DELAYED BURST + DOUBLE SHOT + BODY DOUBLE FEINT`

even if the composition system technically supports them.

The design goal is readable deception, not unreadable complexity.

Recommended first-pass composition limits:
- one major movement deception plus a simple projectile; or
- one special projectile profile plus simple body movement;
- a second surprise only at low frequency.

## Fireball family hierarchy

```text
Ghast Fireball Family
|
+-- STANDARD
|   +-- baseline damage
|   +-- baseline telegraph
|   +-- baseline rally
|
+-- DELAYED_BURST
|   +-- timing pressure
|
+-- CURVE
|   +-- lateral pressure
|
+-- LOB
|   +-- vertical / cover pressure
|
+-- RALLY_NORMALIZED
    +-- common readable state after valid deflection
    +-- enlarged deflection interaction
    +-- straight return trajectory
    +-- shared rally speed cap
```

This keeps the family expandable. Future Ghast-derived projectile types should preferably solve a new combat problem instead of only changing damage or color.



## Major Art system

Soutou Ghast has three major actions. They are not ordinary attacks with larger damage numbers. Each one temporarily changes the combat grammar.

1. **Overhead Bombing** — a pursuit major art that forces movement and vertical attention.
2. **Grand Danmaku** — a stationary showcase / ranged-damage opportunity built around beautiful projectile geometry.
3. **Domain Expansion** — a ruleset-changing major art that replaces normal air combat with a dedicated domain/ground encounter.

Major actions should be scheduled by a dedicated **Major Action Director** rather than mixed into ordinary shot selection.

The director should consider:
- current Mobility Context;
- available clearance;
- recent major actions;
- current combat rhythm;
- whether the previous major action has fully recovered;
- whether the intended major action is physically readable in the current arena.

Major actions must not chain back-to-back without a meaningful return to ordinary combat.

A major action should normally follow:

`NORMAL COMBAT -> MAJOR TELEGRAPH -> MAJOR ACTION -> RECOVERY -> NORMAL COMBAT`

Repeated use of the same major action receives a strong short-term penalty.

### Major Art 1 — Overhead Bombing

**Core fantasy:** Soutou Ghast appears to disengage, suddenly returns at speed, captures the player's vertical axis, stares straight down at them and performs repeated bombing runs while remaining directly overhead.

#### Entry

The signature entry is:

`WITHDRAW FAR -> PLAYER READS RETREAT -> HIGH-SPEED RETURN -> OVERHEAD CAPTURE`

The withdrawal should be large enough to visibly read as disengagement, but it is still part of the attack.

The return may temporarily leave the retained Combat Anchor Volume because this is a deliberate major-art exception.

#### Overhead Axis Lock

Once the attack reaches its bombing state, Soutou Ghast enters a dedicated **Overhead Axis Lock**.

During this state:
- its horizontal X/Z axis is bound to the player's horizontal position;
- it keeps a dedicated bombing altitude above the player;
- the entity rotates/faces downward toward the player;
- ordinary Combat Anchor corrections are suspended;
- ordinary orbiting and dodge behavior are suspended.

The intended player experience is literal:

> if the player looks straight upward, Soutou Ghast is there and looking directly down at them.

The lock is allowed to be much tighter than normal inertia-driven flight. Reaching the lock still uses visible movement; the special lock begins only after the major art has successfully acquired the overhead position.

#### Bomb release

Bombs are released at a fixed or slightly varied cadence.

Each bomb should lock its target/impact point at release. The bomb itself does **not** continue to home horizontally after release.

Thus:

`GHAST FOLLOWS PLAYER -> BOMB RELEASES -> BOMB COMMITS TO RELEASE-TIME GROUND POSITION`

This creates the intended counterplay:
- keep moving to avoid successive impacts;
- optionally look upward and attack Soutou Ghast during the sequence.

Bomb damage, cadence and count remain runtime-tuning parameters. The major art should be dangerous through repeated pressure, not through unavoidable single-hit lethality.

#### Vulnerability

During the stable bombing phase, Soutou Ghast should generally not perform normal projectile dodges.

The player receives a real offensive choice:
- focus on movement and avoid the bombs;
- or risk looking upward / aiming upward to damage the boss.

#### Presentation

Each release may use the firing face as a readable micro-telegraph.

The body remains downward-facing so the player's upward view continuously reinforces the visual relationship between boss and target.

#### Exit

After the final bomb:

`FINAL RELEASE -> AXIS LOCK RELEASE -> BRAKE/CURVE -> RETURN TO COMBAT ANCHOR VOLUME`

Soutou Ghast returns to normal combat rather than immediately starting another major action.

#### Environment rules

Overhead Bombing requires:
- viable overhead clearance;
- enough height for the bombing altitude;
- a clear or mostly clear vertical bombing corridor.

OPEN_AIR strongly favors it.

SEMI_OPEN may allow it if a safe vertical route exists.

CONFINED and GROUND_FORCED normally suppress it.

---

### Major Art 2 — Grand Danmaku

**Core fantasy:** Soutou Ghast becomes an unmoving visual center and personally fires a large Touhou-like projectile composition that grows outward from its body into a beautiful pattern.

This is not a system that places finished bullets in empty space.

The central rule is:

> **Every gameplay projectile originates from Soutou Ghast and the pattern becomes beautiful through flight.**

#### Relative Stationary Lock

During Grand Danmaku, Soutou Ghast establishes a stable relative position at a suitable distance from the player.

It may translate through world space to preserve that player-relative relationship, but from the player's perspective it should appear almost perfectly stationary.

This creates a stable visual center for the composition.

Ordinary dodge behavior is suspended.

That is intentional: Grand Danmaku is also a major ranged-damage opportunity for the player.

#### Danmaku Coordinate Frame

Grand Danmaku uses a player-readable local coordinate frame.

- **origin:** Soutou Ghast's face/mouth firing region;
- **forward axis:** from Soutou Ghast toward the player;
- **right/up axes:** a stable orthogonal basis around that forward axis.

Pattern definitions operate in this frame.

This means the projectile choreography is designed to look coherent from the player's combat view while still existing as real world-space projectiles.

#### Source-integrity rule

Gameplay bullets may not appear fully formed in arbitrary empty space.

Every damaging danmaku projectile must:
1. spawn at or inside a small emission region attached to Soutou Ghast;
2. visibly leave the entity;
3. travel through world space;
4. create the larger pattern through direction, timing and curved/predefined motion.

Cosmetic particles may decorate trails or impacts, but they must not disguise newly spawned damaging bullets away from the boss.

The player should always be able to perceive:

`SOUTOU GHAST -> PROJECTILE EMISSION -> PATTERN BLOOMS`

#### Charge / prelude

Before the first projectile:
- Soutou Ghast becomes still;
- the firing face appears;
- particles/light may gather toward the face or mouth;
- an audio cue announces the start;
- no damaging bullets have yet appeared around the player.

The contrast between normal smooth motion and sudden stillness is itself part of the telegraph.

#### Pattern growth

Patterns are authored as **emission choreography**, not static geometry.

Examples:

##### Halo Bloom

A timed burst of bullets leaves the face in evenly distributed angular directions around the forward axis.

From the player's perspective the small cluster expands into a ring/halo centered on Soutou Ghast.

##### Twin Spiral

Projectile emission angle rotates over time.

Two or more emission arms rotate in opposite directions, creating counter-rotating spirals that visibly grow from the boss.

##### Petal Bloom

Emission direction oscillates while rotating, causing repeated arcs to form petal-like lobes.

The important visual feature is that each petal can be traced back to successive shots leaving Soutou Ghast.

##### Weave

Two low-density families are emitted with different phase offsets or rotation directions.

As they travel, the families cross visually and create a woven/lattice impression without bullets materializing at the crossing points.

##### Finale Bloom

A final broad burst or expanding ring marks the end of the performance.

The finale should remain readable and contain intentional escape lanes rather than becoming a screen-filling unavoidable wall.

These motifs are an initial vocabulary, not a mandatory fixed sequence.

#### Player-perspective beauty contract

Grand Danmaku is judged primarily from the player's combat view.

The design should therefore favor:
- Soutou Ghast remaining a visible compositional center;
- radial or rotational balance around the boss;
- clear color/brightness contrast between bullet families;
- visible motion trails that explain the trajectory;
- gradual growth from small source to large pattern;
- intentional negative space / safe lanes;
- no damaging projectiles spawning behind the player without having travelled there visibly;
- no invisible or misleading collision volume.

The desired reaction is:

> "It is beautiful, I can see how it is being drawn, and I still have to dodge it."

#### Real projectile rule

The visual composition must remain physically honest.

Danmaku bullets are world entities/projectiles, not a screen overlay.

Their trajectories may use deterministic curves, angular drift or other authored motion, but they should not secretly retarget the player's current position after emission unless a future explicitly telegraphed homing motif is designed.

#### Damage model

Danmaku bullets should generally deal low damage per projectile relative to the Standard Fireball.

They should normally:
- avoid large terrain explosions;
- avoid heavy block destruction;
- rely on density, pattern and movement pressure;
- use a visually forgiving collision relationship where the rendered bullet can be slightly larger than its damaging core if helpful for fair dodging.

The major art's threat comes from navigating the composition, not from one bullet deleting the player.

#### Attack opportunity

Soutou Ghast does not use normal evasive maneuvers during the main danmaku performance.

This creates deliberate tension:

`WATCH BULLETS AND DODGE`

versus

`LOOK AT BOSS AND TAKE THE FREE RANGED SHOT`

The player is rewarded for finding moments where both can be managed.

#### Danmaku score rather than one fixed script

Repeated uses should not replay one identical animation from start to finish.

Use a **Danmaku Score**:

`FIXED PRELUDE -> 2-3 SELECTED MOTIFS -> FIXED/RECOGNIZABLE FINALE -> RECOVERY`

Motifs may vary in:
- clockwise/counter-clockwise orientation;
- phase offset;
- density within bounded limits;
- motif order;
- safe-lane orientation.

The grammar remains recognizable while individual performances differ.

The score must obey the same short-term memory principles as normal AI so one motif combination is not repeated mechanically.

#### Cognitive-load rule

Grand Danmaku itself is already the major surprise.

During the main performance:
- no large body feints;
- no Overhead Re-anchor;
- no unrelated heavy fireball profiles;
- no simultaneous second major action.

The boss's stationary presentation and the projectile choreography should carry the scene.

#### Environment rules

Grand Danmaku requires enough clear volume between boss and player for the pattern to visibly develop.

OPEN_AIR strongly favors it.

SEMI_OPEN may allow reduced-width motif sets if collision sampling verifies the necessary space.

CONFINED normally suppresses the full Grand Danmaku rather than truncating it into an ugly or unreadable version.

GROUND_FORCED uses the separate low-damage/high-frequency ground barrage identity, not Grand Danmaku.

#### Exit

After the final motif:
- damaging emission stops;
- existing projectiles continue their valid trajectories / expire according to their own rules;
- the firing face may remain briefly;
- Relative Stationary Lock releases;
- Soutou Ghast returns to normal Combat Anchor Volume behavior.

---

### Major Art 3 — Domain Expansion

Domain Expansion is the third major action.

Unlike Overhead Bombing and Grand Danmaku, it does not primarily change projectile behavior. It changes the combat arena and locomotion rules.

Its detailed world/barrier architecture remains defined in the Domain Expansion section below.

At the Major Action Director level its identity is:

`AIR COMBAT -> DOMAIN CREATION -> LANDING -> DOMAIN/GROUND COMBAT -> DOMAIN RESTORE -> TAKEOFF -> NORMAL AIR COMBAT`

Domain Expansion should also obey major-action spacing and repetition memory.

It must not begin immediately after another major action unless a future explicit exceptional transition is deliberately designed.


## Perception and short-term memory

The AI may use observable information such as:
- player position;
- player velocity;
- player facing/look direction;
- smoothed combat-facing direction;
- distance;
- line of sight;
- local terrain;
- hostile projectiles;
- position within/outside the Combat Anchor Volume;
- recent player movement;
- recent Soutou Ghast maneuvers;
- recent attacks.

A limited reaction history may influence action weights. Do not read raw player inputs or give the AI impossible knowledge.

Recent maneuvers should temporarily reduce their own selection weight so the same feint or attack is not spammed.

## Non-robotic behavior

Avoid both fixed loops and pure randomness.

Use context-aware candidate scoring with bounded variation.

Allow controlled imperfections:
- slight timing variation;
- small preferred-range variation;
- gentle swimming throughout the broad retained anchor volume;
- occasional overshoot;
- curved recovery;
- minor final-position correction;
- pre-commit action cancellation;
- brief stillness after intense motion.

Commit Points prevent the AI from changing its mind every tick and looking jittery.

## Combat rhythm

Soutou Ghast should have quiet moments.

Typical rhythm:

`MOTION -> RECOVERY -> BRIEF STILLNESS -> NEXT INTENT`

A short irregular HOLD after returning to the Combat Anchor Volume helps emphasize the “as if nothing happened” personality.

## Candidate air-AI architecture

```text
Soutou Ghast Brain
|
+-- Perception
|   +-- Player tracking
|   +-- Smoothed combat-facing reference
|   +-- Preferred range band
|   +-- Frontal-view volume
|   +-- Line of sight
|   +-- Projectile detection
|   +-- Terrain awareness
|   +-- Recent player reactions
|
+-- Short-Term Memory
|   +-- Recent maneuvers
|   +-- Recent attacks
|   +-- Recent reactions
|
+-- Combat Anchor Volume
|   +-- Preferred range
|   +-- Preferred altitude
|   +-- Preferred bearing
|   +-- Horizontal band
|   +-- Vertical band
|   +-- Soft preferred point
|
+-- Tactical Evaluator
|   +-- Hold
|   +-- Distance correction
|   +-- Frontal-volume correction
|   +-- Dodge
|   +-- Reposition
|   +-- Feint
|   +-- Attack
|   +-- Rally
|   +-- Domain
|
+-- Maneuver Composer
|   +-- Movement Primitive
|   +-- Feint Recipe
|   +-- Attack Slot
|   +-- Timing Slot
|   +-- Recovery
|
+-- Flight Controller
    +-- Acceleration
    +-- Braking
    +-- Turning
    +-- Momentum
    +-- Collision avoidance
    +-- Arrival correction
```


## Environment-aware combat adaptation

Soutou Ghast must adapt its combat style to the amount and shape of usable space around itself and the player.

The environment must not be treated as a simple difficulty penalty. A confined arena should unlock different behaviors rather than merely disabling open-air behaviors.

Before selecting maneuvers, the AI should maintain a lightweight **Mobility Context** derived from local collision/clearance samples, line-of-sight corridors, ceiling height, horizontal escape space and vertical clearance.

Suggested coarse context classes:

- OPEN_AIR — large horizontal and vertical clearance;
- SEMI_OPEN — enough room for normal flight, but with meaningful walls/terrain;
- CONFINED — limited lateral/vertical freedom, such as caves, corridors or rooms;
- GROUND_FORCED — insufficient clearance for viable air combat.

These are tactical contexts, not rigid biome or dimension labels.

### OPEN_AIR behavior

Open air unlocks the largest maneuvers.

Examples:
- broad curved dodges;
- long False Retreat / Burst Approach sequences;
- large vertical fakes;
- high-speed pass-by maneuvers;
- rare camera-disruption maneuvers.

A notable rare behavior is **Overhead Re-anchor**:

1. From its current swimming position and momentum, Soutou Ghast gently rises high; it need not return to the region center or stop first.
2. It faces its travel direction while passing high above the observed Player position.
3. Only after reaching behind the Player does it turn toward that visible same Player.
4. It gently descends toward a frozen broad candidate region selected from observed geometry.
5. On entering that region's upper height/volume band, commit it and resume ordinary combat with momentum preserved. The center is a reference, not a mandatory arrival/stop point.

This2026-10-09 user clarification supersedes earlier Player-facing transit or final braked-center behavior. [Current relocation implementation/evidence](../OVERHEAD-REANCHOR-VERIFICATION.md) retains historical receipts separately.

This maneuver should be low-frequency because the normal design goal is to keep the fight readable and reduce unnecessary camera spinning.

This is a deliberate tactical relocation, not continuous steering into or out of the Player's field of view.

After enough time, another committed maneuver may choose a different region; subsequent camera turns do not move either region.

### Camera-disruption budget

Deliberate off-screen tactical relocation should be treated as a limited resource. The Player voluntarily looking away does not consume that resource or require relocation.

The AI should track recent off-screen relocations and strongly reduce their likelihood after use.

This prevents repeated behind-the-player loops while allowing rare, memorable spatial deception.

### SEMI_OPEN behavior

Semi-open terrain should favor medium-scale flight:
- curved strafes around obstacles;
- short vertical changes;
- cover-to-cover repositioning;
- controlled pass-bys;
- fireball angles that exploit openings;
- explicit Combat Anchor reselection after sustained line-of-sight or usable-space problems, with hysteresis.

The AI should prefer movement that remains visually readable while using the terrain to alter approach angles.

### CONFINED behavior

Confined space should not make Soutou Ghast a degraded version of its open-air AI.

Large maneuvers that require unavailable clearance should be suppressed, but confinement-specific behaviors should become more likely.

Examples:
- rapid short-range left/right jukes;
- short brake-and-burst movement;
- tight vertical bobbing when ceiling room exists;
- wall-skimming movement;
- repeated front-of-player oscillation;
- intentionally useless-looking short passes used as taunts;
- short-range feints that stay within the same visible lane;
- compact projectile patterns designed for corridors or rooms;
- fast recovery into a nearby anchor volume instead of long arcs.

The result should feel like the same intelligence choosing a different fighting style for a different space.

### GROUND_FORCED fallback

If the ceiling is too low or collision clearance is insufficient for meaningful flight, Soutou Ghast may intentionally land and enter a **Ground Combat Mode** even outside Domain Expansion.

This is a fallback combat form, not a failure state.

Ground Mode should have its own identity:

- very fast, low-profile scuttling movement;
- abrupt but readable left/right changes;
- frequent zig-zagging and lateral crossing;
- strong use of floor space rather than vertical space;
- lower per-hit projectile damage;
- much higher projectile frequency / barrage density;
- short pauses followed by sudden bursts of movement;
- deliberate movement in front of the player to remain visible while being irritating.

The intended visual impression is insect-like / cockroach-like scuttling: fast, restless and difficult to pin down, without becoming random teleportation.

### Ground-mode taunts

Ground Mode may include low-value or intentionally non-optimal movement whose purpose is personality rather than damage.

Example:
- Soutou Ghast rapidly runs left-to-right and right-to-left directly in front of the player several times;
- no immediate positional advantage is gained;
- the movement acts as a visible taunt / provocation;
- it may or may not transition into a real attack afterward.

Such actions should remain rare enough to feel intentional and should be covered by the same short-term repetition memory as feints.

### Ground-mode barrage identity

Ground-mode projectile offense should contrast with air combat:

Air mode:
- stronger individual shots;
- larger spacing;
- deliberate telegraphs;
- stronger positional mind games.

Ground mode:
- lower individual damage;
- many more shots;
- lateral or fan-shaped barrages;
- movement and firing can overlap more often;
- pressure comes from volume and movement rather than single-hit power.

This gives the fallback mode a positive identity rather than making it merely “air combat without flight”.

### Environment-to-tactics rule

The Tactical Evaluator should first ask:

`WHAT SPACE IS ACTUALLY AVAILABLE?`

Only then should it score maneuvers that can physically and visually work inside that space.

Conceptually:

`Perception -> Mobility Context -> Candidate Maneuver Set -> Tactical Scoring -> Maneuver Composer -> Controller`

A maneuver that cannot fit the current clearance should not simply fail at runtime; it should normally be excluded or heavily penalized before selection.

### Environment transition

Mobility Context should be smoothed over time to avoid rapid AIR/GROUND or OPEN/CONFINED flicker at doorways and uneven terrain.

Use hysteresis / minimum dwell times:
- entering a more constrained state requires sustained evidence;
- returning to a more open state also requires sustained evidence;
- Ground Mode should not immediately take off again because of one transient clear sample.

### Design goal

The environment should change **how** Soutou Ghast is dangerous, not whether it is dangerous.

Open air emphasizes:
- space;
- momentum;
- perspective tricks;
- large feints.

Confined areas emphasize:
- pressure;
- compact jukes;
- lane control;
- irritating visible movement.

Ground-forced areas emphasize:
- scuttling;
- barrage density;
- short-range deception;
- taunting movement.


## Domain Expansion

Domain Expansion remains part of Soutou Ghast.

Useful legacy presentation ideas may remain:
- voice/audio;
- landing;
- stand/walk/run;
- recognizable domain presentation beats.

The world/barrier side should be rebuilt from the reusable Jujutsu Craft concepts already documented in KNEEKURA-TECH-HUB:
- Domain Controller;
- owner;
- center;
- radius;
- lifecycle;
- barrier mode;
- interior/floor roles;
- participant tracking;
- reversible world overlay;
- restoration ledger;
- clash state;
- domain attack policy.

The domain must work standalone without requiring Jujutsu Craft.

If Jujutsu Craft is installed later, a compatibility adapter may bridge Domain Clash, Domain Attack, barrier interaction and related semantics where feasible.

## Domain appearance

Exterior: a dark shell that feels light-absorbing rather than merely black.

Possible visual language:
- dark metal;
- faint red fissures;
- dim internal glow;
- subtle pulsing.

Interior: a fictional hostile hideout / underground military facility with clear ground-combat space rather than maze complexity.

## Domain state transition

`AIR -> DOMAIN_START -> LANDING -> GROUND -> DOMAIN_COMBAT -> DOMAIN_END -> TAKEOFF -> AIR`

Legacy stand/walk/run/landing assets from the recovered 1.0.0 generation are candidates for reuse.

Existing wall/ceiling traversal found in that generation is not yet confirmed for the redesign.

## Main design goal

The player should be able to think:

- “It did that on purpose.”
- “I thought it would do the same thing again.”
- “It looked like it was running away, then came straight back.”
- “It dodged and calmly went back to the same place.”
- “It stayed in front of me, but still moved enough to be annoying.”
- “It made the firing face, so I expected the rally return — and then it dodged.”
- “That was irritating, but readable.”

The AI succeeds when Soutou Ghast feels deceptive, smooth, alive and varied enough that repeated fights do not collapse into one obvious routine.

## Reference binary

Recovered latest-known reference JAR:
- file: `Soutou_Ghastmod-1.0.0.jar`
- size: `2,934,797 bytes`
- SHA-256: `5ad5fb4a3ff9a96095c60f87f5adff7c14cfdd3668954427b3396037f1471931`

It is a different generation from the older same-named JAR recorded in the v0.02 audit.
