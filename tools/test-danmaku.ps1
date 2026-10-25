param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) { throw 'Java17 JDK required.' }
$root = Split-Path $PSScriptRoot -Parent
$output = Join-Path $root 'build/danmaku-tests'
New-Item -ItemType Directory -Path $output -Force | Out-Null
$sources = @(Get-ChildItem -LiteralPath "$root/src/main/java/com/genki/soutoughast/danmaku/core" -Filter '*.java' | ForEach-Object FullName)
$sources += @(Get-ChildItem -LiteralPath "$root/src/danmakuTest/java/com/genki/soutoughast/danmaku/core" -Filter '*.java' | ForEach-Object FullName)
& "$JavaHome/bin/javac.exe" --release 17 -encoding UTF-8 -d $output @sources
if ($LASTEXITCODE -ne 0) { throw 'Danmaku core compilation failed.' }
$scores = "$root/src/main/resources/data/soutou_ghast/danmaku"
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.danmaku.core.GrandDanmakuCoreTest "$scores/grand_danmaku_v1.json"
if ($LASTEXITCODE -ne 0) { throw 'Original engine test-score contracts failed.' }
& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.danmaku.core.GrandDanmakuPackageSmoke "$scores/kouengeki_grand_danmaku_v1.json" "$scores/kokuseki_kairo_grand_danmaku_v1.json" "$scores/seisou_tenmaku_grand_danmaku_v2.json"
if ($LASTEXITCODE -ne 0) { throw 'Supplied canonical scores failed strict core acceptance.' }

& "$JavaHome/bin/java.exe" -ea -cp $output com.genki.soutoughast.danmaku.core.WorldDanmakuTest $scores
if ($LASTEXITCODE -ne 0) { throw 'World frame/swarm/collision contracts failed.' }
