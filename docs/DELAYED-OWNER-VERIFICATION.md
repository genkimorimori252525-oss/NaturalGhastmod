# Controlled delayed client owner synchronization

Private TANK_CORE owner-availability RED -> GREEN on Minecraft1.20.1/Forge47.4.10/Java17. [Plan/Astra decisions](superpowers/plans/2026-10-08-delayed-owner-sync.md). Genuine late connection remains a separate open gate. Fresh DOMAIN_RELIABILITY copies; canonical survival Player/boss remain unmodified. Only one newly created, normally active test SoutouGhast's native ChunkMap tracker is controlled. No fake Player, reflection, direct packets, selector forcing, camera/input/health/velocity override, dependency or packet/save-format change. Accepted broad retained-region swimming is unchanged.

Actual StopTracking establishes owned removal; original client owner must be removed AND its ID absent before creating Standard/Ground/Curve and synthetically returning a previously Player-normalized Curve to that absent owner. Four actual unavailable-owner rows precede one native re-add. The same server owner remains alive/registered/loaded and normally ticks; actual re-admission produces a distinct client instance/same UUID/id. Four following client rows per ball check authoritative ownership and ordinary movement. Max240ticks AND20seconds; no ongoing recording. Synthetic production PLAYER_ATTACK/returnByBoss calls are not human melee, natural rally or counterplay proof.

## Reproduced defect and minimal repair

Pinned e114dee trialgESsAz: structural probe PASS19samples/44client+47server rows, but acceptance FAIL with exactly RESOLVED_OWNER_STANDARD/GROUND/CURVE, ABSENT_OWNER_RETURNED_CURVE and RESOLVED_OWNER_RETURNED_CURVE. All other lifecycle/native-motion contracts pass. Born-absent projectiles remain ownerless after actual owner arrival; returned Curve continues reporting the old Player both while its new owner is absent and after re-admission. Retain this immutable pre-product-change failure; it is not a fixture/timing failure.

Mapped Projectile.setOwner(null) does nothing; vanilla client getOwner cannot resolve a UUID. Previous applyFlight discards an unavailable owner ID and leaves a prior cached owner untouched. Product72dac3b retains the authoritative client ID in a small client-only state helper. getOwner retries at most once per world tick, immediately drops a replaced owner, rejects removed objects, and returns null for ID0/removal. onRemovedFromWorld clears pending/resolved state. Duplicate same-ID spawn/metadata notifications retain a valid cache without violating the lookup bound. Server owner/provenance/damage/path-clock behavior and existing flight NBT/spawn data remain unchanged.

Mapped build and Standard/Committed/Ground32checks plus19client-owner state checks pass. Genuine missing-helper RED and same-tick duplicate metadata RED precede their corrections. State tests cover zero, delayed arrival, replacement, duplicate notifications, lookup bounds, removed cache/result, same-ID distinct replacement and pending/resolved cancellation. These test the actual production helper; they do not claim native ID0/removal-fault injection.

## Native GREEN

Pinned72dac3b/TechHub76e587e/host7f14960 trialpxC7gS PASS20samples/44client+47server rows. Both trials have six-native-tick controlled owner gaps and zero probe/cleanup errors. All hidden rows now report no owner; all post-admission rows report the actual new owner, never the old Player. First correct END observations occur1–2ms after the recorded client admission; this is observed receipt timing, not a general network-latency guarantee.

| Projectile | Hidden rows | Following rows | Hidden owner | Following owner |
| --- | ---: | ---: | --- | --- |
| STANDARD | 5 | 4 | null | actual test Ghast |
| GROUND | 5 | 4 | null | actual test Ghast |
| CURVE | 5 | 4 | null | actual test Ghast |
| RETURNED_CURVE | 4 | 4 | null | actual test Ghast |

Standard and normalized returned Curve retain native air displacement/float.95 acceleration. Ground retains actual1.9 zero-power motion/inertia1. Unnormalized Curve advances its native immutable path clock. One compact scoped Astra review caught an analyzer using Standard inertia for Ground; realistic Ground fixture RED, one correction to actual inertia and an unintended-decay negative resolve it. No re-review or unchanged native retry.25new/99related Node contracts, mapped private javac/syntax and cached offline genuine build pass. Product normal JARs positively exclude the private probe and TrackerReaddIntent helper.

