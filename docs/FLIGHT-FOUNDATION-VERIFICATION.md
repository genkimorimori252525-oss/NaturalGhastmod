# Flight foundation verification — 2026-10-07

## Implementation and scope

Initial non-attacking redesign slice: inertial controller, observable target boundary, smoothed frontal anchor and bounded body orientation. Existing entity ID/assets/save compatibility retained. Old attack/orbit Goals are inactive. No final danmaku, boss balance, Mobility Context or complete combat implementation.

Source tested in native run: `9b19f53e8895eb0d3784b60f4bc10d31cfe79c88`, branch `codex/natural-flight-foundation-20261007`. Flight code's final repair is `8d6d839`; subsequent changes adapt only private verification tools. Final documentation/automatic finalization changes do not imply a new Minecraft run.

LAB source: `c3699539388af68495ff1529af3576ff4926e9bc`. Host source: `7f14960999bc2955d85ae9d3619090ad37817c38`, TANK_CORE with a private Forge47.4.10 property override. NaturalGhast is the registered target source; the host is selected separately. Nine loaded mod IDs: minecraft, forge, touhou_little_maid, cloth_config, embeddium, rubidium, mixinextras, soutou_ghast, naturalghast_tank_observer. This does not attest the original Forge47.2.0 profile's compatibility/performance.

## Results

| Check | Result | Limit |
| --- | --- | --- |
| Dependency-free flight/facing/anchor/orientation assertions | PASS, 22 | Pure Java, not Minecraft |
| Mapped Forge swept-AABB regression assertions | PASS, 3 | Bounds mathematics; no live takeoff/navigation |
| Source-bound `compileJava build` | PASS | Existing ResourceLocation/Gradle deprecations remain |
| Whole-slice review | Four findings repaired; repair diff accepted | Reviewed code, not native maneuver coverage |
| Native registration and preflight | PASS; DEBUG_READY, ACTIVE_SCOPED_CONTROL, READY; sampled freshness CURRENT | Exact scoped owner; no full transformed-class attestation |
| Native active idle | PASS; 40 ticks, gameTime40344–40383, NoAI=false, HOLD, no target, speed0, health10 | No player-following scenario |
| Native4×4 body clearance/render | PASS; all40samples collision-free; SoutouGhastRenderer and raw frame inspected | Stationary airborne fixture; not wall departure or combat visual quality |
| Shutdown and canonical finalization | PASS; live=false, clean=true, EVIDENCE_COMPLETE, 29 canonical observations | Supplementary private frame/idle producer is separate |
| Original world preservation | PASS; all85file paths/hashes unchanged | Fresh private copy contains declared fixture changes |
| Native acceleration/braking/turn/anchor recovery/LOS loss/subject change | NOT_RUN | Tank19×19×11 cannot fit22–34range; registered owner excludes Player subjects |

Canonical lanes include SERVER_ENTITY_STATE, AI_TARGET, NAVIGATION, ENTITY_STATE, target tracking, heartbeats and TANK_PRESENTATION_STATUS. The native result is **PASS_ACTIVE_IDLE_4X4_CLEARANCE_RENDER**, not full-flight PASS. The accepted profile explicitly requests grid/brightness on, motion off; the previous retained ingestion snapshot is checked against a fresh owner clock without rewriting either.

## Local retained artifacts

Trial: `C:/Users/genki/Documents/Codex/NaturalGhastmod/build/tank/foundation-0ZDlpd`.

Run: `runtime/sessions/sess-20261007103701-6d6d94f83d14/runs/run-20261007103701-90bda8f67896` under that trial. `report.json` retains the registration/preflight/cleanup result. `evidence/finalization.json` seals canonical evidence. `evidence/derived/naturalghast-foundation/{idle.jsonl,frame.json,frame.png}` are the separate finite read-only supplement. Frame SHA256: `b445eeb06d9b8b1a1ff78856e0f70d884de64fea109ad1a78d56f6688ec2612f`.

Generated local artifacts are not committed or guaranteed by a fresh checkout. Keep the trial while this receipt is needed. Reproduce with the explicit launcher arguments documented in `FLIGHT-FOUNDATION.md`; source must be clean and the target build must succeed. The runner now finalizes canonical evidence after its owned stop; the recorded run used that same LAB API explicitly after stop.

## Failures and repairs retained

- `foundation-8fPbfP`: preparation compilation rejected external access to LAB internal canonical methods. Later `foundation-w0UWGp` exposed the incorrect Node substitute through ARENA_BASELINE_MISMATCH. Repaired by the offline LAB-package adapter reusing typed-Gson receipt hashing; no authority relaxation.
- `foundation-m9tt5N`: BRIDGE_SOURCE_REVISION_MISMATCH. Registered NaturalGhast source separately from the host.
- `foundation-PAMOyj`: INVALID_ASSERTIONS. Added the declared idle health assertion.
- `foundation-IdjQmG`: Forge47.2.0 constructor API mismatch. Aligned only the private host launch to47.4.10.
- `foundation-qTKGQS` / `foundation-6EraWM`: startup warning/timeout. Raw UI diagnostics proved observer.jar lacked pack.mcmeta; repaired the artifact without warning suppression or GUI auto-approval.
- `foundation-47Z5xJ`: owner accepted and idle/frame collected, but preflight NOT_READY. Requested brightness contradicted the bright capsule; newest client sample could be ahead of the owner file's clock. Corrected the explicit display profile and used an earlier unchanged retained sample. This rejected run is not relabeled PASS.

Review fixes: bounded rotation replaced the incorrect rotateIfNecessary use; a dedicated LookControl prevents pitch erasure; the sweep allows departure from existing floor/wall contact; a clean source-bound build prevents stale classes being attributed to another HEAD. Grouped RED/GREEN evidence was retained. No deferred review minors.

## Rulings and next work

- Keep LAB instrumentation in the separate TANK_CORE development host, not a production dependency. If this choice fails, revise launch wiring.
- Limit native acceptance to idle/clearance/render; preserve the full specified range and dimensions. Cost: native flight and player behavior remain unverified.
- Include pitch ownership and stale-build binding in the repair unit because they affect promised behavior/evidence. Cost: dedicated LookControl and one target build per coherent native unit.
- Select the host separately and align this private Forge runtime to the target's47.4.10 API. Cost: adjusted-host compatibility/performance needs its own evidence.

Next: a registered player-observation fixture and a sufficiently large owner-managed private Tank; then native movement/perception coverage, movement primitives/Mobility Context and tactics. Keep danmaku content separate. Follow Tech Hub's English workflow/feedback documents and retain legitimate rejection evidence.
