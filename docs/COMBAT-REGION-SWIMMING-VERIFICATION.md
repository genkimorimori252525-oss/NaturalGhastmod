# Combat region and swimming verification

2026-10-08. Implementation verified within the scopes below; **visual acceptance PENDING_USER**. No attacks/danmaku, multiplayer, ground locomotion or final balance acceptance.

## Source and behavior

Product correction commit `340e315a948228ef9723e13c173dbba0f4a50bae`; enlarged fixture/finite analyzer source `e91fcbf02173d9db558a5f1a1e077ab1b647a672`. Persistent world-space20/8/20 region, camera-independent acquisition, soft range, reasoned reselection40/100/200-tick lifecycle, sustained3D swimming and eased intent. Registry, one physical velocity writer, collision application and conservative4×4 stopping sweeps remain.

RED: old camera reversal moved the anchor; old geometry allocated60552 cells; repeated blocked retries reselected an identical region. All reproduced before correction. First long simulation crossed the boundary at tick782; interior waypoint headroom fixed the overrun without shrinking the home region. Old mapped diagonal test assumed frontal range correction; replacement tests obstruction on a diagonal RETURN into the retained region.

## Grouped verification

- `tools/test-flight-foundation.ps1`:12034 region/swimming checks,22 controller/facing/orientation and25mobility checks PASS.4000 integrated physical ticks span31.0258526 horizontal/10.4786735 vertical blocks, rest0. Camera-invariant region, replacement, dwell/cooldown, temporary LOS/targetless lifecycle, query budgets and safety constraints covered.
- `tools/test-forge-clearance.ps1`:5 actual mapped swept-AABB regressions PASS.
- `tools/test-tank-fixture.ps1`:5 seed-policy and10 mapped fixture checks PASS;52×24×52 interior64896 cells, shell allocation75816, within existing budgets.
- Node owner/historical analyzer/new swimming analyzer:13 tests PASS. Legacy v0.5 analyzer and finalized receipts remain unchanged; new `naturalghast.swimming-tank/v1` has a separate measured-motion scope.
- `compileJava build`:BUILD SUCCESSFUL. Native runner builds its exact clean source. Observer/preparation classes stay outside the production JAR.

Logs and execution rulings are retained under `.superpowers/sdd/2026-10-08-combat-region-swimming/`. Pure simulations establish math/lifecycle behavior, not client appearance or native camera/LOS scenarios.

## Bounded native result

Trial `build/tank/flight-e3CRdb`; source `e91fcbf02173d9db558a5f1a1e077ab1b647a672`; target JAR SHA256 `cd86f7c0ee8b4e8b21de3320d466b0b1554006a4b295b0bdf40e290519d1e1d8`.

LAB `17bed2d161824f3e1b682ea3f1ff9e5714f1d684`; host `7f14960999bc2955d85ae9d3619090ad37817c38`, Minecraft1.20.1/Forge47.4.10. Scoped `PASS_STATIC_PLAYER_PERSISTENT_REGION_PHYSICAL_SWIMMING`:

-600 contiguous server samples;389 visible real-Player acquisition samples;513 region samples;508 swimming samples.
- Region generation1 retained; horizontal span19.3075919, vertical8.7663369; stopped swimming0; maximum speed0.44.
- All samples active4×4 body, health10, collision-free. Fresh fixture removes1 exact saved Reimu; other root NBT retained.
-391 canonical observations; clean stop, EVIDENCE_COMPLETE, original85 file hashes/count unchanged. One explicitly requested supplemental PNG; no default product recording. Class-resource linkage is not resident transformed-class attestation.

Camera-turn, moving Player, deliberate LOS interruption and subjective smoothness remain native NOT_RUN for this automated static fixture. The user's forthcoming manual inspection supplies visual judgment; do not promote this result into those scopes.

## Manual inspection fixture

Fresh private `build/tank/manual-swimming-20261008-beb1755b`, same exact verified JAR.52×24×52 room; observation wall omitted offline; small raised Player platform permits the full vertical region plus body clearance. Player `(26.5,228,3.5)`, Ghast `(26.5,234,30.85)`, survival mode, night vision. No observer JAR; runtime `KNEEKURA_DEBUG_ENABLED=0`; ordinary game logs are not camera recording. New-copy readback verifies Reimu1 removed/other root NBT3 preserved/active Ghast1, and original85 hashes unchanged. This save is manual-only, not canonical acceptance.

Next manual checks: turn yaw/pitch while staying still, walk/fall from the platform, observe sustained floating/swimming, approach region boundaries/obstacles, and report visible jerks or repeated repositioning. Treat new bugs as higher priority than further NaturalGhast features. Prior failure/recovery of original-save inspection padding remains recorded in Tech Hub AF-0016; original NBT is now inspected only through a disposable baseline copy.
