import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeOverhead} from './major-results.mjs';

function scenario(){
 const phases=[...Array(30).fill('IDLE'),...Array(60).fill('WITHDRAW'),...Array(60).fill('RETURN'),...Array(30).fill('ALIGN'),...Array(61).fill('BOMBING'),...Array(60).fill('RECOVER'),...Array(599).fill('IDLE')];
 const paths=[192,216,240].map((tick,n)=>({uuid:`bomb${n}`,kind:'BOMB',path:Array.from({length:11},(_,i)=>[9.5,233.75-i,3.5]),preflightGameTime:tick,preflightSegments:10,preflightScope:'PRE_LAUNCH_LOADED_SWEPT_BODY',terminal:[{x:9,y:223,z:3,state:'minecraft:black_concrete'}]}));
 const rows=phases.map((phase,tick)=>{
  const x=phase==='WITHDRAW'?9.5+16*(tick-30)/59:phase==='RETURN'?25.5-16*(tick-90)/59:9.5;
  const y=phase==='WITHDRAW'?230+4*(tick-30)/59:phase==='RETURN'?234+2*(tick-90)/59:['ALIGN','BOMBING'].includes(phase)?236:phase==='RECOVER'?236-6*(tick-241)/59:230;
  return {tick,majorPhase:phase,majorActive:phase!=='IDLE',majorSequences:tick<30?0:1,majorReason:'NONE',majorPitch:['ALIGN','BOMBING'].includes(phase)?90:0,majorReleaseRequested:paths.some(p=>p.preflightGameTime===tick),bombCount:paths.filter(p=>p.preflightGameTime<=tick).length,x,y,z:3.5,speed:.3,collisionFree:true,health:100,playerMode:'survival',playerHealth:20,playerUuid:'player',playerX:9.5,playerY:224,playerZ:3.5,playerPitch:-90,mobGriefing:false,lineOfSight:true,targetUuid:'player',targetType:'minecraft:player',regionGeneration:1,regionX:9.5,regionY:230,regionZ:3.5,radiusX:20,radiusY:8,radiusZ:20,inCombatRegion:true,attackFire:false,rallyFace:false,tacticalAction:'DRIFT',windowStage:'COMBAT',projectiles:paths.filter(p=>tick>=p.preflightGameTime&&tick<p.preflightGameTime+9).map(p=>{const index=tick-p.preflightGameTime+1;return {uuid:p.uuid,kind:'BOMB',index,normalized:false,x:p.path[index][0],y:p.path[index][1],z:p.path[index][2],speed:1,width:1,height:1,pickRadius:1.5,origin:'boss',savedOrigin:'boss',owner:'boss',returns:0,playerDeflected:false,type:'soutou_ghast:committed_fireball'};})};
 });
 const clients=paths.map(p=>({uuid:p.uuid,kind:'BOMB',index:1,type:'soutou_ghast:committed_fireball',renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer',powerMagnitude:0,speed:1}));
 const impacts=paths.map(p=>({uuid:p.uuid,tick:p.preflightGameTime+9,producer:'NATIVE_PROJECTILE_IMPACT_EVENT_LOWEST_PRE_DAMAGE',hitType:'BLOCK',normalized:false,canceledAtListener:false,resultAtListener:'DEFAULT'}));
 const pose={tick:180,subjectUuid:'boss',renderPitch:90,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer',cameraUuid:'player',cameraPitch:-90};
 const end={samples:rows.length,tick:rows.at(-1).tick,reason:'MAX_TICKS',deathSample:-1};
 const request={maxTicks:900,maxWallMs:50000,postDeathMaxTicks:200,wallDeadlineEpochMs:51000,reserveMs:15000};
 const events=paths.map((p,i)=>({event:'RELEASE_JOIN_LISTENER',tick:p.preflightGameTime,order:i+1,uuid:p.uuid,targetUuid:'player',targetType:'minecraft:player',targetAlive:true,targetHealth:20,lineOfSight:true,targetX:9.5,targetY:224,targetZ:3.5,bossX:9.5,bossY:236,bossZ:3.5,majorPitch:90,canceledAtListener:false}));
 return {rows,clients,paths,impacts,events,pose,end,request,downwardFramePresent:true};
}
const run=s=>analyzeOverhead(s,'boss','player');
test('actual finite major movement, downward pose and three native bomb paths are narrowly accepted',()=>{
 const result=run(scenario());assert.equal(result.status,'PASS',result.failures.join(','));assert.equal(result.bombs.length,3);assert.equal(result.fullThreeBombSequence,true);assert.equal(result.impactOutcome,'PRE_DAMAGE_LISTENER_ONLY');
});
test('labels without physical motion, pose, paths or impact coverage never pass',()=>{
 for(const mutate of [s=>s.rows.forEach(r=>r.x=9.5),s=>s.pose=null,s=>s.downwardFramePresent=false,s=>s.paths=[],s=>s.impacts=[],s=>s.rows[180].regionX++,s=>s.rows[181].speed=.7,s=>s.events[0].lineOfSight=false,s=>s.rows[200].projectiles[0].x++,s=>s.clients[0].powerMagnitude=.1,s=>s.paths[0].terminal=[],s=>s.events[0].majorPitch=0,s=>s.events=[]]){
  const s=scenario();mutate(s);assert.equal(run(s).status,'FAIL');
 }
});
test('target death is accepted only with no later release and bounded successful retained-region recovery',()=>{
 const s=scenario();s.paths=s.paths.slice(0,1);s.clients=s.clients.slice(0,1);s.impacts=s.impacts.slice(0,1);
 s.rows=s.rows.slice(0,250);s.rows.forEach((r,i)=>{r.projectiles=r.projectiles.filter(p=>p.uuid==='bomb0');r.bombCount=i>=192?1:0;r.majorReleaseRequested=i===192;if(i>=205){r.y=236-6*(i-205)/44;r.playerHealth=0;r.windowStage='POST_DEATH_RECOVERY';r.majorPhase=i<249?'RECOVER':'IDLE';r.majorActive=i<249;r.majorReason='TARGET_UNOBSERVED';}});
 s.end={samples:250,tick:249,reason:'PLAYER_DEATH_RECOVERY_COMPLETE',deathSample:205};
 s.events=s.events.slice(0,1);s.events.push({event:'PLAYER_DEATH_LISTENER',tick:205,order:2,uuid:'player',canceledAtListener:false});
 const result=run(s);assert.equal(result.status,'PASS',result.failures.join(','));assert.equal(result.fullThreeBombSequence,false);
 s.rows[220].majorReleaseRequested=true;assert.equal(run(s).status,'FAIL');
});
test('END-tick death allows only an observed alive release ordered before the native death hook',()=>{
 const s=scenario();s.paths=s.paths.slice(0,1);s.clients=s.clients.slice(0,1);s.impacts=s.impacts.slice(0,1);s.events=s.events.slice(0,1);
 s.rows=s.rows.slice(0,240);s.rows.forEach((r,i)=>{r.projectiles=r.projectiles.filter(p=>p.uuid==='bomb0');r.bombCount=i>=192?1:0;r.majorReleaseRequested=i===192;if(i>=192){r.y=236-6*(i-192)/47;r.playerHealth=0;r.windowStage='POST_DEATH_RECOVERY';if(i>192){r.majorPhase=i<239?'RECOVER':'IDLE';r.majorActive=i<239;}r.majorReason='TARGET_UNOBSERVED';}});
 s.events.push({event:'PLAYER_DEATH_LISTENER',tick:192,order:2,uuid:'player',canceledAtListener:false});s.end={samples:240,tick:239,reason:'PLAYER_DEATH_RECOVERY_COMPLETE',deathSample:192};
 const result=run(s);assert.equal(result.status,'PASS',result.failures.join(','));
 s.events[0].targetAlive=false;assert.equal(run(s).status,'FAIL');s.events[0].targetAlive=true;s.events[0].order=3;assert.equal(run(s).status,'FAIL');
});
test('physical boss teleport cannot hide behind a capped speed field',()=>{
 const s=scenario();s.rows[100].x+=4;assert.equal(run(s).status,'FAIL');
});
