# Committed projectile development verification

[Plan](superpowers/plans/2026-10-08-committed-projectile-profiles.md); source branch `codex/natural-tactics-20261008`. Burst/Curve/Lob share the new Standard family, damage/attribution/deflection pipeline and finite lifetime. Grand Danmaku remains excluded; accepted broad retained-region swimming remains unchanged.

## Scope and source checks

Pure suites PASS:12036 swimming,22 foundation,25 mobility,19132 tactics,444 Standard,699 trajectories/committed recipe and21 selection/stationarity/clearance assertions. Mapped10 Standard and10 committed codec/shape checks PASS; these are not instantiated world reload or real input. Affected Node25 PASS. Genuine Java17/Forge47.4.10 build and private observer compilation PASS on each native source. No new dependency.

One fresh compact Astra whole-unit source review `0a33e05..545fe41` found no Critical/Important issue and one candidate-loop minor. A later compact ruling promoted it to Important: rejected high arcs must not suppress feasible lower arcs. Expected range/speed failures are handled per candidate, unexpected exceptions propagate. Regression reproduces rejected height10 and valid8/6. Subsequent native investigation/rulings corrected observable stationarity and profile commitment; these later changes are not claimed to have received a second whole-unit review.

Commit attack type, selected strength/side or Lob height and observed endpoint at charge tick19, with a distinct particle cue. Existing aim lock has an11tick timestamp gap to firing at30. Continue ordinary swimming. At launch, reconstruct only that exact recipe from the actual physical muzzle and revalidate the complete loaded swept1x1 path; cancel if invalid, with no alternative recipe or retargeting. The world path becomes immutable at spawn. This avoids firing from an old/predicted muzzle while preserving accepted motion. Native first-contact/Forge hooks remain authoritative; conservative swept-body preflight is distinct from center-ray impact geometry.

## Preserved failures and rulings

| Trial/source | Result and evidence | Decision |
| --- | --- | --- |
| `flight-2N1R0y`/545fe41 | Standard naturally fired; no profile file; FAIL/ENOENT retained. Initial room roster COMPLETE, clean owned shutdown, original85 exact. Rejection cause was not recorded. | Add only latest selected candidate/preflight rejection telemetry. Missing optional shot telemetry means zero coverage/FAIL, while malformed records and other I/O errors propagate. Final roster is requested before acceptance assertions. |
| `flight-BtQdL4`/5590806 | Standard only; no special candidate attempted; profile coverage FAIL. Both room rosters COMPLETE. | Stationarity should use consecutive visible world-position displacement, reset on LOS/target discontinuity. Internal physics velocity is not displacement. Earlier physics velocity was not recorded; do not retrospectively claim the trial's cause. |
| `flight-YTRiS7`/9397f7c |59stationary intervals,0displacement; attempted Lob rejected for early geometry, Standard fallback; profile coverage FAIL. | Astra chooses recipe/aim commitment at tick19, followed by exact-recipe actual-muzzle validation at launch. Preserve range/speed/loaded collision gates and selection probabilities. |

All failed trials remain sealed and unchanged. No forced profile selection, fake Player, healing, input injection or assertion removal.

## Native Lob receipt

`flight-OFz0Lb` is **PASS_DEVELOPMENT_NATURAL_COMMITTED_PROFILE_ONLY**, exercising **LOB only**.

