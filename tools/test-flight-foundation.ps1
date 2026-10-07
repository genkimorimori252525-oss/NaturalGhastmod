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
$sources += "$root/src/test/java/com/genki/soutoughast/entity/ai/flight/StandardAttackTest.java"
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
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.flight.StandardAttackTest
if ($LASTEXITCODE -ne 0) { throw 'Standard charge/rally assertions failed.' }
