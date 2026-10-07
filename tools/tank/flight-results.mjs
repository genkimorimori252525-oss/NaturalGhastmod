/** Supplemental measured motion acceptance. Never relabels canonical experiment results. */
export function analyzeFlight(rows,playerUuid){
 if(!Array.isArray(rows)||!rows.length||typeof playerUuid!=='string'||!playerUuid)throw Error('Measured trace and player identity required');
 const failures=[];
 const require=(condition,message)=>{if(!condition)failures.push(message);};
 require(rows.length>=120,'TRACE_TOO_SHORT');
 require(rows.every(r=>!r.noAI&&r.collisionFree),'BODY_COLLISION_OR_INACTIVE');
 require(rows.every(r=>['tick','x','y','z','vx','vy','vz','speed','range'].every(k=>Number.isFinite(r[k]))),'NONFINITE_TRACE');
 require(rows.slice(1).every((r,i)=>r.tick===rows[i].tick+1),'TRACE_GAP');
 require(rows.every(r=>r.speed<=.65001&&r.speed>=0),'SPEED_CAP_EXCEEDED');
 const acquired=rows.filter(r=>r.targetUuid===playerUuid&&r.targetType==='minecraft:player');
 require(acquired.length>=50,'TARGET_ACQUISITION_NOT_OBSERVED');
 const first=rows[0],displacement=Math.max(...rows.map(r=>Math.hypot(r.x-first.x,r.y-first.y,r.z-first.z)));
 require(displacement>=10,'FLIGHT_DISPLACEMENT_INSUFFICIENT');
 require(rows.some(r=>r.speed>=.3),'ACCELERATION_NOT_OBSERVED');
 const comfortable=acquired.filter(r=>r.range>=22&&r.range<=34);
 const visibleFrontal=comfortable.filter(r=>r.lineOfSight===true&&r.inFrontalVolume===true);
 require(comfortable.length>=20,'PREFERRED_RANGE_NOT_ESTABLISHED');
 require(visibleFrontal.length>=20,'VISIBLE_FRONTAL_ARRIVAL_NOT_OBSERVED');
 require(visibleFrontal.some(r=>r.speed<.04),'BRAKING_AND_STILLNESS_NOT_OBSERVED');
 let brakingTicks=0,brakingDrop=0,continuousBraking=false,driftTicks=0,driftDistance=0,actualDrift=false;
 for(let i=1;i<rows.length;i++){
  const r=rows[i],previous=rows[i-1],drop=previous.speed-r.speed;
  if(r.targetUuid===playerUuid&&r.lineOfSight===true&&drop>.005&&drop<=.045){brakingTicks++;brakingDrop+=drop;}
  else{brakingTicks=0;brakingDrop=0;}
  if(brakingTicks>=4&&brakingDrop>=.1)continuousBraking=true;
  if(r.targetUuid===playerUuid&&r.lineOfSight===true&&r.inFrontalVolume===true&&r.range>=22&&r.range<=34&&r.primitive==='DRIFT'&&r.speed>.02){
   driftTicks++;driftDistance+=Math.hypot(r.x-previous.x,r.y-previous.y,r.z-previous.z);
  }else{driftTicks=0;driftDistance=0;}
  if(driftTicks>=4&&driftDistance>=.15)actualDrift=true;
 }
 require(continuousBraking,'CONTINUOUS_BRAKING_NOT_OBSERVED');
 require(actualDrift,'ACTUAL_DRIFT_NOT_OBSERVED');
 require(rows.slice(1).every((r,i)=>Math.abs(Math.hypot(r.x-rows[i].x,r.y-rows[i].y,r.z-rows[i].z)-r.speed)<.06),'MOTION_APPLICATION_MISMATCH');
 return {status:failures.length?'FAIL':'PASS',failures,samples:rows.length,acquiredSamples:acquired.length,
  comfortableSamples:comfortable.length,visibleFrontalSamples:visibleFrontal.length,continuousBraking,actualDrift,maximumSpeed:Math.max(...rows.map(r=>r.speed)),maximumDisplacement:displacement,
  contexts:[...new Set(rows.map(r=>r.context))],primitives:[...new Set(rows.map(r=>r.primitive))],
  scope:'STATIC_PLAYER_ACQUISITION_FLIGHT_RANGE_BRAKE_DRIFT',limitations:['No moving player, target change, LOS loss/recovery, ground locomotion, tactics, attacks, multiplayer or performance acceptance.']};
}
