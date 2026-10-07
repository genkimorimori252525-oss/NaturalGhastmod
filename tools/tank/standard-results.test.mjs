import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeStandard} from './standard-results.mjs';
function trace(){return Array.from({length:180},(_,tick)=>{
 const phase=tick<60?'IDLE':tick<90?'CHARGE':tick<110?'RECOVER':'IDLE';
 return {tick,attackPhase:phase,attackTicks:phase==='CHARGE'?tick-60:phase==='RECOVER'?tick-90:0,
 attackFire:tick===90,firingFace:phase!=='IDLE',firedCount:tick<90?0:1,aimX:1,aimY:0,aimZ:0,
 lineOfSight:true,targetType:'minecraft:player',playerMode:'survival',playerHealth:20,mobGriefing:false,regionGeneration:1,inCombatRegion:true,
 x:tick/20,y:230+Math.sin(tick/20),z:10,health:100,tacticalAction:'DRIFT',speed:.1,collisionFree:true,
 regionX:9,regionY:230,regionZ:30,radiusX:20,radiusY:8,radiusZ:20,
 projectiles:tick>=90&&tick<100?[{uuid:'shot',type:'soutou_ghast:standard_fireball',width:1,height:1,pickRadius:1.5,origin:'boss',savedOrigin:'boss',owner:'boss',speed:.5,returns:0,playerDeflected:false}]:[]};
});}
const clients=[{uuid:'shot',type:'soutou_ghast:standard_fireball',renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer',powerMagnitude:.1,speed:.5}];
test('scoped native charge/fire/recover and actual client projectile',()=>{
 const result=analyzeStandard(trace(),clients,'boss');assert.equal(result.status,'PASS');assert.equal(result.realMelee,'NOT_RUN');assert.equal(result.fullRallyAcceptance,'NOT_RUN');
});
test('bounded initial region entry is separate from settled confinement',()=>{
 const rows=trace();for(let i=0;i<20;i++)rows[i].inCombatRegion=false;
 assert.equal(analyzeStandard(rows,clients,'boss').status,'PASS');
 rows[120].inCombatRegion=false;assert.equal(analyzeStandard(rows,clients,'boss').status,'FAIL');
});
test('no fabricated shot, hidden target, wrong timing, steering or health alteration',()=>{
 for(const mutate of [rows=>rows.map(r=>({...r,projectiles:[]})),rows=>rows.map(r=>({...r,playerMode:'creative'})),
 rows=>rows.map(r=>({...r,lineOfSight:false})),rows=>rows.map(r=>({...r,firingFace:false})),
 rows=>rows.map(r=>({...r,aimX:r.tick})),rows=>rows.map(r=>({...r,health:10})),
 rows=>rows.map(r=>({...r,regionGeneration:r.tick})),rows=>rows.map(r=>({...r,x:0,y:230,z:10,speed:0}))])assert.equal(analyzeStandard(mutate(trace()),clients,'boss').status,'FAIL');
 assert.equal(analyzeStandard(trace(),[],'boss').status,'FAIL');
});
