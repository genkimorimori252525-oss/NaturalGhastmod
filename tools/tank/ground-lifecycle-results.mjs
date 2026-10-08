export const groundLifecycleScope='CONTROLLED_NATIVE_GROUND_LIFECYCLE_NOT_INPUT_OR_FULL_RELEASE';
export function analyzeGroundLifecycle(result,request){
 const failures=[];const check=(ok,name)=>{if(!ok)failures.push(name);};
 check(result?.scope===groundLifecycleScope&&result?.nonce===request?.nonce,'IDENTITY');
 check(result?.status==='PASS'&&result.canonicalPlayer===true&&result.overrides===false&&Array.isArray(result.errors)&&!result.errors.length,'NATIVE_STATUS');
 const rows=Array.isArray(result?.rows)?result.rows:[],changes=Array.isArray(result?.changes)?result.changes:[];
 check(rows.length>0&&rows.length<request?.maxTicks&&request?.maxTicks===240&&request?.maxWallMs===20000,'BOUNDS');
 const stages=['WAIT_GROUND','FLOOR_LOST','RESTORED','TRANSIENT','RECLOSED','SUSTAINED','ASCENT'];let last=-1,previous;
 for(const r of rows){
  const index=stages.indexOf(r.stage);check(index>=last&&index>=0,'STAGE_ORDER');last=index;
  check(Number.isSafeInteger(r.tick)&&Number.isSafeInteger(r.elapsed)&&r.elapsed>0&&['x','y','z','vx','vy','vz','playerHealth','bossHealth'].every(k=>Number.isFinite(r[k])),'ROW_FIELDS');
  if(previous){check(r.tick===previous.tick+1,'TICK_CONTINUITY');check(Math.hypot(r.x-previous.x,r.y-previous.y,r.z-previous.z)<=.650001,'PHYSICAL_STEP');check(r.playerHealth<=previous.playerHealth&&r.bossHealth<=previous.bossHealth,'NO_HEALING');}
  check(Math.hypot(r.vx,r.vy,r.vz)<=.650001,'VELOCITY_CAP');previous=r;
 }
 const group=stage=>rows.filter(r=>r.stage===stage),floor=group('FLOOR_LOST'),transient=group('TRANSIENT'),closed=group('RECLOSED'),sustained=group('SUSTAINED'),ascent=group('ASCENT');
 const sameRegion=(a,b)=>a&&b&&['generation','x','y','z','rx','ry','rz'].every(k=>Number.isFinite(a[k])&&a[k]===b[k]);
 const baseline=group('WAIT_GROUND').at(-1),protectedRows=rows.filter(r=>r.stage!=='WAIT_GROUND');check(protectedRows.length>0&&protectedRows.every(r=>sameRegion(r.region,baseline?.region)),'RETAINED_REGION');
 check(stages.every(stage=>group(stage).every((r,i)=>r.elapsed===i+1)),'STAGE_ELAPSED');
 const open=r=>Number.isInteger(r.mask)&&(r.mask&256)!==0&&[0,1,2,3,4,5,6,7].filter(i=>(r.mask&(1<<i))!==0).length>=3;
 check(floor.length===8&&floor.every(r=>r.phase==='SAFE_HOLD'&&r.intent==='BRAKE'&&r.support===false&&r.fired===baseline?.fired),'FLOOR_LOSS_BRAKE_OFFENSE');
 check(floor.length>0&&Math.hypot(floor.at(-1).vx,floor.at(-1).vy,floor.at(-1).vz)<=.080001,'SETTLED_BRAKE');
 check(group('RESTORED').some(r=>r.phase==='GROUNDED'&&r.support===true)&&result?.restoration?.floor==='RESTORED','FLOOR_RESTORED');
 check(transient.length===10&&transient.every(r=>r.phase==='GROUNDED'&&open(r)),'TRANSIENT_NO_TAKEOFF');
 check(closed.length===5&&closed.every(r=>r.phase==='GROUNDED'&&!open(r)),'RECLOSED_RESET');
 check(sustained.length===20&&sustained.every((r,i)=>open(r)&&r.phase===(i===19?'TAKEOFF':'GROUNDED')),'TWENTY_CLEAR_DWELL');
 check(ascent.length>1&&ascent.at(-1).phase==='AIR'&&ascent.slice(0,-1).every(r=>r.phase==='TAKEOFF')&&ascent.at(-1).y-sustained[0]?.y>=3.8,'NATIVE_ASCENT');
 check(sustained.length>0&&ascent.every(r=>r.fired===sustained.at(-1).fired),'TAKEOFF_NO_OFFENSE');
 const expected=[['FLOOR_OPEN','FLOOR_LOST',1],['FLOOR_RESTORE','RESTORED',1],['CEILING_OPEN_TRANSIENT','TRANSIENT',2704],['CEILING_CLOSE','RECLOSED',2704],['CEILING_OPEN_SUSTAINED','SUSTAINED',2704]];
 check(changes.length>=5&&changes.length<=6&&expected.every(([kind,stage,count],i)=>{const c=changes[i],first=group(stage)[0];return c?.kind===kind&&c.count===count&&c.prevalidated===true&&c.readback===true&&first?.tick===c.tick+1;}),'MUTATION_PROVENANCE');
 const closure=result?.restoration;
 check(['RESTORED','RETAINED_ACTOR_BLOCKED'].includes(closure?.ceiling)&&closure.ceilingConflictCells===0&&['ceilingRestoredCells','ceilingActorBlockedCells'].every(k=>Number.isSafeInteger(closure[k])&&closure[k]>=0&&closure[k]<=2704)&&closure.ceilingRestoredCells+closure.ceilingActorBlockedCells<=2704&&(closure.ceiling==='RESTORED'?closure.ceilingActorBlockedCells===0:closure.ceilingActorBlockedCells>0),'CEILING_CLOSURE');
 return {status:failures.length?'FAIL':'PASS',scope:groundLifecycleScope,failures:[...new Set(failures)],rows:rows.length};
}
