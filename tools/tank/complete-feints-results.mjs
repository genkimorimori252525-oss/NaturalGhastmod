export const completeFeintsScope='EXPLICIT_NEW_BOSS_COMPOSER_NATIVE_NOT_NATURAL_SELECTION_OR_HUMAN_READABILITY';
const names=['PASS_BY_FAKE','DOUBLE_FAKE','ABORT_FAKE'];
const sequences=[['TELEGRAPH','COMMIT','BRAKE','REVEAL','BRAKE','RETURN'],['TELEGRAPH','COMMIT','BRAKE','REVEAL','BRAKE','REVEAL','BRAKE','RETURN'],['TELEGRAPH','BRAKE','RETURN']];
const axes=['x','y','z'],finite=v=>v&&axes.every(k=>Number.isFinite(v[k])),distance=(a,b)=>Math.hypot(...axes.map(k=>a[k]-b[k])),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export function analyzeCompleteFeints(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===completeFeintsScope&&result.status==='PASS'&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=240&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 const cases=result?.cases??[],rows=result?.rows??[],joins=result?.joins??[],clients=result?.clientRows??[],cleanup=result?.cleanup??[];
 check(cases.length===3&&new Set(cases.map(x=>x.uuid)).size===3&&joins.length===3&&rows.length<=640&&clients.length<=640,'ENTITY_SET');
 const owned=cases.flatMap(x=>[x.uuid,x.targetUuid]);
 check(owned.length===6&&new Set(owned).size===6&&cleanup.length===6&&new Set(cleanup.map(x=>x.uuid)).size===6&&cleanup.every(x=>owned.includes(x.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(x.status)),'CLEANUP');
 for(const [index,name] of names.entries()){
  const c=cases.find(x=>x.name===name),trace=rows.filter(x=>x.name===name),join=joins.filter(x=>x.name===name),client=clients.filter(x=>x.name===name);
  if(!c||!trace.length){check(false,'CASE_'+name);continue;}
  check(c.privateControlledGoal===true&&c.width===4&&c.height===4&&c.clearBody===true&&c.wallRejected===true&&c.unloadedRejected===true,'FULL_BODY_'+name);
  check(join.length===1&&join[0].uuid===c.uuid&&join[0].id===c.id&&join[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&client.length>=4&&client.every(x=>x.uuid===c.uuid&&x.id===c.id),'CLIENT_'+name);
  check(finite(c.region?.center)&&same(c.region.radii,{x:20,y:8,z:20})&&c.region.generation===1,'BROAD_REGION_'+name);
  const groups=[];
  for(const [i,r] of trace.entries()){
   if(!groups.length||groups.at(-1).phase!==r.phase)groups.push({phase:r.phase,rows:[]});groups.at(-1).rows.push(r);
   const vectors=[r.position,r.before,r.velocity,r.velocityBefore,r.observedTarget,r.waypoint];
   if(!vectors.every(finite)){check(false,'FINITE_'+name);continue;}
   check(r.uuid===c.uuid&&r.id===c.id&&r.registered===true&&r.loaded===true&&r.blocked===false,'NATIVE_BODY_'+name);
   check(r.goalTick===i+1&&r.goalTick<=160&&(i===0||r.tick===trace[i-1].tick+1),'CLOCK_'+name);
   check(same(r.region,c.region)&&same(r.observedTarget,trace[0].observedTarget)&&axes.reduce((sum,k)=>sum+((r.position[k]-c.region.center[k])/c.region.radii[k])**2,0)<=1+1e-6,'FROZEN_REGION_GEOMETRY_'+name);
   check(axes.every(k=>Math.abs(r.position[k]-r.before[k]-r.velocity[k])<1e-6)&&Math.hypot(...axes.map(k=>r.velocity[k]))<=.650001&&distance(r.velocity,r.velocityBefore)<=Math.hypot(.11,.045)+1e-6&&(i===0||distance(r.before,trace[i-1].position)<1e-6&&distance(r.velocityBefore,trace[i-1].velocity)<1e-6),'NATIVE_MOTION_'+name);
   check(r.completed===(i===trace.length-1)&&r.committed===(name!=='ABORT_FAKE'&&r.goalTick>24),'COMMIT_COMPLETION_'+name);
  }
  check(same(groups.map(x=>x.phase),sequences[index])&&groups[0]?.rows.length===24&&groups.filter(x=>x.phase==='BRAKE').every(x=>x.rows.length===12)&&groups.at(-1)?.rows.length===16,'SEQUENCE_'+name);
  if(name==='ABORT_FAKE')check(trace.length===52&&!trace.some(x=>x.committed),'ABORT_BEFORE_COMMIT');
  if(name==='PASS_BY_FAKE'){
   const target=trace[0].observedTarget,start=c.position,dx=target.x-start.x,dz=target.z-start.z,length=Math.hypot(dx,dz),tx=dx/length,tz=dz/length;
   const plane=p=>(p.x-target.x)*tx+(p.z-target.z)*tz,side=p=>(p.x-start.x)*(-tz)+(p.z-start.z)*tx;
   const first=groups[0]?.rows[0]?.waypoint,reveal=groups.find(x=>x.phase==='REVEAL');
   check(length>=6&&first&&plane(first)>=3.99&&Math.abs(side(first))>=5.99&&trace.some(x=>x.phase==='COMMIT'&&plane(x.position)>2)&&reveal&&Math.abs(side(reveal.rows.at(-1).position))-Math.abs(side(first))>=2.5,'ACTUAL_PASS_THEN_BEND');
  }
  if(name==='DOUBLE_FAKE'){
   const reveal=groups.filter(x=>x.phase==='REVEAL'),target=trace[0].observedTarget,start=c.position,dx=target.x-start.x,dz=target.z-start.z,len=Math.hypot(dx,dz),side=p=>(p.x-start.x)*(-dz/len)+(p.z-start.z)*(dx/len);
   check(reveal.length===2&&side(reveal[0].rows[0].waypoint)*side(reveal[1].rows[0].waypoint)<0&&Math.min(...trace.map(x=>side(x.position)))<-3&&Math.max(...trace.map(x=>side(x.position)))>3,'ACTUAL_DOUBLE_REVERSAL');
  }
 }
 return {status:failures.length?'FAIL':'PASS',scope:completeFeintsScope,failures:[...new Set(failures)],cases:cases.length,serverRows:rows.length,clientRows:clients.length};
}
