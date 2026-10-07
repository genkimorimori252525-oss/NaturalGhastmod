param(
    [Parameter(Mandatory=$true)][string]$ClasspathFile,
    [string]$JavaHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$output = Join-Path $root 'build/forge-clearance-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
# The registered host's Java argument file supplies existing mapped dependencies.
$hostClasspath = (Get-Content -LiteralPath $ClasspathFile)[1].Trim('"')
$classpath = "$root/build/classes/java/main;$hostClasspath"
$arguments = @('--release', '17', '-encoding', 'UTF-8', '-cp', $classpath, '-d', $output,
    "$root/src/main/java/com/genki/soutoughast/entity/ai/SoutouGhastInertialMoveControl.java",
    "$root/src/test/java/com/genki/soutoughast/entity/ai/ForgeClearanceTest.java")
$arguments += @(Get-ChildItem -LiteralPath "$root/src/main/java/com/genki/soutoughast/entity/ai/flight" -Filter '*.java' | ForEach-Object FullName)
$argsFile = Join-Path $output 'compile.args'
[System.IO.File]::WriteAllLines($argsFile, @($arguments | ForEach-Object { '"' + $_.Replace('\','/') + '"' }))
& "$JavaHome/bin/javac.exe" "@$argsFile"
if ($LASTEXITCODE -ne 0) { throw 'Forge clearance compilation failed.' }
$runFile = Join-Path $output 'run.args'
$runClasspath = '"' + "$output;$classpath".Replace('\','/') + '"'
[System.IO.File]::WriteAllLines($runFile, @('-ea', '-cp', $runClasspath, 'com.genki.soutoughast.entity.ai.ForgeClearanceTest'))
& "$JavaHome/bin/java.exe" "@$runFile"
if ($LASTEXITCODE -ne 0) { throw 'Forge clearance assertions failed.' }
