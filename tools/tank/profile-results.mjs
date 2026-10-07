import {analyzeStandard} from './standard-results.mjs';
/** Actual positions against one immutable declared trajectory, not profile labels alone. */
export function analyzeProfiles(rows,clients,subjectUuid,paths){
 const common=analyzeStandard(rows,clients,subjectUuid,{profile:true}),failures=[...common.failures];
 const fail=(condition,name)=>{if(!condition)failures.push(name);};
 fail(Array.isArray(paths)&&paths.length===1,'ONE_FINITE_PROFILE_PATH');
 const exercised=[];
 for(const record of Array.isArray(paths)?paths:[]){
  const points=record.path;
  const valid=Array.isArray(points)&&points.length>=2&&points.length<=97&&points.every(p=>Array.isArray(p)&&p.length===3&&p.every(Number.isFinite));
  fail(valid,'BOUNDED_IMMUTABLE_POINTS');if(!valid)continue;
  fail(['BURST','CURVE','LOB'].includes(record.kind),'KNOWN_PROFILE');
  fail(record.preflightScope==='PRE_LAUNCH_LOADED_SWEPT_BODY'&&record.preflightSegments===points.length-1&&Number.isSafeInteger(record.preflightGameTime),'DECLARED_COMPLETE_PREFLIGHT');
  const observations=rows.flatMap(row=>(row.projectiles??[]).filter(p=>p.uuid===record.uuid).map(p=>({...p,tick:row.tick})));
  fail(observations.length>=12,'MEASURED_PROFILE_WINDOW');
  fail(observations.every((p,i)=>Number.isSafeInteger(p.index)&&p.index>=0&&p.index<points.length&&(!i||p.index===observations[i-1].index+1)&&p.tick>=record.preflightGameTime&&p.kind===record.kind&&!p.normalized),'COMMITTED_PATH_CLOCK');
  fail(observations.every(p=>points[p.index]&&Math.hypot(p.x-points[p.index][0],p.y-points[p.index][1],p.z-points[p.index][2])<1e-6),'ACTUAL_WORLD_PATH_POSITIONS');
  const phases=new Set(observations.map(p=>p.phase));
  const required=record.kind==='LOB'?['ASCEND','DESCEND']:record.kind==='CURVE'?['OUTWARD','SWEEP']:['SLOW','WARNING','BURST','FAST'];
  fail(required.every(p=>phases.has(p)),'OBSERVED_PROFILE_PHASES');
  const terminal=record.terminal;
  fail(Array.isArray(terminal)&&terminal.length<=4&&(record.kind!=='LOB'||terminal.length>0)&&terminal.every(p=>Number.isSafeInteger(p.x)&&Number.isSafeInteger(p.y)&&Number.isSafeInteger(p.z)&&typeof p.state==='string'&&p.state.length>0),'BOUNDED_TERMINAL_COLLIDERS');
  fail(clients.some(c=>c.uuid===record.uuid&&c.kind===record.kind&&Number.isSafeInteger(c.index)&&c.index>=0&&c.index<points.length),'ACTUAL_CLIENT_PROFILE_CLOCK');
  if(record.kind==='BURST')fail(observations.some(p=>p.phase==='FAST'&&Math.abs(p.speed-3.04)<1e-6),'MEASURED_SETTLED_REFERENCE_BURST_SPEED');
  exercised.push({kind:record.kind,uuid:record.uuid,samples:observations.length,phases:[...phases],plannedSegments:points.length-1});
 }
 return {...common,status:failures.length?'FAIL':'PASS',failures,exercised,scope:'DEVELOPMENT_NATURAL_COMMITTED_PROFILE_ONLY',
  unexercised:['BURST','CURVE','LOB'].filter(k=>!exercised.some(p=>p.kind===k)),cueReadability:'NOT_RUN',lateClientTracking:'NOT_RUN',instantiatedReload:'NOT_RUN'};
}
