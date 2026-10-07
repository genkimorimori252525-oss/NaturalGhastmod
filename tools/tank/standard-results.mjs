/** Finite natural shot acceptance, deliberately excluding actual melee/rally/explosion balance. */
export function analyzeStandard(rows,clients,subjectUuid){
 const failures=[];const fail=(condition,name)=>{if(!condition)failures.push(name);};
 fail(Array.isArray(rows)&&rows.length===180,'FINITE_180_WINDOW');
 if(!Array.isArray(rows)||rows.length===0)return {status:'FAIL',failures};
 fail(rows.every((r,i)=>Number.isSafeInteger(r.tick)&&(!i||r.tick===rows[i-1].tick+1)),'CONTIGUOUS_TICKS');
 fail(rows.every(r=>r.health===100),'PROVISIONAL_BOSS_HEALTH');
 fail(rows.every(r=>r.playerMode==='survival'),'REAL_SURVIVAL_PLAYER');
 fail(rows[0].playerHealth===20&&rows.every(r=>r.mobGriefing===false),'DECLARED_PRIVATE_FIXTURE');
 const launches=rows.map((r,i)=>r.attackFire?i:-1).filter(i=>i>=0);
 fail(launches.length===1&&Math.max(...rows.map(r=>r.firedCount))===1,'EXACTLY_ONE_NATURAL_SHOT');
 const launch=launches[0];
 if(launch!==undefined){
  const charge=rows.slice(launch-30,launch),recovery=rows.slice(launch,launch+20);
  fail(charge.length===30&&charge.every((r,i)=>r.attackPhase==='CHARGE'&&r.attackTicks===i&&r.firingFace&&r.lineOfSight&&r.targetType==='minecraft:player'&&r.playerHealth>0),'VISIBLE_30_TICK_CHARGE');
  const locked=charge.slice(-11),first=locked[0];
  fail(first&&[...locked,rows[launch]].every(r=>Math.hypot(r.aimX-first.aimX,r.aimY-first.aimY,r.aimZ-first.aimZ)<1e-9),'LOCKED_FINAL_AIM');
  fail(recovery.length===20&&recovery.every((r,i)=>r.attackPhase==='RECOVER'&&r.attackTicks===i&&r.firingFace)&&rows[launch+20]?.firingFace===false,'20_TICK_FIRING_FACE');
 }
 const shots=rows.flatMap(r=>r.projectiles??[]);
 fail(shots.length>0&&shots.every(p=>p.type==='soutou_ghast:standard_fireball'&&p.width===1&&p.height===1&&p.pickRadius===1.5&&p.origin===subjectUuid&&p.savedOrigin===subjectUuid&&p.owner===subjectUuid&&p.returns===0&&!p.playerDeflected&&p.speed>0),'REGISTERED_MOVING_PROJECTILE');
 fail(Array.isArray(clients)&&clients.some(c=>shots.some(p=>p.uuid===c.uuid)&&c.type==='soutou_ghast:standard_fireball'&&c.renderer==='net.minecraft.client.renderer.entity.ThrownItemRenderer'&&Math.abs(c.powerMagnitude-.1)<1e-6&&c.speed>0),'ACTUAL_CLIENT_SPAWN_RENDERER_POWER');
 const movement=rows.filter(r=>r.regionGeneration!==undefined);
 fail(movement.length>=100&&new Set(movement.map(r=>r.regionGeneration)).size===1,'RETAINED_REGION');
 const entry=movement.findIndex(r=>r.inCombatRegion);
 fail(entry>=0&&entry<=20&&movement.slice(entry).every(r=>r.inCombatRegion),'BOUNDED_ENTRY_THEN_WITHIN_REGION');
 fail(movement.every(r=>r.collisionFree),'COLLISION_FREE');
 fail(movement.length>0&&movement.every(r=>r.regionX===movement[0].regionX&&r.regionY===movement[0].regionY&&r.regionZ===movement[0].regionZ&&r.radiusX===20&&r.radiusY===8&&r.radiusZ===20),'RETAINED_BROAD_REGION_GEOMETRY');
 fail(movement.filter(r=>r.tacticalAction==='DRIFT'&&r.speed>.02).length>=100,'ORDINARY_SWIMMING');
 const span=movement.length?Math.hypot(Math.max(...movement.map(r=>r.x))-Math.min(...movement.map(r=>r.x)),Math.max(...movement.map(r=>r.z))-Math.min(...movement.map(r=>r.z))):0;
 fail(span>=4,'MEASURED_SWIMMING_SPAN');
 return {status:failures.length?'FAIL':'PASS',failures,samples:rows.length,launches:launches.map(i=>rows[i].tick),horizontalSpan:span,
  realMelee:'NOT_RUN',fullRallyAcceptance:'NOT_RUN',returnedDamagePipeline:'NOT_RUN',explosionBalance:'NOT_RUN',terrainDestruction:'NOT_RUN',scope:'DEVELOPMENT_NATURAL_STANDARD_SHOT_ONLY'};
}
