# NaturalGhastmod — Soutou Ghast redesign

Minecraft1.20.1 / Forge47.4.10 / Java17. Development branch `codex/natural-tactics-20261008`; registry ID remains `soutou_ghast:soutou_ghast`. This branch implements the scoped non-danmaku redesign. GitHub main may still describe the older0.02snapshot; select the exact revision and artifact before testing.

Read the [current design](docs/design/NATURAL-GHAST-REDESIGN-v0.1.md), its dated human amendments, and the [implementation/evidence audit](docs/NON-DANMAKU-IMPLEMENTATION-AUDIT.md). The latest direct human requirements take precedence over older snapshot descriptions and acceptance checklists.

Implemented scope includes boss-owned broad combat regions and inertial floating; observed-only tactical selection, movement feints and natural-flow overhead relocation; ordinary fallible shots/Rally, learned return-face dodges, committed Burst/Curve/Lob profiles, remembered-cover Lob and observed projectile evasion; Overhead Bombing, positive Ground fallback and reversible Domain support; persisted projectile/entity state and passive client synchronization. The latest human direction now authorizes the supplied three Grand Danmaku scores and four textures. Data, world swarm, Forge server/network/render/director integration and source/model/build checks are implemented. Finite native rendering/channel verification is in progress. Ground barrage/fans remain excluded. See the [current port plan](docs/superpowers/plans/2026-10-25-supplied-grand-danmaku.md). Optional compatibility is conditional.

Ordinary shots briefly slow existing floating intent while aiming. The AI receives one60–90tick decision opportunity, can deliberately decline, and resumes float earlier after declining. Projectile aim commitment is independent from visible-target face attention. The [rhythm plan](docs/superpowers/plans/2026-10-25-ordinary-shot-rhythm.md) and [learned Rally plan](docs/superpowers/plans/2026-10-25-learned-rally-feint.md) specify bounds and safety precedence.

## Verification

Use a JDK17 and the project wrapper. Cached dependencies permit offline verification:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-17'
./tools/test-flight-foundation.ps1 -JavaHome $env:JAVA_HOME
./gradlew.bat compileJava processResources --offline --console=plain
```

The grouped pure tests and actual Forge compilation pass for the current implementation unit. Source/model/build checks, controlled native cases, genuine Player input and subjective acceptance are separate scopes. The audit links detailed receipts and retained FAIL/BLOCKED/UNKNOWN cases. Do not infer general release, multiplayer compatibility, balance or every naturally selected action from compilation or a finite client run. The human has declined further manual inspections; retain unobserved cases honestly.

Use the existing source-bound KNEEKURA-TECH-HUB Tank with finite actor/world/process authority. Never recreate legacy standalone LAB/SimLab as fallback, open original/finalized world NBT, replace accepted artifacts, inject Player inputs or force behavior to fill coverage gaps. Target-MOD technical fixtures and probes remain separate from production JARs.

## Historical snapshot

The original0.02snapshot and its compile/runtime defects are preserved in the [2026-10-06 audit](docs/SOUTOU-GHAST-AUDIT-2026-10-06.md), [publication provenance](docs/audit/provenance.json) and [change log](CHANGELOG.md). Those historical failures do not describe this redesigned branch's current compile result. The original Forge MDK README.txt is reference material.
