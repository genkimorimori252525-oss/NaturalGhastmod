# Flight foundation

Movement foundation for redesign v0.6 and the corrected handoff. This is an incomplete, non-attacking development entity. Registry, armor, renderer, sounds and existing save compatibility are retained. Legacy attack and orbit Goals remain unregistered; they are not redesign implementations.

The isolated `codex/natural-tactics-20261008` branch adds observed tactical scoring/memory and finite feint composition over the accepted swimming. [Tactics verification](OBSERVED-TACTICS-VERIFICATION.md) records two native lateral recipes, ordinary swimming dominance, preserved original/JAR hashes, failed preparations and unverified cases. Current Grand Danmaku work is explicitly excluded. Other attacks/ground/domain units remain pending; this is not full Boss acceptance.

**Correction implemented,2026-10-08:** a boss-owned world-space region is retained during combat, with actual gentle swimming. [Redesign v0.6](design/NATURAL-GHAST-REDESIGN-v0.1.md) and the [approved correction design](superpowers/specs/2026-10-08-combat-region-swimming-design.md) supersede camera-following anchors and stop/start drift. Open-space region40×16×40; visual acceptance remains the user's next check. See the [new verification receipt](COMBAT-REGION-SWIMMING-VERIFICATION.md).

## Active behavior

- A server Goal reads target position/eyes only for a living visible target; Player look is not read. LOS loss preserves the region and uses cached observed geometry/local safety; no hidden position/look tracking.
- CombatAnchor stores20/8/20 radii, world-space center, target identity, generation and reason. Normal Player movement/camera turns do not move it; target replacement or sustained necessary relocation can. Dwell40, cooldown100 and targetless expiry200 ticks. An identical proposed region is not repeatedly reselected.
- The22–34 range is soft. RETURN enters the retained region rather than its exact center; ordinary in-region motion is not canceled by crossing a range boundary.
- The controller reads actual velocity each tick. Development limits: speed 0.65, forward acceleration 0.11, braking 0.035 and lateral acceleration 0.045 blocks/tick. Hold decelerates instead of teleporting or zeroing motion.
- `travel` applies that velocity once through Minecraft collision handling. Vanilla FlyingMob input thrust and drag are replaced to prevent a second competing velocity integrator.
- One conservative swept 4×4 AABB checks the bounded stopping horizon. Obstruction requests braking; motion can resume when the intended sweep becomes clear. This is not full obstacle navigation.
- HOLD/DRIFT includes sustained actual3D swimming:40–120-tick commitments,0.1–0.18 speed target and intent easing0.004/tick. Waypoints use an interior margin for turns, not a smaller home region. Mobility Context enters tighter space after6 consistent samples and opens after20. Six coarse body sweeps and at most two actual candidate sweeps reject infeasible directions; diagonals are conservative. Ground-forced brakes; scuttling/landing are pending.
- A dedicated LookControl applies bounded yaw/pitch after the Goal tick; the vanilla LookControl cannot erase the supplied pitch. Clearance does not inflate behind the movement, so existing floor/wall contact permits safe departure.

These values are development tuning, not validated final balance. Do not add attacks until the movement/perception slice has adequate native coverage.

## Grouped verification

```powershell
./tools/test-flight-foundation.ps1 -JavaHome 'C:/Program Files/Java/jdk-17'
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
./gradlew.bat compileJava build --no-daemon --console=plain
# Optional existing mapped-host classpath, no Minecraft launch:
./tools/test-forge-clearance.ps1 -ClasspathFile PATH_TO_HOST_JAVA_ARGS -JavaHome PATH_TO_JDK17
./tools/test-tank-fixture.ps1 -ClasspathFile PATH_TO_HOST_JAVA_ARGS -JavaHome PATH_TO_JDK17
```

The assertion runner is dependency-free and executes directly; Gradle's default test task does not discover its `main` method. Implement a coherent unit, then run the relevant grouped checks. Run an earlier focused check only to resolve a material risk. Never trade away quality gates to reduce launch count.

## TANK_CORE connection

