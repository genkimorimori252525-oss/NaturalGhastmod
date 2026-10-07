# Standard Fireball and bounded rally

Base: `b393bf8`, authoritative redesign v0.6. User authorizes non-danmaku continuation and compact Astra rulings. Preserve accepted retained-region swimming, original world and accepted JAR. Execute inline with grouped checks and one whole-unit source review.

## Decisions

- Astra recommends a dedicated `Fireball` subclass, not `LargeFireball`, to avoid Ghast's private reflected-fireball1000damage branch. Keep Player attribution and ordinary damage bookkeeping. Only the origin boss's legitimately Player-deflected Standard bypasses its fire immunity. Provisional100HP/20returned damage/armor0; no boss bar. Vanilla LargeFireball compatibility remains deferred.
- Normal charge30ticks (vanilla20), lock direction for the final10ticks, keep firing expression20ticks after launch. Observation requires live LOS until launch; cancellation resets without firing. No tracking after launch. Modest observed-velocity lead is capped, never hidden target position.
- Presentation1.5x, collision1x1, pick radius1.5 independently. Float explosion radius1.5 with direct entity damage9 is development tuning; not a claim that every gameplay effect is1.5x. Preserve mobGriefing and Forge projectile/explosion hooks.
- At most one boss return per projectile. Incoming Player-deflected shots are evaluated only with loaded visible geometry, finite range/approach checks, a short6tick telegraph and fallible reaction. Aim locks at reaction start; no perfect interception/homing or target memory after LOS. Repeated Player deflection remains legal. No rally dodge feint until ordinary rally is validated.
- Preserve separate movement/look ownership. Attack supplies an optional locked look intent to the existing look controller; it never writes boss position or velocity. Do not stack a new shot telegraph onto an active movement feint. No legacy phase/variant goals reactivated.

## Coherent tasks and gates

- [ ] Read genuine Forge projectile/spawn/impact and damage sources; implement pure charge/rally state with RED→GREEN boundary/LOS/lock/cooldown/finite reaction tests.
- [ ] Register dedicated Standard, client renderer and durable origin/deflection/return state; valid melee only; bounds/expiry; use normal Forge hooks/network synchronization.
- [ ] Wire baseline attack and narrow own-return immunity, preserving attributed damage once and normal hurt bookkeeping.
- [ ] Genuine Forge compilation and meaningful mapped tests: registry constructor, separate dimensions/pick size, save/reload, spawn state, provenance and damage admission; measure explosion implications rather than equating integer power with gameplay.
- [ ] Explicit finite private native trial with actual survival target, registered room roster, charge/shot telemetry and retained swimming. Do not claim synthetic melee as real input proof; no infinite fixture invulnerability, fake Player or owner bypass. Keep native/manual gaps explicit.
- [ ] One compact fresh-context Astra source review; fix Important/Critical findings in one RED→GREEN pass. Record receipts, failures and final decision status; commit/push only scoped branches.

Later non-danmaku units: special fireball profiles, overhead bombing, positive ground mode and reversible conflict-aware Domain. **All Grand Danmaku remains excluded.** Completing this unit does not complete the boss.

Astra fixture ruling: real survival Player20HP, fresh copied world, mobGriefing=false, finite one natural charge/fire/recovery window. Development implementation may carry partial verification; full Standard/rally acceptance remains gated on genuine Player melee. Synthetic native damage cases, if later added, require separately declared authority and cannot satisfy real input. Current observer stays read-only. Block-destruction and gameplay1.5x balance remain NOT_RUN.
