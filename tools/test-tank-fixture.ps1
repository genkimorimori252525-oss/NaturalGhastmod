param(
    [Parameter(Mandatory=$true)][string]$ClasspathFile,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/tank-fixture-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$classpath=(Get-Content -LiteralPath $ClasspathFile)[1].Trim('"')
$sources=@('TankSeedEntities.java','PrepareFoundationTank.java','PrepareFlightTank.java','TankSeedEntitiesTest.java','FlightFixtureTest.java')
$arguments=@('--release','17','-encoding','UTF-8','-proc:none','-cp',$classpath,'-d',$output)
$arguments+=@($sources | ForEach-Object { Join-Path $root "tools/tank/$_" })
$argsFile=Join-Path $output 'compile.args'
[IO.File]::WriteAllLines($argsFile,@($arguments | ForEach-Object { '"'+$_.Replace('\','/')+'"' }))
& "$JavaHome/bin/javac.exe" "@$argsFile"
if($LASTEXITCODE){throw 'Tank fixture compilation failed'}
foreach($test in @('TankSeedEntitiesTest','FlightFixtureTest')){
    $runFile=Join-Path $output "$test.args"
    $runClasspath='"'+"$output;$classpath".Replace('\','/')+'"'
    [IO.File]::WriteAllLines($runFile,@('-ea','-cp',$runClasspath,"com.genki.soutoughast.tank.$test"))
    & "$JavaHome/bin/java.exe" "@$runFile"
    if($LASTEXITCODE){throw "Tank fixture regression failed: $test"}
}
