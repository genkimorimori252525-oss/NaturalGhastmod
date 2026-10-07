# NaturalGhast Flight Foundation Implementation Plan

> For agentic workers: use `superpowers:executing-plans` inline. Follow the user's coherent-work-unit verification policy; no per-edit broad suite, repeated Forge build or Minecraft launch.

**Goal:** Establish a readable inertial flight and frontal-anchor foundation, then exercise that slice in a bounded private Tank run. This is the first redesign slice, not a complete boss.

**Status:** Approved by the user on 2026-10-07. `TANK_CORE` is the Tank; danmaku content is excluded.

**Execution ledger:**
- Core grouped RED: missing FlightVector/FlightController/CombatFacing/CombatAnchor confirmed by javac; GREEN: assertion runner passed. Forge `compileJava build`: PASS (24s), baseline's four errors repaired.
- Ruling: use the existing TANK_CORE host with a separately source-built NaturalGhast development artifact instead of adding TLM dependencies to the product. Cost if wrong: revise development launch wiring; production has no LAB instrumentation.
- Ruling: limit the first native Tank window to active idle, actual 4×4 clearance, exact mod identity and rendering. Existing 19×19×11 geometry cannot fit the specified frontal range; scoped owner API forbids Player subjects. Cost if wrong: player-facing/native flight remains unverified and must not be treated as release-ready. No range/dimension weakening or owner-gate bypass.
- Final whole-slice review: Critical 0, Important 2, Minor 2. Fixed rotation API/LookControl tick ownership, swept-AABB backward inflation, and stale-build binding in one repair unit. Orientation RED missing bounded primitive, clearance RED `safe takeoff cannot include the floor`; GREEN: 22 core assertions and 3 mapped Forge clearance assertions. No deferred minors.
- Ruling: promote pitch tick-order and stale source/build binding findings to the repair unit because they affect the promised 3D behavior and evidence identity. Cost if wrong: a dedicated LookControl and one source-bound build before native packaging.
- Ruling: register NaturalGhast as target source, select LAB host separately, and override only this private launch to Forge47.4.10. Earlier prelaunch source/assertion guards and the actual Forge47.2.0 constructor failure were retained as failures, not PASS. Cost if wrong: adjusted-host compatibility remains an explicit native risk; original TANK_CORE performance claims do not transfer.

**Architecture:** Pure Java owns movement intent, constrained velocity updates, smoothed facing and anchor/range decisions. A thin Forge adapter consumes observable player state and actual entity velocity, performs bounded AABB clearance checks, and applies controller output server-side. Existing registry, renderer, armor and sound assets remain the integration boundary; obsolete combat Goals are not extended.

**Tech stack:** Minecraft 1.20.1, Forge 47.4.10, Java 17; existing ForgeGradle/wrapper; selected Tech Hub LAB source. No new production dependency.

**Spec:** `docs/design/NATURAL-GHAST-REDESIGN-v0.1.md` (content v0.5), with `docs/CODEX-HANDOFF-2026-10-07.md`, at main `4e6f1bdd9b9869c516a487f1e3ca35729e7bb4bf`.

**Workspace:** `C:/Users/genki/Documents/Codex/NaturalGhastmod`, isolated fresh checkout; branch `codex/natural-flight-foundation-20261007`. No product-code changes made during planning. Existing Downloads folders and dirty Tech Hub work remain untouched.

## Global constraints

- Preserve `soutou_ghast:soutou_ghast`, no natural spawning/boss bar, visual-only armor.
- `Brain -> Movement Intent -> Maneuver -> Flight Controller -> Actual Motion`; only the controller owns motion changes.
- Acceleration authority exceeds braking; lateral authority is bounded so high speed yields broad turns.
- Normal combat uses a frontal region and range band, not constant orbit or an exact radius. Camera jitter must not drag the region every tick.
- No raw player-input access, teleport correction or omniscient pursuit after LOS loss.
- Retire legacy combat selection from the active slice. New attacks, rally, feints, Mobility Context, major arts and Domain remain later work. Do not expose an incomplete foundation as a finished combat boss.
- Do not merge Draft PR1 or treat its engineering Score as final danmaku content.
- Do not overwrite original worlds, existing runtime configurations or retained evidence. Tank work uses a new private copy/configuration, fresh owner/run identities and finite budgets.
- Implement related changes together, then one combined unit check. Run an earlier focused check only when it resolves a material uncertainty or reproduces a regression.

