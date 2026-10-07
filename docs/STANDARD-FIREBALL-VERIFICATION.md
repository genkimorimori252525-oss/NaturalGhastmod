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

Development natural-shot native verification is pending for the corrected source. **Real Player melee, successful/missed complete rally, returned-damage events/invulnerability/death, instantiated projectile reload and deflected client synchronization, comparative gameplay explosion balance and terrain destruction are NOT_RUN.** Astra authorizes a development-only partial result, with full Standard/rally acceptance gated on genuine input. MobGriefing=false deliberately excludes block destruction. No synthetic interaction is described as real input; no fake Player or owner bypass.

The current fixture uses two finite whole-room roster requests and one explicitly requested supplementary Player-camera framebuffer. It does not claim fixed-cardinal coverage or live whole-room history. Accepted movement-only artifact and original world remain protected. All Grand Danmaku is excluded.
