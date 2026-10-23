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
