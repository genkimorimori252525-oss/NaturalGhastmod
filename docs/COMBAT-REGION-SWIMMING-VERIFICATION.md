# Combat region and swimming verification

2026-10-08. Implementation verified within the scopes below; **user qualitative movement feedback positive**, controlled native scenarios still unverified. No attacks/danmaku, multiplayer, ground locomotion or final balance acceptance.

## Source and behavior

Product correction commit `340e315a948228ef9723e13c173dbba0f4a50bae`; enlarged fixture/finite analyzer source `e91fcbf02173d9db558a5f1a1e077ab1b647a672`. Persistent world-space20/8/20 region, camera-independent acquisition, soft range, reasoned reselection40/100/200-tick lifecycle, sustained3D swimming and eased intent. Registry, one physical velocity writer, collision application and conservative4×4 stopping sweeps remain.

RED: old camera reversal moved the anchor; old geometry allocated60552 cells; repeated blocked retries reselected an identical region. All reproduced before correction. First long simulation crossed the boundary at tick782; interior waypoint headroom fixed the overrun without shrinking the home region. Old mapped diagonal test assumed frontal range correction; replacement tests obstruction on a diagonal RETURN into the retained region.

## Grouped verification

- `tools/test-flight-foundation.ps1`:12036 region/swimming checks,22 controller/facing/orientation and25mobility checks PASS.4000 integrated physical ticks span31.0258526 horizontal/10.4786735 vertical blocks, rest0. Camera-invariant region, replacement, dwell/cooldown, temporary LOS/targetless lifecycle, blocked-return recovery, query budgets and safety constraints covered.
- `tools/test-forge-clearance.ps1`:5 actual mapped swept-AABB regressions PASS.
- `tools/test-tank-fixture.ps1`:5 seed-policy and10 mapped fixture checks PASS;52×24×52 interior64896 cells, shell allocation75816, within existing budgets.
- Node owner/historical analyzer/new swimming analyzer:15 tests PASS. Legacy v0.5 analyzer and finalized receipts remain unchanged; new `naturalghast.swimming-tank/v1` has a separate measured-motion scope.
- `compileJava build`:BUILD SUCCESSFUL. Native runner builds its exact clean source. Observer/preparation classes stay outside the production JAR.

Logs and execution rulings are retained under `.superpowers/sdd/2026-10-08-combat-region-swimming/`. Pure simulations establish math/lifecycle behavior, not client appearance or native camera/LOS scenarios.

## Initial pre-review bounded native result

Trial `build/tank/flight-e3CRdb`; source `e91fcbf02173d9db558a5f1a1e077ab1b647a672`; target JAR SHA256 `cd86f7c0ee8b4e8b21de3320d466b0b1554006a4b295b0bdf40e290519d1e1d8`.

LAB `17bed2d161824f3e1b682ea3f1ff9e5714f1d684`; host `7f14960999bc2955d85ae9d3619090ad37817c38`, Minecraft1.20.1/Forge47.4.10. Scoped `PASS_STATIC_PLAYER_PERSISTENT_REGION_PHYSICAL_SWIMMING`:

- 600 contiguous server samples;389 visible real-Player acquisition samples;513 region samples;508 swimming samples.
- Region generation1 retained; horizontal span19.3075919, vertical8.7663369; stopped swimming0; maximum speed0.44.
- All samples active4×4 body, health10, collision-free. Fresh fixture removes1 exact saved Reimu; other root NBT retained.
- 391 canonical observations; clean stop, EVIDENCE_COMPLETE, original85 file hashes/count unchanged. One explicitly requested supplemental PNG; no default product recording. Class-resource linkage is not resident transformed-class attestation.

Camera-turn, moving Player, deliberate LOS interruption and subjective smoothness remain native NOT_RUN for this automated static fixture. The user's forthcoming manual inspection supplies visual judgment; do not promote this result into those scopes.

## One whole-change review and fix pass

Fresh-context review found Critical0/Important2/Minor0. Both reproduced RED and fixed GREEN: an obstructed RETURN could permanently brake outside an unreachable projected region; sustained SPACE_BLOCKED now selects a bounded local region at the boss's existing feasible position, retaining the same broad radii. A500-tick obstructed-return simulation now resumes swimming. The analyzer could ignore long BRAKE/HOLD tails and per-tick region generation changes; its static-player scope now counts stopped samples across the acquired settled interval and requires one retained generation. Two adversarial traces fail as intended. No second reviewer or navigation subsystem added.