Read Tech Hub `docs/PROJECT-GUIDE.md` and its current LAB/Tank documentation before using it. `TANK_CORE` is the user's Tank profile. The LAB host supplies Forge/TLM debug instrumentation; NaturalGhast supplies its own source-built development jar and target registration. No LAB dependency or observer is added to the production artifact.

The tools under `tools/tank/` prepare a fresh private original-save copy, bind the NaturalGhast mod/container/class resource, and collect a finite read-only supplement alongside canonical LAB evidence. They are opt-in development tools, never automatic product entry points. Original worlds, existing configs and finalized runs must remain unchanged.

NaturalGhast foundation/flight preparation now removes exact root `touhou_little_maid:reimu` seed entities from the bounded room chunks; other IDs and NBT remain intact. `fixture.json` records `seedReimuRemoved`. This does not remove TLM instrumentation or forbid explicit Reimu-subject experiments such as MOB_POV. Already prepared/live copies are not retroactively rewritten. Mapped regression:5 entity-filter and8 fixture checks PASS. Fresh offline flight/foundation readback:1 Reimu removed each, respectively3/2 other entity NBT preserved and one active NaturalGhast added.

Do not open an original save with Minecraft `RegionFile` merely to inspect it: that API may extend files to sector boundaries. Audit a separate disposable copy; use file hashes/counts for original preservation. The first readback audit exposed two zero-padding changes in the original; user-authorized restoration from exact-hash copies returned all85 files to their pre-session hashes/count. Retain this failure and its recovery, not an uninterrupted-preservation claim.

`node tools/tank/run-foundation-tank.mjs` requires explicit `--lab`, `--template`, `--original`, `--classpath-file` and `--java-home` paths. The template must select TANK_CORE and use the established private init-script convention; it is not a generic Minecraft launcher. Commit target sources first. The runner builds that exact clean checkout once before packaging; the host is selected separately with `--project-dir` and its Forge property is scoped to 1.20.1-47.4.10. The original 47.2.0 host cannot load the target's newer constructor API. This adjusted host is not an attestation of the original profile's compatibility/performance.

The historical19×19×11 fixture cannot contain the22–34range; it is not a universal Tank size limit. The current owner excludes Player action subjects. A fresh56×16×56 private fixture now observes a real survival Player without granting Player mutation permissions. Its small sealed action Arena opens one prebuilt aperture; the larger physical room supports read-only flight observation. `TANK_CORE` selects the MOD profile, not geometry. Pure-Java assertions do not certify Minecraft behavior or visual quality.

`node tools/tank/run-flight-tank.mjs` accepts the same five explicit path arguments as the foundation runner. It requires a clean source checkout, builds that exact revision, prepares the bounded fixture, dispatches one registered block operation, observes at most600 server samples and requests one raw PNG explicitly. No default production recording. The current template is an established TANK_CORE host, not an arbitrary save launcher. Use a registered host classpath arg file; mapped fixture tests remain separate from the product build. Run `node --test tools/tank/flight-results.test.mjs tools/tank/flight-owner.test.mjs` before a costly native replay.

Initial pre-review v0.6 native static acceptance used a52×24×52 room: generation1;508 swimming samples span19.31/8.77 with stopped0. The reviewed source's latest static-look report remains FAIL after camera variation (subsequently confirmed by the user); its separate read-only measured-motion audit observes generation1,560 swimming samples span24.68/6.44, stopped0. This is not controlled camera-turn acceptance. Current manual launch uses that exact reviewed artifact with DEBUG0/no observer. Full-room live roster and cardinal views were not collected; selected server coordinates must not imply whole-Tank coverage (Tech Hub AF-0017). The earlier v0.5 receipt remains historical. Moving Player, deliberate LOS and subjective visual judgment remain pending; tactics, ground locomotion and danmaku remain separate.

See the [current swimming receipt](COMBAT-REGION-SWIMMING-VERIFICATION.md), [historical v0.5 movement receipt](FLIGHT-MOBILITY-VERIFICATION.md) and [historical foundation receipt](FLIGHT-FOUNDATION-VERIFICATION.md) for exact checks, scope, failures, source identities and retained evidence.
