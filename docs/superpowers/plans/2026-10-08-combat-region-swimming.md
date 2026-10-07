# Combat Region Swimming Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Retain a broad boss-owned combat region and swim naturally without chasing Player gaze.

**Architecture:** Stateful `CombatAnchor` owns encounter identity/region/reselection; `MovementPlanner` commits feasible swimming waypoints and eases intent. The existing Flight Controller remains the sole physical velocity integrator. Goal wiring reads target geometry only with LOS; fixtures and finite observation remain development-only.

**Tech Stack:** Java17, Minecraft1.20.1/Forge47.4.10 mapped verification, PowerShell/Node; no new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-08-combat-region-swimming-design.md`, approved by the user2026-10-08 with explicit instruction to proceed; manual visual acceptance is reserved for the user. Execute inline.

## Global Constraints

- Open-space radii20/8/20; no roughly10-block default region.
- Camera yaw/pitch never translates/rotates the region; no hidden position/look reads after LOS loss.
- Repeated brief acceleration/stop pulses are not the idle rhythm; normal commitments40–120 ticks, ambient speed0.08–0.18, existing cap0.65.
- Preserve single velocity owner, swept4×4 clearance, registry/save compatibility and original/finalized saves.
- Reselection dwell40 visible ticks, disengagement60 blocks, cooldown100 ticks; clear after200 targetless ticks.
- Implement/verify coherent units; no attacks, final danmaku, Ground Combat or full navigation.

## Review Focus

- Reacquisition after transient LOS loss preserves encounter state; invalid/dead target cannot leak hidden geometry.
- High external velocity near a wall retains braking and full-body stopping checks.
- Blocked or confined candidates cannot create unsafe retry loops or silent default-region shrinkage.
- Waypoint expiry and reversal do not create repeated full stops or hidden second velocity integration.
- Enlarged room/preparation cannot alter original bytes or exceed allocation/authority budgets.

### Task 1: Persistent region and sustained swimming

**Files:** Modify `CombatAnchor.java`, `MovementPlanner.java`, `SoutouGhastAnchorGoal.java`, `SoutouGhastInertialMoveControl.java` under existing AI directories; tests `FlightFoundationTest.java`, `MovementMobilityTest.java`; create `CombatRegionSwimmingTest.java` and add it to `tools/test-flight-foundation.ps1`.

**Interfaces:** `CombatAnchor.observe(UUID subject, FlightVector target, FlightVector boss, boolean blocked)`, `unobserved(boolean targetPresent)`, `region(): Region`; Region holds center/radii/generation/reason. Preserve `evaluate(target,facing,boss)` signature for internal migration, but facing is ignored and evaluation never moves an established region. Planner continues producing `Plan` with primitive/intent/waypoint/range/inRegion and bounded candidate predicates. Goal publishes read-only region state to its MoveControl for finite observation.

- [ ] Write grouped RED tests: assert center/radii/generation invariant under yaw/pitch and ordinary Player shifts; return enters the retained volume; range classification cannot evict an in-region swimmer; replacement/dwell/cooldown/unobserved lifecycle; simulate4000 open-space ticks and assert >10-block horizontal span, actual vertical movement, <2% rest after startup, capped/eased motion and stable generation. Assert blocked/ground safety and bounded query budget.
- [ ] Run `./tools/test-flight-foundation.ps1 -JavaHome 'C:/Program Files/Java/jdk-17'`, redirect to the plan workspace. Expected: new behavior assertion FAIL against old implementation; retain output.
- [ ] Implement interfaces and adapt explicitly superseded frontal/radius-hole tests. Initial region selection uses observed relative boss/Player geometry toward preferred28-block range, never Player gaze. Retain region while swimming; return to its interior boundary, not exact center. Choose broad feasible3D waypoints with soft range scoring, <=2 actual candidate queries,40–120-tick commitment and eased intent. Preserve safety controller and orientation tests.
- [ ] Run grouped pure assertions, mapped clearance assertions, then `./gradlew.bat compileJava build --no-daemon --console=plain` with JDK17. Expected: PASS/BUILD SUCCESSFUL; product artifact contains no observer helper.
- [ ] Commit selected product/tests/tools files; record results and decisions in the ledger.

### Task 2: Enlarged Tank and reviewable manual build

**Files:** `tools/tank/PrepareFlightTank.java`, `FlightFixtureTest.java`, `ObserveFlightTank.java`, related finite runner/results tests if their old assertions conflict; `docs/FLIGHT-FOUNDATION.md`, current spec/handoff/receipt and Tech Hub feedback.

**Interfaces:** Consume Task1 region/read-only MoveControl state and committed source. Fixture interior52×24×52,64896 cells,75816 with shell; same private-copy/exclusive-lock constraints, original hash preservation and intentional seed Reimu exclusion.

- [ ] Write/run RED fixture checks for new vertical clearance and dimensions; update only old numerical expectations that the approved geometry supersedes. Expected: FAIL with old16-high/56-wide geometry.
- [ ] Implement52×24×52 preparation and finite region telemetry; preserve registered action authority. Existing static-player v0.5 receipt stays historical. If its acceptance harness requires obsolete camera-relative behavior, do not relabel that result; use a separate versioned scoped audit or manual fixture, recording the limitation.
- [ ] Run mapped fixture/clearance and relevant Node checks. Prepare fresh disposable copies and inspect only another private baseline copy; raw hashes/counts establish original preservation. Expected: no Reimu, one active NaturalGhast, adequate body/room clearance, original85 hashes unchanged.
- [ ] Run a finite TANK_CORE movement pilot where the established host/authority allows it. Prepare a separately named manual Tank using the committed built artifact, optional explicit opening of the offline private observation wall and finite launch with runtime recording disabled. User performs camera-turn/movement/visual judgment; do not claim that acceptance before their inspection.
- [ ] Update English documentation/feedback with actual source/artifact identity, verification scope and failures; commit selected files.

## Final gate

One fresh-context whole-change review after the coherent implementation units; fix Important/Critical findings with RED→GREEN regressions. Preserve failed receipts and test logs. Leave an exact build/manual-save location and clearly separate implementation verification from the user's pending visual acceptance; no merge/push/automatic recording.
