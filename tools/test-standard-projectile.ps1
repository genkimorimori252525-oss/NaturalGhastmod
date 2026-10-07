param([Parameter(Mandatory=$true)][string]$ClasspathFile,[string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/standard-projectile-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$hostClasspath=(Get-Content -LiteralPath $ClasspathFile)[1].Trim('"')
$classpath="$root/build/classes/java/main;$hostClasspath"
$arguments=@('--release','17','-encoding','UTF-8','-proc:none','-cp',$classpath,'-d',$output,
    "$root/src/main/java/com/genki/soutoughast/entity/projectile/StandardProvenance.java",
    "$root/src/main/java/com/genki/soutoughast/entity/projectile/CommittedTrajectoryCodec.java",
    "$root/src/main/java/com/genki/soutoughast/entity/ai/CommittedPathClearance.java",
    "$root/src/test/java/com/genki/soutoughast/entity/projectile/StandardProjectileTest.java",
    "$root/src/test/java/com/genki/soutoughast/entity/projectile/GroundProjectileTest.java",
    "$root/src/test/java/com/genki/soutoughast/entity/projectile/CommittedProjectileTest.java")
$arguments+=@(Get-ChildItem -LiteralPath "$root/src/main/java/com/genki/soutoughast/entity/ai/flight" -Filter '*.java'|ForEach-Object FullName)
$file=Join-Path $output 'compile.args'
[IO.File]::WriteAllLines($file,@($arguments|ForEach-Object{'"'+$_.Replace('\','/')+'"'}))
& "$JavaHome/bin/javac.exe" "@$file"
if($LASTEXITCODE){throw 'Standard mapped compilation failed'}
$run=Join-Path $output 'run.args'
$runClasspath='"'+"$output;$classpath".Replace('\','/')+'"'
[IO.File]::WriteAllLines($run,@('-ea','-cp',$runClasspath,'com.genki.soutoughast.entity.projectile.StandardProjectileTest'))
& "$JavaHome/bin/java.exe" "@$run"
if($LASTEXITCODE){throw 'Standard mapped assertions failed'}
$profileRun=Join-Path $output 'profile-run.args'
[IO.File]::WriteAllLines($profileRun,@('-ea','-cp',$runClasspath,'com.genki.soutoughast.entity.projectile.CommittedProjectileTest'))
& "$JavaHome/bin/java.exe" "@$profileRun"
if($LASTEXITCODE){throw 'Committed mapped assertions failed'}
$groundRun=Join-Path $output 'ground-run.args'
[IO.File]::WriteAllLines($groundRun,@('-ea','-cp',$runClasspath,'com.genki.soutoughast.entity.projectile.GroundProjectileTest'))
& "$JavaHome/bin/java.exe" "@$groundRun"
if($LASTEXITCODE){throw 'Ground mapped assertions failed'}
