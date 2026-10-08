param([Parameter(Mandatory=$true)][string]$ClasspathFile,[string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JavaHome){throw 'Set JAVA_HOME or pass -JavaHome'}
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/domain-state-codec-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$classpath=(Get-Content -LiteralPath $ClasspathFile)[1].Trim('"')
$package="$root/src/main/java/com/genki/soutoughast/entity/ai/domain"
$sources=@('DomainGeometry','DomainOverlay','DomainJournalCodec','DomainStateCodec')|ForEach-Object{"$package/$_.java"}
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainStateCodecTest.java"
$compile=Join-Path $output 'compile.args'
[IO.File]::WriteAllLines($compile,@(@('--release','17','-encoding','UTF-8','-proc:none','-cp',$classpath,'-d',$output)+$sources|ForEach-Object{'"'+$_.Replace('\','/')+'"'}))
& "$JavaHome/bin/javac.exe" "@$compile"
if($LASTEXITCODE){throw 'Domain state codec compilation failed'}
$run=Join-Path $output 'run.args'
$runClasspath='"'+"$output;$classpath".Replace('\','/')+'"'
[IO.File]::WriteAllLines($run,@('-ea','-cp',$runClasspath,'com.genki.soutoughast.entity.ai.domain.DomainStateCodecTest'))
& "$JavaHome/bin/java.exe" "@$run"
if($LASTEXITCODE){throw 'Domain state codec checks failed'}
