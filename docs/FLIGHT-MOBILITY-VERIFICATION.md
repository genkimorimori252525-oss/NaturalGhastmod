# Flight / Mobility verification — 2026-10-07

Redesign v0.5 at upstream main `4e6f1bdd9b9869c516a487f1e3ca35729e7bb4bf`; local foundation `6fbb21d`. This unit connects HOLD, compact DRIFT and frontal range correction to smoothed Mobility Context. Ground-forced safely brakes; ground locomotion, tactics, attack recipes and final danmaku remain pending. See [usage](FLIGHT-FOUNDATION.md), [implementation plan and rulings](superpowers/plans/2026-10-07-flight-mobility.md) and [historical idle-only receipt](FLIGHT-FOUNDATION-VERIFICATION.md).

## Source and grouped checks

Native target: `b9938e6d017b9764bd460d2b4ce62c9889c9283c`, branch `codex/natural-flight-foundation-20261007`; product repair `3e1e325`. LAB `57bd4e011b1072bf143c9643a1b50ef0a5531954`; separate clean host `7f14960999bc2955d85ae9d3619090ad37817c38`, privately overridden Forge1.20.1-47.4.10. Whole target artifact SHA256 `4e12db4ec676749d6ddc7e93c6f68b64a0200f7e0329e20f5fb4749a79d09735`. Later documentation changes do not imply another native run.

| Check | Result | Scope |
| --- | --- | --- |
| Pure Java foundation / movement | 22 + 25 PASS | Facing, range, inertia, context dwell, annulus paths, bounded feasibility |
| Mapped Forge swept body regressions | 5 PASS | Real AABB math, departure and diagonal obstacle; no world navigation |
| Mapped fixture palette / bounds | 8 PASS | Offline codec, shell, supported seal, near/distant aperture |
| Node acceptance / owner-wait regressions | 6 PASS | Refuse idle, unsafe/gapped, invisible rear movement, instant-stop and label-only drift; await idle plus action ID |
| Exact target `compileJava build` | PASS | Native source snapshot; Gradle deprecations remain |
| Independent whole-unit review | Critical0 / Important3 / Minor0 | Three Important issues repaired with regressions in one pass; native judgments separately withheld |

Main-method Java tests require the explicit runners; Gradle build alone does not execute them. RED traces and failed trials remain under ignored `build/flight-mobility-work` and `build/tank`.

Mobility uses six coarse body sweeps, conservatively derived diagonals and at most two actual candidate sweeps before movement selection. The existing actual-velocity stopping-horizon sweep remains the final guard. Inner-range checks cover the entire drift chord. This is conservative avoidance, not general pathfinding or a hard range barrier.

## Measured native result

`PASS_STATIC_PLAYER_ACQUISITION_FLIGHT_MOBILITY`, trial `C:/Users/genki/Documents/Codex/NaturalGhastmod/build/tank/flight-92WZ2m`. Run-relative path: `runtime/sessions/sess-20261007145055-1c9e40ed88c5/runs/run-20261007145055-1a348ec196c2`.

- New private save copy: room56x16x56 at (0,224,0), allocated shell60552 cells. Physical room differs from action Arena min[7,224,6]/max[13,235,13]. A prebuilt aperture is sealed on the initial eye ray until one registered STONE->AIR operation; that action was VERIFIED. No Player action grant or target/transform/AI setter.
- Real survival Player UUID `380df991-f603-344c-a090-369bad2a924a`: measured static position(9.5,224,3.5), yaw0/pitch-18 throughout. No fake-player dependency. Ghast UUID `67676767-1007-4000-8000-000000000001`.
- 600 contiguous END-server samples, gameTime40347–40946; 556 actual Player-target samples, 531 visible/frontal/22..34-range samples. Maximum speed0.65 blocks/tick, maximum displacement22.920195630837153 blocks. Continuous deceleration and multiple ticks of actual DRIFT movement observed; contexts CONFINED/SEMI_OPEN/OPEN_AIR, primitives HOLD/WITHDRAW/DRIFT/BRAKE. All600 bodies4x4, collision-free, NoAI=false, health10.
- Canonical preflight READY, freshness CURRENT; 217 canonical observations. The finite supplemental observer is a separate producer, read-only and absent from the production artifact.
- One explicitly requested640x480 raw PNG inspected: actual `SoutouGhastRenderer`, visible ghast through the aperture and Tank grid. No automatic product recording or frame history. This frame establishes scoped presentation, not combat visual quality or AI perception.
- Owned stop: live=false, clean=true, writerDroppedTotal0, remainingQueue0; canonical EVIDENCE_COMPLETE. All85 original file paths/hashes unchanged. Original/config/old finalized runs were not overwritten.

