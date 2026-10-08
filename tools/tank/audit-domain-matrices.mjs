import assert from 'node:assert/strict';
import {readFile,writeFile,realpath,lstat} from 'node:fs/promises';
import {execFileSync} from 'node:child_process';
import path from 'node:path';
import {createHash} from 'node:crypto';

// Requested finite derived analysis. Never edits an input receipt, log or world.
assert.equal(process.argv.length,5,'Usage: audit-domain-matrices.mjs <prefix-result> <suffix-result> <new-output>');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const readJson=async file=>{assert((await lstat(file)).isFile()&&!(await lstat(file)).isSymbolicLink());const bytes=await readFile(file);assert(bytes.length<=4*1024*1024);return {value:JSON.parse(bytes),sha256:sha(bytes)};};
const batches=[];
for(const input of process.argv.slice(2,4)){
 const file=await realpath(input),root=path.dirname(file),repo=path.resolve(root,'../../..');
 assert(path.basename(root).startsWith('domain-matrix-'));
 const data=await readJson(file),matrix=data.value;
 assert.equal(matrix.scope,'DIRECT_NATIVE_DOMAIN_INTERRUPTION_MATRIX');assert.equal(matrix.sourceUnchanged,true);
 const tree=execFileSync('git',['rev-parse',matrix.sourceRevision+':src'],{cwd:repo,encoding:'utf8',windowsHide:true}).trim();
 assert(/^[a-f0-9]{40}$/.test(tree));batches.push({file,root,repo,matrix,inputSha256:data.sha256,productionTree:tree});
}
const [prefix,suffix]=batches;
assert.equal(prefix.matrix.verdict,'FAIL');assert.equal(prefix.matrix.results.length,12);
assert.equal(prefix.matrix.failure,'DOMAIN_MATRIX_CHILD_FAILED_13-RESTORE_PREPARE_BEGIN-crash');
assert.equal(suffix.matrix.verdict,'PASS');assert.equal(suffix.matrix.results.length,24);
assert.equal(prefix.productionTree,suffix.productionTree,'Production sources differ');
assert.deepEqual(suffix.matrix.cases,prefix.matrix.cases.slice(12));
const results=[],corrections=[];
for(const batch of batches)for(const item of batch.matrix.results){
 const pair=[];
 for(const scenario of ['crash','recover']){
  const receiptFile=await realpath(item[scenario+'Receipt']),world=path.dirname(receiptFile);
  assert.equal(path.dirname(world),path.join(batch.repo,'build','tank'));assert(path.basename(world).startsWith('domain-probe-'));
  const data=await readJson(receiptFile),r=data.value;assert.equal(data.sha256,item[scenario+'Sha256']);
  assert.equal(r.sourceRevision,batch.matrix.sourceRevision);assert.equal(r.sourceUnchanged,true);assert.equal(r.scenario,scenario);assert.equal(r.faultCase,item.faultCase);
  assert.equal(r.timedOut,false);assert.equal(r.overflow,false);assert.equal(r.stopError,'');assert.deepEqual(r.remainingOwnedJavaPids,[]);
  assert.equal(r.verdict,scenario==='crash'?'EXPECTED_CRASH':'PASS');assert.equal(r.nativeExit73,scenario==='crash');
  if(scenario==='recover')assert.equal(r.exitCode,0);
  const log=await readFile(path.join(world,scenario+'.log'));assert.equal(sha(log),r.logSha256);
  const native=await readJson(path.join(world,scenario+'-native.json'));assert.deepEqual(native.value,r.native);
  assert.equal(r.native.sourceRevision,r.sourceRevision);assert.equal(r.native.nonce,r.nonce);assert.equal(r.native.scenario,scenario);
  pair.push({receiptFile,receiptSha256:data.sha256,logSha256:r.logSha256,nativeSha256:native.sha256,receipt:r});
 }
 const [crash,recover]=pair;assert.equal(path.dirname(crash.receiptFile),path.dirname(recover.receiptFile));assert.equal(crash.receipt.nonce,recover.receipt.nonce);
 assert.notEqual(crash.receipt.native.processStartedAt,recover.receipt.native.processStartedAt);
 assert.equal(crash.receipt.native.worldFlushBeforeHalt,!item.faultCase.startsWith('DURABILITY_'));
 const boundary=recover.receipt.native.boundary;assert(['RECOVERY_VERIFIED','RECOVERY_DURABILITY_CONFLICT_RETAINED'].includes(boundary));
 const retained=boundary==='RECOVERY_DURABILITY_CONFLICT_RETAINED';
 if(retained!==item.retainedUnresolved)corrections.push({faultCase:item.faultCase,originalSummary:item.retainedUnresolved,derivedRetained:retained,authority:'IMMUTABLE_NATIVE_BOUNDARY',recoverReceiptSha256:recover.receiptSha256});
 results.push({faultCase:item.faultCase,sourceRevision:batch.matrix.sourceRevision,retainedUnresolved:retained,worldFlushBeforeHalt:crash.receipt.native.worldFlushBeforeHalt,pair:pair.map(({receipt,...proof})=>proof)});
}
assert.deepEqual(results.map(r=>r.faultCase),prefix.matrix.cases);assert.equal(new Set(results.map(r=>r.faultCase)).size,36);
const failedInput=JSON.parse((await readFile(path.join(prefix.root,'13-RESTORE_PREPARE_BEGIN-crash.log'))).toString().split(/\r?\n/).find(line=>{try{return !!JSON.parse(line).receipt;}catch{return false;}}));
const failed=await readJson(await realpath(failedInput.receipt));assert.equal(failed.value.verdict,'FAIL');assert.equal(failed.value.native,null);
assert(failed.value.sourceUnchanged&&!failed.value.timedOut&&!failed.value.overflow&&!failed.value.stopError&&failed.value.remainingOwnedJavaPids.length===0);
const failedLog=await readFile(path.join(path.dirname(failedInput.receipt),'crash.log'));assert.equal(sha(failedLog),failed.value.logSha256);assert(failedLog.includes(Buffer.from('SSLHandshakeException')));
const result={scope:'HASH_VERIFIED_COMPOSITE_DIRECT_DOMAIN_INTERRUPTION_COVERAGE',verdict:'SCOPED_COVERAGE_PASS',inputBatches:batches.map(b=>({file:b.file,sha256:b.inputSha256,originalVerdict:b.matrix.verdict,sourceRevision:b.matrix.sourceRevision,productionTree:b.productionTree})),failedStartup:{file:failedInput.receipt,sha256:failed.sha256,logSha256:failed.value.logSha256,originalVerdict:'FAIL',nativeStarted:false},cases:results.length,closedAcceptedProcesses:results.length*2,restoredCases:results.filter(r=>!r.retainedUnresolved).length,retainedCases:results.filter(r=>r.retainedUnresolved).length,metadataCorrections:corrections,results,limitations:['Original failed matrix and startup receipt remain FAIL and unchanged.','Different tool/metadata revisions share an identical production src tree; sidecar metadata move is recorded separately in history.','Retained conflict is safety coverage, not restored-world acceptance.','Two durability cases omit extra flush; remaining cases deliberately flush. No power-loss or natural boss claim.']};
const output=path.resolve(process.argv[4]);assert(!batches.some(b=>output.startsWith(b.root+path.sep)),'Derived output must be outside retained batches');
await writeFile(output,JSON.stringify(result,null,2)+'\n',{flag:'wx'});
console.log(JSON.stringify({output,sha256:sha(await readFile(output)),cases:result.cases,restored:result.restoredCases,retained:result.retainedCases,correctedSummaries:corrections.length}));
