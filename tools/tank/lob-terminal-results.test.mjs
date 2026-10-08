import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeLobTerminal,lobTerminalScope} from './lob-terminal-results.mjs';
const coverFixture=()=>{
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
 const result={scope:lobTerminalScope,status:'PASS',nonce:'nonce',canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,samples:68,errors:[],actors:actor,rows,shots:[{uuid:'shot',tick:131,origin:'boss',owner:'boss',position:start,path}],joins:[{uuid:'boss',id:4,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'}],clientRows:Array.from({length:4},()=>({uuid:'boss',id:4})),cleanup:['boss','target','shot'].map(uuid=>({uuid,status:'DISCARDED_PRIVATE_ACTOR'})),wallCleanup:wall.map(position=>({position,status:'RESTORED_NEW_PRIVATE_AIR'}))};
 return {result,request:{nonce:'nonce'}};
};

const fixture=()=>{
 const f=coverFixture(),r=f.result;r.scope=lobTerminalScope;const shot=r.shots[0],raw=structuredClone(shot.path),points=shot.path.points,from=points.at(-2),end=points.at(-1);
 const split=Object.fromEntries(['x','y','z'].map(k=>[k,from[k]+(end[k]-from[k])*.65]));points.splice(points.length-1,0,split);shot.rawPath=raw;shot.rawTerminalCells=6;shot.declaredTerminal=[{position:[41,223,26],state:'Block{minecraft:black_concrete}'}];
 for(const [i,row] of r.rows.entries()){
  row.segments=25;row.projectiles=[];row.endShots=[];
  if(i>=31&&i<=55)row.projectiles=[{uuid:'shot',index:i-31,kind:'LOB',phase:i<42?'ASCEND':'DESCEND',position:structuredClone(points[i-31])}];
  if(i>=31)row.endShots=[{uuid:'shot',tick:row.tick,removed:i>=56,registered:i<56,position:structuredClone(i<56?points[i-31]:split)}];
 }
 const t=(224-split.y)/(end.y-split.y),hit=Object.fromEntries(['x','y','z'].map(k=>[k,split[k]+(end[k]-split[k])*t]));
 r.impacts=[{uuid:'shot',tick:156,index:25,canceled:false,kind:'BLOCK',block:[41,223,26],state:'Block{minecraft:black_concrete}',position:split,hit}];
 r.explosions=[{uuid:'shot',tick:156,position:split,explosionPosition:split}];f.result=JSON.parse(JSON.stringify(r));return f;
};
test('native terminal with unchanged cover/path contract',()=>{const f=fixture();assert.equal(analyzeLobTerminal(f.result,f.request).status,'PASS');});
for(const [name,mutate] of Object.entries({
 noSplit:r=>r.shots[0].path=structuredClone(r.shots[0].rawPath),changedOldArc:r=>r.shots[0].rawPath.points[3].x++,retarget:r=>r.shots[0].path.points.at(-1).x++,tooManyPoints:r=>r.shots[0].path.points.push(...Array(100).fill({x:42,y:224,z:26})),
 splitAfterContact:r=>r.shots[0].path.points.at(-2).y=223.99,noRealRejection:r=>r.shots[0].rawTerminalCells=4,missingImpact:r=>r.impacts=[],canceledImpact:r=>r.impacts[0].canceled=true,earlyImpact:r=>r.impacts[0].index--,
 wrongTerminal:r=>r.impacts[0].block[0]++,changedCollider:r=>r.impacts[0].state='Block{minecraft:dirt}',wrongImpactRay:r=>r.impacts[0].hit.x+=.2,wrongNativeBefore:r=>r.impacts[0].position.x+=.2,
 missingExplosion:r=>r.explosions=[],wrongExplosionOwner:r=>r.explosions[0].uuid='other',wrongExplosionOrigin:r=>r.explosions[0].explosionPosition.x+=.2,stillRegistered:r=>r.rows[56].endShots[0].registered=true,
 notRemoved:r=>r.rows[56].endShots[0].removed=false,wrongNativeClock:r=>r.impacts[0].tick++,staleHiddenAim:r=>r.rows[5].recipe.endpoint.z++,additionalImpact:r=>r.impacts.push({...r.impacts[0]})
}))test(name,()=>{const f=fixture();mutate(f.result);assert.equal(analyzeLobTerminal(f.result,f.request).status,'FAIL');});
