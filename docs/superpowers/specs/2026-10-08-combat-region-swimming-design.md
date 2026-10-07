# Persistent combat region and Ghast swimming

Status: approved2026-10-08 and implemented; user visual acceptance pending. User-confirmed semantics: retain the region during combat and reselect only when necessary; give the boss broad freedom, not a roughly10-block region. Numerical tuning is delegated. See [the implementation/native receipt](../../COMBAT-REGION-SWIMMING-VERIFICATION.md); historical diagnosis below describes the superseded implementation.

## Evidence and scope

The current v0.5 implementation recalculates `CombatAnchor` from Player position and smoothed facing each visible tick. Its HOLD/braking converges to zero velocity; the planner alternates8-tick compact drift with quiet intervals. These explain camera-coupled correction and missing ambient motion. They do not establish every cause of client-visible jitter. The user's native observation supersedes any inference of visual quality from the previous static-player600-sample receipt.

Correct anchor lifetime, range policy and sustained movement together, preserving registry/save compatibility, the single velocity owner, swept4×4 clearance and observation boundaries. Attacks, final danmaku, Ground Combat and full navigation remain later work.

## Region ownership

- Store a world-space center, radii, target identity, generation and selection reason for the encounter. Initial open-space radii20/8/20 give40×16×40 blocks of swimming volume. A four-block body requires route clearance beyond its center; this is not a collision-free room-size guarantee.
- Select near the boss's feasible combat position using currently observed Player geometry and terrain; Player gaze cannot select or move the region. Range influences initial choice and tactical scoring, not continuous center recomputation.
- Retain the region through normal swimming, dodge and recovery. RETURN enters the region, then releases to swimming; it never demands exact-center arrival.
- Reselect for target replacement, a committed tactical transfer, sustained unusable space/LOS, or substantial observed disengagement. Proposed ordinary thresholds:40 consistent visible ticks beyond60 blocks from the retained center, or40 visible ticks with no safe candidate route;100-tick cooldown. These are development values to verify, not approved balance. Emergency braking does not wait for a cooldown. Clear encounter state after200 targetless ticks, not a one-tick visibility loss.
- LOS loss stops reading hidden Player position/look immediately. Retain the last region and use bounded local safety/floating; do not relocate using an unseen target. Dead/invalid targets cannot retain attack intent. Reacquisition starts with the retained region unless a valid reselection reason is observed.
- Confinement limits route choice and swimming scale explicitly; it must not silently redefine the default open-space region as a tiny tether. Ground-forced fallback remains pending.

## Motion policy

Use gentle actual horizontal and vertical swimming during normal HOLD/quiet periods. Choose feasible waypoints broadly across the retained region; distance from the Player is a soft preference. Keep commitments for roughly40–120 ticks, ending early for collision risk, target invalidation or a meaningful maneuver. Avoid per-tick waypoint replacement and repeated8-tick acceleration/stop pulses.

Initial ambient speed target0.08–0.18 blocks/tick, below the existing0.65 cap. Ease intent changes and vertical reversals through the existing controller. Broader route variety and measured displacement must demonstrate free swimming; do not implement cosmetic bobbing around a fixed spot. Brake hard enough for safety while preserving the established acceleration/braking distinction. Low-speed movement must not turn a predicted obstruction into repeated unsafe retries.

Keep one Flight Controller as the only velocity writer and `travel` as the single collision application. No teleports, hidden target tracking, renderer-only motion, second vanilla thrust, or temporary removal of clearance guards. Player-looking-away behavior is normal; no correction is required just to enter the screen.

## Coherent implementation units and grouped checks

1. Persistent region state and planner policy: replace camera-driven anchor evaluation; connect acquisition, retention, reasoned reselection and return. Group deterministic lifecycle, camera-turn invariance, ordinary Player displacement, disengagement dwell/cooldown and LOS tests. Adapt old frontal-volume tests because that requirement is explicitly superseded; retain velocity/clearance coverage.
2. Sustained swimming: integrate committed feasible3D waypoints, smooth low-speed intent and safety interruption with the existing movement controller. Group acceleration continuity, vertical/horizontal motion, broad waypoint reachability, boundary recovery and swept-body collision checks. Then run the related pure and mapped suites and Forge build once for this coherent unit; earlier focused tests are justified by risk or failures.
3. Bounded TANK_CORE acceptance: fresh Reimu-free copied save, large enough room for the proposed region and stopping clearance. Proposed physical interior52×24×52:64896 interior cells,75816 including the shell, within existing65536/100000-cell budgets. The old56×16×56 room cannot contain16 blocks of vertical center freedom plus the4-block body. Placement and actual stopping sweeps still require validation; do not claim room dimensions alone prove clearance. First establish actual owner/world geometry; then test fixed-position Player camera turns, ordinary Player movement, LOS interruption/reacquisition and obstructed clearance in finite runs. Use genuine Player inputs, never inject fake perception or grant unregistered Player mutation. A human may turn the camera for the manual portion. Collect finite server motion/region-generation observations and explicitly requested frames only. Keep original/finalized saves unchanged.

## Acceptance and limits

- Yaw/pitch changes with a fixed Player position preserve exact region center/radii/generation and do not generate camera-recovery movement.
- Normal swimming spans substantially more than a10-block neighborhood over a bounded open-space run, while remaining/recovering inside the40×16×40 region. It must include both horizontal and vertical movement; waypoint coverage alone is insufficient evidence of actual swimming.
- Route commitments avoid frequent acceleration/zero-speed cycling. Inspect speed, acceleration and direction changes plus real-client presentation. Pure controller tests do not certify visible smoothness.
- Explicit reselection records reason and dwell/cooldown outcome; ordinary camera turns and one-tick LOS loss never count. LOS tests demonstrate no hidden position/look reads.
- Every sample retains4×4 collision clearance and existing speed/health/NoAI expectations. Safety may override floating near obstacles; do not relax guards to pass a visual test.
- A too-short Tank or inadequate registered interaction can make acceptance INCONCLUSIVE. Enlarge/reprepare a private fixture or require finite human input; never turn a failed fixture into a reduced product requirement.

Existing static-player acceptance remains historical v0.5 evidence, not acceptance of this design. Final visual feel, multiplayer and performance remain unverified until the corresponding native checks run.
