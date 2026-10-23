# Optional higher flight speed

2026-10-23 human direction: the original grid looks good and Rally passed; continue implementation and allow the boss to choose up to2.5times the previous maximum flight speed. Existing broad swimming and natural rise/cross/turn/descent remain accepted. Grand Danmaku remains excluded.

## Decision and implementation

Raise the intent/controller ceiling from0.65 to1.625blocks/tick (32.5blocks/second at20TPS). This is optional capability, not a multiplier on all movement. Existing distance-aware CombatAnchor recovery and OverheadBombing RETURN can request the higher ceiling; their stopping-distance speed limits remain authoritative. Ordinary swimming0.1–0.18, gentle overhead relocation0.1/0.24/0.12, Ground0.55/0.65, projectile speed and attack timing are unchanged. Near goals and obstructed space may never permit the ceiling.

Keep forward acceleration0.11, braking0.035 and lateral acceleration0.045. The sole server velocity writer remains responsible for actual motion. A maximum-speed stop takes about47ticks and38blocks before safety margin, so increasing only the intent clamp is insufficient. Derive stopping lookahead from max(actual,next) velocity, including above-cap external impulses. Check the complete final Ground-adjusted swept body against loaded chunks before collision queries. At most16chunks may be checked; missing/invalid/over-budget coverage selects BRAKE without loading chunks. This preserves finite work and does not impose the maneuver helper's32block route limit on legitimate long stopping sweeps.

Compact Astra review identified the missing loaded-chunk gate as Important; adopt the gate. No new runtime input, selector, target, position or velocity-forcing API. These scoped changes are authorized by the direct human request and standing implementation delegation; ordinary decisions are recorded rather than reapproved.

## Verification and limits

Grouped flight-core suite PASS, including12,036region/swimming assertions and high-speed reach, bounded acceleration, broad turn/reversal, full simulated braking distance, actual CombatAnchor fast recovery/handoff,16chunk boundary, unavailable chunk, pre-query budget rejection, integer-overflow rejection and above-cap impulse coverage. Actual Forge compileJava/processResources PASS. TechHub task MANUAL-RALLY-ADAPTER-20261023 receipts:4f80b3300a7e46758a6f25e0f7e5031b (grouped tests),7095c0a808c44dfcb3082e931e852454 (Forge).

These are source/model/build checks. A new packaged product/profile has no native high-speed or subjective movement acceptance until actually observed. The previous product's Rally acceptance does not establish the new artifact's flight acceptance. Ground plain-Java Bootstrap remains BLOCKED pending a transformed Forge harness; do not weaken it or infer full non-danmaku release.

Recovery: select the previous immutable product/profile55a361e9; revert only this source unit if needed. Preserve original85raw world files, accepted/verified JARs, failed runs, finite leases, native STOP/owned exit and evidence finalization. Do not restore old runtime infrastructure.


## Packaged speed product and native window

Source2b21bd285c89215fc0cb1169abf7c23477b897cc pushed; distinct uninstrumented mapped product SHA2563fffaa3b09a6db7dedd69e34c5a367daccb811615106181e975f18085a7f8156,178classes. Compared with human-accepted Rally product55a361e9,172classes remain byte-identical; five expected controller/intent/anchor/Overhead/sole-writer classes change and FlightChunkCoverage is added. No private probes. Exact package/material preservation receipt9dd26e9e0f7849eab746388d3312c349; fresh sealed profile preparationec7b27b3d76b4128bd9eda4e3a475af1. Previous accepted/verified artifacts remain unchanged. Final compact Astra source check found no Important/Critical blocker after the loaded-route fix.

Human explicitly available before fresh case161f8906 launch. Same immutable TechHubf09595cb host and safety/Night-Vision/original-grid fixture; finite2400ticks/120s/512events/0captures. Actual READY27.345s. This is startup only; await independent closure/preservation and human observations. The sparse interaction lane does not measure peak flight speed. Do not claim measured1.625speed, arbitrary obstacle safety or all-move acceptance from this window.

## Human pace rejection and baseline correction

Human inspection of161f8906 rejects the pace: ordinary movement and turning remain sluggish despite optional boost. Explicit direction: raise overall movement pace and reduce the relative boost headroom; do not compound a2.5multiplier after raising the baseline. That case closed with17exact raw/native/canonical events, clean ACK/VERIFIED_EXIT/EVIDENCE_COMPLETE and original85/template/oldJAR preservation (receipta2151cbd1a644bfc992af618f56445b4). Lifecycle PASS and human pace FAIL remain separate. Previous grid/Rally acceptance remains scoped to its original material.

The following tuning supersedes the preserved earlier values above:

| Control | Previous | Revised |
| --- | --- | --- |
| Ambient swim target |0.1–0.18|0.2–0.36blocks/tick|
| Ambient intent easing |0.004|0.016blocks/tick per tick|
| Forward/braking/lateral acceleration |0.11/0.035/0.045|0.14/0.055/0.09|
| Body yaw/pitch step |4/3|7/5degrees/tick|
| Feint telegraph/reveal |0.14/0.32|0.21/0.48|
| Behind-player tell/cross/climb-descent |0.1/0.24/0.12|0.15/0.36/0.18|
| Observed Dodge confined/open |0.25/0.42|0.375/0.63|
| Bombing withdraw/align-recovery |0.35/0.4|0.525/0.6|
| Absolute optional ceiling |1.625|1.625fixed, no compounded multiplier|

Feint travel budgets use the new0.48speed; original finite recipe/relocation bounds and phase/facing/LOS/clearance gates remain. Shared Ground acceleration/braking changes, while its caps remain0.55/0.65. Projectile speeds, tells/cadence and damage remain unchanged. This is faster floating, not snapping to waypoints or Player camera.

Retain genuine regression discovery: doubling swim speed with0.008easing left the retained region at tick331;0.024easing violated the existing0.02per-tick smoothness guard.0.016passes both original containment and smoothness guards over4,000ticks without shrinking the region or relaxing those assertions. New checks require actual ordinary peak>0.34 and mean>0.2, plus bounded body half-turn within26ticks. Replace obsolete hardcoded acceleration bounds with current controller constants in Dodge/relocation tests; stopping-distance tests sum actual braking travel rather than retaining a stale distance threshold. Existing finite/geometry/ownership assertions remain.

Grouped full flight suite including Ground/shared-major PASS (f1097953ebd74121a38930772ff118f6); genuine Forge compile/resources PASS (fb0fd36b512c4ebd87ff969737599075). Compact Astra found no Important/Critical blocker, requiring affected Ground checks because shared response changed; these passed. Revised native pace acceptance remains pending.
