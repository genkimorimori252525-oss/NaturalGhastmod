# NaturalGhast Flight / Mobility Implementation Plan

> Use `superpowers:executing-plans` inline; verify coherent units. User explicitly resumed implementation on 2026-10-07 and authorized prioritizing evidenced Tech Hub defects.

**Goal:** Connect reusable movement and smoothed space constraints to the existing non-attacking frontal-anchor foundation, and establish actual player acquisition / flight in a roomy disposable TANK_CORE fixture.
**Spec:** upstream `docs/design/NATURAL-GHAST-REDESIGN-v0.1.md` v0.5 and handoff at main `4e6f1bdd9b9869c516a487f1e3ca35729e7bb4bf` (remote rechecked unchanged). Existing foundation at local `6fbb21d`; prior native evidence remains scoped idle only.
**Architecture:** observable target -> anchor -> mobility-filtered movement intent -> existing inertial controller. Pure bounded policy is separate from Forge collision sampling. Private offline world preparation and finite read-only observer remain outside production packaging; runtime block edits use the existing sealed owner action API.
**Stack:** Java17 / MC1.20.1 / Forge47.4.10; current Tech Hub LAB at `57bd4e0`, separate clean TANK_CORE host `7f1496`. No new dependency.

## Constraints

- Preserve 4x4 body, 22..34 range, sole velocity writer, observable-target/LOS reset and acceleration > braking. No legacy attacks or final danmaku.
- Each mobility sample uses at most10 body-sized collision sweeps, each <=8 blocks. Context enters tighter space after6 matching samples, opens after20; missing observations reset subject-bound policy.
- HOLD stays common; drift inside the frontal region is bounded and transient. Return uses the anchor; do not replace it with a maneuver endpoint. Ground-forced classification safely holds/brakes; actual scuttling/landing requires a subsequent locomotion unit, not an untested flight disguise.
- Native fixture is a new copied save only. Room56x16x56 at (0,224,0), bounded allocated shell58x18x58 (<100000), <=65536 interior. Original files never changed. Offline survival player is an observation fixture, not an owner action subject; no Player permission change or fake-player dependency.
- Initial opaque wall prevents target-driven flight before the owner installation baseline. A declared bounded window is opened via registered set-block operations. Observer never sets target, position, look, velocity or AI. Source-bound finite receipts/lease/cleanup remain required.
- One explicitly requested finite raw frame and <=600 supplemental server samples; no automatic recording or persistent frame history. Supplementary observations are separate from canonical evidence.

## Task 1 — Mobility-filtered movement

**Files:** new `entity/ai/flight/MobilityContext.java`, `MovementPrimitive.java`, `MovementPlanner.java`; new `MovementMobilityTest.java`, existing test runner; `SoutouGhastAnchorGoal` and `SoutouGhastInertialMoveControl` integration.
**Interfaces:** context `update(Sample)` / `reset()` with directional clearances; planner `step(anchor, targetPosition, bossPosition, facing, context, sample, variation, clearance)` returns primitive plus existing FlightController.Intent. Variation supplied by the entity random source, finite [0,1); it cannot override feasibility. Sampling uses actual AABB and smoothed context without terrain mutation.

- [x] Write grouped missing-feature tests: six/twenty sample dwell, alternating constrained/open evidence, ground recovery dwell, finite/invalid input rejection, unavailable movement excluded, common HOLD, bounded drift radius/return, reset and varied feasible choices. Run RED.
- [x] Implement context, primitive mapping and planner as one unit; wire actual collision queries and subject loss reset, expose finite diagnostics. Run foundation22 + new grouped assertions GREEN.
- [x] Source compile/build once at integration boundary; keep actual diagnostics. Commit exact source before native binding.

## Task 2 — Roomy registered native fixture

**Files:** new opt-in `tools/tank/PrepareFlightTank.java`, `ObserveFlightTank.java`, `run-flight-tank.mjs` / finite results analyzer; mapped fixture tests. Reuse foundation launcher identity/material/build/stop plumbing.
**Interfaces:** offline fixture emits exact typed-Gson baseline + UUID/geometry/window declaration; launcher registers exact request/actions/resources; observer only records real position/velocity/target/LOS/intent/context/collision/range/ticks, capped600. Analyzer requires contiguous measured scopes and refuses absent acquisition/motion/return/collision/cleanup facts.

- [x] Test fixture palette round-trip and bounds, baseline typing, analyzer refusal of idle-only / synthetic discontinuous or collision traces before implementation; retain RED.
- [x] Implement offline bounded shell and survival-player fixture, native-owned window action sequence, source pins (entity/goal/controller/planner/context/look/travel), read-only finite observer and immutable reports.
- [x] Compile mapped helpers and source-bound target; launch fresh private TANK_CORE once. Verify natural Player acquisition, actual acceleration/braking/frontal arrival, body clearance, finite traces/frame, source linkage, clean finalization and original hashes. If a material defect appears, fix the affected seam first with a regression; preserve failed run.

