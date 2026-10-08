export const clientSyncScope='PASSIVE_NATIVE_TRACKER_SPAWN_DEFLECTION_NOT_RETRACK_OR_LATE_JOIN';
export function analyzeClientSync(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.status==='PASS'&&result.scope===clientSyncScope&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.overrides===false&&Array.isArray(result.errors)&&!result.errors.length&&result.samples>0&&result.samples<=240,'AUTHORITY');
 const names=['BURST','CURVE','LOB','BOMB','DEFLECTED_CURVE','STANDARD','GROUND'];
 const spawns=result?.spawns??[],joins=result?.joins??[],client=result?.clientRows??[],server=result?.serverRows??[];
 check(spawns.length===7&&joins.length===7&&new Set(spawns.map(s=>s.uuid)).size===7,'ENTITY_SET');
 check(client.length<=2048&&server.length<=1536,'ROW_BOUND');
 const numeric=['x','y','z','vx','vy','vz','px','py','pz','index'];
 const d=result?.deflection;
 for(const name of names){
  const s=spawns.find(s=>s.name===name),j=joins.filter(s=>s.name===name),c=client.filter(s=>s.name===name),t=server.filter(s=>s.name===name);
  if(!s||j.length!==1){check(false,'JOIN_'+name);continue;}const join=j[0];
  check(join.uuid===s.uuid&&join.path===s.path&&join.index===s.index&&join.normalized===s.normalized&&join.owner===s.owner&&numeric.every(k=>Number.isFinite(join[k])&&Math.abs(join[k]-s[k])<(k.startsWith('v')?.000126:1e-6)),'SPAWN_'+name);
  check(join.renderer==='net.minecraft.client.renderer.entity.ThrownItemRenderer','RENDERER_'+name);
  check(c.length>=4&&t.length>=4&&t.every(r=>r.uuid===s.uuid&&r.path===s.path),'CONTINUATION_'+name);
  let previous=join;let normalizedSeen=false,normalizedRows=[];
  for(const r of c){
   check(r.uuid===s.uuid&&r.path===s.path&&numeric.every(k=>Number.isFinite(r[k]))&&Number.isSafeInteger(r.frame),'CLIENT_ROW_'+name);
   if(previous.frame!==undefined)check(r.frame===previous.frame+1,'CLIENT_FRAME_'+name);
   check(r.index>=previous.index&&r.index-previous.index<=1,'CLIENT_CLOCK_'+name);
   const pre=r.before;
   check(pre&&pre.uuid===s.uuid&&pre.path===s.path&&numeric.every(k=>Number.isFinite(pre[k])),'CLIENT_BEFORE_'+name);
   if(pre?.path&&!pre.normalized&&!r.normalized)check(r.index===pre.index+1&&['x','y','z'].every(k=>Number.isFinite(pre['d'+k])&&Math.abs(r[k]-pre[k]-pre['d'+k])<.001),'CLIENT_NATIVE_STEP_'+name);
   if(name!=='DEFLECTED_CURVE')check(!r.normalized&&r.owner===s.owner,'CLIENT_OWNER_'+name);
   else{if(normalizedSeen)check(r.normalized&&r.index===previous.index,'NORMALIZED_CLOCK');if(r.normalized){
    normalizedSeen=true;normalizedRows.push(r);
    if(pre?.normalized&&d){
     const axes=['x','y','z'],inertia=Math.fround(.95);
     check(pre.inWater===false&&axes.every(k=>Math.abs(r[k]-pre[k]-pre['v'+k])<1e-6&&Math.abs(r['v'+k]-(pre['v'+k]+d['p'+k])*inertia)<1e-6),'NORMALIZED_NATIVE_STEP');
     let v=axes.map(k=>d['v'+k]),matches=false;
     for(let elapsed=0;elapsed<=Math.min(240,result.samples);elapsed++){
      if(axes.every((k,i)=>Math.abs(pre['v'+k]-v[i])<.0005))matches=true;
      v=v.map((value,i)=>(value+d['p'+axes[i]])*inertia);
     }
     check(matches,'AUTHORITATIVE_REDIRECT_VELOCITY');
    }else check(false,'NORMALIZED_PREDECESSOR');
   }}
   previous=r;
  }
  check(c.length&&Math.hypot(c.at(-1).x-join.x,c.at(-1).y-join.y,c.at(-1).z-join.z)>.1,'CLIENT_MOTION_'+name);
  if(name==='DEFLECTED_CURVE')check(d?.synthetic===true&&d.uuid===s.uuid&&d.path===s.path&&normalizedRows.length>=4&&normalizedRows.every(r=>r.owner===d.owner&&['px','py','pz'].every(k=>Math.abs(r[k]-d[k])<1e-9)),'DEFLECTION_SYNC');
 }
 return {status:failures.length?'FAIL':'PASS',scope:clientSyncScope,failures:[...new Set(failures)],entities:joins.length,clientRows:client.length,serverRows:server.length};
}
