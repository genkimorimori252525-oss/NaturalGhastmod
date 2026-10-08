import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeChargeResponse,chargeResponseScope} from './charge-response-results.mjs';
function fixture(){
 const region={center:{x:3,y:230,z:26},radii:{x:20,y:8,z:20},generation:1},actors={uuid:'boss',id:2,position:{x:3,y:230,z:26},targetUuid:'cow',privateControlledGoals:true,targetMovement:'NEW_COW_NATIVE_NAVIGATION_1.4_TWO_WAYPOINTS_PER_CHARGE',width:4,height:4,region};
 const r={scope:chargeResponseScope,nonce:'n',status:'PASS',samples:351,canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,errors:[],actors,rows:[],shots:[],joins:[{uuid:'boss',id:2,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'}],clientRows:Array.from({length:4},(_,tick)=>({uuid:'boss',id:2,tick})),cleanup:[{uuid:'boss',status:'DISCARDED_PRIVATE_ACTOR'},{uuid:'cow',status:'DISCARDED_PRIVATE_ACTOR'}]};
 let z=26,history=0;
 for(let tick=1;tick<=350;tick++){
  const start=[60,190,320].find(x=>tick>=x&&tick<=x+49),age=start===undefined?-1:tick-start,phase=age<0?'IDLE':age<30?'CHARGE':'RECOVER',chargeTick=age<30?Math.max(0,age):age-30,fire=age===30,side=start===190?1:-1;
  const moving=age>=1&&age<=4||age>=20&&age<=38;if(moving)z+=side*.2;
  if(fire){history++;const uuid='shot'+history;r.shots.push({uuid,origin:'boss',owner:'boss',tick});r.cleanup.push({uuid,status:'ALREADY_REMOVED'});}
  const qualified=phase==='CHARGE'&&age>=3,onset=phase==='CHARGE'&&age>=1?1:-1,position={x:50,y:224,z};
  const row={uuid:'boss',id:2,tick,goalTick:tick,position:{...actors.position},phase,chargeTick,fire,emitted:fire,fired:history,visible:true,history,repeated:history>=2,stationary:phase==='IDLE'?10:0,onset,qualified,choice:'STANDARD',lastFired:'STANDARD',rejection:'NONE',navigationCalls:Math.min(6,2*(history+1)),observedTarget:position,targetNow:{...position},observedVelocity:{x:0,y:0,z:moving?side*.2:0},region,registered:true,loaded:true,targetHealth:20};
  if(phase==='CHARGE'&&chargeTick===19)Object.assign(row,{variation:.5,range:50,context:'SEMI_OPEN',stationaryTicks:0,withoutHistory:'STANDARD',withHistory:'STANDARD'});
  r.rows.push(row);
 }
 return {result:r,request:{nonce:'n'}};
}
test('accept declared controlled-target native observation and three completed emissions',()=>{const f=fixture();assert.deepEqual(analyzeChargeResponse(f.result,f.request).failures,[]);});
for(const [name,mutate] of [
 ['nonce',f=>f.result.nonce='bad'],['scope',f=>f.result.scope='HUMAN_DODGE'],['canonical override',f=>f.result.canonicalOverrides=true],['fixture boss',f=>f.result.fixtureSubjectAbsent=false],['fake private movement label',f=>f.result.actors.targetMovement='TELEPORT'],['missing client',f=>f.result.joins=[]],['wrong renderer',f=>f.result.joins[0].renderer='OTHER'],['unloaded',f=>f.result.rows[0].loaded=false],['clock',f=>f.result.rows[1].goalTick=7],['hidden position',f=>f.result.rows[0].visible=false],['changed region',f=>f.result.rows[0].region={...f.result.rows[0].region,generation:2}],['teleported boss',f=>f.result.rows[0].position={x:9,y:230,z:26}],['fabricated first response',f=>f.result.rows[0].history=1],['qualified too early',f=>f.result.rows.find(x=>x.phase==='CHARGE'&&x.chargeTick===1).qualified=true],['history without emission',f=>f.result.rows.find(x=>x.emitted).emitted=false],['missing shot',f=>f.result.shots.pop()],['wrong shot owner',f=>f.result.shots[0].owner='Player'],['missing cleanup',f=>f.result.cleanup.pop()],['unsafe cleanup',f=>f.result.cleanup[0].status='FAIL'],['false repeated timing',f=>f.result.rows.at(-1).repeated=false],['live disagreement',f=>f.result.rows[0].targetNow={x:0,y:224,z:26}],['unbounded navigation',f=>f.result.rows[0].navigationCalls=99]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeChargeResponse(f.result,f.request).status,'FAIL');});
