export const coverLobScope='NEW_PRIVATE_STATIC_COVER_NATIVE_PERCEPTION_NOT_PLAYER_INPUT_OR_PRODUCTION_GOAL';
const finite=v=>v&&['x','y','z'].every(k=>Number.isFinite(v[k])),distance=(a,b)=>finite(a)&&finite(b)?Math.hypot(a.x-b.x,a.y-b.y,a.z-b.z):Infinity,same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export function analyzeCoverLob(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===coverLobScope&&result.status==='PASS'&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=180&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 const actor=result?.actors,rows=result?.rows??[],shots=result?.shots??[],joins=result?.joins??[],clients=result?.clientRows??[],cleanup=result?.cleanup??[];
 if(!actor||!rows.length)return {status:'FAIL',scope:coverLobScope,failures:[...failures,'ACTORS_OR_ROWS']};
 check(actor.privateControlledGoals===true&&actor.targetMovement==='NEW_COW_NATIVE_NAVIGATION_AFTER_ACTUAL_LOS_LOSS'&&actor.bossMovement==='HOLD62_THEN_CONTROLLER_DRIFT_Z_0.14_UNTIL_LOS_LOSS'&&actor.width===4&&actor.height===4&&finite(actor.position)&&actor.uuid!==actor.targetUuid,'PRIVATE_ACTORS');
 const expectedWall=Array.from({length:136},(_,i)=>[27,224+Math.floor(i/17),26+i%17]);
 check(same(actor.wall,expectedWall)&&same(actor.region?.radii,{x:20,y:8,z:20})&&actor.region?.generation===1,'FIXED_PRIVATE_WALL_REGION');
 check(joins.length===1&&joins[0].uuid===actor.uuid&&joins[0].id===actor.id&&joins[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&clients.length===4&&clients.every(x=>x.uuid===actor.uuid&&x.id===actor.id),'CLIENT_IDENTITY_ONLY');
 check(rows.length<=180,'ROW_BOUND');
 for(const [i,r] of rows.entries()){
  check(r.uuid===actor.uuid&&r.id===actor.id&&r.loaded===true&&r.registered===true&&r.targetHealth>0&&r.navigationCalls>=0&&r.navigationCalls<=1,'NATIVE_IDENTITY');
  check(r.goalTick===i+1&&Number.isSafeInteger(r.tick)&&(i===0||r.tick===rows[i-1].tick+1),'CLOCK');
  check(finite(r.position)&&finite(r.targetNow)&&same(r.region,actor.region)&&Number.isFinite(r.attackNanos)&&r.attackNanos>=0,'FINITE_GEOMETRY_REGION');
  const region=actor.region;if(finite(r.position)&&region?.center&&region?.radii)check(['x','y','z'].reduce((sum,k)=>sum+((r.position[k]-region.center[k])/region.radii[k])**2,0)<=1.000001,'RETAINED_SWIM_VOLUME');
 }
 const admissionIndex=rows.findIndex(r=>r.coverActive),admit=rows[admissionIndex],previous=rows[admissionIndex-1];
 check(admissionIndex>0&&previous?.visible===true&&previous.phase==='CHARGE'&&previous.chargeTick>0&&admit.visible===false&&admit.chargeTick===0&&admit.phase==='CHARGE'&&admit.age>=1&&admit.age<=10,'ACTUAL_LOS_LOSS_FULL_RESTART');
 const emissions=rows.filter(r=>r.emitted);check(emissions.length===1&&shots.length===1,'SINGLE_COVER_EMISSION');
 const emission=emissions[0],shot=shots[0];
 if(!admit||!emission||!shot)return {status:'FAIL',scope:coverLobScope,failures:[...new Set([...failures,'MISSING_NATIVE_COVER'])]};
 const frozen=admit.snapshot,recipe=admit.recipe,window=rows.filter(r=>r.tick>=admit.tick&&r.tick<=emission.tick),cue=window.find(r=>r.chargeTick===19);
 check(frozen?.subject===actor.targetUuid&&frozen.tick===previous.tick&&finite(frozen.eye)&&finite(frozen.landing)&&recipe?.kind==='LOB'&&same(recipe.endpoint,frozen.landing),'LAST_VISIBLE_ENDPOINT');
 check(window.length===31&&emission.tick-admit.tick===30&&emission.fire===true&&emission.phase==='RECOVER'&&emission.chargeTick===0&&emission.age<=40,'COMPLETE_TELL_AND_FRESH_LAUNCH');
 for(const [i,r] of window.entries()){
  check(r.visible===false&&r.storedRayLoaded===true&&r.storedRay==='BLOCK'&&expectedWall.some(p=>same(p,r.storedRayBlock)),'LOADED_STORED_TERRAIN_OBSTRUCTION');
  check(same(r.snapshot,frozen)&&r.age===r.tick-frozen?.tick&&r.context===frozen?.context&&same(r.recipe,recipe)&&r.choice==='LOB'&&r.clear===true&&r.segments>0&&Array.isArray(r.terminal)&&r.terminal.length>0&&r.terminal.length<=4,'FROZEN_RECIPE_COMPLETE_PREFLIGHT');
  check(i===30?r.fire===true:r.phase==='CHARGE'&&r.chargeTick===i&&r.coverActive===true&&!r.fire,'CHARGE_CLOCK');
 }
 check(cue?.profileCueTick===cue?.tick&&emission.tick-cue?.tick===11&&window.filter(r=>r.tick>=cue?.tick).every(r=>r.profileCueTick===cue.tick),'DISTINCT_CUE19_LAUNCH30');
 check(rows.some(r=>!r.visible&&distance(r.targetNow,previous.targetNow)>=2),'ACTUAL_HIDDEN_TARGET_MOVEMENT');
 check(shot.origin===actor.uuid&&shot.owner===actor.uuid&&shot.tick===emission.tick&&shot.path?.kind==='LOB','REAL_OWNED_LOB');
 const points=shot.path?.points;const valid=Array.isArray(points)&&points.length>=2&&points.length<=97&&points.every(finite);
 check(valid,'BOUNDED_NATIVE_PATH');let measured=[],phases=[];
 if(valid){
  check(distance(points[0],shot.position)<1e-6&&same(points.at(-1),recipe?.endpoint)&&emission.segments===points.length-1,'ACTUAL_MUZZLE_LOCKED_ENDPOINT');
  check(points.slice(1).every((p,i)=>distance(p,points[i])>0&&distance(p,points[i])<=1.900001),'SEGMENT_SPEED_BOUND');
  measured=rows.flatMap(r=>(r.projectiles??[]).filter(p=>p.uuid===shot.uuid).map(p=>({...p,tick:r.tick})));phases=[...new Set(measured.map(p=>p.phase))];
  check(measured.length>=12&&phases.includes('ASCEND')&&phases.includes('DESCEND'),'NATIVE_ASCEND_DESCEND_WINDOW');
  check(measured.every((p,i)=>p.kind==='LOB'&&Number.isSafeInteger(p.index)&&p.index>=0&&p.index<points.length&&distance(p.position,points[p.index])<1e-6&&(!i||p.index===measured[i-1].index+1&&p.tick===measured[i-1].tick+1)),'ACTUAL_IMMUTABLE_PATH_MOTION');
 }
 const owned=[actor.uuid,actor.targetUuid,...shots.map(x=>x.uuid)];check(cleanup.length===owned.length&&new Set(cleanup.map(x=>x.uuid)).size===owned.length&&cleanup.every(x=>owned.includes(x.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(x.status)),'OWNED_CLEANUP');
 check(result.wallCleanup?.length===136&&result.wallCleanup.every((x,i)=>same(x.position,expectedWall[i])&&x.status==='RESTORED_NEW_PRIVATE_AIR'),'PRIVATE_WALL_CONDITIONAL_RESTORE');
 return {status:failures.length?'FAIL':'PASS',scope:coverLobScope,failures:[...new Set(failures)],serverRows:rows.length,clientIdentityRows:clients.length,admissionTick:admit.tick,cueTick:cue?.tick,launchTick:emission.tick,observationAgeAtLaunch:emission.age,projectileRows:measured.length,phases,maxAttackNanos:Math.max(...rows.map(r=>r.attackNanos))};
}
