import fs from 'node:fs/promises';
import path from 'node:path';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {pathToFileURL,fileURLToPath} from 'node:url';
import {privateFlightLaunchArgs} from './flight-launch.mjs';
import {analyzeCompleteFeints,completeFeintsScope} from './complete-feints-results.mjs';

// Finite explicit composer actors; no natural-selection or readability claim.
const option=name=>{const i=process.argv.indexOf('--'+name);assert(i>=0&&process.argv[i+1],'Missing --'+name);return path.resolve(process.argv[i+1]);};
const lab=option('lab'),templateFile=option('template'),original=option('original'),classpathFile=option('classpath-file'),javaHome=option('java-home');
const repository=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const template=JSON.parse(await fs.readFile(templateFile));assert.equal(template.launch.env.KNEEKURA_DEBUG_MOD_PROFILE,'TANK_CORE');
const host=path.isAbsolute(template.launch.command)?path.dirname(template.launch.command):template.workspaceDir;
const parent=path.join(repository,'build/tank');await fs.mkdir(parent,{recursive:true});const trial=await fs.mkdtemp(path.join(parent,'complete-feints-'));
const load=name=>import(pathToFileURL(path.join(lab,'debug-workspace',name)));
const {launchDebugRun,readCurrent,stopCurrent}=await load('core.mjs');
const {finalizeNativeTrial}=await load('bridge/native/trial-finalization.mjs');const {finalizeEvidenceRun}=await load('evidence/finalize.mjs');
const {verifyCompiledClasses}=await load('bridge/native/class-readiness.mjs');
const read=file=>fs.readFile(file,'utf8').then(JSON.parse),sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const write=(file,value)=>fs.writeFile(file,JSON.stringify(value)+'\n',{flag:'wx'});
const git=(cwd,...args)=>execFileSync('git',args,{cwd,encoding:'utf8',windowsHide:true}).trim();
const java=(tool,args)=>execFileSync(path.join(javaHome,'bin',tool+'.exe'),args,{cwd:trial,windowsHide:true,maxBuffer:16*1024*1024});
async function argsFile(name,values){const file=path.join(trial,name);assert(values.every(v=>!/[\r\n"]/.test(v)),'Argument quoting');await fs.writeFile(file,values.map(v=>'"'+v.replaceAll('\\','/')+'"').join('\n'),{flag:'wx'});return file;}
async function inventory(root,relative=''){const rows=[];for(const entry of await fs.readdir(path.join(root,relative),{withFileTypes:true})){assert(!entry.isSymbolicLink(),'World symlink');const file=path.join(relative,entry.name);if(entry.isDirectory())rows.push(...await inventory(root,file));else if(entry.isFile())rows.push({file,sha256:sha(await fs.readFile(path.join(root,file)))});else throw Error('Unknown world entry');}return rows.sort((a,b)=>a.file.localeCompare(b.file));}
const report={schema:'naturalghast.complete-feints-integration/v1',trial,scope:completeFeintsScope,failures:[],limitations:['Explicit new private controlled Goals; production composer/controller/native travel, not natural selection.','Real client identity/renderer; no visual readability, human input, balance or attack acceptance.','No ScopedOwner, canonical boss or actor overrides; no restart/late-client acceptance.']};
let originalRows=[],config,current;
console.log('TRIAL '+trial);
try{
 for(const repo of [repository,lab,host])assert.equal(git(repo,'status','--porcelain'),'','Clean pinned source required');
 report.sourceRevision=git(repository,'rev-parse','HEAD');report.labRevision=git(lab,'rev-parse','HEAD');report.hostRevision=git(host,'rev-parse','HEAD');
 try{const out=execFileSync('powershell.exe',['-NoProfile','-NonInteractive','-Command',"& $env:NATURALGHAST_VERIFY_WRAPPER compileJava build --offline '-Dnet.minecraftforge.gradle.check.certs=false' --no-daemon --console=plain; exit $LASTEXITCODE"],{cwd:repository,windowsHide:true,maxBuffer:16*1024*1024,env:{...process.env,JAVA_HOME:javaHome,NATURALGHAST_VERIFY_WRAPPER:path.join(repository,'gradlew.bat')}});await fs.writeFile(path.join(trial,'target-build.log'),out,{flag:'wx'});}catch(error){await fs.writeFile(path.join(trial,'target-build.log'),error.stdout??error.message,{flag:'wx'});throw error;}
 report.bridgeReadiness=await verifyCompiledClasses({outputRoot:path.join(host,'build/classes/java/kneekuraDebug'),classes:['ClientBootstrap','EvidenceWriter'].map(n=>({className:'com.github.tartaricacid.touhoulittlemaid.sim.debug.KneekuraDebug'+n}))});
 originalRows=await inventory(original);await write(path.join(trial,'original-hashes.json'),originalRows);
 const world=path.join(trial,'game/saves/KNEEKURA_DEBUG_WORLD');await fs.mkdir(path.dirname(world),{recursive:true});await fs.cp(original,world,{recursive:true,errorOnExist:true,force:false});
 for(const name of ['options.txt','resourcepacks']){const source=path.join(template.gameDir,name);if(await fs.stat(source).catch(()=>null))await fs.cp(source,path.join(trial,'game',name),{recursive:true,errorOnExist:true,force:false});}
 const cp=path.join(repository,'build/classes/java/main')+';'+report.bridgeReadiness.outputRoot+';'+(await fs.readFile(classpathFile,'utf8')).split(/\r?\n/)[1].replace(/^"|"$/g,'');
 const classes=path.join(trial,'classes');await fs.mkdir(classes);
 const sources=['TankSeedEntities.java','PrepareFlightTank.java','PrepareFeintTank.java','CompleteFeintsProbe.java'].map(n=>path.join(repository,'tools/tank',n));
 java('javac',['@'+await argsFile('javac.args',['--release','17','-encoding','UTF-8','-proc:none','-cp',cp,'-d',classes,...sources])]);
 java('java',['@'+await argsFile('java.args',['-cp',classes+';'+cp,'com.github.tartaricacid.touhoulittlemaid.sim.debug.PrepareFeintTank',trial,parent,'DOMAIN_RELIABILITY'])]);
 const fixture=await read(path.join(trial,'fixture.json'));assert.equal(fixture.variant,'FEINT_RELIABILITY');assert.equal(fixture.newFixtureCombatSubjectOmitted,true);
 const inputs=path.join(trial,'inputs');await fs.mkdir(inputs);
 const product=path.join(inputs,'naturalghast-private-reliability.jar');
 java('jar',['--create','--file',product,'-C',path.join(repository,'build/classes/java/main'),'.','-C',path.join(repository,'build/resources/main'),'.']);
 const normalJars=(await fs.readdir(path.join(repository,'build/libs'))).filter(n=>n.endsWith('.jar')&&!n.endsWith('-sources.jar'));
 assert(normalJars.length,'Normal product JAR missing');
 for(const name of normalJars){const listed=java('jar',['--list','--file',path.join(repository,'build/libs',name)]).toString();assert(!listed.includes('CompleteFeintsProbe')&&!listed.includes('PrepareFeintTank'),'Private probe leaked into product');}
 const resources=path.join(trial,'probe-resource');await fs.mkdir(path.join(resources,'META-INF'),{recursive:true});
 await fs.writeFile(path.join(resources,'META-INF/mods.toml'),'modLoader="javafml"\nloaderVersion="[47,)"\nlicense="Private verification"\n[[mods]]\nmodId="naturalghast_complete_feints_probe"\nversion="1"\ndisplayName="Finite explicit feint composer probe"\n',{flag:'wx'});
 await write(path.join(resources,'pack.mcmeta'),{pack:{pack_format:15,description:'Finite explicit feint composer'}});
 const probe=path.join(inputs,'complete-feints-probe.jar');java('jar',['--create','--file',probe,'-C',classes,'com/genki/soutoughast/entity/ai','-C',resources,'.']);
 report.productHash=sha(await fs.readFile(product));report.probeHash=sha(await fs.readFile(probe));report.fixtureHash=sha(await fs.readFile(path.join(trial,'fixture.json')));
 const init=path.join(trial,'native.init.gradle');
 await fs.writeFile(init,`gradle.beforeProject { p ->\n p.plugins.withId('net.minecraftforge.gradle') {\n p.dependencies.add('runtimeOnly',p.files('${product.replaceAll('\\','/')}','${probe.replaceAll('\\','/')}'))\n p.afterEvaluate { p.minecraft.runs.client.workingDirectory p.file('${path.join(trial,'game').replaceAll('\\','/')}')\n p.tasks.matching { it.name=='runClient' }.configureEach { task -> task.environment System.getenv().findAll { k,v -> k.startsWith('KNEEKURA_DEBUG_') } } }\n }\n}\n`,{flag:'wx'});
 const nonce=crypto.randomUUID();config=structuredClone(template);delete config.ownerControl;
 config.workspaceId='complete-feints-'+nonce;config.workspaceDir=repository;config.runtimeRoot=path.join(trial,'runtime');config.gameDir=path.join(trial,'game');config.readyTimeoutMs=240000;
 config.launch.command=path.resolve(host,template.launch.command);config.launch.args=privateFlightLaunchArgs(template.launch.args,host,init);if(!config.launch.args.includes('--offline'))config.launch.args.push('--offline');config.launch.args.push('-Dnet.minecraftforge.gradle.check.certs=false');
 config.launch.env={JAVA_HOME:javaHome,KNEEKURA_DEBUG_MOD_PROFILE:'TANK_CORE',KNEEKURA_DEBUG_COMPLETE_FEINTS:'1',KNEEKURA_DEBUG_COMPLETE_FEINTS_NONCE:nonce,KNEEKURA_DEBUG_COMPLETE_FEINTS_WORLD:world};
 await write(path.join(trial,'config.json'),config);assert.deepEqual(await inventory(original),originalRows);
 await launchDebugRun(config,lab);current=await readCurrent(config,lab);assert(current.live&&current.runtimeOwnership?.owned);report.runDir=current.runDir;
 for(const name of ['owner-envelope.json','owner-status.json'])assert(!(await fs.stat(path.join(current.runDir,'control',name)).catch(()=>null)),'ScopedOwner authority present');
 const output=path.join(current.runDir,'evidence/derived/complete-feints');await fs.mkdir(output,{recursive:true});const dispatch=Date.now();
 const request={nonce,world,subjectUuid:fixture.subjectUuid,playerUuid:fixture.playerUuid,maxTicks:240,maxWallMs:20000,dispatchEpochMs:dispatch};await write(path.join(output,'request.json'),request);
 let result;do{result=await read(path.join(output,'result.json')).catch(error=>{if(error.code==='ENOENT')return null;throw error;});if(result)break;await new Promise(resolve=>setTimeout(resolve,250));}while(Date.now()<dispatch+25000);
 assert(result,'Bounded native result missing');assert.equal(result.nonce,nonce);report.native=result;
 report.analysis=analyzeCompleteFeints(result,request);await write(path.join(output,'analysis.json'),report.analysis);
 report.artifacts=await Promise.all((await fs.readdir(output)).filter(n=>/^(request\.json|cases\.jsonl|rows\.jsonl|joins\.jsonl|clientRows\.jsonl|result\.json|analysis\.json)$/.test(n)).sort().map(async name=>({name,sha256:sha(await fs.readFile(path.join(output,name)))})));
 assert.equal(report.analysis.status,'PASS',report.analysis.failures.join(','));report.nativeChecks='PASS';
}catch(error){report.failures.push(error.message);if(error.stderr)await fs.writeFile(path.join(trial,'tool-error.log'),error.stderr,{flag:'wx'});console.log('FAIL '+error.message.split('\n')[0]);}
finally{
 if(config){current??=await readCurrent(config,lab).catch(()=>null);if(current?.runDir){try{const finish=await finalizeNativeTrial({current,stop:()=>stopCurrent(config,lab),read:()=>readCurrent(config,lab),finalize:finalizeEvidenceRun});report.cleanup=finish.stopped.cleanup;report.shutdown=finish.shutdown;report.finalization=finish.finalization;assert.equal(finish.status,'PASS',finish.reason);}catch(error){report.failures.push('Closure: '+error.message);}}}
 if(originalRows.length){try{assert.deepEqual(await inventory(original),originalRows);report.originalFilesVerified=originalRows.length;}catch(error){report.failures.push('Original: '+error.message);}}
 try{assert.equal(git(repository,'rev-parse','HEAD'),report.sourceRevision);assert.equal(git(lab,'rev-parse','HEAD'),report.labRevision);assert.equal(git(repository,'status','--porcelain'),'');assert.equal(git(lab,'status','--porcelain'),'');}catch(error){report.failures.push('Pinned source: '+error.message);}
 report.status=report.failures.length||report.nativeChecks!=='PASS'?'FAIL':'PASS';await write(path.join(trial,'report.json'),report);console.log(report.status+' '+trial);process.exitCode=report.status==='PASS'?0:1;
}
