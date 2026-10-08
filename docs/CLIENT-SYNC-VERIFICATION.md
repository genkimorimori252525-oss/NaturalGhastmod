# Passive native projectile synchronization

TANK_CORE client-sync-25zRu9 PASS on NaturalGhast51d3aea/TechHub0499b28/host7f14960, Minecraft1.20.1/Forge47.4.10/Java17. [Plan and compact Astra decisions](superpowers/plans/2026-10-08-passive-client-sync.md). Fresh DOMAIN_RELIABILITY private copy, genuine connected canonical survival Player and active boss, no ScopedOwner/input/camera/health/velocity/target overrides. Seven explicit native projectiles, unchanged disjoint loaded preflight corridors; one production PLAYER_ATTACK call normalizes Curve after actual client admission. This is synthetic deflection reliability, not physical melee/counterplay. Accepted broad20/8/20 swimming and danmaku exclusion remain unchanged. No product source/API/dependency changes; the observer is absent from normal product JARs.

19native server samples/126server rows (40363–40380), seven ordinary server StartTracking receipts/seven actual client joins/112client END rows, within240ticks AND20seconds. No injected packets, manual buffer decoding, forced tracking or extra connection mechanism. Exact Forge source shows addPairing sends the ordinary bundle before StartTracking; PlayMessages.SpawnEntity adds ClientLevel before packet velocity/readSpawnData. Therefore the join callback records identity/admission, while its first following ClientTick.START captures the populated instance before native movement. Same-frame START/END snapshots establish actual continuation. Birth position error0; known vanilla pairing motion quantization explains maximum velocity difference0.000125, covered by the source-based0.000126 tolerance. No simultaneous server/client-clock equality assumption.

| Projectile | Ordinary server/client initial index | Client rows | Last client index |
| --- | --- | --- | --- |
| Burst |0/0|18|18|
| Curve |0/0|18|18|
| Lob |13/13|4|17|
| Bomb |0/0|18|18|
| Deflected Curve |0/0|18, including13normalized|5; frozen after normalization|
| Standard |0/0|18|0; baseline motion|
| Ground |0/0|18|0; baseline motion|

Lob naturally enters ordinary tracking at index13: the transmitted path/current clock and subsequent native movement match. This is first ordinary mid-flight tracker admission; no deliberately recreated tracker or newly connected client. All seven UUIDs have one actual client instance/registered ThrownItemRenderer, immutable path and correct native owner; no invalid numeric motion or clock rewind. This establishes registered renderer/instance identity, not duplicate drawn-pixel or visual readability acceptance. Persisted provenance/age are not presumed network fields.

Server normalizes Curve at index4 with actual Player owner; delivery occurs after one further client special tick, so its clock freezes at5. This is allowed delivery ordering, not clock reset. Thirteen normalized rows retain owner/power/path and cease special-clock advancement. Actual normalized position-step error<=1.28e-14 and velocity-recurrence error0 against mapped AbstractHurtingProjectile physics ((v+power)*float.95 in air). Before-tick velocity also corresponds to the authoritative redirect over a bounded elapsed-tick range, allowing known motion quantization. Water changes this physics and is explicitly rejected for this fixture. Finite profile terminal impacts/expiry are outside this short synchronization prefix; their separate restart/impact receipts remain unchanged.

One compact scoped Astra review found Important missing deflection-velocity proof: the initial analyzer could accept stale/corrupt normalized velocity. Focused genuine RED proves corrupt/stale velocity and incorrect displacement passed incorrectly. One correction pass adds actual native predecessor/displacement/velocity recurrence and bounded authoritative redirect correspondence;79related Node tests/mapped javac/syntax PASS. Genuine product build8seconds; one fresh native unit passes without product fix, packet intervention, retry or re-review.

Raw spawns/joins/clientRows/serverRows arrays independently equal native results; all seven supplement hashes and private material bytes rechecked. CLEAN_EVIDENCE_SHUTDOWN ACK/dropped0/remaining0, VERIFIED_EXIT674ms/no live owned process, EVIDENCE_COMPLETE11canonical observations/2tick lanes, zero canonical errors/drops/partial files. Supplementary rows are a finite observed prefix, not canonical camera/roster coverage; no captures. Historical original85raw file hashes/count and accepted manual JAR6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca remain exact after closure. No original/finalized NBT/RegionFile reopening.

| Artifact | SHA256 |
| --- | --- |
| report.json |eebbbac3944d7e56f636f969a6dfbfd65b12eb35ca18d4a4e3d43e5a94d52ac7|
| request.json |a414715e7e5357c64f654c36f5d4ced327e5a2aa7c17282fd522e75fcf420ab6|
| result.json |320f7b2094792338b31c8971a4de9b72227a5a56c2fe45ca75324f10aba6a277|
| analysis.json |ff8113ce431ad25e605860d9d4d9ca1228fe311a7e062c5a786b73e4fb8b6159|
| spawns.jsonl |896105e86e89edcdc5ef1d559b660503f79ded02021df2c41cde5f2dd88eb57e|
| joins.jsonl |766ec4173190d70829e441ba3f63af73a5afb6c4a405a27eddd6cd40c8322397|
| clientRows.jsonl |6b966c468c5de91fb9a2ab2420338e88b836564721a6afd97c9bbb1cf28647d9|
| serverRows.jsonl |c3bc9e2d788733ed4bb652c5a6dcdf7d2b16593d04c79ad5835973477fa4fa80|
| Private product JAR |6b3caa6e7acefe1a5a8c3b8cf77507c2d8f256c38ed4dd34e4e74de52a4b40e2|
| Private observer JAR |856958bcb5f0d9609d95c48acf61aec30979083cee08163ff5ace2c603f29604|
| fixture.json |5db741d169e05b5f244bf46112398166c2b8abe7642c7c2e8fd5ee559031f294|

Controlled re-track, genuine late connection/re-spawn, delayed owner arrival, human input/cues/balance, unobserved natural selectors, arbitrary unload/crash behavior and full release remain separate gates. Ordinary mid-flight Lob admission does not waive them. Astra defers controlled untracking/new connection mechanisms in this unit; record the boundaries rather than adding them solely to obtain broader PASS.