## Task 3 — Review and handoff

- [x] One fresh whole-unit source review after implementation, with native evidence separately identified. Fix Important/Critical with regressions; record minors and declined native judgments.
- [x] Update English movement/verification docs and Tech Hub feedback with measured facts, source IDs, retained failures, unresolved scope and rulings/costs. Keep clean local branch; no publication requested.

## Review focus

Directional feasibility before selection; inward drift does not leave frontal volume; sustained context transitions; subject reset/LOS discipline; actual flight writer remains sole integrator; offline region encoding/heightmaps and shell bounds; runtime observer is read-only; Player excluded from action grant; baseline stationary until owned window opening; incomplete native coverage remains explicit.

## Ledger

Ruling: reuse the clean dedicated NaturalGhast checkout and existing local branch — resumed prior foundation, no unrelated files — cost: new unit shares that branch's integration step.
Ruling: apply the existing approved v0.5 design and explicit resume instruction without repeated planning approval — no new gameplay scope — cost: revisions remain local/reviewable.
Ruling: use the real offline-prepared survival player as an observed target and sealed block edits for activation — tests ordinary acquisition without a Player action API — cost: moving/facing-change scenarios remain a later fixture.
Ruling: keep ground-forced behavior safely non-attacking until ground locomotion is implemented — avoids pretending airborne braking is scuttling — cost: ground mode remains partial.
Ruling: keep the mapped fixture test in tools/tank rather than product test sources — actual Gradle build exposed its LAB-only helper dependency — cost: explicit separate mapped-fixture test command.
Ruling: retain native fixed anchor allowlist and request only the supported MOD-class anchor — flight-ynfKNZ correctly rejected eight arbitrary target classes — cost: remaining compiled class hashes are declarations, not loaded/transformed-class attestation; whole target artifact still hash-bound.
Ruling: use six coarse body sweeps with conservative derived diagonals and at most two actual candidate sweeps — fixed unsampled diagonal selection within the existing collision-query cap — cost: ambiguous narrow paths may conservatively brake.
Ruling: use STONE inside the registered Arena and open its critical eye-ray cell last — native flight-d2a0jq correctly rejected unsupported BLACK_CONCRETE; activation must follow preparation — cost: a static fixture wall differs visually from the outer shell.
Ruling: retain scratch and failed trials rather than deleting the skill workspace — user requires approval before deletion and evidence aids feedback — cost: ignored local disk usage.
Ruling: wait for the fresh owner next-action state after each journal receipt — flight-VPYIdi exposed asynchronous publication, correctly rejected by order checks — cost: bounded waiting; no replay or weakened order guard.
Ruling: prepare the aperture offline and activate with one sealed cell operation; require idle as well as the next action ID — minimizes fixture mutations and fixes early busy-state acceptance — cost: static activation fixture only. flight-pxuVE6 ended AccessDeniedException:null with its exact I/O path unreported; do not claim its root cause or a generic LAB filesystem repair.
Ruling: extend the aperture down to the distant target eye ray and observe the actual player's static pose — flight-pN18hN acquired/accelerated then correctly stopped on LOS loss caused by its narrow window — cost: larger declared offline aperture; does not validate arbitrary occlusion/recovery scenarios.
Ruling: leave both implementation branches local after coherent verification — no push/merge/publication requested — cost: integration remains a later user-controlled step.

Final: all tasks completed. Core22+mobility25, mapped body5, fixture8, Node6, exact compileJava/build PASS. Native source b9938e6 / LAB57bd4e0 / host7f1496: flight-92WZ2m PASS_STATIC_PLAYER_ACQUISITION_FLIGHT_MOBILITY,600samples/556acquired/531visiblefrontalcomfortable, cap0.65/displacement22.9201956, continuous braking and actual drift,217canonical observations, clean/dropped0/EVIDENCE_COMPLETE/original85 unchanged. Explicit raw frame inspected. Review Critical0 Important3 Minor0; repairs verified, no deferred minors. Declined/unverified judgment scopes and retained rejected trials are in `docs/FLIGHT-MOBILITY-VERIFICATION.md`; generic filesystem denial remains unconfirmed (Tech Hub AF0013). Documentation-only final commits do not imply another native run.
Pre-flight: planner consumes existing anchor/FlightController.Intent unchanged; Forge integration supplies actual directional clearances; private fixture consumes exact compiled unit and registered set-block transport. No API/grant conflict found.
