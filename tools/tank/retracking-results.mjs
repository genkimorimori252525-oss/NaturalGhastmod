import {analyzeClientSync,clientSyncScope} from './client-sync-results.mjs';
export const retrackingScope='CONTROLLED_NATIVE_TRACKER_REMOVE_READD_NOT_ENTITY_RELOAD_OR_LATE_JOIN';
export function analyzeRetracking(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===retrackingScope&&result.status==='PASS'&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.overrides===false&&result.samples>0&&result.samples<=240&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 check(Array.isArray(result?.cleanup)&&result.cleanup.every(x=>x.status==='RESTORED'||x.status==='ALREADY_REMOVED'),'CLEANUP');
 const names=['BURST','CURVE','LOB','BOMB','DEFLECTED_CURVE','STANDARD','GROUND'];
 const projections=[];
 // Reuse the proven motion contract on two actual observation subsets, not two alleged passive trials.
 for(const generation of [0,1]){
  const projected={...result,scope:clientSyncScope,spawns:(result?.spawns??[]).filter(x=>x.generation===generation),joins:(result?.joins??[]).filter(x=>x.generation===generation),clientRows:(result?.clientRows??[]).filter(x=>x.generation===generation),serverRows:(result?.serverRows??[]).filter(x=>x.generation===generation)};
  const a=analyzeClientSync(projected,request);projections.push({generation,status:a.status,failures:a.failures});check(a.status==='PASS','MOTION_GENERATION_'+generation);
 }
 const spawns=result?.spawns??[],joins=result?.joins??[],rows=result?.serverRows??[],stops=result?.stops??[],absences=result?.absences??[],adds=result?.readds??[];
 check(spawns.length===14&&joins.length===14&&stops.length===7&&absences.length===7&&adds.length===7&&rows.length<=1536,'LIFECYCLE_SET');
 for(const name of names){
  const initial=spawns.find(x=>x.name===name&&x.generation===0),next=spawns.find(x=>x.name===name&&x.generation===1),trace=rows.filter(x=>x.name===name),stop=stops.filter(x=>x.name===name),absence=absences.filter(x=>x.name===name),add=adds.filter(x=>x.name===name),newJoin=joins.find(x=>x.name===name&&x.generation===1);
  if(!initial||!next||stop.length!==1||absence.length!==1||add.length!==1){check(false,'LIFECYCLE_'+name);continue;}
  const [s]=stop,[a]=absence,[r]=add;
  check([next,s,a,r,newJoin,...trace].every(x=>x?.uuid===initial.uuid&&x.id===initial.id),'IDENTITY_'+name);
  check(s.owned===true&&a.oldRemoved===true&&a.idAbsent===true&&newJoin?.distinct===true,'CLIENT_REMOVAL_'+name);
  check(!s.path||s.normalized||s.remaining>=12,'REMAINING_PATH_'+name);
  check(s.epoch<=a.epoch&&a.epoch<=r.epoch&&r.epoch<=newJoin?.epoch&&r.tick>s.tick&&r.tick-s.tick<=6,'ORDER_'+name);
  check(trace.every(x=>x.registered===true&&x.alive===true&&x.loaded===true),'NATIVE_REGISTERED_'+name);
  for(let i=1;i<trace.length;i++)check(trace[i].tick===trace[i-1].tick+1&&trace[i].age===trace[i-1].age+1&&trace[i].index>=trace[i-1].index&&trace[i].index-trace[i-1].index<=1&&(!trace[i].path||trace[i].normalized||trace[i].index===trace[i-1].index+1),'NATIVE_CLOCK_'+name);
  const current=trace.find(x=>x.tick===r.tick);check(current&&next.tick===r.tick&&['age','index','path','normalized','owner'].every(k=>next[k]===current[k])&&['x','y','z','vx','vy','vz','px','py','pz'].every(k=>Number.isFinite(next[k])&&Math.abs(next[k]-current[k])<1e-9),'CURRENT_READD_STATE_'+name);
  check(trace.some(x=>x.tick>s.tick&&x.tick<r.tick)&&trace.filter(x=>x.tick>r.tick).length>=4,'NATIVE_GAP_CONTINUATION_'+name);
  check((result.clientRows??[]).filter(x=>x.name===name&&x.generation===1).every(x=>x.oldRemoved===true),'OLD_CLIENT_STAYS_REMOVED_'+name);
 }
 return {status:failures.length?'FAIL':'PASS',scope:retrackingScope,failures:[...new Set(failures)],generations:projections,entities:joins.length,clientRows:result?.clientRows?.length??0,serverRows:rows.length};
}
