# Fireball presentation and Rally endpoint correction

Human inspection on2026-10-23 explicitly requests larger individual normal-fireball visuals and more accurate boss returns; Grand Danmaku stays excluded. Preserve broad world-space swimming, current natural relocation, damage/collision and accepted artifacts.

The initial v0.6 Standard plan's presentation scale1.5 is superseded by visual scale4.0 for the shared Standard family. Pick radius1.5, physical1×1body, damage and explosion remain unchanged. This changes readability, not reach or damage balance.

The old return aim is a frozen direction originating at the boss eye, although the actual ball can be several blocks away at the admitted return. Lock the observed Player body-center endpoint plus observed-velocity lead at reaction start (at most2blocks/12ticks of prediction). Derive the launch direction from the actual ball position to that frozen endpoint. Preserve6tick tell, finite reach/approach/loaded LOS, fallible65%admission, one return per ball and no homing. Lost target LOS aborts; actual return ray blocked by a collider declines. The accepted flight controller retains sole boss-velocity ownership.

- [x] Add pure RallyAim endpoint/origin tests and keep old charge/fallibility/dodge regressions.18new checks and full flight core suite PASS, including12036broad-region assertions.
- [x] Forge compileJava/processResources PASS21s. Mapped Standard10 and Committed10 checks PASS.
- [x] Package a distinct uninstrumented mapped JAR, pin actual new source/classes/hash and preserve old verified/accepted JARs. Use the repaired TechHub v2 grid host and safety/Night-Vision fixture for one genuine manual case; input/path/cues/visible size acceptance requires actual matching observation.

Existing `GroundProjectileTest` under plain Java fails in untransformed Forge Bootstrap (`NetworkEvent` constructor/listener setup). Removing Bootstrap experimentally instead fails registry initialization; that test edit is reverted. Preserve both failures, do not claim the whole mapped projectile script passed, weaken assertions or add reflection/bootstrap bypasses. Unchanged Ground policy and its native acceptance are separate from these scoped Standard/Rally changes. A proper transformed Forge test harness is required for that raw-Java gate.

Recovery: use installed normal STOP and actual owned-exit/evidence closure; retain fresh failed cases and revert only this source unit/profile selection if needed. No original/finalized NBT edits, fake input, forced selector, lease renewal or accepted-JAR replacement.

## 2026-10-23 original-grid and Rally acceptance

TechHub source9e1011e/bundlef09595cb with Natural sourcea6f79d3/product55a361e9: fresh casea2499511 reached actual READY26.752s. Original KneekuraDebugTankView.Grid style submitted524lines/5576passes and cleared on STOP. Actual clean ACK, VERIFIED_EXIT, exact24raw/native/canonical events and EVIDENCE_COMPLETE; zero drops/errors/partial files/images. Original85raw files, sealed template/inputs and old accepted/verified JARs remain byte-identical. Closure/preservation receipt6deb72ccf48e4e66aadcded6a8a9176f, task MANUAL-RALLY-ADAPTER-20261023.

Direct human feedback after this case: grid looks good and Rally passed. Record scoped human acceptance of the observed grid/Rally interaction, separately from machine lifecycle/event metadata. Do not infer all missed-return cases, moving counterplay, damage/balance, special profiles, Domain400combat, fixed/mob cameras or overall release. Earlier cyan-style rejection and driver/completeness guard failure remain unchanged. New speed product requires its own observation.
