# Committed fireball profiles

Base `0a33e05`; authoritative v0.6 projectile sections. User authorizes non-danmaku continuation; Astra recommends a coherent DelayedBurst/Curve/Lob unit. Preserve the accepted boss movement and Standard fallback. No world writes, new dependencies, hidden target reads, active ground profiles or Grand Danmaku.

## Contracts and rulings

- One registered dedicated profile entity derives from the new Standard family, sharing damage, impact, attribution and deflection rules. Existing legacy variant entity remains inactive. Profiles have finite precommitted paths; no late target steering or unvalidated continuation. A miss discards without airburst.
- Standard settled terminal speed1.9blocks/tick is the explicit tuning reference. Burst starts0.6x=1.14, slow12ticks, warning4ticks, acceleration4ticks, then1.6x=3.04; select only at least44blocks of travel to preserve several fast-phase reaction ticks. These are provisional, not a launch-speed comparison.
- Curve: SHALLOW2/NORMAL4/DEEP6block lateral amplitude, one smooth outward/inward sine arc, monotonic forward progress; choose side from observed strafe with deliberate variation. DEEP only open space. Candidate strengths fall back when conservative clearance fails.
- Lob: locked visible landing point;6–10block vertical arc candidates in open space, lower4–6 elsewhere. Stationary observed targets favor it. Cover memory/hidden landing targets remain outside this initial unit; confined lob requires full verified corridor, ground lob suppressed.
- Every complete finite segment has loaded swept1x1 clearance before launch; never call a loading lookup first. Runtime native projectile collision/Forge hooks remain authoritative for entities/changed geometry. Swept-body preflight is conservative; actual Fireball block impact uses the native center ray, so do not equate their geometry.
- Astra permits an explicit terminal collider set of at most four loaded blocks, exact BlockPos/collider states on the declared landing surface. Only the last segment may intersect that set; preceding/other collisions reject. Expected native center-ray final hit must belong to the set. No terrain changes are needed to validate.
- Valid melee immediately cancels profile and enters straight bounded rally. Reload/spawn retain path, progress and normalization independently of the owner; malformed/over-limit paths fail closed. No per-tick full-path network replication.
- Selection responds to observed range/strafe/stationarity/context plus repetition memory, not a random rotation. One offensive tell at a time; keep ordinary swimming. Real melee/rally/damage/balance gates from Standard remain NOT_RUN until genuine input.
- Astra whole-unit review `0a33e05..545fe41` found no Critical/Important issue and one candidate-loop minor. After natural trial `flight-2N1R0y` exercised Standard only, a compact Astra ruling promoted candidate fallback to Important: one rejected high arc must not suppress valid lower arcs. Fix per candidate only for expected range/speed failures; propagate unexpected exceptions. Regression reproduces height10 rejection with valid8/6 alternatives, RED→GREEN. Add only latest candidate/rejection telemetry, without altering selection probabilities. No retrospective causal claim for that trial; preserve it as FAIL for profile coverage.

## Coherent verification tasks

- [ ] Pure RED→GREEN finite trajectories/phase speeds/no U-turn/normalization/selection and bounded segment-validator tests. Preserve prior suites.
- [ ] Mapped loaded swept-body/declared terminal checks and bounded trajectory NBT codec tests; reject unknown/malformed state without silent Standard fallback.
- [ ] Register client/server profile entity, readable particle cues and spawn/reload clock; integrate observed selection and initial collision validation.
- [ ] Genuine Forge build, affected tests, one fresh compact Astra whole-unit source review and one Important/Critical correction pass.
- [ ] Fresh finite TANK_CORE natural profile trial, paired room roster and explicit telemetry/cue metadata; preserve original85 and lifecycle/evidence. Scope only the profiles actually exercised; no fabricated input or forced product selection.
- [ ] Publish decisions/receipts/remaining gates on scoped GitHub branches; record useful Tank friction. This is an implementation unit, not full-boss completion.
