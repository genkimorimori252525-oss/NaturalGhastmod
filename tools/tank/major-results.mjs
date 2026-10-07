/** Static survival fixture: development coverage only, never moving-input/balance acceptance. */
export function analyzeOverhead({rows,clients,paths,impacts,pose,end,request,downwardFramePresent},subjectUuid,playerUuid){
 const failures=[];const fail=(condition,name)=>{if(!condition)failures.push(name);};
 fail(Array.isArray(rows)&&rows.length>0&&rows.length<=900,'FINITE_900_MAX_WINDOW');
 if(!Array.isArray(rows)||!rows.length)return {status:'FAIL',failures};
 fail(request?.maxTicks===900&&request.maxWallMs===50000&&request.postDeathMaxTicks===200&&request.reserveMs===15000&&Number.isSafeInteger(request.wallDeadlineEpochMs),'FIXED_WALL_AND_CLEANUP_BUDGET');
 fail(end?.samples===rows.length&&end.tick===rows.at(-1).tick&&['MAX_TICKS','PLAYER_DEATH_RECOVERY_COMPLETE'].includes(end.reason)&&(end.reason!=='MAX_TICKS'||rows.length===900),'SUCCESSFUL_FINITE_WINDOW_END');
 fail(rows.every((r,i)=>Number.isSafeInteger(r.tick)&&(!i||r.tick===rows[i-1].tick+1)),'CONTIGUOUS_TICKS');
 fail(rows[0].playerHealth===20&&rows.every(r=>r.playerMode==='survival'&&r.playerUuid===playerUuid&&r.health===100&&r.mobGriefing===false&&r.speed<=.650001&&r.collisionFree),'AUTHENTIC_FIXTURE_AND_COLLISION_CAP');
 const movement=rows.filter(r=>r.regionGeneration!==undefined),first=movement[0];
 fail(first&&movement.every(r=>r.regionGeneration===first.regionGeneration&&r.regionX===first.regionX&&r.regionY===first.regionY&&r.regionZ===first.regionZ&&r.radiusX===20&&r.radiusY===8&&r.radiusZ===20),'RETAINED_BROAD_REGION');
 const phases=['WITHDRAW','RETURN','ALIGN','BOMBING','RECOVER'];let cursor=-1;
 for(const phase of phases){const next=rows.findIndex((r,i)=>i>cursor&&r.majorPhase===phase);fail(next>cursor,`OBSERVED_${phase}`);if(next>=0)cursor=next;}
 const major=rows.filter(r=>r.majorActive);
 fail(major.length>0&&major.every(r=>r.majorSequences===1&&!r.attackFire&&!r.rallyFace&&r.tacticalAction==='DRIFT'),'ONE_EXCLUSIVE_MAJOR_SEQUENCE');
 const withdraw=rows.filter(r=>r.majorPhase==='WITHDRAW');
 fail(withdraw.length>=2&&Math.hypot(withdraw.at(-1).x-withdraw[0].x,withdraw.at(-1).z-withdraw[0].z)>=14,'PHYSICAL_WITHDRAWAL');
 const recovered=rows.find((r,i)=>i>cursor&&!r.majorActive&&r.inCombatRegion&&r.majorReason!=='RECOVERY_TIMEOUT');
 fail(recovered&&Math.hypot(recovered.x-recovered.regionX,recovered.y-recovered.regionY,recovered.z-recovered.regionZ)<=1.5,'PHYSICAL_RETAINED_CENTER_RECOVERY');
 fail(pose&&pose.subjectUuid===subjectUuid&&pose.cameraUuid===playerUuid&&pose.cameraPitch===-90&&pose.renderPitch>=80&&pose.renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&downwardFramePresent&&rows.some(r=>Math.abs(r.tick-pose.tick)<=2&&r.majorDownward!==false&&['ALIGN','BOMBING'].includes(r.majorPhase)),'ACTUAL_CLIENT_DOWNWARD_POSE_AND_REQUESTED_FRAME');
 const deaths=rows.map((r,i)=>r.playerHealth<=0?i:-1).filter(i=>i>=0),death=deaths[0];
 if(death!==undefined){
  fail(end.deathSample===death&&end.reason==='PLAYER_DEATH_RECOVERY_COMPLETE'&&rows.length-death<=200&&rows.slice(death).every(r=>!r.majorReleaseRequested&&r.windowStage==='POST_DEATH_RECOVERY'),'BOUNDED_POST_DEATH_RECOVERY_NO_RELEASE');
 }else fail(end.deathSample===-1,'NO_UNOBSERVED_DEATH');
 const bombs=(Array.isArray(paths)?paths:[]).filter(p=>p.kind==='BOMB');
 fail(bombs.length>=1&&bombs.length<=3,'NATURALLY_EXERCISED_BOMBS');
 const releases=rows.filter(r=>r.majorReleaseRequested);
 fail(releases.length===bombs.length&&releases.every((r,i)=>!i||r.tick-releases[i-1].tick===24),'DECLARED_24_TICK_RELEASE_CADENCE');
 for(const p of bombs){
  const points=p.path,valid=Array.isArray(points)&&points.length>=2&&points.length<=97&&points.every(v=>Array.isArray(v)&&v.length===3&&v.every(Number.isFinite));
  fail(valid,'FINITE_BOMB_PATH');if(!valid)continue;
  fail(points.every((v,i)=>!i||(v[1]<points[i-1][1]&&Math.hypot(...v.map((n,j)=>n-points[i-1][j]))<=1.200001))&&points[0][1]-points.at(-1)[1]>=4&&Math.hypot(points[0][0]-points.at(-1)[0],points[0][2]-points.at(-1)[2])<=1,'LOCKED_DOWNWARD_PATH');
  const release=releases.find(r=>r.tick===p.preflightGameTime);
  fail(release&&release.playerHealth>0&&release.lineOfSight&&release.targetUuid===playerUuid&&release.targetType==='minecraft:player'&&release.majorPhase==='BOMBING'&&release.majorPitch>=80&&Math.hypot(release.x-release.playerX,release.z-release.playerZ)<=.75&&Math.abs(release.y-release.playerY-12)<=1&&Math.hypot(points.at(-1)[0]-release.playerX,points.at(-1)[2]-release.playerZ)<1e-6,'OBSERVED_OVERHEAD_RELEASE');
  fail(p.preflightScope==='PRE_LAUNCH_LOADED_SWEPT_BODY'&&p.preflightSegments===points.length-1&&Array.isArray(p.terminal)&&p.terminal.length>0&&p.terminal.length<=4&&p.terminal.every(c=>Number.isSafeInteger(c.x)&&Number.isSafeInteger(c.y)&&Number.isSafeInteger(c.z)&&typeof c.state==='string'&&c.state.length>0),'COMPLETE_LOADED_PREFLIGHT_AND_TERMINAL');
  const observations=rows.flatMap(r=>(r.projectiles??[]).filter(v=>v.uuid===p.uuid).map(v=>({...v,tick:r.tick})));
  fail(observations.length>=3&&observations.every((v,i)=>v.kind==='BOMB'&&!v.normalized&&v.type==='soutou_ghast:committed_fireball'&&v.width===1&&v.height===1&&v.pickRadius===1.5&&v.origin===subjectUuid&&v.savedOrigin===subjectUuid&&v.owner===subjectUuid&&!v.playerDeflected&&v.returns===0&&v.speed>0&&v.speed<=1.200001&&Number.isSafeInteger(v.index)&&v.index>=0&&v.index<points.length&&(!i||v.index===observations[i-1].index+1)&&Math.hypot(v.x-points[v.index][0],v.y-points[v.index][1],v.z-points[v.index][2])<1e-6),'ACTUAL_BOMB_POSITIONS_AND_CLOCK');
  fail((clients??[]).some(c=>c.uuid===p.uuid&&c.kind==='BOMB'&&c.type==='soutou_ghast:committed_fireball'&&c.renderer==='net.minecraft.client.renderer.entity.ThrownItemRenderer'&&c.powerMagnitude===0&&c.speed>0&&Number.isSafeInteger(c.index)&&c.index>=0&&c.index<points.length),'ACTUAL_CLIENT_BOMB');
  fail((impacts??[]).some(h=>h.uuid===p.uuid&&h.tick>=p.preflightGameTime&&h.producer==='NATIVE_PROJECTILE_IMPACT_EVENT_LOWEST_PRE_DAMAGE'&&['ENTITY','BLOCK'].includes(h.hitType)&&!h.normalized&&!h.canceledAtListener&&h.resultAtListener==='DEFAULT'),'OBSERVED_NATIVE_PRE_DAMAGE_IMPACT_HOOK');
 }
 return {status:failures.length?'FAIL':'PASS',failures,samples:rows.length,bombs:bombs.map(p=>({uuid:p.uuid,launchTick:p.preflightGameTime,segments:p.path?.length-1})),fullThreeBombSequence:bombs.length===3&&death===undefined,playerDeathTick:death===undefined?null:rows[death].tick,impactOutcome:'PRE_DAMAGE_LISTENER_ONLY',scope:'DEVELOPMENT_STATIC_PLAYER_OVERHEAD',movingPlayerAvoidance:'NOT_RUN',damageBalance:'NOT_RUN',cueReadability:'NOT_RUN',realCounterattack:'NOT_RUN'};
}