## Preflight evidence

- Main and local checkout match the pinned SHA above.
- `JAVA_HOME=C:/Program Files/Java/jdk-17; ./gradlew.bat compileJava --no-daemon --console=plain` reached genuine Java compilation and exited 1: four errors from `BaseFireBlock` import/use and two `LargeFireball.explosionPower` accesses. This is a current baseline failure, not a redesign regression.
- Current entity actively registers legacy combat, random-float, look and shoot Goals; its `aiStep` updates old phases. New motion must replace these active routes instead of competing with them.
- NaturalGhast's build has no `KNEEKURA_DEBUG_FORGE_BRIDGE_SRC` / debug source-set integration. Existing Tank configurations point at reimu-mod, so they cannot attest a NaturalGhast run.
- Local integrated LAB source is available at `C:/temp/kneekura-tank-mod-profile-20261006/tech-hub`, HEAD `c3699539388af68495ff1529af3576ff4926e9bc`. Verify its exact identity/contracts again before use; it is not the old standalone LAB repository.

## Work unit 1 — Flight, facing and anchor core

**Create**, under `src/main/java/com/genki/soutoughast/entity/ai/flight/`:

- `FlightVector.java`: finite immutable 3-D value and bounded vector operations.
- `FlightController.java`: one-tick velocity update from actual velocity and an explicit intent; separate acceleration/braking/turn constraints and soft arrival.
- `CombatFacing.java`: horizontal look reference with deadband, sustained-change tracking and bounded rotation; reset on subject change.
- `CombatAnchor.java`: player-relative region, preferred range state and soft return intent; transient maneuver intent does not overwrite the anchor.
- `FlightFoundationTest.java`, under matching `src/test/java/...`: dependency-free assertions for this entire core unit.
- `tools/test-flight-foundation.ps1`: compile/run these exact core/test files with JDK17 into ignored `build/flight-tests`, fail on any compile/assertion error.

**Interfaces:** `FlightController.step(actualVelocity, intent)` returns the next bounded velocity. Intent explicitly distinguishes movement, brake and hold. `CombatFacing.update(observedLook)` returns a normalized horizontal reference. `CombatAnchor.evaluate(playerPosition, facing, bossPosition)` returns TOO_CLOSE / COMFORTABLE / TOO_FAR plus a preferred point/return intent; no attack or world mutation.

- [ ] Write grouped regressions for accelerate vs brake, finite/zero vectors, opposite desired direction, high-speed lateral turn, soft arrival, small look jitter, sustained 180-degree facing change, vertical look, translated player, range boundaries, maneuver/return anchor preservation and subject reset.
- [ ] Run the grouped missing-feature/reproduction checks once; inspect the actual failure before implementation.
- [ ] Implement the whole core unit. Choose explicit conservative tuning as provisional development parameters, not final balance; record the selected values and rationale.
- [ ] Run the grouped core suite once after related edits. It must prove bounded changes, no velocity reversal/snap, finite state, no fixed orbit, and eventual smoothed-facing/anchor recovery. Fix only observed failures and rerun the affected group.

## Work unit 2 — Forge integration and compile readiness

**Create:** `entity/ai/SoutouGhastAnchorGoal.java`, `entity/ai/SoutouGhastInertialMoveControl.java`.

**Modify:** `entity/SoutouGhast.java`; the two established compile defects in `entity/projectile/SoutouGhastFireball.java` only as needed for a genuine full-source build. Existing renderer/assets remain unchanged. Retained legacy source is historical and must not be registered by the new foundation.

