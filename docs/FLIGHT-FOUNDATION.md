# Flight foundation

Initial implementation of redesign v0.5 and the 2026-10-07 handoff. This is an incomplete, non-attacking development entity. Registry, armor, renderer, sounds and existing save compatibility are retained. Legacy attack and orbit sources remain unregistered; they are not redesign implementations.

**Correction, 2026-10-08:** the user requires a broad boss-owned world-space region retained during combat, plus natural floating/swimming. Camera-following anchors and stop/start idle motion described below are the current implementation, not the corrected target. [Redesign v0.6](design/NATURAL-GHAST-REDESIGN-v0.1.md) and the [reviewable correction design](superpowers/specs/2026-10-08-combat-region-swimming-design.md) supersede that behavior; the product rewrite is pending. Initial proposed open-space size40×16×40, not a10-block tether.

## Active behavior

- A server Goal observes only a living, visible target. Target change or LOS loss resets facing history; loss requests hold without reading unseen position or look.
- Horizontal facing rejects 4-degree camera jitter and turns at up to 1.5 degrees/tick after three sustained observations. Vertical look preserves the last horizontal direction.
- The frontal anchor accepts 22–34 blocks of 3D distance, 4–10 blocks of altitude and a 35-degree horizontal bearing band. Its soft preferred point is 28 blocks away and 6 blocks higher. A maneuver's current position cannot replace the anchor.
- The controller reads actual velocity each tick. Development limits: speed 0.65, forward acceleration 0.11, braking 0.035 and lateral acceleration 0.045 blocks/tick. Hold decelerates instead of teleporting or zeroing motion.
- `travel` applies that velocity once through Minecraft collision handling. Vanilla FlyingMob input thrust and drag are replaced to prevent a second competing velocity integrator.
- One conservative swept 4×4 AABB checks the bounded stopping horizon. Obstruction requests braking; motion can resume when the intended sweep becomes clear. This is not full obstacle navigation.
- Movement selects common committed HOLD intervals, short compact DRIFT and anchor-based correction. Mobility Context enters tighter space after6 consistent samples and opens after20. Six coarse body sweeps and at most two actual candidate sweeps reject infeasible directions; diagonals are conservative, and drift checks the inner radius along its entire chord. Ground-forced brakes; scuttling/landing are pending.
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

Static Player acquisition, acceleration, visible frontal arrival, gradual braking and actual short drift have scoped native acceptance under v0.5. Moving/facing-changing Player, target replacement, deliberate LOS recovery, ground locomotion and tactics remain unverified. Next correct persistent region ownership and smooth swimming before Tactical Evaluator/repetition memory; danmaku content is separate.

See the [current movement receipt](FLIGHT-MOBILITY-VERIFICATION.md) and [historical foundation receipt](FLIGHT-FOUNDATION-VERIFICATION.md) for exact checks, scope, failures, source identities and retained evidence.
