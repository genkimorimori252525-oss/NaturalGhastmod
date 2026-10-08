export const delayedOwnerScope='CONTROLLED_OWNER_AVAILABILITY_NOT_GENUINE_LATE_JOIN';
export function analyzeDelayedOwner(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===delayedOwnerScope&&result.status==='PASS'&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.overrides===false&&result.samples>0&&result.samples<=240&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 check(Array.isArray(result?.cleanup)&&result.cleanup.every(x=>x.status==='RESTORED'||x.status==='ALREADY_REMOVED'),'CLEANUP');
 const o=result?.ownerInitial,s=result?.ownerStop,a=result?.ownerAbsence,r=result?.ownerReadd,j=result?.ownerJoin;
 check(o&&[s,a,r,j].every(x=>x?.uuid===o.uuid&&x.id===o.id),'OWNER_IDENTITY');
 check(s?.owned===true&&a?.oldRemoved===true&&a.idAbsent===true&&r?.registered===true&&j?.distinct===true,'OWNER_NATIVE_LIFECYCLE');
 check(o?.epoch<=s?.epoch&&s?.epoch<=a?.epoch&&a?.epoch<r?.epoch&&r?.epoch<=j?.epoch&&r?.tick>s?.tick&&r.tick-s.tick<=40,'OWNER_ORDER');
 const names=['STANDARD','GROUND','CURVE','RETURNED_CURVE'],spawns=result?.spawns??[],joins=result?.joins??[],rows=result?.clientRows??[],server=result?.serverRows??[],d=result?.deflection,b=result?.bossReturn;
 check(spawns.length===4&&joins.length===4&&new Set(spawns.map(x=>x.uuid)).size===4&&rows.length<=2048&&server.length<=1536,'ENTITY_SET');
 check(d?.synthetic===true&&b?.synthetic===true&&d.uuid===b.uuid&&d.owner===request?.playerUuid,'PLAYER_DEFLECTION');
 check(b?.owner===o?.uuid&&a?.epoch<=b?.epoch&&b?.epoch<r?.epoch,'HIDDEN_BOSS_RETURN');
 for(const name of names){
  const spawn=spawns.find(x=>x.name===name),join=joins.filter(x=>x.name===name),trace=rows.filter(x=>x.name===name),native=server.filter(x=>x.name===name),hidden=trace.filter(x=>x.phase==='HIDDEN'),resolved=trace.filter(x=>x.phase==='RESOLVED');
  if(!spawn||join.length!==1){check(false,'JOIN_'+name);continue;}
  check(join[0].uuid===spawn.uuid&&join[0].id===spawn.id&&join[0].path===spawn.path&&join[0].renderer==='net.minecraft.client.renderer.entity.ThrownItemRenderer','ADMISSION_'+name);
  check(name==='RETURNED_CURVE'?spawn.uuid===b?.uuid&&trace.filter(x=>x.phase==='PLAYER'&&x.owner===d?.owner).length>=4:spawn.epoch>=a?.epoch&&spawn.epoch<r?.epoch&&join[0].owner==='','INITIAL_OWNER_'+name);
  check(hidden.length>=4&&hidden.every(x=>x.owner===''&&x.ownerAbsent===true&&x.epoch>=a?.epoch&&x.epoch<r?.epoch)&&(name!=='RETURNED_CURVE'||hidden.every(x=>x.epoch>=b?.epoch)),'ABSENT_OWNER_'+name);
  check(resolved.length>=4&&resolved.every(x=>x.owner===o?.uuid&&x.ownerAbsent===false&&x.epoch>=j?.epoch)&&resolved[0]?.epoch-j?.epoch<=1000,'RESOLVED_OWNER_'+name);
  check(native.length>=4&&native.every(x=>x.registered===true&&x.loaded===true&&x.alive===true),'NATIVE_ENTITY_'+name);
  for(let i=1;i<native.length;i++)check(native[i].tick===native[i-1].tick+1&&native[i].age===native[i-1].age+1,'SERVER_CLOCK_'+name);
  for(const x of [...hidden,...resolved]){
   const pre=x.before,axis=['x','y','z'];check(x.uuid===spawn.uuid&&x.id===spawn.id&&x.path===spawn.path&&pre?.uuid===spawn.uuid&&pre.path===spawn.path,'CONTINUITY_'+name);
   check(axis.every(k=>Number.isFinite(x[k])&&Number.isFinite(x['v'+k])&&Number.isFinite(x['p'+k])&&Number.isFinite(pre?.[k])&&Number.isFinite(pre?.['v'+k])),'FINITE_'+name);
   if(name==='CURVE')check(!x.normalized&&!pre?.normalized&&x.index===pre.index+1&&axis.every(k=>Math.abs(x[k]-pre[k]-pre['d'+k])<.001),'NATIVE_MOTION_'+name);
   else check((name!=='RETURNED_CURVE'||x.normalized&&pre?.normalized&&x.index===pre.index)&&pre?.inWater===false&&axis.every(k=>Math.abs(x[k]-pre[k]-pre['v'+k])<1e-6&&Math.abs(x['v'+k]-(pre['v'+k]+pre['p'+k])*(name==='GROUND'?1:Math.fround(.95)))<1e-6),'NATIVE_MOTION_'+name);
  }
 }
 return {status:failures.length?'FAIL':'PASS',scope:delayedOwnerScope,failures:[...new Set(failures)],entities:joins.length,clientRows:rows.length,serverRows:server.length};
}
