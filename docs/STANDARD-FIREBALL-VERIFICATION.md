# Standard Fireball development verification

Implementation plan: [Standard/rally](superpowers/plans/2026-10-08-standard-fireball-rally.md). Dedicated Standard leaves legacy LargeFireball variants inactive and preserves accepted swimming. Current100HP/armor0, direct9/own-return20 and float explosion radius1.5 are development tuning, not final gameplay balance.

## Source verification and review

Pure tests:12036 swimming/22 foundation/25 mobility/19132 tactics/444 Standard charge/rally assertions PASS. Mapped Standard10 provenance/type/packet-NBT codec checks, mapped clearance5 and fixture14/seed5 PASS. These codecs do not instantiate a real projectile reload or establish melee, networking after deflection, damage events/invulnerability/death or explosion balance. Node21 affected checks PASS.

Initial genuine Forge compile failed on the wrong action-enum owner; corrected to `TacticalEvaluator.Action`. Genuine Java17/Forge47.4.10 compile/build then passed. The first mapped runner launched with a malformed argument array and failed before its test main; quoted classpath was corrected and the real checks passed. Preserve logs in the private task ledger.

One fresh-context `gpt-6-astra` whole-unit review of `b393bf8..93effb8`, response capped220words, found one Important: target death/removal/replacement cleared committed recovery early. Added RED regression, then target invalidation now preserves and advances20tick post-shot recovery independently of target identity. Charge still cancels on invalidation; no further hidden-target shot. No Critical finding; no second review loop.

## Preserved native failure

`flight-8at5rP`, source93effb8: genuine build/runtime and180samples, one naturally fired Standard,30tick charge/20tick face/locked aim, registered moving server projectile and actual client renderer/power checks passed. Initial room roster COMPLETE, no Reimu. Player20→11.833333HP and natural displacement were recorded; no invulnerability/healing/teleport. Failed **WITHIN_REGION**: region first existed at sample64; samples64–67 were initial return-to-region movement, first entry68; all subsequent positions stayed inside. The analyzer wrongly required initial acquisition to be already contained. Report remains FAIL and is never rewritten.

Correction separates bounded entry (at most20 measured region samples) from settled containment and still rejects every later exit, center/radii drift, collision or missing movement. The new explicit180tick observation request starts after the registered aperture action, avoiding consuming the attack window on setup/initial roster. It does not freeze product behavior. Original85 raw hashes match after the failed trial; owned evidence shutdown is clean and retained. Source recovery fix requires a fresh trial.

## Acceptance gates

Corrected-source native trial `flight-lGyhyy` is **PASS_DEVELOPMENT_NATURAL_STANDARD_SHOT_ONLY**. **Real Player melee, successful/missed complete rally, returned-damage events/invulnerability/death, instantiated projectile reload and deflected client synchronization, comparative gameplay explosion balance/visual-size comparison and terrain destruction are NOT_RUN.** Astra authorizes a development-only partial result, with full Standard/rally acceptance gated on genuine input. MobGriefing=false deliberately excludes block destruction. No synthetic interaction is described as real input; no fake Player or owner bypass.

The current fixture uses two finite whole-room roster requests and one explicitly requested supplementary Player-camera framebuffer. It does not claim fixed-cardinal coverage or live whole-room history. Accepted movement-only artifact and original world remain protected. All Grand Danmaku is excluded.

## Corrected native receipt

- Source `ef2230935074fdee6fce01d66483b5d51db5c88b`; genuine Java17/Forge47.4.10 build; product SHA256 `e55c99e11046a99cca4fc087c9ba510874a3fe8f2b89e82ae0870355cb7f8e00`.
-180 contiguous explicitly requested samples, gameTime40405–40584; one launch40505;30charge ticks, final11 sampled aim vectors unchanged (ten complete pre-launch intervals),20post-shot firing-face ticks. Actual server projectile and client `ThrownItemRenderer`/acceleration observed. No real deflection occurred.
- Retained generation1 and radii20/8/20; measured horizontal movement11.778blocks in this short window, collision-free and confined after bounded initial entry. This short trial does not replace the earlier900sample broad-swimming acceptance.
- Canonical whole-room rosters at serverTick84/gameTime40391 and288/40595 are COMPLETE, all chunks loaded, only real Player and the selected Ghast; no Reimu. Boss base position[9.5,230,9.5]→[9.1033126,227.9294899,18.8775028]. These are two samples, not continuous discovery. Transient projectile coverage comes from a separate bounded producer.
- Survival Player20→12.833333HP, natural knockback position[10.5857688,224,0.3000000]; no intervention. This single damage outcome does not establish final explosion balance.
- VERIFIED_EXIT, clean shutdown ACK, EVIDENCE_COMPLETE/STOPPED; original85 files match both trial and historical raw inventory. Accepted swimming JAR remains SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca`.

Private report `build/tank/flight-lGyhyy/report.json` SHA256 `e85103e9f4ca6cd56d758c59605afceafd45a66d7e013597d972c0de40200548`. Supplement hashes: flightJSONL `d25789469ee47fb2af2d88615bb9524e1c1dfc45538cd137613960cecdefd169`; clientJSONL `ec2f6e3eeba3479ef983587785fbe7c218d26b33d76023fcadafeb1017c81be9`; raw frame `40dc83dc40f5d4b50a985ecc6fe322e1e4dbade3b74edf3e991eede1910d34d0`; summary `9262ac4b0d084022b2de5425bf517a2f077549fa2dcd0223ec5966d4cbf73891`. Supplements have distinct producer/hash provenance and are not claimed as canonical roster/capture artifacts. The post-recovery raw frame was inspected and shows the Ghast; it does not show the earlier projectile or charge sequence.

## Astra continuation ruling

Proceed with committed DelayedBurst/Curve/Lob profiles sharing trajectory validation, damage budget and deflection normalization; partial input gates persist. Standard settled terminal speed1.9blocks/tick is the explicit reference; provisional Burst1.6x=3.04blocks/tick, not a launch-speed comparison. Every finite travel segment requires loaded swept1x1 clearance, including curve strengths. Only an explicitly declared final block impact may be permitted on the last segment; native first-contact/Forge impact hooks remain authoritative. Unexpected earlier obstacles cancel preflight. Misses discard at finite lifetime without invented airburst or unvalidated continuation. No hidden-target tracking, chunk loads, ground profiles or Grand Danmaku. Rulings came from two compact Astra packets capped150/100words.
