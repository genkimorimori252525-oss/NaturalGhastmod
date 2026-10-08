# Lob terminal subdivision verification

2026-10-09 Asia/Tokyo. Product source `dd0622ed3d0ea424a8672ee37d64f6412fa7f7bc`; LAB `941b2cfd66bcd0575ec9455302109546211c9c59`; host `7f14960999bc2955d85ae9d3619090ad37817c38`. Trial `build/tank/lob-terminal-GlT2NU`. [Plan](superpowers/plans/2026-10-09-lob-terminal-subdivision.md); [preceding cover evidence and adoption decision](COVER-LOB-VERIFICATION.md).

## Decision and implementation

Compact Astra adopted a separate bounded geometry repair after mapped tests showed that a legitimate integer landing can require six terminal cells, exceeding the unchanged four-collider limit. The preceding failed cover trial did not record its exact rejected native collider; its cause is not retroactively asserted. Both historical cover trials remain immutable.

`LobTerminalSubdivision.refine` can insert one point on the existing final Lob line, strictly before the earliest full-height floor contact. It preserves every original point and the exact endpoint; full loaded swept-body/native-ray validation still applies at admission, cue and launch. Successful refinement adds one trajectory tick. It preserves the spatial arc, not identical old timing. The existing 1.9 speed bound, 97-point cap and four exact terminal colliders remain authoritative. Insufficient contact margin, excessive points or unsafe geometry leave the original path unchanged; its invalid terminal footprint still fails the existing validator. Other profiles, selection, damage and boss movement are unchanged.

No new dependency, public control, packet/save format, input/camera override or canonical-world change. Grand Danmaku and Ground barrage/fans remain excluded. One compact scoped Astra review found no Important/Critical issue; no correction pass or re-review.

## Pre-native verification

Genuine `MISSING_SAFE_LOB_TERMINAL_SUBDIVISION` runtime RED precedes 11,273 new geometry/guard assertions and all preserved flight suites GREEN. Cases cover integer, half-grid, near-boundary, negative, diagonal and sloped approaches, insufficient pre-contact margin, point cap and unchanged non-Lob paths. An initially invalid diagonal fixture exceeded the existing speed bound before refinement; the valid-case fixture was corrected and a separate explicit invalid-speed rejection retained.

250 mapped footprint/pre-contact/private-integer-fixture checks, private probe compilation and a genuine 15-second cached offline Forge build PASS. Missing-analyzer RED precedes 22 terminal analyzer and two launcher tests; all 49 related Node contracts PASS. No validation or probability guard was relaxed.

## One finite native trial

Fresh private 52x24x52 room; 136 verified AIR cells become a declared static wall before new actors, then restore conditionally. A new Cow starts at integer `(42,224,26)` and uses native navigation after actual LOS loss. A new controlled Ghast Goal exercises the production attack adapter, actual Sensing, ordinary variation, immutable committed projectile and native travel. The real survival Player and canonical actors/input/health/velocity/target remain unmodified. This explicit fixture does not exercise production AnchorGoal admission or human hiding behavior.

PASS once: 161 samples, 160 server rows, four client identity rows, 7,971ms. Cover admission at tick40455 restarts the full tell after a partial visible charge; cue40474 and launch40485 retain observation age31 and endpoint `(42,223.75,26)`. The actual muzzle's raw path has 37 points and six terminal cells; refinement produces 38 points and four exact stone colliders. All original points and endpoint reconcile; 37 actual registered projectile positions cover ASCEND/DESCEND.

At tick40522, the real `ProjectileImpactEvent` observed at LOWEST is uncanceled BLOCK impact against declared stone `(41,223,26)`. Its index37 and actual prior position `(41.664324040,224.239745525,26.002909192)` match the inserted point and last native ray. Actual hit location `(41.828647766,224,26.001485053)` lies on that ray and floor surface. The native explosion names the same projectile/tick and occurs at its prior current position, as the existing native hit handler specifies. That position differs from the ray hit location. At END, the projectile is removed and no longer registered. The scope is `LOWEST_UNCANCELED_FORGE_BLOCK_RAY_PLUS_NATIVE_EXPLOSION_AND_END_REMOVAL`, not rendered impact or damage acceptance.

Three owned actors close: the consumed shot is already removed; the new boss/Cow are discarded. All 136 wall cells restore. Six raw arrays and actor metadata reconcile with the result/report; ten supplements, three materials and seven canonical artifacts match independently. Historical original 85 files/full count and the accepted swimming JAR match raw SHA256. Clean shutdown ACK, owned `VERIFIED_EXIT`1106ms and `EVIDENCE_COMPLETE`27observations/two lanes; zero errors, dropped/queued/partial records or captures. Original/finalized worlds are inspected as bytes only. No repeated trial or continuous recording.

| Material | SHA256 |
| --- | --- |
| report.json | 0ecad64a42f0871d4c9b2d13743fefb6c5365a7b193cb9fdb9a4428f74a4fa0b |
| product JAR | b232cec45e48e235a350e8a8e1ed74c03e22ee7e7b253f507c0758d069b378b3 |
| private probe JAR | ac8a894df686e008960626f57efe70d6e5ac75f485b73a8c63066694c6333e5e |
| fixture.json | 7d86b1d3bc2be23085ecf5e2e9fd6c8b3a6fe15f26c22a1129b437a597e8a62f |
| independent lob-terminal-audit.json | 429171450ffafc3a57c0e5a0ff24a6b350a96c5fd6da9bbc867fbc2dfde2a5e1 |

## Remaining scope and Tank feedback

This proves one integer-endpoint native terminal case, not guaranteed admission/hits, production Goal/natural frequency, a native negative/fault matrix, moving human-Player interpretation, visible presentation, balance or full boss/release. Pure/mapped negative cases do not prove native failure transitions. Client renderer identity does not prove visible cues.

AF0051 records the reusable Tank lesson: label collision stage and separately reconcile prior projectile position, ray hit location, declared block/state, native explosion center and END removal. Record narrow on-demand event supplements rather than infer impact from disappearance or broaden a generic observer API. Do not snap endpoints, weaken terminal guards or repeat unchanged probabilistic trials to obtain acceptance. The full non-danmaku goal remains active.
