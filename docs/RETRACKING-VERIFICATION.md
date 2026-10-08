# Controlled native projectile re-tracking

TANK_CORE retracking-WA4pYa PASS on NaturalGhastc3fff5e/TechHub9798ec2/host7f14960, Minecraft1.20.1/Forge47.4.10/Java17. [Plan/compact Astra ruling](superpowers/plans/2026-10-08-native-retracking.md). Fresh DOMAIN_RELIABILITY private copy, real connected canonical survival Player/active boss; no ScopedOwner, actor position/velocity/health/input/camera/target overrides. Seven explicit preflighted native test projectiles; Curve normalization uses an injected actual-Player production PLAYER_ATTACK call, not physical melee. Accepted broad20/8/20 swimming and danmaku exclusion remain unchanged. No product source/API/dependency changes; both probe/intent helper are positively absent from normal product JARs.

The private probe calls actual ServerChunkCache.removeEntity/addEntity only on its new projectiles. Mapped source confirms these remove/recreate ChunkMap tracking while ServerLevel registration and ordinary ticking continue. Vanilla removal/pairing packets and Forge Stop/StartTracking remain the real path; no reflection/direct packet injection or EntityType reconstruction. Four initial client rows (plus four normalized Curve rows) precede controlled removal. Actual StopTracking establishes owned removal. The original client object must be removed AND its native ID absent before the one re-add. Require >=12remaining finite segments and gap<=6ticks; never lengthen/reset trajectories. This proves controlled re-tracking, not genuine late connection/delayed owner arrival or arbitrary gameplay unload.

17native samples/88server rows/67client rows,14actual server pairing receipts/14client admissions, seven owned stop/absence/re-add receipts; within240ticks AND20seconds. Each server object remains the same alive/registered/loaded UUID/id with continuous age/path clock through its gap. All seven actual gaps are2native ticks. Each re-created client is a distinct actual object with the same UUID/id; old objects stay removed throughout the observed continuation. Source-row/re-pairing/current client state agree on position/motion/power/path/current clock/normalization/owner with the established vanilla motion-packet quantization allowance. Same-frame native movement checks pass. Shared motion contracts are applied to two explicitly filtered observation generations; these are not two alleged passive trials.

| Projectile | Initial/re-add index | Remaining finite segments at removal | Client rows, initial/new |
| --- | --- | --- | --- |
| Burst |0/7|22|4/5|
| Curve |0/7|28|4/5|
| Lob |5/11|38|4/4|
| Bomb |0/7|14|4/5|
| Normalized Curve |0/4|already normalized|8/4|
| Standard |0/0|baseline age continues|5/5|
| Ground |0/0|baseline age continues|5/5|

The first Lob admission is naturally mid-flight index5; this differs from the earlier passive trial's13 and is recorded as observed, not copied from that receipt. Normalized Curve's new client uses the server's current frozen index4, independently of the old client's transport-delayed clock. No clock rewind of a retained instance is claimed or permitted. All cases have at least four new-client and four post-add native-server continuation ticks. Renderer/instance identity is checked; visual readability/duplicate drawn pixels, terminal impact/expiry and human counterplay remain separate.

One scoped Astra review found Important premature add intent: setting addInvoked before fallible metadata publication prevented valid cleanup when publication failed before any add call. One correction pass extracts the actual pre-review ordering and demonstrates PRE_CALL_FAILURE_PERMITS_CLEANUP RED. TrackerReaddIntent then completes preparation before marking the call begun. Four source fault cases cover normal, publication, invocation and after-add failures: pre-call failure permits exactly one owned cleanup add; begun/uncertain calls never blindly retry. Native restore additionally guards same alive/registered/loaded entity and records cleanup failures separately. No native injected write-fault claim. Current pose/velocity/power source-row negatives also have genuine RED->GREEN.23new/102related Node checks, mapped javac/intent/syntax and genuine11second product build pass. No second review or native retry; no tracker is outstanding at successful closure (cleanup array empty).

Raw spawns/joins/clientRows/serverRows/stops/absences/readds arrays independently equal native results; ten supplement hashes and all material bytes rechecked. CLEAN_EVIDENCE_SHUTDOWN ACK, zero dropped/remaining queue, VERIFIED_EXIT1075ms/no live owned process, EVIDENCE_COMPLETE12canonical observations/2tick lanes, zero canonical errors/drops/partial files. Supplements are finite prefixes, not canonical fixed-camera/room-roster coverage; no captures. Historical original85raw hashes/count and accepted manual JAR6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca remain exact. No original/finalized NBT/RegionFile reopening.

| Artifact | SHA256 |
| --- | --- |
| report.json |229da8727317d747344c9a3d6192037bd59602bec9375e963dad46a0b4924fd6|
| request.json |72093616ba53e5ccbfb5c419a014d195254933d6338768a85dfebdd1623d4d6e|
| result.json |e99a8ef06a6dc56c4b9b3850ddc0d75ee8a4acc11022ef51669fd996b05812cf|
| analysis.json |d3e394b2f324fac70a797890a415fda238aa3cba0a59c22e1fc55377fb2ae9d8|
| spawns.jsonl |007cf19ac6a5ea9bba3f98f059ccaca3251781e976a364e261bf6232df09eabb|
| joins.jsonl |4e08e416843ee035647143f796a3d5b96b56d9f606d35865171a55950075e4ed|
| clientRows.jsonl |33bf50b9094dbc296e312f8b29fb1ecef6f390b26b77ce56d2525f748e4b0e76|
| serverRows.jsonl |cdba511b56f2fdba00d8f8ea0755cbdcd9db918aa58515363ad287587ddc80e0|
| stops.jsonl |f9d4e8061bf9891fe76c2407b9b24713cb9735061c6375e9bfcfc303e3488956|
| absences.jsonl |7d44c9ef358144110fbef75dc48e6720a645223a0e64430d2b0fd8700dabccc9|
| readds.jsonl |6cdd31c09492ea80300b98cfc6439247d7779b88654a61d8b1ce532f2829e76d|
| Private product JAR |28a634f694818207178331f8c05dd69e23e0240b867511c978c1c628a3498fc7|
| Private probe JAR |7519f34337dbb436c48e86168ce3ec52a9764e2a891527f9095da3df4ed42d5f|
| fixture.json |5db741d169e05b5f244bf46112398166c2b8abe7642c7c2e8fd5ee559031f294|

Genuine late connection/re-spawn, delayed owner arrival, human input/cues/balance, unobserved natural selectors, arbitrary unload/crash and full release remain open. Controlled disappearance/re-pairing cannot waive those gates or broaden prior receipts. Astra's deferred connection/owner mechanisms remain separate implementation choices.