- Source `d1a51bc6b92019e598fdcc755698706cce11b666`; product SHA256 `759bb2aa3056854039acc2207eda5ce1adbbbea20edf46f53753b6fe9ac762c3`.
-180 contiguous explicitly requested samples, gameTime40400–40579. Natural30tick charge; Lob height10/endpoint[9.5,223.75,3.5] locked with cue40492; launch40503; recipe/endpoint unchanged through launch;20tick post-shot face. Preflight23segments at commitment and24from the changed actual muzzle at launch, preserving the same recipe/endpoint.
- Projectile UUID `11acdc56-7270-4ba5-bc1c-f51f4bf6ca8b`:23 actual server position samples match the immutable24segment path within1e-6; both ASCEND/DESCEND observed. Actual client sees registered `committed_fireball`, `ThrownItemRenderer`,0acceleration power, positive speed, LOB/ASCEND/index1. No real deflection occurred.
- Final permitted collider set: four exact black-concrete blocks at x9/10,y223,z3/4; complete preflight records only final-segment admission. This proves launch preflight and observed flight, not the ultimate direct-victim/native impact classification.
- Canonical COMPLETE room rosters gameTime40387/40590, all chunks loaded: real Player and selected Ghast only, no Reimu. Boss[9.5,230,9.5]→[16.0826977,231.2019301,23.6791441]. Retained generation1/radii20/8/20, collision-free after bounded initial entry; horizontal span14.440blocks in this short window. Earlier900sample broad-swimming proof remains separate.
- Real survival Player20→13.666666HP, final[9.2868619,224,2.5366068], natural displacement without intervention. MobGriefing=false excludes terrain destruction.
- VERIFIED_EXIT, CLEAN_EVIDENCE_SHUTDOWN ACK (finalWriterSeq177,dropped0,remaining0), EVIDENCE_COMPLETE/STOPPED. Original85 raw hashes/count match historical baseline; accepted movement artifact remains SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca`.

Private report `build/tank/flight-OFz0Lb/report.json` SHA256 `5a34fad0713f790c9b5f04d6e69b08d47521acb5168e302bec730a2295743b9f`. Separate producer supplement hashes:

| Artifact | SHA256 |
| --- | --- |
| flight.jsonl | `8bf4af7756a2eb14f7e328587dfe7b413d6412b427c8a1d87ec2e497b5c628df` |
| client-projectiles.jsonl | `fda0ae2735db927ebbcb5fabdaebce556da1b3f72b2f5c44a1cb7096c0b1004f` |
| profile-paths.jsonl | `c933ed4c1bb0f70ce72829063d7605a12b068ccbeb2b6e1b960c10af071a5a86` |
| profile-summary.json | `402f27c553353872a946f1920a8d52940ed9d27c76a5e04464f4b2cde955329a` |
| frame.png | `e85f5a72562ec0aa59675d67fa628ff78c4b3cf0c1b9d719d2aad152ae14e8f9` |

The one requested post-recovery Player-camera framebuffer was inspected and shows the Ghast/room grid. It does not establish particle readability, charge imagery, fixed-camera coverage, continuous discovery or AI perception. Supplements are not falsely included in canonical roster/capture provenance.

## Remaining gates and next work

Explicit instantiated Burst/Curve/Lob save/reload/continuation and malformed spawn rejection now pass [the separate reliability unit](PROFILE-RELOAD-VERIFICATION.md). Natural Burst/Curve selection, genuine Player melee/full rally/returned-damage event pipeline, durable unload/restart, late-client/deflected synchronization, particle readability, explosion balance and terrain destruction remain **NOT_RUN**. Last-index Lob removal is not classified impact evidence. This is not full boss completion.

Subsequent [native impact reliability](IMPACT-DAMAGE-VERIFICATION.md) proves executed declared Lob block collision and explicit Standard/Ground/returned damage bookkeeping. Human melee/full natural rally remains separate; these later results do not relabel earlier natural Lob/profile receipts or grant balance/full-release acceptance.

Astra recommends Overhead Bombing next (provisional12block altitude, three bombs24ticks apart, visible withdrawal/return/acquisition/downward pose, sole controller/existing speed cap, LOS/clearance/alignment release gates and safe return to retained region), then positive Ground with readable scuttling and faster lower-damage single shots, then standalone persistent conflict-aware Domain with restart recovery. Ground fan geometry is deferred scope, not implicitly prohibited by the separate Grand Danmaku exclusion. Real moving-Player tracking/avoidance/cumulative damage, ground transitions/collisions and Domain restoration after intervening edits need distinct native gates. Do not manufacture their PASS from this static Lob receipt.
