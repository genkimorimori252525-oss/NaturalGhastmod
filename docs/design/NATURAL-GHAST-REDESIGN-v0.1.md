# Natural Ghast redesign — current design

Revision: v0.3  
Date: 2026-10-06

## Core identity

- Non-natural-spawning boss-class special Ghast; no boss bar.
- External armor is visual-only and has no combat stats.
- Air-combat AI may be rewritten from scratch.
- Legacy attacks are retired except Domain Expansion; existing voice, sound, particle, texture and animation assets may be reused.
- The goal is not a perfect machine. Soutou Ghast should look intentional, deceptive, smooth, readable enough to learn, but variable enough to avoid becoming a fixed routine.

## Air-combat foundation

Normal combat uses a player-relative **Combat Anchor Volume** instead of constant orbiting.

The anchor is not a fixed world coordinate and not a single exact point. It is a preferred three-dimensional combat region expressed relative to the player.

The volume combines:
- approximate range;
- approximate altitude;
- preferred bearing;
- horizontal freedom;
- vertical freedom;
- a soft preferred point inside the region.

The volume follows meaningful player displacement but does not continuously spin around the player.

Temporary maneuvers such as dodge, feint, pass-by, attack reposition and emergency movement normally preserve the same anchor volume and return to it afterward.

Typical lifecycle:

`ANCHOR VOLUME -> TEMPORARY MANEUVER -> BRAKE -> RETURN -> ANCHOR VOLUME`

After avoiding an attack, Soutou Ghast should often return to roughly the same frontal combat region as if nothing happened.

## Frontal-view combat volume

The preferred combat region should normally stay within the player's **front-facing field of view**.

The purpose is not to lock Soutou Ghast to the center of the screen. The purpose is to prevent the fight from becoming a camera-search exercise where the player must constantly spin around to find the boss.

The frontal volume should therefore have:
- a preferred horizontal sector around the player's forward direction;
- a preferred vertical sector that allows visible altitude changes;
- the normal preferred range band;
- enough width for drifting, strafing, dodging and feints.

Soutou Ghast may temporarily leave the frontal volume for a meaningful maneuver, but should usually recover back into it afterward.

The player's instantaneous look vector must not directly drag the volume every tick. Use a **smoothed combat-facing reference** that follows sustained changes in player orientation rather than tiny camera motions.

Concept:

`PLAYER LOOK -> SMOOTHED COMBAT FACING -> FRONTAL ANCHOR VOLUME`

Candidate movement points should receive a preference bonus for remaining inside or near this frontal volume, not an absolute hard constraint.

This preserves:
- visibility of feints;
- visibility of attack-face telegraphs;
- readable fireball rally interactions;
- reduced need for constant camera rotation.

The target experience is: Soutou Ghast feels mobile and evasive, but the player can usually keep watching it.

## Preferred range band

Soutou Ghast should actively try to preserve a **comfortable combat-distance band**, not one exact distance.

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

Once the temporary reason ends, the AI should generally recover toward its normal band and frontal Combat Anchor Volume.

## Flight feel

Motion should be smooth and inertia-heavy.

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
- RETURN — return toward the frontal Combat Anchor Volume
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

Most feints should be designed to remain visible within the frontal combat space. The player should be deceived by the movement, not lose track of the entity entirely.

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
- slight movement within the frontal anchor volume;
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
