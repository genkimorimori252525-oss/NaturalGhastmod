param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JavaHome){throw 'Set JAVA_HOME or pass -JavaHome'}
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/domain-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$sources=@('DomainGeometry','DomainOverlay','DomainPersistenceBarrier','DomainCoordinator') | ForEach-Object {"$root/src/main/java/com/genki/soutoughast/entity/ai/domain/$_.java"}
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainOverlayTest.java"
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainDurabilityTest.java"
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainPersistenceTest.java"
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainCoordinatorTest.java"
& "$JavaHome/bin/javac.exe" --release 17 -encoding UTF-8 -d $output @sources
if($LASTEXITCODE){throw 'Domain source compilation failed'}
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.domain.DomainOverlayTest
if($LASTEXITCODE){throw 'Domain source contracts failed'}
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.domain.DomainDurabilityTest
if($LASTEXITCODE){throw 'Domain durability contracts failed'}
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.domain.DomainPersistenceTest
if($LASTEXITCODE){throw 'Domain persistence contracts failed'}
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.domain.DomainCoordinatorTest
if($LASTEXITCODE){throw 'Domain coordinator contracts failed'}
