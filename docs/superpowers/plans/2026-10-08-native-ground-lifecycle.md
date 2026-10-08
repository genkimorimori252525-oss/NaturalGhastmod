# Native Ground lifecycle implementation plan

> Execute inline with superpowers:executing-plans. User authorizes continued implementation and compact Astra decisions without another approval round.

Goal: verify actual Ground AI/controller support-loss braking and clearance hysteresis in one bounded private TANK_CORE environment trial. Implements redesign v0.6 Ground transitions and the existing Ground fallback plan; does not force selection or add gameplay.

Compact Astra ruling: prioritize deterministic native Ground lifecycle over further probabilistic trials. Transition safety blocks acceptance of Ground; durable entity/server restart and late-client synchronization follow as correctness gates. Human counterplay/cues/balance require gameplay acceptance. Keep Ground barrage deferred under the user's broader danmaku exclusion. Cosmetic polish is optional.

Architecture: existing fresh DOMAIN_RELIABILITY fixture gains a separately named GROUND_RELIABILITY variant: centered genuine20HP Player, active100HP boss bottom225, full stone floor223 and ceiling230, same52x24x52 room, no seal/ScopedOwner. A private-only Forge probe observes native server END ticks and changes only declared ceiling cells and one confirmed floor tile. Actual AI, sensing, target selection, health, movement, randomness and sole velocity writer remain authoritative. No tick/setTarget/teleport/velocity/input/invulnerability overrides.

Bounds:240ticks AND20seconds after fresh authenticated request; fail if expected behavior is absent. At most2704ceiling cells plus one floor tile, four ceiling transitions, bounded restoration with exact owned-state guards; no chunk loading/block entities/third-party overwrites. Record finite rows and mutation summaries only. Prevalidate every batch and its loaded private-world identity. Preserve failed runs and original85raw files/acceptedJAR; never reopen original/finalized worlds via RegionFile/NBT.

One grouped sequence: wait for natural supported Ground/readable pause; remove one body-footprint support tile for8ticks, require SAFE_HOLD/brake/no new offense, restore it; wait for native supported Ground; open ceiling for10ticks, require no takeoff, close for5ticks; reopen ceiling, require20consecutive valid samples before native TAKEOFF, then physical ascent at least3.8blocks and AIR. Retain identical world-space anchor center/radii/generation throughout. Ground cooldown/health may legitimately limit the scenario; report FAIL/partial rather than healing/freezing actors. On exception restore only cells still equal to the probe's own overlay; never force restoration over unknown state.

Files: `tools/tank/GroundLifecycleProbe.java` (private protocol/native observation/environment mutation), `ground-lifecycle-results.mjs` and `.test.mjs` (strict trace-derived acceptance), `run-ground-lifecycle.mjs` (source-pinned private build/launch/closure), `PrepareFlightTank.java` (new isolated fixture variant), `GroundLifecycleGeometryTest.java` (real mapped palette/geometry checks). No new dependencies/public product APIs.

- [ ] Write analyzer negatives first; retain genuine RED. Reject premature takeoff, missing reset, continued offense after support loss, invented braking/teleport, changed anchor, actor/input overrides, missing rows/mutations or exceeded bounds.
- [ ] Implement minimal analyzer/probe/fixture/harness; grouped relevant Node and mapped javac/geometry verification. Include actual changed-support/native controller regressions if a product defect is found.
- [ ] One compact scoped Astra review, one Important/Critical correction pass; no repeat whole Ground/Domain review.
- [ ] Commit/push pinned source, run one fresh finite native scenario; independently verify raw rows/mutation/result hashes, canonical cleanup/evidence closure and historical85/acceptedJAR. Keep failed receipts unchanged.
- [ ] Publish English scope/evidence/decision and Tank feedback. Do not claim unload/restart/client synchronization, human input/cues/balance or full NaturalGhast release from this controlled environment trial.
