import {spawn,execFileSync} from 'node:child_process';
import {mkdir,mkdtemp,readFile,writeFile,realpath} from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {randomUUID,createHash} from 'node:crypto';

// One finite, sequential batch. Each runner owns its process watchdog and fresh world.
const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const git=(...args)=>execFileSync('git',args,{cwd:repo,encoding:'utf8',windowsHide:true}).trim();
if(process.argv.length!==2||process.platform!=='win32'||!process.env.JAVA_HOME)throw new Error('DOMAIN_MATRIX_WINDOWS_JAVA17_NO_ARGUMENTS');
if(git('status','--porcelain'))throw new Error('DOMAIN_MATRIX_COMMITTED_SOURCE_REQUIRED');
const revision=git('rev-parse','HEAD'),nonce=randomUUID();
const parent=path.join(repo,'build','tank');await mkdir(parent,{recursive:true});
const root=await realpath(await mkdtemp(path.join(parent,'domain-matrix-')));
await writeFile(path.join(root,'matrix-owner.json'),JSON.stringify({nonce,sourceRevision:revision,scope:'DIRECT_NATIVE_DOMAIN_INTERRUPTION_MATRIX'})+'\n',{flag:'wx'});
const boundaries=['PREPARE_BEGIN','PREPARE_FORCED','BEFORE_RENAME','AFTER_RENAME','AFTER_READBACK','PUBLISHED_BEFORE_RETURN'];
const cases=[...['INIT','PLACE','RESTORE','TERMINAL'].flatMap(prefix=>boundaries.map(boundary=>`${prefix}_${boundary}`)),'BEFORE_FIRST_MUTATION','AFTER_FIRST_MUTATION','PARTIAL_PLACEMENT','BEFORE_FIRST_RESTORE','AFTER_FIRST_RESTORE','PARTIAL_RESTORATION','UNPUBLISHED_INITIAL_THIRD_PARTY','UNPUBLISHED_PENDING_THIRD_PARTY'];
console.log(JSON.stringify({matrixRoot:root,sourceRevision:revision,cases:cases.length,scope:'DIRECT_NATIVE_DOMAIN_INTERRUPTION_MATRIX'}));
const results=[];let failure='';
const run=async (args,label)=>{
 const child=spawn(process.execPath,[path.join(repo,'tools/tank/run-domain-probe.mjs'),...args],{cwd:repo,env:process.env,windowsHide:true,stdio:['ignore','pipe','pipe']});
 const chunks=[];let bytes=0,overflow=false;
 const accept=chunk=>{bytes+=chunk.length;if(bytes<=256*1024)chunks.push(chunk);else overflow=true;};
 child.stdout.on('data',accept);child.stderr.on('data',accept);
 const exitCode=await new Promise((resolve,reject)=>{child.once('error',reject);child.once('close',resolve);});
 const output=Buffer.concat(chunks);await writeFile(path.join(root,label+'.log'),output,{flag:'wx'});
 if(overflow||exitCode!==0)throw new Error('DOMAIN_MATRIX_CHILD_FAILED_'+label);
 const lines=output.toString('utf8').split(/\r?\n/).flatMap(line=>{try{return [JSON.parse(line)];}catch{return [];}});
 const receiptPath=lines.findLast(item=>item.receipt)?.receipt;
 if(!receiptPath)throw new Error('DOMAIN_MATRIX_MISSING_RECEIPT');
 const actualPath=await realpath(receiptPath),worldRoot=path.dirname(actualPath);
 if(path.dirname(worldRoot)!==await realpath(parent)||!path.basename(worldRoot).startsWith('domain-probe-'))throw new Error('DOMAIN_MATRIX_RECEIPT_PATH');
 const receiptBytes=await readFile(actualPath),receipt=JSON.parse(receiptBytes);
 if(receipt.sourceRevision!==revision||!receipt.sourceUnchanged||receipt.timedOut||receipt.overflow||receipt.stopError||receipt.remainingOwnedJavaPids?.length!==0)throw new Error('DOMAIN_MATRIX_RECEIPT_NOT_CLOSED');
 return {worldRoot,receipt,path:actualPath,sha256:createHash('sha256').update(receiptBytes).digest('hex')};
};
for(let i=0;i<cases.length;i++){
 const faultCase=cases[i],label=String(i+1).padStart(2,'0')+'-'+faultCase;
 try{
  const crashed=await run(['--crash',faultCase],label+'-crash');
  if(crashed.receipt.verdict!=='EXPECTED_CRASH'||crashed.receipt.faultCase!==faultCase||crashed.receipt.nativeExit73!==true)throw new Error('DOMAIN_MATRIX_EXPECTED_ABRUPT_EXIT');
  const recovered=await run(['--recover',crashed.worldRoot],label+'-recover');
  if(recovered.receipt.verdict!=='PASS'||recovered.receipt.faultCase!==faultCase||recovered.receipt.nonce!==crashed.receipt.nonce||recovered.worldRoot!==crashed.worldRoot)throw new Error('DOMAIN_MATRIX_RECOVERY_NOT_PASSED');
  const result={faultCase,verdict:'PASS',crashReceipt:crashed.path,crashSha256:crashed.sha256,recoverReceipt:recovered.path,recoverSha256:recovered.sha256};results.push(result);
  await writeFile(path.join(root,label+'.json'),JSON.stringify(result,null,2)+'\n',{flag:'wx'});
  console.log(JSON.stringify({completed:results.length,total:cases.length,faultCase,verdict:'PASS'}));
 }catch(error){failure=error.message;console.log(JSON.stringify({completed:results.length,total:cases.length,faultCase,verdict:'FAIL',failure}));break;}
}
const sourceUnchanged=git('rev-parse','HEAD')===revision&&!git('status','--porcelain');
const pass=!failure&&sourceUnchanged&&results.length===cases.length;
const receipt=path.join(root,'matrix-result.json');
await writeFile(receipt,JSON.stringify({scope:'DIRECT_NATIVE_DOMAIN_INTERRUPTION_MATRIX',verdict:pass?'PASS':'FAIL',nonce,sourceRevision:revision,sourceUnchanged,cases,results,failure,limitations:['Deliberately flushed private world; no power-loss claim.','Direct restoration reliability; no natural boss combat acceptance.']},null,2)+'\n',{flag:'wx'});
console.log(JSON.stringify({receipt,verdict:pass?'PASS':'FAIL',completed:results.length,total:cases.length}));
if(!pass)process.exitCode=1;
