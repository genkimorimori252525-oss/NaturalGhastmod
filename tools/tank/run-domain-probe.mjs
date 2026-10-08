import {spawn, execFileSync} from 'node:child_process';
import {createWriteStream} from 'node:fs';
import {mkdir, mkdtemp, readFile, writeFile, realpath, lstat} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {randomUUID, createHash} from 'node:crypto';
import {gzipSync} from 'node:zlib';

// Finite dedicated GameTest process in a newly owned world; zero original-world API access.
const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const javaHome=process.env.JAVA_HOME;
if(process.platform!=='win32'||!javaHome)throw new Error('DOMAIN_PROBE_REQUIRES_WINDOWS_JAVA_HOME17');
const git=(...args)=>execFileSync('git',args,{cwd:repo,encoding:'utf8',windowsHide:true}).trim();
if(git('status','--porcelain'))throw new Error('DOMAIN_PROBE_REQUIRES_COMMITTED_CLEAN_SOURCE');
const revision=git('rev-parse','HEAD');
const parent=path.join(repo,'build','tank');await mkdir(parent,{recursive:true});
const reopen=process.argv[2]==='--reopen';
if(process.argv.length!==(reopen?4:2))throw new Error('Usage: node run-domain-probe.mjs [--reopen <owned-root>]');
let root,nonce;
if(reopen){
 root=await realpath(process.argv[3]);if(path.dirname(root)!==await realpath(parent)||!path.basename(root).startsWith('domain-probe-'))throw new Error('DOMAIN_PROBE_REOPEN_PATH');
 const owner=(await readFile(path.join(root,'probe-owner.txt'),'utf8')).split('\n');nonce=owner[0];if(owner[1]!==revision||owner[2]!==''||owner.length!==3)throw new Error('DOMAIN_PROBE_REOPEN_SOURCE');
 const baseline=JSON.parse(await readFile(path.join(root,'baseline-run.json'),'utf8'));if(baseline.verdict!=='PASS'||baseline.nonce!==nonce)throw new Error('DOMAIN_PROBE_REOPEN_REQUIRES_PASS');
}else{
 root=await realpath(await mkdtemp(path.join(parent,'domain-probe-')));nonce=randomUUID();await writeFile(path.join(root,'probe-owner.txt'),`${nonce}\n${revision}\n`,{flag:'wx'});
 for(const folder of ['runtime','universe','sidecar-resources/data/natural_domain_probe/structures'])await mkdir(path.join(root,folder),{recursive:true});
 const int=n=>{const b=Buffer.alloc(4);b.writeInt32BE(n);return b;};
 const name=s=>{const bytes=Buffer.from(s,'utf8'),n=Buffer.alloc(2);n.writeUInt16BE(bytes.length);return Buffer.concat([n,bytes]);};
 const tag=(type,key,data)=>Buffer.concat([Buffer.from([type]),name(key),data]);
 const list=(type,values)=>Buffer.concat([Buffer.from([type]),int(values.length),...values]);
 const positions=[];for(let x=0;x<3;x++)for(let y=0;y<3;y++)for(let z=0;z<3;z++)positions.push(Buffer.concat([tag(3,'state',int(0)),tag(9,'pos',list(3,[int(x),int(y),int(z)])),Buffer.from([0])]));
 const template=Buffer.concat([Buffer.from([10]),name(''),tag(3,'DataVersion',int(3465)),tag(9,'size',list(3,[int(3),int(3),int(3)])),tag(9,'palette',list(10,[Buffer.concat([tag(8,'Name',name('minecraft:air')),Buffer.from([0])])])),tag(9,'blocks',list(10,positions)),tag(9,'entities',list(10,[])),Buffer.from([0])]);
 await writeFile(path.join(root,'sidecar-resources/data/natural_domain_probe/structures/empty.nbt'),gzipSync(template),{flag:'wx'});
}
if(!/^[a-f0-9-]{36}$/.test(nonce))throw new Error('DOMAIN_PROBE_NONCE');
const safeDirectories=async target=>{for(let current=target;;current=path.dirname(current)){const state=await lstat(current);if(!state.isDirectory()||state.isSymbolicLink())throw new Error('DOMAIN_PROBE_UNSAFE_DIRECTORY');if(path.dirname(current)===current)break;}};
await safeDirectories(root);await safeDirectories(path.join(root,'runtime'));await safeDirectories(path.join(root,'universe'));if(reopen)await safeDirectories(path.join(root,'universe/domain-owned'));
const scenario=reopen?'reopen-terminal':'baseline';
const env={...process.env,JAVA_HOME:javaHome,NATURAL_DOMAIN_PROBE_ROOT:root,NATURAL_DOMAIN_PROBE_NONCE:nonce,NATURAL_DOMAIN_PROBE_SOURCE:revision,NATURAL_DOMAIN_PROBE_SCENARIO:scenario};
const ownedPids=()=>{
 const code="@(Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($env:NATURAL_DOMAIN_PROBE_NONCE) -and $_.CommandLine.Contains('naturalghast.domainProbe.root') } | ForEach-Object { $_.ProcessId }) -join ','";
 const raw=execFileSync('powershell.exe',['-NoLogo','-NoProfile','-Command',code],{env,encoding:'utf8',windowsHide:true,timeout:15000}).trim();return raw?raw.split(',').map(Number):[];
};
if(ownedPids().length)throw new Error('DOMAIN_PROBE_ALREADY_RUNNING');
const logPath=path.join(root,`${scenario}.log`),log=createWriteStream(logPath,{flags:'wx'});let size=0,timedOut=false,overflow=false,stopError='';
const child=spawn('powershell.exe',['-NoLogo','-NoProfile','-Command','& ./gradlew.bat -I tools/tank/domain-probe.gradle runGameTestServer --no-daemon; exit $LASTEXITCODE'],{cwd:repo,env,windowsHide:true,stdio:['ignore','pipe','pipe']});
const stop=()=>{
 timedOut=true;
 // Stop only Java processes attested by this unique private nonce/property; never a global Java kill.
 const code="Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($env:NATURAL_DOMAIN_PROBE_NONCE) -and $_.CommandLine.Contains('naturalghast.domainProbe.root') } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction Stop }";
 try{execFileSync('powershell.exe',['-NoLogo','-NoProfile','-Command',code],{env,windowsHide:true,timeout:15000});}catch(error){stopError=error.name;}finally{child.kill();}
};
const accept=chunk=>{size+=chunk.length;if(size<=8*1024*1024)log.write(chunk);else if(!overflow){overflow=true;stop();}};child.stdout.on('data',accept);child.stderr.on('data',accept);
const timer=setTimeout(stop,240000);console.log(JSON.stringify({root,scenario,sourceRevision:revision,scope:'DIRECT_NATIVE_DOMAIN_RELIABILITY_BASELINE',wallLimitSeconds:240}));
let exitCode;
try{exitCode=await new Promise((resolve,reject)=>{child.once('error',reject);child.once('close',resolve);});}finally{clearTimeout(timer);await new Promise(resolve=>log.end(resolve));}
const remaining=ownedPids();let native=null;try{native=JSON.parse(await readFile(path.join(root,`${scenario}-native.json`),'utf8'));}catch{}
const logBytes=await readFile(logPath);const unchanged=git('rev-parse','HEAD')===revision&&!git('status','--porcelain');
const pass=exitCode===0&&!timedOut&&!overflow&&!remaining.length&&unchanged&&native?.verdict==='PASS'&&native.nonce===nonce&&native.sourceRevision===revision&&native.scenario===scenario&&native.scope==='DIRECT_NATIVE_DOMAIN_RELIABILITY_BASELINE';
const result={scope:'DIRECT_NATIVE_DOMAIN_RELIABILITY_BASELINE',verdict:pass?'PASS':'FAIL',nonce,sourceRevision:revision,scenario,exitCode,timedOut,overflow,stopError,remainingOwnedJavaPids:remaining,sourceUnchanged:unchanged,logSha256:createHash('sha256').update(logBytes).digest('hex'),native};
const receipt=path.join(root,`${scenario}-run.json`);await writeFile(receipt,JSON.stringify(result,null,2)+'\n',{flag:'wx'});console.log(JSON.stringify({receipt,verdict:result.verdict,exitCode,nativeChecks:native?.checks,nativeGameTicks:native?.gameTicks,remainingOwnedJavaPids:remaining}));
if(!pass){console.log(logBytes.toString('utf8').split(/\r?\n/).slice(-20).map(line=>line.slice(0,500)).join('\n'));process.exitCode=1;}
