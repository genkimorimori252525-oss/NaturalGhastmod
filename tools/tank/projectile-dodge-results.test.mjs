import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeProjectileDodge,projectileDodgeScope} from './projectile-dodge-results.mjs';
const axes=['x','y','z'],v=(x=0,y=0,z=0)=>({x,y,z}),add=(a,b)=>Object.fromEntries(axes.map(k=>[k,a[k]+b[k]])),sub=(a,b)=>Object.fromEntries(axes.map(k=>[k,a[k]-b[k]])),scale=(a,s)=>Object.fromEntries(axes.map(k=>[k,a[k]*s])),len=a=>Math.hypot(...axes.map(k=>a[k])),limited=(a,n)=>len(a)>n?scale(a,n/len(a)):a;
function step(actual,direction,speed){if(len(actual)<1e-9)return limited(scale(direction,speed),.11);const forward=scale(actual,1/len(actual)),correction=sub(scale(direction,speed),actual),longitudinal=axes.reduce((n,k)=>n+correction[k]*forward[k],0),lateral=limited(sub(correction,scale(forward,longitudinal)),.045);return limited(add(add(actual,scale(forward,Math.max(-.035,Math.min(.11,longitudinal)))),lateral),Math.max(.65,len(actual)));}
function fixture(withDeclinedHit=false){
 const result={scope:projectileDodgeScope,status:'PASS',nonce:'native',canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,samples:91,errors:[],actors:[],births:[],rows:[],impacts:[],joins:[],clientRows:[],cleanup:[]};
 for(let c=0;c<3;c++){
  const actor={case:c,uuid:'boss'+c,id:10+c,targetUuid:'cow'+c,privateControlledGoal:true,decisionRngOverridden:false,width:4,height:4,initialHealth:100,position:v(12+c*14,230,34),region:{center:v(12+c*14,230,49.34958866235469),radii:v(20,8,20),generation:1,reason:'ACQUIRED'}};
  result.actors.push(actor);result.joins.push({case:c,uuid:actor.uuid,id:actor.id,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'});for(let i=0;i<4;i++)result.clientRows.push({case:c,uuid:actor.uuid,id:actor.id,position:actor.position});
  let position=actor.position,velocity=v(),health=100,arrow=null,normalTicks=0,phase='IDLE',startTick=-1,impactTicks=-1,waypoint=null,attempted=false;
  for(let i=0;i<90;i++){
   const tick=100+i,before=position,velocityBefore=Object.fromEntries(axes.map(k=>[k,Math.abs(velocity[k])<.003?0:velocity[k]]));let mode='HOLD',primitive='HOLD',direction=v(),speed=0;
   if(i===26){attempted=true;if(c===0){phase='EVADE';startTick=tick;waypoint=add(before,v(-4,0,0));impactTicks=(arrow.center.z-before.z-2.5)/.995;}}
   if(phase==='EVADE'&&(tick-startTick>=24||len(sub(waypoint,before))<=.7))phase='RECOVER';
   if(phase==='EVADE'){mode='MOVE';primitive='STRAFE';const error=sub(waypoint,before);direction=scale(error,1/len(error));speed=Math.min(.42,Math.sqrt(2*.035*Math.max(0,len(error)-.4)));}
   else if(attempted){normalTicks++;mode='MOVE';primitive='DRIFT';direction=scale(v(.02,0,.08),1/len(v(.02,0,.08)));speed=.08;}
   velocity=mode==='MOVE'?step(velocityBefore,direction,speed):len(velocityBefore)<=.035?v():scale(velocityBefore,(len(velocityBefore)-.035)/len(velocityBefore));position=add(before,velocity);
   const travelVelocity=velocity,travelHealth=health,impacts=[];
   if(withDeclinedHit&&c===1&&i===45){velocity=add(velocity,v(0,0,.3));health=98;const hit={case:c,tick,afterTravelTick:tick,bossUuid:actor.uuid,directUuid:arrow.uuid,ownerUuid:actor.targetUuid,accepted:true,amount:2,beforePosition:position,afterPosition:position,beforeVelocity:travelVelocity,afterVelocity:velocity,beforeHealth:100,afterHealth:98};impacts.push(hit);result.impacts.push(hit);}
   const arrowBefore=arrow&&structuredClone(arrow);
   if(arrow){arrow.position=add(arrow.position,arrow.velocity);arrow.center=add(arrow.position,v(0,.25,0));arrow.velocity=scale(arrow.velocity,.9900000095367432);}
   result.rows.push({case:c,uuid:actor.uuid,id:actor.id,tick,goalTick:i+1,loaded:true,registered:true,visible:true,blocked:false,health,before,position,velocityBefore,velocity,travelPosition:position,travelVelocity,travelHealth,travelTick:tick,impacts,direction,speed,mode,primitive,phase,startTick,impactTicks,waypoint,region:actor.region,attempted,normalTicks,arrowBefore,arrowAfter:arrow&&structuredClone(arrow),arrowVisible:arrowBefore!==null});
   if(i===23){arrow={case:c,uuid:'arrow'+c,id:20+c,ownerUuid:actor.targetUuid,position:add(position,v(0,2,16)),center:add(position,v(0,2.25,16)),halfSize:v(.25,.25,.25),velocity:v(0,0,-1),alive:true,loaded:true,noGravity:true,declaredFixtureNoGravity:true};result.births.push(structuredClone(arrow));}
  }
  for(const uuid of [actor.uuid,actor.targetUuid,'arrow'+c])result.cleanup.push({uuid,status:'DISCARDED_PRIVATE_ACTOR'});
 }
 return result;
}
const request={nonce:'native'},analyze=r=>analyzeProjectileDodge(r,request);
test('finite native-like unforced admission and declared declines',()=>{const r=analyze(fixture());assert.equal(r.status,'PASS',r.failures.join(','));assert.equal(r.cases.filter(x=>x.admitted).length,1);});
test('declined arrow native hit is separate from post-travel controller velocity',()=>{const r=analyze(fixture(true));assert.equal(r.status,'PASS',r.failures.join(','));});
function rejected(name,mutate,label){test(name,()=>{const r=fixture();mutate(r);assert.ok(analyze(r).failures.includes(label),JSON.stringify(analyze(r)));});}
rejected('nonce authority',r=>r.nonce='different','AUTHORITY');
rejected('canonical override cannot pass',r=>r.canonicalOverrides=true,'AUTHORITY');
rejected('forced RNG cannot pass',r=>r.actors[0].decisionRngOverridden=true,'ACTOR_CONTRACT');
rejected('wrong body scale',r=>r.actors[0].width=1,'ACTOR_CONTRACT');
rejected('region resize',r=>r.rows.find(x=>x.phase==='EVADE').region={...r.actors[0].region,radii:v(5,5,5)},'RETAINED_REGION');
rejected('declared fixture gravity boundary',r=>r.births[0].noGravity=false,'DECLARED_ARROW');
rejected('wrong projectile owner',r=>r.births[0].ownerUuid='boss0','DECLARED_ARROW');
rejected('missing client identity',r=>r.joins.pop(),'CLIENT_ADMISSION');
rejected('wrong renderer',r=>r.joins[0].renderer='unknown','CLIENT_ADMISSION');
rejected('short motion window',r=>r.rows=r.rows.filter(x=>x.case!==0||x.goalTick<80),'FINITE_CASE');
rejected('physics teleport',r=>r.rows.find(x=>x.phase==='EVADE').position=v(999,999,999),'CONTROLLER_NATIVE_MOTION');
rejected('direct velocity snap',r=>r.rows.find(x=>x.phase==='EVADE').velocity=v(.65,0,0),'CONTROLLER_NATIVE_MOTION');
rejected('native collision blocked',r=>r.rows.find(x=>x.phase==='EVADE').blocked=true,'NATIVE_IDENTITY_CLOCK');
rejected('no prior visibility',r=>r.rows.find(x=>x.case===0&&x.tick===124).arrowVisible=false,'TWO_VISIBLE_OBSERVATIONS');
rejected('swapped observed projectile',r=>r.rows.find(x=>x.case===0&&x.tick===124).arrowBefore.uuid='different','TWO_VISIBLE_OBSERVATIONS');
rejected('missing closing displacement',r=>r.rows.find(x=>x.case===0&&x.tick===124).arrowBefore.center=r.rows.find(x=>x.phase==='EVADE').arrowBefore.center,'OBSERVED_FULL_BODY_THREAT');
rejected('false prediction',r=>r.rows.find(x=>x.phase==='EVADE').impactTicks=99,'OBSERVED_FULL_BODY_THREAT');
rejected('off-region route',r=>r.rows.find(x=>x.phase==='EVADE').waypoint=v(100,230,34),'FINITE_FROZEN_ROUTE');
rejected('retargeted lateral point',r=>r.rows.filter(x=>x.phase==='EVADE')[2].waypoint=v(13,230,35),'ONE_LOCKED_EVASION');
rejected('attempt memory lost',r=>r.rows.find(x=>x.case===0&&x.tick===150).attempted=false,'ATTEMPT_MEMORY');
rejected('recovery omitted',r=>r.rows.filter(x=>x.case===0&&x.phase==='RECOVER').forEach(x=>x.normalTicks=0),'ORDINARY_RECOVERY');
rejected('stopped handoff',r=>r.rows.find(x=>x.case===0&&x.phase==='RECOVER').velocity=v(),'ORDINARY_RECOVERY');
rejected('admitted actor hit',r=>r.rows.find(x=>x.case===0&&x.phase==='RECOVER').health=98,'NATIVE_LANE_AVOIDANCE');
rejected('no actual arrow passage',r=>r.rows.filter(x=>x.case===0).forEach(x=>{if(x.arrowAfter)x.arrowAfter.alive=false;}),'NATIVE_LANE_AVOIDANCE');
rejected('missing owned cleanup',r=>r.cleanup.pop(),'OWNED_CLEANUP');
rejected('foreign actor cleanup',r=>r.cleanup[0].uuid='canonical','OWNED_CLEANUP');
test('unrecorded END impulse cannot become physics tolerance',()=>{const r=fixture();r.rows.find(x=>x.phase==='RECOVER').velocity=add(r.rows.find(x=>x.phase==='RECOVER').velocity,v(0,0,.3));assert.ok(analyze(r).failures.includes('POST_TRAVEL_NATIVE_IMPULSE'));});
test('impact source must be the actual owned arrow',()=>{const r=fixture(true);r.impacts[0].directUuid='foreign';assert.ok(analyze(r).failures.includes('POST_TRAVEL_NATIVE_IMPULSE'));});
test('impulse cannot predate boss native movement',()=>{const r=fixture(true);r.impacts[0].afterTravelTick--;assert.ok(analyze(r).failures.includes('POST_TRAVEL_NATIVE_IMPULSE'));});
test('global native impacts must match ordered motion rows',()=>{const r=fixture(true);r.impacts=[];assert.ok(analyze(r).failures.includes('ORDERED_IMPACT_PUBLICATION'));});
