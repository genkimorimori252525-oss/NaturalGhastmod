import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeEntityRestart,entityRestartScope} from './entity-restart-results.mjs';
function fixture(){
 const names=['BOSS','BURST','CURVE','LOB','BOMB','NORMALIZED_CURVE','STANDARD','GROUND'];
 const saved=names.map((name,i)=>({name,uuid:'entity-'+i,state:'actual-native-state-'+i,ownerUuid:name==='NORMALIZED_CURVE'?'player':'entity-0',kind:['BURST','CURVE','LOB','BOMB'].includes(name)?name:name==='NORMALIZED_CURVE'?'CURVE':'NONE',index:name==='NORMALIZED_CURVE'?0:9,normalized:name==='NORMALIZED_CURVE',age:9,points:Array.from({length:16},(_,j)=>[j,236,i*4]),x:9,y:236,z:i*4}));
 const loaded=saved.map(s=>({...s,joinTick:100,ownerResolved:true,bossSafeReset:s.name==='BOSS'}));
 const rows=[];for(const s of saved){
  if(s.name==='BOSS'){for(let j=1;j<=7;j++)rows.push({name:s.name,uuid:s.uuid,tick:100+j,x:9+j*.1,y:236,z:s.z,age:0,index:0,alive:true,ownerUuid:'',ownerResolved:true});continue;}
  const profile=['BURST','CURVE','LOB','BOMB'].includes(s.name);
  for(let j=1;j<=(profile?7:4);j++)rows.push({name:s.name,uuid:s.uuid,tick:100+j,x:profile?Math.min(15,9+j):9+j,y:236,z:s.z,age:profile?Math.min(15,9+j):9+j,index:profile?Math.min(15,9+j):s.index,alive:!profile||j<7,ownerUuid:s.ownerUuid,ownerResolved:true,ownerAvailable:true,normalized:s.normalized});
 }
 const closure={clean:true,exit:'VERIFIED_EXIT',evidence:'EVIDENCE_COMPLETE'};
 return {scope:entityRestartScope,nonce:'finite',status:'PASS',sourceUnchanged:true,originalFilesVerified:85,worldCopy:{source:[{file:'level.dat',sha256:'same'}],target:[{file:'level.dat',sha256:'same'}]},phases:[{native:{phase:'A',status:'STOPPED',nonce:'finite',pid:1,processStart:10,serverStopped:true,worldClosed:true,overrides:false,errors:[],saved},closure},{native:{phase:'B',status:'STOPPED',nonce:'finite',pid:2,processStart:20,serverStopped:true,worldClosed:true,canonicalPlayer:true,overrides:false,errors:[],loaded,rows},closure}]};
}
test('accepts disk-loaded new-process state and native continuation',()=>assert.equal(analyzeEntityRestart(fixture()).status,'PASS'));
const reject=(name,change)=>test(name,()=>{const f=fixture();change(f);assert.equal(analyzeEntityRestart(f).status,'FAIL');});
reject('rejects same JVM restart claims',f=>{f.phases[1].native.pid=1;f.phases[1].native.processStart=10;});
test('recognizes genuinely later process creation with an OS-reused PID',()=>{const f=fixture();f.phases[1].native.pid=1;assert.equal(analyzeEntityRestart(f).status,'PASS');});
reject('rejects mismatched copied world',f=>f.worldCopy.target[0].sha256='other');
reject('rejects missing normal stop completion',f=>f.phases[0].native.serverStopped=false);
reject('rejects missing native entity load',f=>f.phases[1].native.loaded.pop());
reject('rejects changed native saved data',f=>f.phases[1].native.loaded[2].state='other');
reject('rejects clock reset after restart',f=>f.phases[1].native.rows.find(r=>r.name==='BURST').index=1);
reject('rejects a stalled native profile clock',f=>{
 const trace=f.phases[1].native.rows.filter(r=>r.name==='BURST');
 for(const r of trace.slice(1)){r.index--;r.x--;r.alive=true;}trace.at(-1).age=16;
 f.phases[1].native.rows.push({...trace.at(-1),tick:108,index:15,age:17,alive:false});
});
reject('rejects lost normalization',f=>f.phases[1].native.loaded[5].normalized=false);
reject('rejects wrong resolved owner',f=>f.phases[1].native.rows.find(r=>r.name==='GROUND').ownerUuid='other');
reject('rejects changed path movement',f=>f.phases[1].native.rows.find(r=>r.name==='LOB').x+=1);
reject('rejects premature removal',f=>f.phases[1].native.rows.find(r=>r.name==='CURVE').alive=false);
reject('rejects dormant continuation',f=>f.phases[1].native.rows.filter(r=>r.name==='STANDARD').forEach(r=>{r.x=9;r.age=9;}));
reject('rejects unsafe resumed boss state',f=>f.phases[1].native.loaded[0].bossSafeReset=false);
reject('rejects missing process/evidence closure',f=>f.phases[1].closure={...f.phases[1].closure,evidence:'PARTIAL'});
reject('rejects fabricated Player resolution',f=>f.phases[1].native.canonicalPlayer=false);
reject('rejects source mismatch',f=>f.sourceUnchanged=false);
