param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not $JavaHome) { throw 'Set JAVA_HOME to a Java 17 JDK or pass -JavaHome.' }
$output = Join-Path $root 'build/flight-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$core = "$root/src/main/java/com/genki/soutoughast/entity/ai/flight"
$sources = @()
if (Test-Path -LiteralPath $core) {
    $sources = @(Get-ChildItem -LiteralPath $core -Filter '*.java' | ForEach-Object FullName)
}
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/FlightFoundationTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/MovementMobilityTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/CombatRegionSwimmingTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/ObservedTacticsTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/CompleteFeintsTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/StandardAttackTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/CommittedTrajectoryTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/ProjectileSelectionTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/OverheadBombingTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/GroundCombatTest.java"
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/DomainGroundPolicyTest.java"
$sources += "$root/tools/tank/ObservationFailureBoundary.java"
$sources += "$root/tools/tank/ObservationFailureBoundaryTest.java"
& "$JavaHome/bin/javac.exe" --release 17 -encoding UTF-8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Flight foundation compilation failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.CombatRegionSwimmingTest
if ($LASTEXITCODE -ne 0) { throw 'Combat region/swimming assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.FlightFoundationTest
if ($LASTEXITCODE -ne 0) { throw 'Flight foundation assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.MovementMobilityTest
if ($LASTEXITCODE -ne 0) { throw 'Movement/mobility assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.ObservedTacticsTest
if ($LASTEXITCODE -ne 0) { throw 'Observed tactics assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.CompleteFeintsTest
if ($LASTEXITCODE -ne 0) { throw 'Complete feint assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.StandardAttackTest
if ($LASTEXITCODE -ne 0) { throw 'Standard charge/rally assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.CommittedTrajectoryTest
if ($LASTEXITCODE -ne 0) { throw 'Committed trajectory assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.ProjectileSelectionTest
if ($LASTEXITCODE -ne 0) { throw 'Projectile selection/validation assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.OverheadBombingTest
if ($LASTEXITCODE -ne 0) { throw 'Overhead/director assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.GroundCombatTest
if ($LASTEXITCODE -ne 0) { throw 'Ground movement/cadence assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.DomainGroundPolicyTest
if ($LASTEXITCODE -ne 0) { throw 'Domain preparation/shared-major assertions failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.tank.ObservationFailureBoundaryTest
if ($LASTEXITCODE -ne 0) { throw 'Observer failure containment assertions failed.' }