`report.json` SHA256 `0216e4746d742f849e53dc7894aa6474e21aa72d83625a9c8d50a4b8708b91ee`; `evidence/finalization.json` SHA256 `b23334e56e8778fe09bd521779a40ff36e715ec9164429b6fd05777a22ea257b`; `evidence/derived/naturalghast-flight/frame.png` SHA256 `496e65167ec6088eecbe594e73f7d1d195106ee99ee90848971bf58d9416ae34`. Supplement files also include `flight.jsonl`, `flight-summary.json`, `frame.json` and the explicit `request-frame.json`. Local ignored evidence is not supplied by a fresh checkout; retain it while this receipt is needed.

## Failures retained and repaired

- `flight-RzdfJx`: build rejected a LAB-only fixture test in product test sources. Moved to `tools/tank`; no product LAB dependency.
- `flight-ynfKNZ`: UNSUPPORTED_CLASS_ANCHOR. Kept the fixed allowlist; request only the supported actual MOD-class anchor plus whole artifact binding. Other compiled hashes remain declarations.
- `flight-d2a0jq`: UNCONTROLLED_BLOCK_STATE. Use STONE inside the action Arena; no palette relaxation.
- `flight-VPYIdi`: first action VERIFIED, then OWNER_ACTION_ORDER_MISMATCH. Journal completion and next-action publication are asynchronous; await fresh state.
- `flight-pxuVE6`: two actions VERIFIED, then OUTCOME_UNKNOWN / AccessDeniedException:null. Exact I/O path is unreported; root cause unconfirmed. Idle plus next-ID waiting and one-action activation reduce fixture operations; do not claim a generic LAB filesystem repair.
- `flight-pN18hN`: 600 samples,66 acquired-target samples, speed0.55/displacement5.7, then LOS loss correctly caused gradual HOLD braking. Narrow aperture blocked the distant eye ray. Extended its lower cells and measured actual static Player pose. This run remains FAIL, not relabeled by the successful replay.

Every launched failed trial stopped cleanly, finalized EVIDENCE_COMPLETE and retained all85 original hashes. Whole-unit review repaired inward annulus crossing, uninspected diagonal permission and acceptance false positives. Do not hide legitimate rejections or weaken guards to obtain PASS.

## Limits and next unit

Reviewer withheld native judgment until the measured run; that static flight/render scope is now evidenced. Still unverified: full-room heightmap/lighting/boundary readback, moving/facing-changing Player, target replacement, deliberate LOS loss/recovery, ground landing/scuttling, other movement primitives, feints/tactics/attacks/danmaku, multiplayer, performance and external impulses. Inactive legacy AI is outside this unit. Compiled hashes/class-resource-container linkage do not attest resident transformed classes. No deferred review minors.

Next: Tactical Evaluator, short-term repetition memory and Commit Points, with a bounded movement/facing/occlusion fixture where needed. Implement and verify a coherent unit; prioritize evidenced tooling defects, preserve authority and source identities, and keep final danmaku separate. Branch and evidence remain local; no push/merge requested.
