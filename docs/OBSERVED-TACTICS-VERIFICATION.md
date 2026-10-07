# Observed tactics verification — 2026-10-08

## Implementation

Base dce89a7; product unit09d53aa, classpath repair1d0ad48, review correctiondf4065f, launch repair4089bdb, bounded fixturef1d2ba3. The isolated branch adds observed-only tactical scoring, fixed-size recent-action weights and four finite motion recipes. Ordinary `MovementPlanner` swimming remains the default; the sole Flight Controller integrates velocity. Recipes lock endpoints within the retained20/8/20 region, telegraph24ticks, commit8, reveal24, brake12 and return16. Quiet swimming lasts240ticks afterward; safety overrides commitment and LOS loss cancels before commitment. Player camera/facing never moves the region. There are no active attacks or Grand Danmaku changes.

The user explicitly accepted the earlier broad retained swimming. Preserve its exact artifact SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca`; no overwrite or original-workspace upgrade was performed.

## Verification

- Pure JVM:12036 regional/swimming,22 foundation,25 mobility and19132 observed-tactics assertions PASS. Source tests cover all four recipes, pre/post-commit LOS behavior, safety abort, physical containment, ordinary behavior dominance and a two-completed-cycle memory regression.
- Genuine Minecraft1.20.1/Forge47.4.10 build PASS. Mapped swept-AABB5, fixture14 and seed policy5 PASS. Relevant Node18 PASS, including trace false positives and reused private launch arguments. No new dependency.
- One fresh gpt-6-astra source review of dce89a7..09d53aa found one Important issue:300tick memory expired during84tick recipe +240tick quiet recovery. A brain-level test reproduced repeated selection;600tick bounded memory fixes it. Other native preparation repairs and their evidence are described below. Native visual/rally/ground behavior were outside the review.

Native trial `build/tank/flight-i9Ez38/report.json`, source `f1d2ba32d40dcf9f8ae52b02b4b8234e54b727ee`, development artifact SHA256 `1c158dfb67b32b0e98f20e6b1765ed23c5893172d97a813bc82c1f2816ec0333`. The finite read-only development observer is packaged separately and absent from the product artifact. It observes at most900server samples and one explicitly requested supplementary Player framebuffer; it is not a fixed-cardinal camera or canonical same-tick room capture.

Native scope: **PASS_STATIC_PLAYER_OBSERVED_TACTICAL_MOVEMENT** in a fresh52×24×52 TANK_CORE copy. A single offline initial LOS seal prevents pre-owner acquisition; one registered block action opens it. Allocation75816/scope64896 cells and action authority stay bounded. The variant removes the wall from this specific open-room scenario, without constraining/teleporting/freezing the Ghast or changing product tuning.

Measured:900 samples;848visible target/region samples; generation1 throughout;707swimming samples span20.911blocks horizontally/7.164vertically;0stopped swimming samples. Across843settled samples,26stopped samples include intentional rare maneuver braking/recovery.732ordinary and168maneuver samples; two84tick `LATERAL_FAKE` recipes complete (40686–40769 and41089–41172). Other recipes were not measured natively; weighted memory is not a blanket prohibition on reusing the only feasible recipe.

Owned exit VERIFIED_EXIT, clean shutdown ACK, EVIDENCE_COMPLETE; original85 raw files match both trial baseline and historical pre-session hashes/count. Accepted baseline JAR remains byte-identical. Raw requested frame SHA256 `a230514115261351915e730a859889fc57d826e525e0fd5ec9de95c4529c5b47` was inspected: visible Ghast in the grid room. It does not establish perceived AI sight, whole-room roster completeness, or subjective feint quality.

## Preserved failures and limits

- `flight-CRzVHb`: observer compilation selected older product classes from the registered argument file; `getTacticalState` was absent. Current genuinely built product classes now precede inherited classpath entries; no stale-source fallback. Original85unchanged.
- `flight-6R1tPq`: reused config duplicated Gradle's single-value `--project-dir`, exiting before READY. Genuine Gradle diagnostic reproduced it; bounded argument normalization preserves base init scripts and replaces only the prior private init. Cleanup VERIFIED_EXIT. Finalization did not pass; its failure initially obscured the primary launch failure, now preserved independently in future reports. No gameplay claim;85unchanged.
- `flight-heqGZr`:600sample opaque-wall scenario PASS for broad physical swimming (19.452horizontal/4.144vertical), but23telegraph samples and no complete recipe. The intended full-recipe gate correctly FAILS. Do not relabel it as full tactics acceptance. Exit/ACK/seal are complete;85unchanged. Retain it as a separate observed interruption scenario, without claiming controlled causal LOS acceptance.
- A malformed diagnostic Node one-liner accidentally ran Gradle's default help rather than the intended private-init help; it launched no Minecraft. The actual corrected private configuration was subsequently exercised by the successful native trial. Do not count that command as private-init validation.

Pure checks are not runtime proof for all recipes. Moving Player, controlled LOS interruption/target replacement, ground mode, attacks, multiplayer/performance and subjective readability remain unverified. This trial has selected-subject telemetry and a supplementary Player image; it does not have a current whole-room roster or fixed-cardinal frame set. Future attack trials should explicitly register the existing room-read scope and collect finite roster samples, rather than infer resident coverage from the image.

## Next authorized decision

One compact Astra consultation recommends the next isolated Standard Fireball/rally unit: longer readable charge, locked direction,20tick firing-face retention,1.5×visual scale, separate enlarged deflection radius, at most one imperfect boss return; no other attacks/Grand Danmaku. Provisional development durability:100HP, own reflected Standard damage20, gameplay armor0, no boss bar. These are **approved development tuning, not implemented here or final balance**.

Mapped Ghast source's private reflected-fireball branch forces1000damage for Player-owned `LargeFireball`. Astra adopts a dedicated `Fireball` subclass for Standard and a narrow immunity exception only for the boss's own legitimately deflected Standard, preserving Player attribution and ordinary damage/invulnerability bookkeeping. Vanilla `LargeFireball`1000damage remains a documented deferred compatibility gap. Do not fake attribution or clamp only final HP while leaving damage bookkeeping inconsistent.

Mandatory next verification: real survival target/LOS cancellation, timing/dimensions, enlarged melee registration, successful/missed rally, ownership/deflection attribution, one20damage application, invulnerability timing/ordinary damage/death, measured explosion comparison, retained swimming, owned shutdown and complete evidence. Integer power alone cannot substantiate the1.5×gameplay explosion claim. Missing input coverage stays explicit; no owner bypass, fake Player or unregistered input injection.
