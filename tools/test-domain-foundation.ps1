param([string]$JavaHome=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(!$JavaHome){throw 'Set JAVA_HOME or pass -JavaHome'}
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/domain-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$sources=@(Get-ChildItem -LiteralPath "$root/src/main/java/com/genki/soutoughast/entity/ai/domain" -Filter '*.java' -ErrorAction SilentlyContinue | ForEach-Object FullName)
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainOverlayTest.java"
& "$JavaHome/bin/javac.exe" --release 17 -encoding UTF-8 -d $output @sources
if($LASTEXITCODE){throw 'Domain source compilation failed'}
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.entity.ai.domain.DomainOverlayTest
if($LASTEXITCODE){throw 'Domain source contracts failed'}
