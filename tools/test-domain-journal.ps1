param([string]$JavaHome=$env:JAVA_HOME,[Parameter(Mandatory=$true)][string]$GsonJar)
$ErrorActionPreference='Stop'
if(!$JavaHome){throw 'Set JAVA_HOME or pass -JavaHome'}
$root=Split-Path $PSScriptRoot -Parent
$output=Join-Path $root 'build/domain-journal-tests'
New-Item -ItemType Directory -Force $output | Out-Null
$package="$root/src/main/java/com/genki/soutoughast/entity/ai/domain"
$sources=@('DomainGeometry','DomainOverlay','DomainJournalCodec','DomainJournalRepository') | ForEach-Object {"$package/$_.java"}
$sources+="$root/src/test/java/com/genki/soutoughast/entity/ai/domain/DomainJournalTest.java"
& "$JavaHome/bin/javac.exe" --release 17 -encoding UTF-8 -cp $GsonJar -d $output @sources
if($LASTEXITCODE){throw 'Domain journal compilation failed'}
& "$JavaHome/bin/java.exe" -ea -cp "$output;$GsonJar" com.genki.soutoughast.entity.ai.domain.DomainJournalTest
if($LASTEXITCODE){throw 'Domain journal checks failed'}
