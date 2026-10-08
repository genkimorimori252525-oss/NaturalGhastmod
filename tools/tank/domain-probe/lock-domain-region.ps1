param([Parameter(Mandatory=$true)][string]$ProbeRoot,[Parameter(Mandatory=$true)][string]$Nonce)
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath($ProbeRoot)
$taskName=Split-Path -Leaf $taskRoot
if($Nonce -notmatch '^[a-f0-9-]{36}$' -or !$taskName.StartsWith('domain-probe-')){throw 'REGION_LOCK_OWNER'}
$taskOwner=[IO.File]::ReadAllText((Join-Path $taskRoot 'probe-owner.txt')) -split "`n"
if($taskOwner.Count -ne 3 -or $taskOwner[0] -ne $Nonce -or $taskOwner[1] -notmatch '^[a-f0-9]{40}$' -or $taskOwner[2] -ne ''){throw 'REGION_LOCK_OWNER'}
$taskWorld=[IO.Path]::GetFullPath((Join-Path $taskRoot 'universe/domain-owned'))
$taskRegion=[IO.Path]::GetFullPath((Join-Path $taskWorld 'region/r.0.0.mca'))
if(!$taskRegion.StartsWith($taskWorld+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'REGION_LOCK_PATH'}
for($taskAncestor=$taskRegion;$taskAncestor;$taskAncestor=[IO.Path]::GetDirectoryName($taskAncestor)){
 $taskItem=Get-Item -LiteralPath $taskAncestor
 if($taskItem.Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'REGION_LOCK_REPARSE'}
}
function Write-Receipt([string]$Name,$Data){
 $taskBytes=[Text.UTF8Encoding]::new($false).GetBytes(($Data|ConvertTo-Json -Compress)+"`n")
 $taskPrepared=Join-Path $taskRoot ($Name+'.prepared')
 $taskOut=[IO.File]::Open($taskPrepared,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
 try{$taskOut.Write($taskBytes,0,$taskBytes.Length);$taskOut.Flush($true)}finally{$taskOut.Dispose()}
 [IO.File]::Move($taskPrepared,(Join-Path $taskRoot $Name))
}
$taskStream=$null;$taskAcquired=$false;$taskReleased=$false;$taskTimeout=$false;$taskError=''
try{
 $taskStream=[IO.File]::Open($taskRegion,[IO.FileMode]::Open,[IO.FileAccess]::ReadWrite,[IO.FileShare]::ReadWrite)
 if($taskStream.Length -lt 8192){throw 'REGION_LOCK_HEADER_MISSING'}
 $taskStream.Lock(0,8192);$taskAcquired=$true
 Write-Receipt 'region-lock-ready.json' @{nonce=$Nonce;pid=$PID;acquired=$true;offset=0;length=8192;region='r.0.0.mca'}
 $taskClock=[Diagnostics.Stopwatch]::StartNew();$taskRelease=Join-Path $taskRoot 'region-lock-release.txt'
 while(!(Test-Path -LiteralPath $taskRelease) -and $taskClock.Elapsed.TotalSeconds -lt 20){Start-Sleep -Milliseconds 25}
 if(!(Test-Path -LiteralPath $taskRelease)){$taskTimeout=$true}else{if([IO.File]::ReadAllText($taskRelease) -ne ($Nonce+"`n")){throw 'REGION_LOCK_RELEASE_OWNER'}}
}catch{$taskError=$_.Exception.GetType().Name}
finally{
 if($taskStream){try{if($taskAcquired){$taskStream.Unlock(0,8192);$taskReleased=$true}}catch{$taskError=$_.Exception.GetType().Name}finally{$taskStream.Dispose()}}
 Write-Receipt 'region-lock-end.json' @{nonce=$Nonce;pid=$PID;acquired=$taskAcquired;released=$taskReleased;timedOut=$taskTimeout;errorClass=$taskError}
}
if(!$taskAcquired -or !$taskReleased -or $taskTimeout -or $taskError){exit 1}
