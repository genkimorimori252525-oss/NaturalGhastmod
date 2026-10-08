export const reanchorScope='EXPLICIT_NEW_BOSS_TRANSIT_NOT_NATURAL_SELECTION_OR_HUMAN_READABILITY';
const axes=['x','y','z'],finite=v=>v&&axes.every(k=>Number.isFinite(v[k])),distance=(a,b)=>Math.hypot(...axes.map(k=>a[k]-b[k])),length=v=>distance(v,{x:0,y:0,z:0}),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const goalVelocity=v=>Object.fromEntries(axes.map(k=>[k,Math.abs(v[k])<.003?0:v[k]]));
export function analyzeReanchor(result,request){
 const failures=[],check=(v,label)=>{if(!v)failures.push(label);},actor=result?.actors,rows=result?.rows??[];
 check(result?.status==='PASS'&&result.scope===reanchorScope&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=200&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 if(!actor||!rows.length||!finite(actor.position)||!finite(actor.observedTarget)||!Number.isFinite(actor.targetTop))return {status:'FAIL',scope:reanchorScope,failures:[...failures,'ACTOR_GEOMETRY']};
 const old=actor.region,joins=result.joins??[],clients=result.clientRows??[],cleanup=result.cleanup??[];
 check(actor.privateControlledGoal===true&&actor.width===4&&actor.height===4&&actor.uuid!==actor.targetUuid,'PRIVATE_BODY');
 check(finite(old?.center)&&same(old?.radii,{x:20,y:8,z:20})&&old.generation===1,'BROAD_REGION');
 check(joins.length===1&&joins[0].uuid===actor.uuid&&joins[0].id===actor.id&&joins[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&clients.length===4&&clients.every(x=>x.uuid===actor.uuid&&x.id===actor.id),'CLIENT_ADMISSION');
 const start=actor.position,target=actor.observedTarget,dx=target.x-start.x,dz=target.z-start.z,horizontal=Math.hypot(dx,dz),height=Math.max(start.y,actor.targetTop+6+.75),overhead={x:start.x,y:height,z:start.z},destination={x:target.x+dx/horizontal*8,y:height,z:target.z+dz/horizontal*8},routeLength=height-start.y+horizontal+8;
 check(horizontal>=8&&horizontal<=16&&actor.targetTop>=target.y&&routeLength<=24&&Math.abs(actor.routeLength-routeLength)<1e-6,'FROZEN_ROUTE_BOUNDS');
 const phases=[];let commits=0,crossed=false;
 for(const [i,r] of rows.entries()){
  if(!phases.length||phases.at(-1).phase!==r.phase)phases.push({phase:r.phase,count:0});phases.at(-1).count++;
  check(r.uuid===actor.uuid&&r.id===actor.id&&r.loaded===true&&r.registered===true&&r.blocked===false&&r.visible===true&&r.targetHealth>0,'NATIVE_IDENTITY');
  check(r.goalTick===i+1&&r.goalTick<=160&&(i===0||r.tick===rows[i-1].tick+1),'CLOCK');
  if(![r.before,r.position,r.velocityBefore,r.velocity,r.overhead,r.destination].every(finite)){check(false,'FINITE_MOTION');continue;}
  check(distance(r.overhead,overhead)<1e-6&&distance(r.destination,destination)<1e-6&&same(r.regionBefore,old),'FROZEN_GEOMETRY');
  check(axes.every(k=>Math.abs(r.position[k]-r.before[k]-r.velocity[k])<1e-6)&&length(r.velocity)<=.320001&&distance(r.velocity,r.velocityBefore)<=Math.hypot(.11,.045)+1e-6&&(i===0||distance(r.before,rows[i-1].position)<1e-6&&distance(r.velocityBefore,goalVelocity(rows[i-1].velocity))<1e-6),'NATIVE_MOTION');
  if(r.phase==='TELL'||r.phase==='CLIMB')check(Math.hypot(r.position.x-start.x,r.position.z-start.z)<1e-6,'CLIMB_BEFORE_CROSS');
  if(r.phase==='CROSS')check(r.before.y>=actor.targetTop+6-1e-6,'ABOVE_OBSERVED_BODY');
  const plane=(r.position.x-target.x)*dx/horizontal+(r.position.z-target.z)*dz/horizontal;
  if(plane>0){crossed=true;check(r.position.y>=actor.targetTop+6-1e-6,'ABOVE_TARGET_PLANE');}
  if(r.commit){
   commits++;check(i===rows.length-1&&r.phase==='DONE'&&length(r.velocityBefore)<=.04+1e-6&&distance(r.before,destination)<=.7+1e-6,'BRAKED_COMMIT');
   check(r.region?.generation===old.generation+1&&r.region.reason==='TACTICAL_RELOCATION'&&same(r.region.radii,old.radii)&&distance(r.region.center,r.before)<1e-6,'NEW_RETAINED_REGION');
  }else check(same(r.region,old),'NO_PREMATURE_RELOCATION');
 }
 check(same(phases.map(x=>x.phase),['TELL','CLIMB','CROSS','BRAKE','DONE'])&&phases[0]?.count===12&&phases.at(-1)?.count===1&&rows.length<=160&&commits===1&&crossed,'FINITE_TRANSIT_SEQUENCE');
 const owned=[actor.uuid,actor.targetUuid];check(cleanup.length===2&&new Set(cleanup.map(x=>x.uuid)).size===2&&cleanup.every(x=>owned.includes(x.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(x.status)),'OWNED_CLEANUP');
 return {status:failures.length?'FAIL':'PASS',scope:reanchorScope,failures:[...new Set(failures)],serverRows:rows.length,clientRows:clients.length,commits,phases,routeLength};
}
