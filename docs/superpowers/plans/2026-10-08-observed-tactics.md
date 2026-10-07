# Observable tactics and maneuver composition

User authorizes continued implementation after the Tank feedback unit. Execute inline with `superpowers:executing-plans`; group related edits and relevant verification. GitHub main4e6f1bdd remains redesign v0.5. Local v0.6/user2026-10-08 retained broad world-space region supersedes camera-relative anchors. Human approval of current swimming is explicit; the previous controlled-camera fixture limitation remains historical.

## Protected baseline and scope

Product base dce89a7 (accepted implementation182fcb7), accepted artifact SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca`. Work in isolated `codex/natural-tactics-20261008`; do not overwrite the original checkout, old artifact, finalized saves or evidence. Keep20/8/20 region radii, reasoned reselection and the sole Flight Controller velocity writer. Normal swimming remains the common/default behavior. No Player-facing/camera input drives region or idle steering.

This first continuation unit implements tactical movement only: context/range scoring, bounded recent-action memory, observed target velocity, simple reusable feints with telegraph/commit/reveal/recovery. Movement, attack and timing remain separate. **No attacks or Grand Danmaku** in this unit. Standard Fireball/rally, special profiles, Overhead Bombing, positive Ground Mode and conflict-aware reversible Domain remain subsequent non-danmaku units; no claim that the boss is finished.

## Implementation

- [x] Add pure RED regressions for context/range selection and repetition penalty; pre-commit LOS cancellation, post-commit locked movement, safety cancellation, region preservation, rare/default swimming and single-controller physical simulation.
- [x] Implement bounded `TacticalMemory` and `TacticalEvaluator`; implement `ManeuverComposer` phases and future timing slots independently of attack effects. Four initial recipes: false approach, false retreat, lateral fake, vertical fake. Long quiet swimming intervals, bounded variation and candidate clearance prevent per-tick redecisions. Suppress ground-forced/unavailable maneuvers. Safety can override commitment.
- [x] Add `TacticalBrain` and wire observed geometry only after living-target LOS in the existing Goal. Publish compact read-only tactical state through MoveControl for finite development observation; no production logs/history.
- [x] Run full pure flight/regional assertions and genuine Forge build/mapped clearance; no new dependencies or changes to registry/save compatibility. Core12036 regional +22 foundation +25 mobility +19130 tactics assertions PASS; mapped swept-AABB5 PASS; Forge BUILD SUCCESSFUL.
- [ ] Extend the existing finite read-only development observer with action/phase/timing telemetry and add an honest bounded tactics analyzer. Preserve supplementary/canonical producer distinction and verified shutdown/finalization. Commit clean sources before fresh TANK_CORE trial; original raw85 hashes and accepted JAR must remain equal.
- [ ] One fresh whole-unit review, one Important/Critical correction pass if needed; retain failures and measured limitations. Publish source/decision history on the scoped branch, no merge.

Ruling: defer detached mob-eye E per Astra; retained room coordinates/cardinal views and selected server telemetry suffice. New movement is not tuned by constraining the boss for a camera. Unknown native recipe coverage remains unknown and is not inferred from passing simulations.
