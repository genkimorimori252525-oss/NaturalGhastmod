import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeGround} from './ground-results.mjs';
const boss='boss',player='player';
function fixture(){
 const rows=Array.from({length:180},(_,i)=>({tick:100+i,uuid:boss,x:9.5+i*.08,y:i<10?225-i*.1:224,z:20,speed:.1,collisionFree:true,width:4,height:4,health:100,
  playerUuid:player,playerMode:'survival',playerHealth:i<150?20:17,mobGriefing:false,regionGeneration:1,regionX:9.5,regionY:230,regionZ:30.85,radiusX:20,radiusY:8,radiusZ:20,
  groundPhase:i<10?'LANDING':'GROUNDED',grounded:i>=10,groundSupport:i>=10,groundActive:true,overheadActive:false,rallyFace:false,firedCount:0,
  groundFire:[40,74,108,142].includes(i),groundFiredCount:[40,74,108,142].filter(t=>t<=i).length,
  groundAttackPhase:'IDLE',groundAttackTicks:0,projectileCoverage:'LOADED_ROOM_SELECTED_TYPE',projectiles:[],targetUuid:player,lineOfSight:true,firingFace:false,groundLastProjectileUuid:null}));
 const clients=[],events=[];
 for(const t of [40,74,108,142]){
  for(let i=t-8;i<t;i++){rows[i].groundAttackPhase='CHARGE';rows[i].groundAttackTicks=i-t+8;rows[i].firingFace=true;}
  for(let i=t;i<t+6;i++){rows[i].groundAttackPhase='RECOVER';rows[i].groundAttackTicks=i-t;rows[i].firingFace=true;}
  for(let i=t;i<180;i++)rows[i].groundLastProjectileUuid='shot'+t;
  for(let n=0;n<3;n++)rows[t+n].projectiles=[{uuid:'shot'+t,type:'soutou_ghast:ground_fireball',x:15+n*1.9,y:226,z:20,speed:1.9,width:1,height:1,pickRadius:1.5,origin:boss,owner:boss,savedOrigin:boss,playerDeflected:false}];
  clients.push({uuid:'shot'+t,type:'soutou_ghast:ground_fireball',renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer',powerMagnitude:0,speed:1.9});
  events.push({event:'RELEASE_JOIN_LISTENER',tick:100+t,order:events.length+1,uuid:'shot'+t,canceledAtListener:false,targetUuid:player,targetType:'minecraft:player',targetAlive:true,targetHealth:20,lineOfSight:true});
 }
 return {rows,clients,events,damage:[],clientGround:{subjectUuid:boss,grounded:true,width:4,height:4,cameraUuid:player},end:{samples:180,tick:279,reason:'MAX_TICKS'},request:{maxTicks:180,maxWallMs:15000,reserveMs:15000}};
}
test('actual supported Ground motion and fixed-speed native single flight are scoped independently of damage',()=>{
 const result=analyzeGround(fixture(),boss,player);assert.equal(result.status,'PASS',result.failures.join(','));
 assert.equal(result.damagePipeline,'NOT_ACCEPTED');assert.equal(result.nativeDamageListeners,0);
});
test('support, body, teleport, overlap, retained region and camera evidence cannot be inferred from labels',()=>{
 for(const mutate of [f=>f.rows[50].groundSupport=false,f=>f.rows[50].width=1,f=>f.rows[50].x+=3,
  f=>f.rows[50].overheadActive=true,f=>f.rows[50].regionX+=1,f=>f.clientGround.cameraUuid='foreign']){
  const f=fixture();mutate(f);assert.equal(analyzeGround(f,boss,player).status,'FAIL');
 }
});
test('missing client, changed speed/path, wrong provenance or insufficient finite coverage fail',()=>{
 for(const mutate of [f=>f.clients=[],f=>f.rows[41].projectiles[0].x+=.8,f=>f.rows[40].projectiles[0].origin='foreign',
  f=>f.rows=f.rows.slice(0,40),f=>f.end.reason='WALL_CLOCK_DEADLINE',f=>f.request.maxWallMs=60000]){
  const f=fixture();mutate(f);assert.equal(analyzeGround(f,boss,player).status,'FAIL');
 }
});
test('cadence requires observed target,8charge/6face/20quiet and actual successful-shot counter/UUID',()=>{
 for(const mutate of [f=>f.rows[40].targetUuid='foreign',f=>f.rows[40].lineOfSight=false,
  f=>f.rows[36].groundAttackPhase='IDLE',f=>f.rows[42].firingFace=false,
  f=>f.rows[52].groundAttackPhase='CHARGE',f=>f.rows[40].groundFiredCount=0,
  f=>f.rows[40].groundLastProjectileUuid='invented',f=>f.events[0].targetAlive=false]){
  const f=fixture();mutate(f);assert.equal(analyzeGround(f,boss,player).status,'FAIL');
 }
});
