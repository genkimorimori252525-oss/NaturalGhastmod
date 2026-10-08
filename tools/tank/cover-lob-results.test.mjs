import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeCoverLob,coverLobScope} from './cover-lob-results.mjs';

const fixture=()=>{
 const region={center:{x:12,y:230,z:23},radii:{x:20,y:8,z:20},generation:1},eye={x:42,y:225.3,z:26},landing={x:42,y:223.75,z:26},start={x:15,y:232,z:26};
 const wall=Array.from({length:136},(_,i)=>[27,224+Math.floor(i/17),26+i%17]);
 const snapshot={subject:'target',tick:100,eye,landing,context:'OPEN_AIR'},recipe={kind:'LOB',side:0,lobHeight:8,endpoint:landing};
 const path={kind:'LOB',points:Array.from({length:25},(_,i)=>({x:start.x+(landing.x-start.x)*i/24,y:start.y+(landing.y-start.y)*i/24+8*Math.sin(Math.PI*i/24),z:26}))};
 const actor={uuid:'boss',id:4,position:region.center,targetUuid:'target',width:4,height:4,privateControlledGoals:true,targetMovement:'NEW_COW_NATIVE_NAVIGATION_AFTER_ACTUAL_LOS_LOSS',bossMovement:'HOLD62_THEN_CONTROLLER_DRIFT_Z_0.14_UNTIL_LOS_LOSS',region,wall};
 const rows=Array.from({length:67},(_,i)=>{
  const tick=100+i,visible=i===0,charge=i-1;
  const r={uuid:'boss',id:4,tick,epoch:1000+i*50,goalTick:i+1,loaded:true,registered:true,visible,phase:i===0||i<=30?'CHARGE':i<=50?'RECOVER':'IDLE',chargeTick:i===0?20:i<=30?charge:i<=50?i-31:0,fire:i===31,emitted:i===31,fired:i>=31?1:0,coverActive:i>=1&&i<=30,age:i<=40?i:-1,choice:i?'LOB':'STANDARD',lastFired:'LOB',context:'OPEN_AIR',region,position:{x:12,y:230,z:26.5},targetNow:{x:42,y:224,z:26+Math.min(i*.1,10)},targetHealth:10,navigationCalls:i?1:0,attackNanos:10000,storedRayLoaded:true,storedRay:i?'BLOCK':'MISS',storedRayBlock:[27,228,26],clear:true,segments:24,terminal:[[42,223,26]],profileCueTick:i>=20?120:-1,projectiles:[]};
  r.region=structuredClone(region);
  if(i<=40)r.snapshot={...snapshot,tick:visible?tick:snapshot.tick};
  if(i>=1)r.recipe=structuredClone(recipe);
  if(i>=31&&i<=54)r.projectiles=[{uuid:'shot',kind:'LOB',tick,index:i-31,position:structuredClone(path.points[i-31]),phase:i<42?'ASCEND':'DESCEND'}];
  return r;
 });
 const result={scope:coverLobScope,status:'PASS',nonce:'nonce',canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,samples:68,errors:[],actors:actor,rows,shots:[{uuid:'shot',tick:131,origin:'boss',owner:'boss',position:start,path}],joins:[{uuid:'boss',id:4,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'}],clientRows:Array.from({length:4},()=>({uuid:'boss',id:4})),cleanup:['boss','target','shot'].map(uuid=>({uuid,status:'DISCARDED_PRIVATE_ACTOR'})),wallCleanup:wall.map(position=>({position,status:'RESTORED_NEW_PRIVATE_AIR'}))};
 return {result,request:{nonce:'nonce'}};
};
test('finite native-cover contract',()=>{const f=fixture();assert.equal(analyzeCoverLob(f.result,f.request).status,'PASS');});
for(const [name,mutate] of Object.entries({
 noActualLoss:f=>f.rows.forEach(r=>r.visible=true),hiddenRetarget:f=>f.rows[5].recipe.endpoint.z++,staleAdmission:f=>f.rows.slice(1,41).forEach(r=>r.snapshot.tick-=11),inheritedCharge:f=>f.rows[1].chargeTick=20,
 missingCue:f=>f.rows[20].profileCueTick=-1,earlyLaunch:f=>f.shots[0].tick--,hiddenStandard:f=>f.shots[0].path.kind='BURST',unloadedRay:f=>f.rows[4].storedRayLoaded=false,
 noTerrainCover:f=>f.rows[4].storedRay='MISS',roofBlock:f=>f.rows[4].clear=false,changedRecipe:f=>f.rows[15].recipe.lobHeight=6,changedContext:f=>f.rows[15].context='SEMI_OPEN',
 noTargetMovement:f=>f.rows.forEach(r=>r.targetNow.z=26),changedRegion:f=>f.rows[15].region.generation++,missingPhysicalPath:f=>f.rows.forEach(r=>r.projectiles=[]),wrongPhysicalPath:f=>f.rows[36].projectiles[0].position.x++,
 ownerWrong:f=>f.shots[0].owner='other',badWallCleanup:f=>f.wallCleanup.pop(),hiddenSecondShot:f=>f.shots.push({...f.shots[0],uuid:'second'}),wrongClock:f=>f.rows[15].tick++,errors:f=>f.errors.push('observer'),canonicalOverride:f=>f.canonicalOverrides=true
}))test(name,()=>{const f=fixture();mutate(f.result);assert.equal(analyzeCoverLob(f.result,f.request).status,'FAIL');});
