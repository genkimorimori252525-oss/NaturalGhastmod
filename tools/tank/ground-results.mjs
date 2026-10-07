/** Supplementary actual native rows; never substitutes for canonical roster/owned closure. */
export function analyzeGround({rows,clients,damage,clientGround,end,request},boss,player){
 const failures=[];const require=(ok,name)=>{if(!ok)failures.push(name);};
 require(Array.isArray(rows)&&rows.length>=80&&rows.length<=180,'FINITE_GROUND_COVERAGE');
 if(!rows?.length)return {status:'FAIL',failures,damagePipeline:'NOT_ACCEPTED'};
 require(request?.maxTicks===180&&request.maxWallMs===15000&&request.reserveMs===15000,'DECLARED_FINITE_BUDGET');
 require(end?.samples===rows.length&&end.tick===rows.at(-1).tick&&(['MAX_TICKS','PLAYER_DEATH'].includes(end.reason)),'COMPLETE_WINDOW');
 require(end?.reason!=='PLAYER_DEATH'||rows.at(-1).playerHealth===0,'AUTHENTIC_DEATH_END');
 require(end?.reason!=='MAX_TICKS'||rows.length===180,'FULL_TICK_WINDOW');
 require(rows.every((r,i)=>r.uuid===boss&&r.playerUuid===player&&r.playerMode==='survival'&&Number.isFinite(r.playerHealth)&&r.playerHealth>=0&&r.playerHealth<=20&&(!i||r.tick===rows[i-1].tick+1)),'CONTIGUOUS_AUTHENTIC_ACTORS');
 require(rows[0].playerHealth===20,'GENUINE_20HP_INITIAL');
 require(rows.every(r=>r.width===4&&r.height===4&&r.health===100&&r.collisionFree===true&&r.mobGriefing===false&&Number.isFinite(r.speed)&&r.speed<=.650001),'BODY_COLLISION_SPEED');
 require(rows.every((r,i)=>[r.x,r.y,r.z].every(Number.isFinite)&&(!i||Math.hypot(r.x-rows[i-1].x,r.y-rows[i-1].y,r.z-rows[i-1].z)<=.650001)),'MEASURED_POSITION_SPEED');
 const first=rows[0];
 require(rows.every(r=>r.regionGeneration===first.regionGeneration&&r.regionX===first.regionX&&r.regionY===first.regionY&&r.regionZ===first.regionZ&&r.radiusX===20&&r.radiusY===8&&r.radiusZ===20),'RETAINED_REGION');
 const grounded=rows.filter(r=>r.grounded);
 require(grounded.length>=40&&grounded.every(r=>r.groundPhase==='GROUNDED'&&r.groundSupport===true&&Math.abs(r.y-224)<.021),'ACTUAL_SUPPORTED_GROUND');
 require(grounded.length>0&&Math.max(...grounded.map(r=>r.x))-Math.min(...grounded.map(r=>r.x))+Math.max(...grounded.map(r=>r.z))-Math.min(...grounded.map(r=>r.z))>=6,'PHYSICAL_SCUTTLE_SPAN');
 let entered=false;
 require(grounded.every((r,i)=>{const inside=((r.x-r.regionX)/20)**2+((r.z-r.regionZ)/20)**2<=1.000001;if(inside)entered=true;return entered?inside:i<40;})&&entered,'GROUND_XZ_PROJECTION');
 require(rows.every(r=>!r.overheadActive&&!r.rallyFace&&r.firedCount===0),'ONE_GROUND_OFFENSE_ONLY');
 require(clientGround?.subjectUuid===boss&&clientGround.grounded===true&&clientGround.width===4&&clientGround.height===4&&clientGround.cameraUuid===player,'ACTUAL_CLIENT_GROUND_STATE');
 const launches=rows.filter(r=>r.groundFire);require(launches.length>=2&&launches.every((r,i)=>r.grounded&&r.groundSupport&&(!i||r.tick-launches[i-1].tick>=34)),'NATURAL_SINGLE_CADENCE');
 const shots=new Map();for(const r of rows){require(r.projectileCoverage==='LOADED_ROOM_SELECTED_TYPE','COMPLETE_SELECTED_PROJECTILES');for(const p of r.projectiles??[]){if(!shots.has(p.uuid))shots.set(p.uuid,[]);shots.get(p.uuid).push({tick:r.tick,...p});}}
 const proven=[];
 for(const [uuid,points] of shots){
  require(points.every(p=>p.type==='soutou_ghast:ground_fireball'&&p.origin===boss&&p.owner===boss&&p.savedOrigin===boss&&!p.playerDeflected&&p.width===1&&p.height===1&&p.pickRadius===1.5&&Math.abs(p.speed-1.9)<1e-6),'GROUND_PROJECTILE_PROVENANCE');
  require(points.every((p,i)=>[p.x,p.y,p.z].every(Number.isFinite)&&(!i||p.tick===points[i-1].tick+1&&Math.abs(Math.hypot(p.x-points[i-1].x,p.y-points[i-1].y,p.z-points[i-1].z)-1.9)<1e-6)),'ACTUAL_FIXED_SPEED_FLIGHT');
  if(points.length>=3){const client=clients?.find(c=>c.uuid===uuid);require(client?.type==='soutou_ghast:ground_fireball'&&client.renderer?.endsWith('ThrownItemRenderer')&&client.powerMagnitude===0&&Math.abs(client.speed-1.9)<1e-6,'ACTUAL_CLIENT_SINGLE_RENDER');proven.push(uuid);}
 }
 require(proven.length>=2,'TWO_MEASURED_SINGLE_FLIGHTS');
 require(Array.isArray(damage)&&damage.length<=32&&damage.every(d=>Number.isFinite(d.amount)&&d.amount>=0&&Number.isFinite(d.healthBefore)&&typeof d.canceledAtListener==='boolean'&&['NATIVE_HURT_LISTENER_PRE_ARMOR','NATIVE_DAMAGE_LISTENER_PRE_HEALTH_WRITE'].includes(d.producer)),'BOUNDED_DAMAGE_OBSERVATIONS');
 return {status:failures.length?'FAIL':'PASS',failures:[...new Set(failures)],samples:rows.length,measuredProjectiles:proven,launchTicks:launches.map(r=>r.tick),nativeDamageListeners:damage?.length??0,combinedObservedHealthLoss:rows[0].playerHealth-rows.at(-1).playerHealth,
  scope:'DEVELOPMENT_STATIC_SUPPORTED_GROUND_MOVEMENT_AND_SINGLE_FLIGHT',damagePipeline:'NOT_ACCEPTED',nativeTakeoff:'NOT_RUN',realCounterplay:'NOT_RUN',cueReadability:'NOT_RUN',reloadLateClient:'NOT_RUN'};
}
