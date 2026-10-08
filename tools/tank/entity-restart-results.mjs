export const entityRestartScope='CLEAN_STOP_NEW_PROCESS_NATIVE_ENTITY_PERSISTENCE_NOT_POWER_LOSS_OR_INPUT';
export function analyzeEntityRestart(report){
 const failures=[];const check=(ok,name)=>{if(!ok)failures.push(name);};
 check(report?.scope===entityRestartScope&&report.status==='PASS'&&report.sourceUnchanged===true&&report.originalFilesVerified===85,'REPORT_AUTHORITY');
 const [a,b]=(report?.phases??[]).map(p=>p.native??{});
 check(report?.phases?.length===2&&a?.phase==='A'&&b?.phase==='B'&&a.nonce===report.nonce&&b.nonce===report.nonce,'PHASE_IDENTITY');
 check(Number.isSafeInteger(a?.pid)&&Number.isSafeInteger(b?.pid)&&Number.isFinite(a.processStart)&&b.processStart>a.processStart,'NEW_PROCESS');
 check((report?.phases??[]).every(p=>p.native?.status==='STOPPED'&&p.native.serverStopped===true&&p.native.worldClosed===true&&p.native.overrides===false&&Array.isArray(p.native.errors)&&!p.native.errors.length&&p.closure?.clean===true&&p.closure.exit==='VERIFIED_EXIT'&&p.closure.evidence==='EVIDENCE_COMPLETE'),'NORMAL_CLOSED');
 check(b?.canonicalPlayer===true,'GENUINE_OWNER');
 const copy=report?.worldCopy;check(Array.isArray(copy?.source)&&copy.source.length>0&&JSON.stringify(copy.source)===JSON.stringify(copy?.target),'WORLD_COPY');
 const names=['BOSS','BURST','CURVE','LOB','BOMB','NORMALIZED_CURVE','STANDARD','GROUND'];
 const saved=a?.saved??[],loaded=b?.loaded??[],rows=b?.rows??[];
 check(saved.length===8&&loaded.length===8&&names.every(n=>saved.filter(s=>s.name===n).length===1&&loaded.filter(s=>s.name===n).length===1),'ENTITY_SET');
 check(new Set(saved.map(s=>s.uuid)).size===8,'UNIQUE_ENTITIES');
 for(const name of names){
  const s=saved.find(x=>x.name===name),l=loaded.find(x=>x.name===name),trace=rows.filter(r=>r.name===name);if(!s||!l){check(false,'MISSING_'+name);continue;}
  check(s.uuid===l.uuid&&s.state===l.state&&s.kind===l.kind&&s.normalized===l.normalized&&s.index===l.index&&s.age===l.age&&s.ownerUuid===l.ownerUuid,'INITIAL_STATE_'+name);
  if(name==='BOSS'){
   check(l.bossSafeReset===true&&trace.length>1,'SAFE_BOSS_RESET');
   for(let i=1;i<trace.length;i++)check(Math.hypot(trace[i].x-trace[i-1].x,trace[i].y-trace[i-1].y,trace[i].z-trace[i-1].z)<=.650001,'BOSS_NATIVE_STEP');continue;
  }
  check(trace.length>=4&&trace.some(r=>r.ownerResolved===true&&r.ownerUuid===s.ownerUuid)&&trace.every(r=>r.uuid===s.uuid&&(r.ownerAvailable===false?r.ownerResolved===false&&r.ownerUuid==='':r.ownerResolved===true&&r.ownerUuid===s.ownerUuid)),'OWNER_CONTINUATION_'+name);
  let previous={tick:l.joinTick-1,age:s.age,index:s.index,x:s.x,y:s.y,z:s.z,alive:true};
  const profile=['BURST','CURVE','LOB','BOMB'].includes(name);
  for(let i=0;i<trace.length;i++){
   const r=trace[i];check(['tick','age','index','x','y','z'].every(k=>Number.isFinite(r[k]))&&r.tick>=l.joinTick,'ROW_'+name);
   if(i>0)check(r.tick===previous.tick+1,'TICK_'+name);
   const ageDelta=r.age-previous.age;check(ageDelta===(i===0&&r.age===s.age?0:!r.alive&&r.age===previous.age?0:1),'AGE_'+name);
   if(profile){
    check(r.index>=previous.index&&r.index-previous.index<=1&&r.index-previous.index===ageDelta,'CLOCK_'+name);
    if(r.alive){const p=s.points?.[r.index];check(p&&Math.hypot(r.x-p[0],r.y-p[1],r.z-p[2])<=1e-6,'PATH_'+name);}
    else check(r.index===s.points?.length-1&&i===trace.length-1,'FINITE_REMOVAL_'+name);
   }else if(name==='NORMALIZED_CURVE')check(r.index===s.index&&r.normalized===true,'NORMALIZED_CONTINUATION');
   previous=r;
  }
  if(profile)check(trace.at(-1)?.alive===false&&trace.at(-1).index===s.points?.length-1,'PROFILE_END_'+name);
  else check(trace.at(-1)?.age>=s.age+4&&Math.hypot(trace.at(-1).x-s.x,trace.at(-1).y-s.y,trace.at(-1).z-s.z)>.1,'NATIVE_MOVEMENT_'+name);
 }
 return {status:failures.length?'FAIL':'PASS',scope:entityRestartScope,failures:[...new Set(failures)],entities:loaded.length,rows:rows.length};
}
