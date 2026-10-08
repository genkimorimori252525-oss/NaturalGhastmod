export const reanchorScope='EXPLICIT_NEW_BOSS_TRANSIT_NOT_NATURAL_SELECTION_OR_HUMAN_READABILITY';
const axes=['x','y','z'],finite=v=>v&&axes.every(k=>Number.isFinite(v[k])),distance=(a,b)=>Math.hypot(...axes.map(k=>a[k]-b[k])),length=v=>distance(v,{x:0,y:0,z:0}),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const goalVelocity=v=>Object.fromEntries(axes.map(k=>[k,Math.abs(v[k])<.003?0:v[k]]));
const angle=v=>({yaw:-Math.atan2(v.x,v.z)*180/Math.PI,pitch:-Math.atan2(v.y,Math.hypot(v.x,v.z))*180/Math.PI});
function delta(a,b){let d=(b-a)%360;if(d>=180)d-=360;if(d< -180)d+=360;return d;}
export function analyzeReanchor(result,request){
 const failures=[],check=(v,label)=>{if(!v)failures.push(label);},actor=result?.actors,rows=result?.rows??[];
 check(result?.status==='PASS'&&result.scope===reanchorScope&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=360&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 if(!actor||!rows.length||!finite(actor.position)||!finite(actor.observedTarget)||!Number.isFinite(actor.targetTop))return {status:'FAIL',scope:reanchorScope,failures:[...failures,'ACTOR_GEOMETRY']};
 const old=actor.region,joins=result.joins??[],clients=result.clientRows??[],cleanup=result.cleanup??[];
 check(actor.privateControlledGoal===true&&actor.width===4&&actor.height===4&&actor.uuid!==actor.targetUuid,'PRIVATE_BODY');
 check(finite(old?.center)&&same(old?.radii,{x:20,y:8,z:20})&&old.generation===1,'BROAD_REGION');
 check(joins.length===1&&joins[0].uuid===actor.uuid&&joins[0].id===actor.id&&joins[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&clients.length>=4&&clients.length<=400&&clients.every(x=>x.uuid===actor.uuid&&x.id===actor.id),'CLIENT_ADMISSION');
 const start=actor.position,target=actor.observedTarget,dx=target.x-start.x,dz=target.z-start.z,horizontal=Math.hypot(dx,dz),centerY=start.y>=actor.targetTop+4?start.y:actor.targetTop+6,height=Math.max(centerY+12,actor.targetTop+16+.75);
 const heading={x:dx/horizontal,y:0,z:dz/horizontal},overhead={x:start.x,y:height,z:start.z},beyond={x:target.x+heading.x*8,y:height,z:target.z+heading.z*8},center={x:beyond.x,y:centerY,z:beyond.z},destination={...center,y:centerY+6.8},routeLength=height-start.y+horizontal+8+height-destination.y;
 const budget=12+Math.max(Math.ceil((height-start.y)/.12)+5,48)+Math.ceil((horizontal+8)/.24)+5+48+Math.ceil((height-centerY-7.6)/.12)+5+12;
 const next={center,radii:{x:20,y:8,z:20},generation:old.generation+1,reason:'TACTICAL_RELOCATION'},radius=p=>Math.hypot((p.x-center.x)/20,(p.y-center.y)/8,(p.z-center.z)/20);
 check(horizontal>=8&&horizontal<=16&&actor.targetTop>=target.y&&routeLength<=48&&Math.abs(actor.routeLength-routeLength)<1e-6&&budget<=320&&actor.budget===budget,'FROZEN_ROUTE_BOUNDS');
 const phases=[];let commits=0,crossed=false,handoff=-1;
 for(const [i,r] of rows.entries()){
  if(!phases.length||phases.at(-1).phase!==r.phase)phases.push({phase:r.phase,count:0});phases.at(-1).count++;
  check(r.uuid===actor.uuid&&r.id===actor.id&&r.loaded===true&&r.registered===true&&r.blocked===false&&r.visible===true&&r.targetHealth>0,'NATIVE_IDENTITY');
  check(r.goalTick===i+1&&r.goalTick<=332&&(i===0||r.tick===rows[i-1].tick+1),'CLOCK');
  if(![r.before,r.position,r.velocityBefore,r.velocity,r.overhead,r.beyond,r.destination,r.candidateCenter,r.look,r.observedLook].every(finite)){check(false,'FINITE_MOTION');continue;}
  check(distance(r.overhead,overhead)<1e-6&&distance(r.beyond,beyond)<1e-6&&distance(r.destination,destination)<1e-6&&distance(r.candidateCenter,center)<1e-6&&same(r.regionBefore,handoff<0?old:next),'FROZEN_GEOMETRY');
  check(axes.every(k=>Math.abs(r.position[k]-r.before[k]-r.velocity[k])<1e-6)&&length(r.velocity)<=.240001&&distance(r.velocity,r.velocityBefore)<=Math.hypot(.11,.045)+1e-6&&(i===0||distance(r.before,rows[i-1].position)<1e-6&&distance(r.velocityBefore,goalVelocity(rows[i-1].velocity))<1e-6),'NATIVE_MOTION');
  const travel=['TELL','CLIMB','ALIGN','CROSS','BRAKE'].includes(r.phase),desired=angle(travel?heading:r.observedLook);
  check(Number.isFinite(r.yawBefore)&&Number.isFinite(r.pitchBefore)&&Number.isFinite(r.yaw)&&Number.isFinite(r.pitch)&&Number.isFinite(r.bodyYaw)&&Math.abs(delta(r.yawBefore,r.yaw))<=4.0001&&Math.abs(delta(r.pitchBefore,r.pitch))<=3.0001&&Math.abs(delta(r.yaw,r.bodyYaw))<.0001&&(i===0||Math.abs(delta(rows[i-1].yaw,r.yawBefore))<.0001&&Math.abs(delta(rows[i-1].pitch,r.pitchBefore))<.0001),'NATIVE_ROTATION');
  check(travel?distance(r.look,heading)<1e-6:distance(r.look,r.observedLook)<1e-6,'LOOK_PHASE');
  if(r.phase==='TELL'||r.phase==='CLIMB'||r.phase==='ALIGN')check(Math.hypot(r.position.x-start.x,r.position.z-start.z)<1e-6,'CLIMB_BEFORE_CROSS');
  if(r.phase==='CROSS')check(r.before.y>=actor.targetTop+16-1e-6&&Math.abs(delta(r.yawBefore,desired.yaw))<=15.0001&&Math.abs(delta(r.pitchBefore,desired.pitch))<=15.0001,'HIGH_TRAVEL_FACING_CROSS');
  if(r.phase==='TURN')check(Math.hypot(r.before.x-beyond.x,r.before.z-beyond.z)<=.7+1e-6&&length(r.velocityBefore)<=.04+1e-6,'BEHIND_BEFORE_TURN');
  if(r.phase==='DESCEND'||r.phase==='HANDOFF')check(Math.abs(delta(r.yawBefore,desired.yaw))<=8.0001&&Math.abs(delta(r.pitchBefore,desired.pitch))<=8.0001&&r.velocity.y<0,'TARGET_FACING_DESCENT');
  if(r.phase==='DESCEND')check(radius(r.before)>.95,'FIRST_HEIGHT_ENTRY');
  const plane=(r.position.x-target.x)*dx/horizontal+(r.position.z-target.z)*dz/horizontal;
  if(r.phase==='CROSS'&&plane>0){crossed=true;check(r.position.y>=actor.targetTop+16-1e-6,'ABOVE_TARGET_PLANE');}
  if(r.commit){
   commits++;check(handoff<0&&r.phase==='HANDOFF'&&r.goalTick<=320&&radius(r.before)<=.95&&r.before.y>centerY+7&&r.velocityBefore.y<-.05&&r.velocity.y<-.05,'MOVING_HEIGHT_HANDOFF');handoff=i;
  }
  check(same(r.region,handoff<0?old:next),'NO_PREMATURE_RELOCATION');
 }
 check(same(phases.map(x=>x.phase).filter(x=>x!=='ALIGN'),['TELL','CLIMB','CROSS','BRAKE','TURN','DESCEND','HANDOFF','NORMAL'])&&phases[0]?.count===12&&phases.find(x=>x.phase==='TURN')?.count<=49&&phases.at(-1)?.count===12&&commits===1&&crossed&&handoff>=0&&rows[handoff+1]?.velocity.y<-.01,'FINITE_TRANSIT_SEQUENCE');
 const travelClient=clients.filter((r,i)=>finite(r.position)&&i>0&&r.position.y>=actor.targetTop+16-1e-6&&(r.position.x-target.x)*heading.x+(r.position.z-target.z)*heading.z>-7&&(r.position.x-start.x)*heading.x+(r.position.z-start.z)*heading.z>2&&((r.position.x-clients[i-1].position.x)*heading.x+(r.position.z-clients[i-1].position.z)*heading.z)>.05);
 const descentClient=clients.filter((r,i)=>finite(r.position)&&finite(r.targetEye)&&i>0&&Math.hypot(r.position.x-center.x,r.position.z-center.z)<1&&r.position.y>centerY+8.5&&r.position.y<height-1&&r.position.y-clients[i-1].position.y<-.02);
 check(travelClient.length>=5&&travelClient.every(r=>Number.isFinite(r.bodyYaw)&&Math.abs(delta(r.bodyYaw,angle(heading).yaw))<=15),'CLIENT_TRAVEL_FACING');
 check(descentClient.length>=5&&descentClient.every(r=>Number.isFinite(r.bodyYaw)&&Math.abs(delta(r.bodyYaw,angle({x:r.targetEye.x-r.position.x,y:0,z:r.targetEye.z-r.position.z}).yaw))<=20),'CLIENT_PLAYER_FACING_DESCENT');
 const owned=[actor.uuid,actor.targetUuid];check(cleanup.length===2&&new Set(cleanup.map(x=>x.uuid)).size===2&&cleanup.every(x=>owned.includes(x.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(x.status)),'OWNED_CLEANUP');
 return {status:failures.length?'FAIL':'PASS',scope:reanchorScope,failures:[...new Set(failures)],serverRows:rows.length,clientRows:clients.length,commits,phases,routeLength,budget,travelClientRows:travelClient.length,descentClientRows:descentClient.length};
}