## Closure and immutable provenance

RED/GREEN both receive canonical CLEAN_EVIDENCE_SHUTDOWN with zero dropped/queued rows, VERIFIED_EXIT843/1080ms, EVIDENCE_COMPLETE11/13canonical observations with2lanes each; no canonical errors, partial files, image captures or ongoing recording. Owned tracker is re-admitted normally; cleanup arrays are empty, with no outstanding removal. Failure cleanup remains guarded by actual owned StopTracking and reviewed TrackerReaddIntent: no repeat after a begun/uncertain add. Source fault coverage from the prior unit is not native I/O-fault proof.

Independently reconcile both12raw JSONL sets (four arrays, seven scalar receipts and actual owner re-pairing identity), all15supplement hashes and product/probe/fixture bytes. Historical original85files AND full count match by raw SHA256 after closure; accepted manual-swimming JAR remains6c5d2156e9ad83d437d3106721221c13e5221531c1518a069274564364ab3bca. Never reopen original/finalized worlds through NBT/RegionFile; only fresh copies are prepared. Private trial reports under build/tank record complete run/source/material paths and supplement hashes; these are separately hashed diagnostic supplements, not canonical room-roster/fixed-camera proof.

| Material SHA256 | RED gESsAz | GREEN pxC7gS |
| --- | --- | --- |
| report.json | 0509c30a49301df4e16c32146c5d48acfee989af65f298060caa8b72c79bae4e | d6cf591aa40a2313e1caa80793df3d57b090f01c1e8f712dfd5288156b8e63da |
| private product JAR | 7aada6bd9cc6cc845cce1abd7bd55abe10eff114d0acff957e7c302e73236866 | 8266d8447ca8ffeea24b212173192bed5da154115b1de0475c27a388a9889495 |
| private probe JAR | ee90d86be85453f2258de41b6a8a7e3499ade584c2a761644de34b8b2e3e889e | 73b02766a2c09992c441c75ad4d4871cc6569d2e231e26289f8d537ebe51ac83 |
| fixture.json | 5db741d169e05b5f244bf46112398166c2b8abe7642c7c2e8fd5ee559031f294 | 5db741d169e05b5f244bf46112398166c2b8abe7642c7c2e8fd5ee559031f294 |
| request.json | f0c50f51d2612a0861404c29b1af51a3ea982e38e305e867f063d798a84dc526 | b3a3e6386a4ca59df84df03806a50ac7b43fba2e34bbf88e27d1e1ae55954bf4 |
| result.json | 9ba9196356637c66d8bbbe5c593b2fbff3bed8b99ad512aa39a2349e558e3af4 | 436872a1d029d55ee7bcab67b980cf40e5a281d5d42a3887441cf3cf6ae1bcc7 |
| analysis.json | d1323791403d2993d52129d792b87a81ac8944f1f9bbed944e35f3be168dc0a4 | fe78a5bfb24db23b0c6a4ec59fb0732e62aa06c398058b40f5b9815f9cd1fb07 |

## Remaining scope

This closes controlled owner availability/replacement only. Genuine new/late client connection, native missing-owner removal/ID0 fault scenarios, arbitrary unload/crash, natural unobserved selectors, human melee/input/readability/balance and full release remain open. Prior passive/re-tracking/restart receipts keep their original boundaries. Grand Danmaku and Ground barrage/fans remain deferred. Tank improvement: authenticate finite test ownership and separate unavailable owner ID, actual client presence, authoritative replacement and observed movement; retain RED and GREEN, with cleanup/evidence/process closure independent of gameplay verdict. No automatic acceptance widening.

Documentation correction: Windows default cp932 output failed on an en dash;47fe5a2 inadvertently committed an empty receipt. Explicit UTF-8 repair preserves that history, verifies the populated document before the next commit, and does not alter any immutable native evidence.
