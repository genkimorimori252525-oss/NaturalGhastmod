import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeProfiles} from './profile-results.mjs';
test('no fabricated profile acceptance from empty or ordinary-only observations',()=>{
 assert.equal(analyzeProfiles([],[],'boss',[]).status,'FAIL');
 assert.equal(analyzeProfiles([{tick:1,projectiles:[]}],[],'boss',[]).status,'FAIL');
});
function scenario(){
 const path=Array.from({length:31},(_,i)=>[9.5,232-8.25*i/30+10*Math.sin(Math.PI*i/30),20-16.5*i/30]);
 const shots=Array.from({length:180},(_,tick)=>{
  const phase=tick<60?'IDLE':tick<90?'CHARGE':tick<110?'RECOVER':'IDLE',index=tick-90;
  let projectiles=[];
  if(index>=0&&index<30){
   const point=path[index],motion=index===0?path[1].map((v,j)=>v-path[0][j]):path[index].map((v,j)=>v-path[index-1][j]);
   projectiles=[{uuid:'shot',type:'soutou_ghast:committed_fireball',width:1,height:1,pickRadius:1.5,origin:'boss',savedOrigin:'boss',owner:'boss',speed:Math.hypot(...motion),returns:0,playerDeflected:false,kind:'LOB',phase:motion[1]>0?'ASCEND':'DESCEND',index,normalized:false,x:point[0],y:point[1],z:point[2]}];
  }
  return {tick,attackPhase:phase,attackTicks:phase==='CHARGE'?tick-60:phase==='RECOVER'?tick-90:0,attackFire:tick===90,firingFace:phase!=='IDLE',firedCount:tick<90?0:1,aimX:1,aimY:0,aimZ:0,lineOfSight:true,targetType:'minecraft:player',playerMode:'survival',playerHealth:20,mobGriefing:false,regionGeneration:1,inCombatRegion:true,x:tick/20,y:230,z:10,health:100,tacticalAction:'DRIFT',speed:.1,collisionFree:true,regionX:9,regionY:230,regionZ:30,radiusX:20,radiusY:8,radiusZ:20,projectiles};
 });
 return {rows:shots,clients:[{uuid:'shot',kind:'LOB',index:0,type:'soutou_ghast:committed_fireball',renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer',powerMagnitude:0,speed:1}],paths:[{uuid:'shot',kind:'LOB',path,preflightScope:'PRE_LAUNCH_LOADED_SWEPT_BODY',preflightGameTime:90,preflightSegments:30,terminal:[{x:9,y:223,z:3,state:'minecraft:black_concrete'}]}]};
}
test('actual finite lob positions and both phases give only lob-scoped coverage',()=>{
 const s=scenario(),result=analyzeProfiles(s.rows,s.clients,'boss',s.paths);assert.equal(result.status,'PASS');assert.deepEqual(result.unexercised,['BURST','CURVE']);
});
test('labels without path, movement, phase, clock or preflight cannot pass',()=>{
 for(const mutate of [s=>s.rows.forEach(r=>r.projectiles.forEach(p=>p.y=232)),s=>s.rows.forEach(r=>r.projectiles.forEach(p=>p.phase='ASCEND')),
  s=>s.rows.forEach(r=>r.projectiles.forEach(p=>p.index=0)),s=>s.paths[0].preflightScope='UNKNOWN',s=>s.paths[0].terminal=[],s=>s.clients[0].kind='BURST']){
  const s=scenario();mutate(s);assert.equal(analyzeProfiles(s.rows,s.clients,'boss',s.paths).status,'FAIL');
 }
});
