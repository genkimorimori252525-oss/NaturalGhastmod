/** Separate v0.6 measured-motion scope; does not rewrite v0.5 receipts or judge visual feel. */
export function analyzeSwimming(rows,playerUuid){
 if(!Array.isArray(rows)||!rows.length||typeof playerUuid!=='string'||!playerUuid)throw Error('Measured trace and player identity required');
 const failures=[];const require=(ok,message)=>{if(!ok)failures.push(message);};
 const finite=rows.every(r=>['tick','x','y','z','vx','vy','vz','speed'].every(k=>Number.isFinite(r[k])));
 require(rows.length>=300,'TRACE_TOO_SHORT');require(finite,'NONFINITE_TRACE');
 require(rows.every(r=>!r.noAI&&r.collisionFree&&r.width===4&&r.height===4&&r.health===10),'BODY_COLLISION_OR_INACTIVE');
 require(rows.slice(1).every((r,i)=>r.tick===rows[i].tick+1),'TRACE_GAP');
 require(rows.every(r=>r.speed>=0&&r.speed<=.65001),'SPEED_CAP_EXCEEDED');
 require(rows.every(r=>Math.abs(Math.hypot(r.vx,r.vy,r.vz)-r.speed)<1e-5),'VELOCITY_SPEED_MISMATCH');
 require(rows.slice(1).every((r,i)=>Math.abs(Math.hypot(r.x-rows[i].x,r.y-rows[i].y,r.z-rows[i].z)-r.speed)<.06),'MOTION_APPLICATION_MISMATCH');
 const acquired=rows.filter(r=>r.targetUuid===playerUuid&&r.targetType==='minecraft:player'&&r.lineOfSight===true);
 require(acquired.length>=200,'VISIBLE_TARGET_ACQUISITION_NOT_OBSERVED');
 const regions=rows.filter(r=>Number.isSafeInteger(r.regionGeneration)&&r.regionGeneration>0);
 require(regions.length>=200,'REGION_NOT_OBSERVED');
 require(regions.every(r=>['regionX','regionY','regionZ','radiusX','radiusY','radiusZ'].every(k=>Number.isFinite(r[k]))),'NONFINITE_REGION');
 require(regions.every(r=>r.radiusX===20&&r.radiusY===8&&r.radiusZ===20),'REGION_SIZE_MISMATCH');
 const centers=new Map();let stable=true;
 for(const row of regions){
  const center=[row.regionX,row.regionY,row.regionZ];const previous=centers.get(row.regionGeneration);
  if(previous&&center.some((v,i)=>v!==previous[i]))stable=false;centers.set(row.regionGeneration,center);
 }
 require(stable,'REGION_MOVED_WITHOUT_RESELECTION');
 const swimming=regions.filter(r=>r.inCombatRegion===true&&r.intent==='MOVE'&&['DRIFT','HOLD'].includes(r.primitive));
 require(swimming.length>=100,'SUSTAINED_SWIMMING_NOT_OBSERVED');
 const span=key=>swimming.length?Math.max(...swimming.map(r=>r[key]))-Math.min(...swimming.map(r=>r[key])):0;
 const horizontalSpan=Math.max(span('x'),span('z')),verticalSpan=span('y');
 require(horizontalSpan>10,'SWIMMING_SPAN_INSUFFICIENT');require(verticalSpan>.5,'VERTICAL_SWIMMING_NOT_OBSERVED');
 const stopped=swimming.filter(r=>r.speed<.005).length;
 require(swimming.length>0&&stopped/swimming.length<.1,'REPEATED_STOPPING');
 return {status:failures.length?'FAIL':'PASS',failures,samples:rows.length,acquiredSamples:acquired.length,regionSamples:regions.length,
  swimmingSamples:swimming.length,horizontalSpan,verticalSpan,stoppedSwimmingSamples:stopped,regionGenerations:[...centers.keys()],
  maximumSpeed:finite?Math.max(...rows.map(r=>r.speed)):null,scope:'STATIC_PLAYER_PERSISTENT_REGION_PHYSICAL_SWIMMING',
  limitations:['Camera-turn/moving-player/LOS-native cases and visual smoothness require separate user inspection; no attacks, multiplayer or performance acceptance.']};
}
