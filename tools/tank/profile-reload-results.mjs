export const reloadScope='EXPLICIT_INSTANTIATED_PROFILE_RELOAD_NOT_DURABLE_RESTART_OR_MELEE';
const names=['DEFLECTED_CURVE','BURST','CURVE','LOB','MISSING_PROFILE','INVALID_INDEX','SHIFTED_POSITION','INCONSISTENT_DEFLECTION'];
/** Supplemental coverage only; native source/material/closure must be attested separately. */
export function analyzeProfileReload(rows,result,request){
 const failures=[];const check=(ok,why)=>{if(!ok)failures.push(why);};
 check(request?.maxTicks===180&&request?.maxWallMs===15000,'REQUEST_SCOPE');
 check(result?.scope===reloadScope&&result?.nonce===request.nonce&&result?.status==='PASS'&&result?.failures?.length===0&&result?.connectedPlayer===true,'NATIVE_RESULT');
 const cases=result?.cases??[];check(cases.length===8&&new Set(cases.map(c=>c.name)).size===8&&names.every(n=>cases.some(c=>c.name===n)),'CASES');
 check(rows.length>0&&rows.length<=1024&&rows.length===result?.rows&&rows.every((r,i)=>Number.isSafeInteger(r.tick)&&r.tick>=result.startGameTime&&r.tick<=result.endGameTime&&(!i||r.tick>=rows[i-1].tick)),'BOUNDED_ROWS');
 check(result?.endGameTime-result?.startGameTime<180,'TICK_WINDOW');
 check(rows.every(r=>[r.x,r.y,r.z].every(Number.isFinite)&&r.x>=0&&r.x<=52&&r.y>=223.5&&r.y<=248&&r.z>=0&&r.z<=52&&cases.some(c=>c.name===r.case&&c.uuid===r.uuid)),'IDENTITY_POSITION');
 for(const c of cases){
  const rs=rows.filter(r=>r.case===c.name),load=rs.find(r=>r.event==='LOAD'),removed=rs.find(r=>r.event==='REMOVED'),native=rs.filter(r=>r.event==='NATIVE');
  check(load&&removed&&removed.tick>load.tick&&removed.alive===false,'LIFECYCLE_'+c.name);
  if(c.negative){check(c.checks?.invalidSpawnRejectMarker===true&&native.length===0&&removed&&load&&removed.tick-load.tick===1,'FAIL_CLOSED_'+c.name);continue;}
  check(c.savedIndex===(c.normalized?0:9),'INTERIOR_SAVE_'+c.name);
  check(c.checks&&['stateEqual','ownerResolved','preflightEqual','provenanceEqual','unregisteredBeforeLoad'].every(k=>c.checks[k]===true),'LOAD_EQUALITY_'+c.name);
  check(rs.some(r=>r.event==='SAVE')&&native.length>0&&load?.index===c.savedIndex,'SAVE_CONTINUATION_'+c.name);
  check(Array.isArray(c.points)&&c.points.length>=2&&c.points.length<=97&&c.points.every(p=>p.length===3&&p.every(Number.isFinite)),'PATH_'+c.name);
  const live=rs.filter(r=>['SAVE','LOAD','NATIVE'].includes(r.event));
  check(live.every(r=>r.normalized===c.normalized&&r.alive===true&&typeof r.ownerUuid==='string'&&r.ownerUuid.length>0),'STATE_'+c.name);
  if(!c.normalized){
   check(live.every(r=>Number.isSafeInteger(r.index)&&r.index>=0&&r.index<c.points.length&&Math.hypot(r.x-c.points[r.index][0],r.y-c.points[r.index][1],r.z-c.points[r.index][2])<=1e-6),'PATH_POSITION_'+c.name);
   check(native.every((r,i)=>r.index===c.savedIndex+i+1)&&removed?.index===c.points.length-1,'CLOCK_END_'+c.name);
  }else check(native.every(r=>r.index===c.savedIndex)&&native.some(r=>load&&Math.hypot(r.x-load.x,r.y-load.y,r.z-load.z)>.1),'NORMALIZED_CONTINUATION_'+c.name);
 }
 return {status:failures.length?'FAIL':'PASS',scope:reloadScope,cases:cases.length,rows:rows.length,failures,limitations:['In-memory instantiated save/reload; not durable unload/server restart.','Explicit projectile reliability and injected actual-Player damage source; not natural selector or human melee/input.','Native provenance, original files and closure require independent receipts; no late-client acceptance.']};
}
