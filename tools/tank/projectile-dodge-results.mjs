export const projectileDodgeScope='NEW_NATIVE_ARROW_DODGE_ADAPTER_NOT_PRODUCTION_GOAL_OR_PLAYER_INPUT';
const axes=['x','y','z'],finite=v=>v&&axes.every(k=>Number.isFinite(v[k])),distance=(a,b)=>Math.hypot(...axes.map(k=>a[k]-b[k])),zero={x:0,y:0,z:0},length=v=>distance(v,zero),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const subtract=(a,b)=>Object.fromEntries(axes.map(k=>[k,a[k]-b[k]])),add=(a,b)=>Object.fromEntries(axes.map(k=>[k,a[k]+b[k]])),scale=(a,s)=>Object.fromEntries(axes.map(k=>[k,a[k]*s])),limited=(v,max)=>length(v)>max?scale(v,max/length(v)):v,goalVelocity=v=>Object.fromEntries(axes.map(k=>[k,Math.abs(v[k])<.003?0:v[k]]));
const dot=(a,b)=>axes.reduce((s,k)=>s+a[k]*b[k],0);
function controller(v,r){
 const speed=length(v);if(r.mode!=='MOVE'||length(r.direction)<1e-9)return speed<=.035?zero:scale(v,(speed-.035)/speed);
 const desired=scale(r.direction,r.speed);if(speed<1e-9)return limited(desired,.11);
 const forward=scale(v,1/speed),correction=subtract(desired,v),longitudinal=dot(correction,forward),lateral=limited(subtract(correction,scale(forward,longitudinal)),.045);
 return limited(add(add(v,scale(forward,Math.max(-.035,Math.min(.11,longitudinal)))),lateral),Math.max(.65,speed));
}
function impact(offset,relative,half){
 if(dot(offset,relative)>=0)return NaN;let enter=0,exit=12,outside=false;
 for(const k of axes){const h=2.25+half[k];outside||=Math.abs(offset[k])>h;if(Math.abs(relative[k])<1e-9){if(Math.abs(offset[k])>h)return NaN;continue;}const a=(-h-offset[k])/relative[k],b=(h-offset[k])/relative[k];enter=Math.max(enter,Math.min(a,b));exit=Math.min(exit,Math.max(a,b));if(enter>exit)return NaN;}
 return outside&&enter>0&&enter<=12&&exit>=enter?enter:NaN;
}
export function analyzeProjectileDodge(result,request){
 const failures=[],check=(v,label)=>{if(!v)failures.push(label);},actors=result?.actors??[],rows=result?.rows??[],births=result?.births??[],joins=result?.joins??[],clients=result?.clientRows??[],cleanup=result?.cleanup??[];
 check(result?.status==='PASS'&&result.scope===projectileDodgeScope&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=160&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 check(actors.length===3&&births.length===3&&rows.length<=480&&new Set(actors.map(a=>a.uuid)).size===3&&new Set(births.map(a=>a.uuid)).size===3,'BOUNDED_ACTORS');
 const summary=[];
 for(const a of actors){
  const motion=rows.filter(r=>r.case===a.case),birth=births.find(b=>b.case===a.case),client=clients.filter(r=>r.case===a.case),join=joins.filter(r=>r.case===a.case);
  check(a.privateControlledGoal===true&&a.decisionRngOverridden===false&&a.width===4&&a.height===4&&a.initialHealth===100&&finite(a.position)&&finite(a.region?.center)&&same(a.region?.radii,{x:20,y:8,z:20})&&a.region.generation===1,'ACTOR_CONTRACT');
  check(birth&&birth.declaredFixtureNoGravity===true&&birth.noGravity===true&&birth.ownerUuid===a.targetUuid&&finite(birth.center)&&finite(birth.velocity)&&birth.uuid!==a.uuid&&birth.uuid!==a.targetUuid,'DECLARED_ARROW');
  check(join.length===1&&join[0].uuid===a.uuid&&join[0].id===a.id&&join[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&client.length>=4&&client.length<=24&&client.every(r=>r.uuid===a.uuid&&r.id===a.id&&finite(r.position)),'CLIENT_ADMISSION');
  check(motion.length>=90&&motion.length<=160,'FINITE_CASE');
  let start=-1,end=-1,waypoint,evades=0,attempted=false;const radius=p=>length({x:(p.x-a.region.center.x)/20,y:(p.y-a.region.center.y)/8,z:(p.z-a.region.center.z)/20});
  for(const [i,r] of motion.entries()){
   check(r.uuid===a.uuid&&r.id===a.id&&r.loaded===true&&r.registered===true&&r.visible===true&&r.blocked===false&&r.health>0&&r.health<=100&&r.goalTick===i+1&&(i===0||r.tick===motion[i-1].tick+1),'NATIVE_IDENTITY_CLOCK');
   if(![r.before,r.position,r.velocityBefore,r.velocity,r.travelPosition,r.travelVelocity,r.direction].every(finite)){check(false,'FINITE_MOTION');continue;}
   check(same(r.region,a.region),'RETAINED_REGION');
   check(r.travelTick===r.tick&&distance(subtract(r.travelPosition,r.before),r.travelVelocity)<1e-6&&distance(r.travelVelocity,controller(r.velocityBefore,r))<1e-6&&distance(r.position,r.travelPosition)<1e-6&&length(r.travelVelocity)<=.650001&&(i===0||distance(r.before,motion[i-1].position)<1e-6&&distance(r.velocityBefore,goalVelocity(motion[i-1].velocity))<1e-6),'CONTROLLER_NATIVE_MOTION');
   const hits=r.impacts??[];let postVelocity=r.travelVelocity,postHealth=r.travelHealth;
   check(Array.isArray(r.impacts)&&Number.isFinite(postHealth),'POST_TRAVEL_NATIVE_IMPULSE');
   for(const hit of hits){
    check(hit.case===a.case&&hit.tick===r.tick&&hit.afterTravelTick===r.tick&&hit.bossUuid===a.uuid&&hit.directUuid===birth?.uuid&&hit.ownerUuid===a.targetUuid&&[hit.beforePosition,hit.afterPosition,hit.beforeVelocity,hit.afterVelocity].every(finite)&&distance(hit.beforePosition,r.travelPosition)<1e-6&&distance(hit.afterPosition,r.travelPosition)<1e-6&&distance(hit.beforeVelocity,postVelocity)<1e-6&&hit.beforeHealth===postHealth&&Number.isFinite(hit.amount)&&hit.amount>0&&typeof hit.accepted==='boolean'&&hit.afterHealth>0&&hit.afterHealth<=hit.beforeHealth&&(hit.accepted||hit.afterHealth===hit.beforeHealth&&distance(hit.afterVelocity,hit.beforeVelocity)<1e-6),'POST_TRAVEL_NATIVE_IMPULSE');
    postVelocity=hit.afterVelocity;postHealth=hit.afterHealth;
   }
   check(distance(postVelocity,r.velocity)<1e-6&&postHealth===r.health,'POST_TRAVEL_NATIVE_IMPULSE');
   if(attempted)check(r.attempted===true,'ATTEMPT_MEMORY');attempted||=r.attempted===true;
   if(r.phase==='EVADE'){
    evades++;check(r.mode==='MOVE'&&r.primitive==='STRAFE'&&finite(r.waypoint)&&radius(r.waypoint)<=.9&&r.startTick<=r.tick&&r.tick-r.startTick<24,'FINITE_FROZEN_ROUTE');
    if(start<0){
     start=i;waypoint=r.waypoint;check(distance(waypoint,r.before)<=4.000001&&Math.abs(waypoint.y-r.before.y)<1e-6&&r.tick%2===0&&r.startTick===r.tick&&r.attempted===true,'LATERAL_ADMISSION');
     const previous=motion[i-2],arrow=r.arrowBefore;
     check(previous&&previous.tick===r.tick-2&&previous.arrowVisible===true&&r.arrowVisible===true&&arrow?.alive===true&&arrow?.loaded===true&&arrow?.uuid===birth?.uuid&&previous.arrowBefore?.uuid===arrow?.uuid&&arrow?.ownerUuid===a.targetUuid,'TWO_VISIBLE_OBSERVATIONS');
     if(previous&&finite(previous.arrowBefore?.center)&&finite(arrow?.center)&&finite(arrow?.halfSize)){
      const observed=scale(subtract(arrow.center,previous.arrowBefore.center),.5),relative=subtract(observed,r.velocityBefore),bodyCenter=add(r.before,{x:0,y:2,z:0}),predicted=impact(subtract(arrow.center,bodyCenter),relative,arrow.halfSize);
      check(length(observed)>=.05&&length(observed)<=4&&Number.isFinite(predicted)&&Math.abs(predicted-r.impactTicks)<1e-6,'OBSERVED_FULL_BODY_THREAT');
     }else check(false,'OBSERVED_FULL_BODY_THREAT');
    }else check(end<0&&same(r.waypoint,waypoint),'ONE_LOCKED_EVASION');
   }else if(start>=0&&end<0)end=i;
  }
  const admitted=start>=0;
  if(admitted){
   check(end>start&&evades<=24&&motion.slice(end).length>=12&&motion[end].phase==='RECOVER'&&motion[end].normalTicks>=1&&motion.at(-1).normalTicks>=12&&length(motion[end].velocity)>.001,'ORDINARY_RECOVERY');
   const passing=motion.filter(r=>r.arrowAfter?.alive===true&&finite(r.arrowAfter.center)&&Math.abs(r.arrowAfter.center.z-r.position.z)<=2.5);
   check(passing.length>0&&passing.every(r=>Math.abs(r.arrowAfter.center.x-r.position.x)>2.5)&&motion.some(r=>r.arrowAfter?.alive===true&&finite(r.arrowAfter.center)&&r.arrowAfter.center.z<r.position.z-2.5)&&motion.every(r=>r.health===a.initialHealth),'NATIVE_LANE_AVOIDANCE');
  }else check(attempted&&motion.every(r=>r.startTick===-1),'DECLINED_NOT_RELABELED_DODGE');
  summary.push({case:a.case,admitted,evades,normalTicks:motion.at(-1)?.normalTicks,health:motion.at(-1)?.health});
 }
 check(summary.some(r=>r.admitted),'AT_LEAST_ONE_UNFORCED_ADMISSION');
 check(same(rows.flatMap(r=>r.impacts??[]),result.impacts??[]),'ORDERED_IMPACT_PUBLICATION');
 const owned=[...actors.flatMap(a=>[a.uuid,a.targetUuid]),...births.map(b=>b.uuid)];check(new Set(owned).size===9&&cleanup.length===9&&new Set(cleanup.map(r=>r.uuid)).size===9&&cleanup.every(r=>owned.includes(r.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(r.status)),'OWNED_CLEANUP');
 return {status:failures.length?'FAIL':'PASS',scope:projectileDodgeScope,failures:[...new Set(failures)],serverRows:rows.length,clientRows:clients.length,cases:summary};
}
