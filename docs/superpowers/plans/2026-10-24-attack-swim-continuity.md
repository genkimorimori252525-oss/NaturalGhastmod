# Attack and ambient swimming continuity

Human requests uninterrupted natural floating during the attack animation, even while the body/shot direction is committed. Interpret this as continuing the existing broad-region course, not adding a new target-centered anchor or forcing forward motion into the Player. Grand Danmaku remains excluded.

Production AnchorGoal previously passed `engaged ? 0 : random` to both TacticalBrain and the ordinary MovementPlanner. Suppressing new movement feints during committed attacks therefore also forced new ambient waypoints to the same variation and slowest target speed. Separate the inputs: draw ordinary variation normally, pass zero only to tactical admission while engaged, and preserve the ordinary planner's independent variation and retained waypoint. Keep the frozen shot/body tell, charge/recovery duration, aim/fallibility, sole velocity writer, region lifetime, LOS and clearance rules. Extra ordinary random draws change the random sequence, not selection probabilities; no deterministic replay equivalence is claimed.

The human permits further overall pace increase. Raise ordinary targets0.2–0.36to0.24–0.4blocks/tick, easing0.016to0.019. Keep the independent absolute ceiling1.625; do not compound multipliers. Other aerial legs, yaw/pitch rates, acceleration/braking and projectile timing/damage stay as in82915b9.

Full grouped flight suite PASS, including12,037region assertions over4,000ticks, original0.02smoothness and containment gates, ordinary peak>0.38/mean>0.24, attack/Rally, relocation, Dodge and Ground policies. This verifies models, not production human course acceptance; Forge compilation and a new exact-class/hash package remain separate gates. No new test that merely mirrors the two input assignments is added. Compact Astra found no Important blocker in the separation.

The earlier jitter cause remains UNKNOWN. Same productea622335 was used in jitter-reportedf761f112 and human-non-reproducingec480857. That diagnostic trace sampled a stationary subject, so it cannot establish moving interpolation or a fix. Do not change network update cadence or claim PC inadequacy from these observations. TechHub's independently versioned optional motion trigger now starts from actual consecutive server-END displacement, retains the original deadline and records truncation/missing subjects. Source/observer changes and human acceptance remain distinct.

Recovery: choose immutable previous product/profileea622335 and retain all old runs/failed parameters. Revert only this source unit if needed. Never overwrite accepted artifacts or read original/finalized NBT.


## Final source/package preparation (2026-10-24JST)

Natural source68c7866, genuine Forge compile/resources c9dbc557c5b64d30860bdb7d2d416d23 PASS; grouped flight suite PASS (12,037region checks with original containment/smoothness plus attack/Rally/relocation/Dodge/Ground). Distinct mapped productSHA2568c0885c54272a0b394bbc7457ccd5950809af5b2667c2f944ca2ba44c99d816d,178classes. Two intentional classes changed: AnchorGoal and MovementPlanner. Strict packaging initially rejected an additional generated AnchorGoal$1 difference (retained FAIL2e36e088); actual javap instructions/signatures match exactly, permitting that metadata-only difference.175previousclasses remain byte-identical. Package9465ac37 PASS; accepted/verified artifacts preserved.

New TechHub sourcef468cd8/immutablebundlebe6ef8e8,98Node47044b53 and88mappedJava checks; warmd6eee8ea and freshprofilea30700f5 PASS. Prepared case7b27a7cd has finite motion-triggered diagnostics and unchanged safety/originalgrid contracts. PREPARED_NOT_RUN: new source behavior, diagnostic trigger and subjective course remain native-unaccepted. All previous runtimes exited cleanly; no automatic replay or lease extension. Choose actual stable next-manual profile at the human's next available inspection. This is source/model/build/material verification, not full goal completion.
