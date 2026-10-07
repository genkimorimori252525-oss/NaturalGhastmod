# Standard Fireball and bounded rally

Base: `b393bf8`, authoritative redesign v0.6. User authorizes non-danmaku continuation and compact Astra rulings. Preserve accepted retained-region swimming, original world and accepted JAR. Execute inline with grouped checks and one whole-unit source review.

## Decisions

- Astra recommends a dedicated `Fireball` subclass, not `LargeFireball`, to avoid Ghast's private reflected-fireball1000damage branch. Keep Player attribution and ordinary damage bookkeeping. Only the origin boss's legitimately Player-deflected Standard bypasses its fire immunity. Provisional100HP/20returned damage/armor0; no boss bar. Vanilla LargeFireball compatibility remains deferred.
- Normal charge30ticks (vanilla20), lock direction for the final10ticks, keep firing expression20ticks after launch. Observation requires live LOS until launch; cancellation resets without firing. No tracking after launch. Modest observed-velocity lead is capped, never hidden target position.
- Presentation1.5x, collision1x1, pick radius1.5 independently. Float explosion radius1.5 with direct entity damage9 is development tuning; not a claim that every gameplay effect is1.5x. Preserve mobGriefing and Forge projectile/explosion hooks.
- At most one boss return per projectile. Incoming Player-deflected shots are evaluated only with loaded visible geometry, finite range/approach checks, a short6tick telegraph and fallible reaction. Aim locks at reaction start; no perfect interception/homing or target memory after LOS. Repeated Player deflection remains legal. No rally dodge feint until ordinary rally is validated.
- Preserve separate movement/look ownership. Attack supplies an optional locked look intent to the existing look controller; it never writes boss position or velocity. Do not stack a new shot telegraph onto an active movement feint. No legacy phase/variant goals reactivated.

## Coherent tasks and gates

- [x] Read genuine sources and implement pure state;444charge/rally regressions PASS, including Astra's recovery correction.
- [x] Register dedicated Standard/client renderer and persistent provenance; valid melee only, one boss return, finite expiry and Forge hooks. Real instantiated reload/deflected synchronization acceptance remains NOT_RUN.
- [x] Wire baseline attack and narrow attributed own-return admission; no HP-only clamp. Native20damage pipeline/event/invulnerability/death acceptance remains NOT_RUN.
- [x] Genuine Forge build and mapped10 codec/type, clearance5, fixture14/seed5 checks PASS; scope is explicit. Gameplay explosion/visual size comparison remains NOT_RUN.
- [x] Fresh `flight-lGyhyy` passes scoped native natural-shot verification, two COMPLETE room rosters and retained swimming; original85/historic artifact unchanged. See [receipt](../../STANDARD-FIREBALL-VERIFICATION.md).
- [x] One compact Astra review; one Important fixed RED→GREEN. Preserved failing `flight-8at5rP`; published scoped decisions/results. Full Standard/rally acceptance is gated, not marked complete.

Later non-danmaku units: special fireball profiles, overhead bombing, positive ground mode and reversible conflict-aware Domain. **All Grand Danmaku remains excluded.** Completing this unit does not complete the boss.

Astra fixture ruling: real survival Player20HP, fresh copied world, mobGriefing=false, finite one natural charge/fire/recovery window. Development implementation may carry partial verification; full Standard/rally acceptance remains gated on genuine Player melee. Synthetic native damage cases, if later added, require separately declared authority and cannot satisfy real input. Current observer stays read-only. Block-destruction and gameplay1.5x balance remain NOT_RUN.