**Consumes:** Work unit 1 interfaces and actual server position/velocity/target/LOS. **Produces:** an inertial, non-attacking foundation that can be summoned and observed; controller diagnostics identify active intent/range state without claiming a causal thought model.

- [ ] Bind the controller to actual Minecraft velocity each tick; do not run a second movement integrator or double-apply gravity/drag. Verify Ghast travel semantics from the exact Forge artifact before selecting the application site.
- [ ] Replace active legacy movement/shoot Goals and old phase ticking in the foundation boundary. Reset subject-bound facing/anchor state on target loss/change; unseen targets do not supply live position/facing to the planner.
- [ ] Gate candidate motion using bounded AABB sweeps of the real 4x4 entity. Obstruction requests braking/clearance recovery; it does not teleport, silently pass through blocks or issue unbounded world scans.
- [ ] Repair the wrong import. Replace private explosion-field access through owned state or a verified public contract, with constructor/save/load consistency; do not revive obsolete attack behavior or change explosion policy as a workaround.
- [ ] Run core regressions and one exact `compileJava build` pass. Record deprecations and actual task/exit results separately. Compile success is not flight/runtime acceptance.

## Work unit 3 — Registered Tank verification

**Integration scope:** opt-in LAB bridge build wiring plus a new private NaturalGhast launch profile/test harness. Review exact bridge API and source-set ownership first. Preserve normal product packaging, existing settings and original worlds. Confirm any required tracked configuration overwrite with the user; creating a fresh private test configuration does not authorize replacing another project's profile.

- [ ] Bind exact NaturalGhast source/build/resources, selected LAB observer, Java/Forge, disposable world baseline, run/process/owner/lease and selected entity UUID. No old reimu READY/owner receipt may stand in for these identities.
- [ ] Preflight world geometry and the 4x4 boss envelope against planned movement. The historical 19x19x11 Tank cannot establish unrestricted preferred-range flight. Use only fitting bounded checks there; any larger private region requires the existing registered geometry/owner flow, never resizing the original save.
- [ ] Start one bounded session only after source/unit/compile readiness and current owner/time-budget checks. Observe stationary hold, acceleration, braking, turning, clearance, frontal recovery and target-loss/change. Group scenarios in the same run when their identities and cleanup remain valid.
- [ ] Collect canonical position/velocity/intent observations and visible frames; preserve gaps and raw/derived distinctions. Confirm no old shots/phase actions, no collision penetration, no controller snap, correct cleanup/finalization and unchanged original-world hashes.
- [ ] Record PASS/FAIL/INCONCLUSIVE/NOT_RUN per criterion. Pure-Java success, a submitted draw or READY alone does not prove movement, visual quality, performance or multiplayer. If exact integration is unavailable, record its concrete blocker and leave runtime criteria open.

## Review focus and stopping conditions

- Vanilla Ghast travel may damp motion after controller application: check real traces, not only pure-core arithmetic.
- LOS loss, dimension change and target replacement must not retain another player's anchor or disclose unseen live state.
- Vertical/opposite facing and floating-point extremes must not create NaN, instant 180-degree turns or anchor spin.
- Geometry sweeps must account for the full entity box and prediction horizon; a clear ray is insufficient.
- Client interpolation, save/reload, old runtime-owned state and cancellation/cleanup may differ from normal single-session unit inputs; retain those limits until exercised.

After all three units, obtain one whole-slice review, fix material findings and run only affected grouped checks. Leave new attacks, tactical selection, feints, environment transitions, major arts, final content/balance, broad performance and multiplayer acceptance explicitly open. No push, merge or original-world mutation is included in this initial plan.

## Status

2026-10-07: proposal ready for user review under the supplied AGENTS.md large-change rule. Shared Tech Hub docs are updated and checked; design/handoff read; baseline compilation failure reproduced. Implementation and Tank launch have not started.