Declined judgments: native camera/moving-Player/LOS/subjective appearance await the user; attacks/Ground Combat/full navigation/multiplayer/performance are later scope; unchanged over-cap external-impulse stopping-horizon tuning is not newly certified. Rulings: preserve these limits and existing physical safety checks; verify the changed fallback by regression and rerun the finite native pilot with the reviewed artifact. No deferred minors. Earlier native receipt remains tied to its exact pre-review source; a final replay is recorded separately.

## Final reviewed artifact: preserved failures and measured scope

Source `182fcb7ee88084c5dc1aecaba6cfa870cefa13b0`. `flight-kPfHZB` failed before movement acceptance with `OWNER_OUTCOME_UNKNOWN:AccessDeniedException:null` (Tech Hub AF-0013); target build/load succeeded, clean stop/EVIDENCE_COMPLETE/original85 preserved. No uncertain command replay or gate override.

`flight-5jC8dG` target JAR SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca` failed its static-look condition: `Static player fixture changed`. All600 Player positions remained `(9.5,224,3.5)`; observed yaw/pitch spans12.3000036/16.799961. The user subsequently confirms occasionally moving the camera. Preserve formal FAIL and canonical/finalized bytes; no additional static replay is needed for this user-confirmed intervention.

Separate read-only `.superpowers/sdd/2026-10-08-combat-region-swimming/final-measured-audit.json`: `PASS_MEASURED_MOTION_ONLY`, not replacement static-fixture acceptance.600 samples,374 visible acquisition,565 region,560 swimming; one retained generation; horizontal span24.67963799/vertical6.43613320; stopped swimming0 and settled369/rest0;maxspeed0.44.386 canonical observations; clean/EVIDENCE_COMPLETE/original85 unchanged. Report SHA256 `cf883c17884cf0c13f8a76f60adf138aa1301869313ce2c0b0f46f68575ed5c5`; trace SHA256 `af64a87382441802d9ce1e12cf9919dbd584cdadae0912b4aaa58a36495c10d3`. Audit records input cause as not inferred at creation; later human confirmation is recorded here without rewriting it. Controlled180-degree turn, moving Player, deliberate LOS and subjective smoothness remain unverified.

## Manual inspection fixture

Fresh private `build/tank/manual-swimming-20261008-beb1755b`, exact reviewed-source/latest measured JAR `inputs/naturalghast-final-observed-dev.jar`, SHA256 `6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca`. `manual-final-manifest.json` binds source182fcb7/artifact/geometry/limits; `launch-final.ps1` launches this copy.52×24×52 room; observation wall omitted offline; small raised Player platform permits the full vertical region plus body clearance. Initial saved Player `(26.5,228,3.5)`, Ghast `(26.5,234,30.85)`, survival mode, night vision. No observer JAR; runtime `KNEEKURA_DEBUG_ENABLED=0`; ordinary game logs are not camera recording. New-copy readback verifies Reimu1 removed/other root NBT3 preserved/active Ghast1, and original85 hashes unchanged. Actual integrated game launched; visual acceptance remains PENDING_USER. This save is manual-only, not canonical acceptance.

Observation limits: prior native motion is selected-Ghast server telemetry, not full-room roster coverage. Supplemental raw images use the Player presentation; no cardinal four-view capture was used. Current manual session has no live telemetry roster. Room interior cells `[0,224,0]`..`[52,248,52)` give local `(x,y-224,z)`; body containment also needs AABB. Do not treat initial NBT poses as current positions. Tech Hub guide/AF-0017 distinguish existing selected-subject spatial maps, frozen cardinal snapshots and opt-in mob POV from an unimplemented on-demand whole-room roster.

Human follow-up,2026-10-08: user reports the current Ghast looks very good. This supplies positive qualitative movement feedback; it does not establish a controlled camera-turn/moving-Player/LOS protocol or complete Boss acceptance. The earlier manual manifest's PENDING_USER describes its preparation time and is preserved. Remaining scoped checks: turn yaw/pitch while staying still, walk/fall from the platform, approach region boundaries/obstacles, and report visible jerks or repeated repositioning when relevant. Treat new bugs as higher priority than further NaturalGhast features. This MOD also tests Tank usability; Tech Hub's `docs/TANK-OPERATING-GUIDE.md` and AF-0018/0019 consolidate operation and camera ideas without new runtime changes. Prior failure/recovery of original-save inspection padding remains AF-0016; original NBT is inspected only through a disposable baseline copy.
