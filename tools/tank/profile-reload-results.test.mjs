import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeProfileReload} from './profile-reload-results.mjs';

const scope='EXPLICIT_INSTANTIATED_PROFILE_RELOAD_NOT_DURABLE_RESTART_OR_MELEE';
function fixture(){
 const names=['DEFLECTED_CURVE','BURST','CURVE','LOB','MISSING_PROFILE','INVALID_INDEX','SHIFTED_POSITION','INCONSISTENT_DEFLECTION'];
 const cases=names.map((name,i)=>({name,uuid:'projectile-'+i,kind:name==='BURST'?'BURST':name==='LOB'?'LOB':'CURVE',negative:i>=4,normalized:i===0,points:Array.from({length:11},(_,j)=>[3+j,236,5]),savedIndex:i===0?0:9,checks:{stateEqual:true,ownerResolved:true,preflightEqual:true,provenanceEqual:true,unregisteredBeforeLoad:true}}));
 const rows=cases.flatMap((c,i)=>{
  const r=(event,index,x)=>({case:c.name,uuid:c.uuid,event,index,x,y:236,z:5,tick:100+i*5+(event==='LOAD'?1:event==='NATIVE'?2:event==='REMOVED'?3:0),normalized:c.normalized,alive:event!=='REMOVED',ownerUuid:'owner',positionError:0});
  if(c.negative){const end=r('REMOVED',1,4);end.tick--;return [r('LOAD',1,4),end];}
  return [r('SAVE',c.savedIndex,c.normalized?4:12),r('LOAD',c.savedIndex,c.normalized?4:12),r('NATIVE',c.normalized?0:10,c.normalized?5:13),r('REMOVED',c.normalized?0:10,c.normalized?5:13)];
 });
 const request={nonce:'nonce',maxTicks:180,maxWallMs:15000};
 const result={scope,nonce:'nonce',status:'PASS',failures:[],cases,rows:rows.length,startGameTime:100,endGameTime:138,connectedPlayer:true};
 return {rows,result,request};
}
test('separate scope and eight instantiated cases',()=>{const f=fixture();assert.equal(analyzeProfileReload(f.rows,f.result,f.request).status,'PASS');});
test('reject missing native continuation or unregister proof',()=>{for(const mutate of [f=>{f.rows=f.rows.filter(r=>r.event!=='NATIVE');f.result.rows=f.rows.length;},f=>f.result.cases[1].checks.unregisteredBeforeLoad=false]){const f=fixture();mutate(f);assert.equal(analyzeProfileReload(f.rows,f.result,f.request).status,'FAIL');}});
test('reject changed path position, clock or normalization',()=>{for(const key of ['x','index','normalized']){const f=fixture();const r=f.rows.find(r=>r.case==='CURVE'&&r.event==='NATIVE');r[key]=key==='normalized'?true:99;assert.equal(analyzeProfileReload(f.rows,f.result,f.request).status,'FAIL');}});
test('reject delayed malformed discard, missing case or wrong request',()=>{for(const mutate of [f=>f.rows.find(r=>r.case==='MISSING_PROFILE'&&r.event==='REMOVED').tick+=2,f=>f.result.cases.pop(),f=>f.request.maxTicks=400]){const f=fixture();mutate(f);assert.equal(analyzeProfileReload(f.rows,f.result,f.request).status,'FAIL');}});
