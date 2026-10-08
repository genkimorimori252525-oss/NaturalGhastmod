export const chargeResponseScope='CONTROLLED_NEW_TARGET_CHARGE_RESPONSE_NOT_PLAYER_INPUT_OR_NATURAL_SELECTION';
const finite=v=>v&&['x','y','z'].every(k=>Number.isFinite(v[k])),horizontal=(a,b)=>Math.hypot(a.x-b.x,a.z-b.z),same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
export function analyzeChargeResponse(result,request){
 const failures=[],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===chargeResponseScope&&result.status==='PASS'&&result.nonce===request?.nonce&&result.canonicalPlayer===true&&result.fixtureSubjectAbsent===true&&result.canonicalOverrides===false&&result.samples>0&&result.samples<=430&&Array.isArray(result.errors)&&!result.errors.length,'AUTHORITY');
 const actor=result?.actors,rows=result?.rows??[],shots=result?.shots??[],joins=result?.joins??[],clients=result?.clientRows??[],cleanup=result?.cleanup??[];
 if(!actor||!rows.length)return {status:'FAIL',scope:chargeResponseScope,failures:[...failures,'ACTORS_OR_ROWS']};
 check(actor.privateControlledGoals===true&&actor.targetMovement==='NEW_COW_NATIVE_NAVIGATION_1.4_TWO_WAYPOINTS_PER_CHARGE'&&actor.width===4&&actor.height===4&&finite(actor.position)&&actor.uuid!==actor.targetUuid,'PRIVATE_ACTORS');
 check(joins.length===1&&joins[0].uuid===actor.uuid&&joins[0].id===actor.id&&joins[0].renderer==='com.genki.soutoughast.client.renderer.SoutouGhastRenderer'&&clients.length===4&&clients.every(x=>x.uuid===actor.uuid&&x.id===actor.id),'CLIENT_ADMISSION');
 check(rows.length<=430&&same(actor.region?.radii,{x:20,y:8,z:20})&&actor.region?.generation===1,'BOUNDS');
 let pending=false,qualified=false,onset=-1,persistent=0,origin=null,chargeTick=-1,stationary=0,emissions=0;const history=[],cues=[];
 for(const [i,r] of rows.entries()){
  check(r.uuid===actor.uuid&&r.id===actor.id&&r.loaded===true&&r.registered===true&&r.visible===true&&r.targetHealth>0&&r.navigationCalls>=0&&r.navigationCalls<=6,'NATIVE_IDENTITY');
  check(r.goalTick===i+1&&(i===0||r.tick===rows[i-1].tick+1),'CLOCK');
  if(!finite(r.position)||!finite(r.observedTarget)||!finite(r.targetNow)){check(false,'FINITE_GEOMETRY');continue;}
  check(same(r.region,actor.region)&&['x','y','z'].every(k=>Math.abs(r.position[k]-actor.position[k])<1e-6)&&same(r.observedTarget,r.targetNow),'OBSERVED_GEOMETRY');
  const step=i?horizontal(r.observedTarget,rows[i-1].observedTarget):-1;
  if(r.phase==='CHARGE'){
   if(r.chargeTick===0){pending=stationary>=10&&step>=0&&step<.025;qualified=false;onset=-1;persistent=0;origin=null;chargeTick=0;}
   else if(pending){
    if(r.chargeTick!==chargeTick+1||!qualified&&r.chargeTick>13){pending=false;qualified=false;onset=-1;}
    else{
     chargeTick=r.chargeTick;
     if(!qualified){
      if(step>=.15-1e-8){if(onset<0&&chargeTick<=10){onset=chargeTick;origin=rows[i-1].observedTarget;}if(onset>=0){persistent++;qualified=persistent>=3&&horizontal(r.observedTarget,origin)>=.6-1e-8;}}
      else{onset=-1;persistent=0;origin=null;}
      if(onset<0&&chargeTick>10){pending=false;qualified=false;onset=-1;}
     }
    }
   }
  }else{
   if(r.emitted){check(r.fire===true&&r.phase==='RECOVER'&&r.chargeTick===0,'EMISSION_PHASE');emissions++;if(pending&&qualified){if(history.length===3)history.shift();history.push({onset,tick:r.tick});}}
   pending=false;qualified=false;onset=-1;
  }
  while(history.length&&r.tick-history[0].tick>=600)history.shift();
  const repeated=history.some((x,a)=>history.some((y,b)=>a<b&&Math.abs(x.onset-y.onset)<=3));
  check(r.history===history.length&&r.repeated===repeated&&r.onset===onset&&r.qualified===qualified&&r.fired===emissions,'OBSERVATION_HISTORY');
  if(r.phase==='CHARGE'&&r.chargeTick===19){
   cues.push(r);check(Number.isFinite(r.range)&&Number.isFinite(r.variation)&&r.variation>=0&&r.variation<1&&['OPEN_AIR','SEMI_OPEN','CONFINED'].includes(r.context),'CUE_CONTEXT');
   check(['STANDARD','BURST','CURVE','LOB'].includes(r.withHistory)&&['STANDARD','BURST','CURVE','LOB'].includes(r.withoutHistory)&&(r.choice===r.withHistory||r.choice==='STANDARD'&&r.rejection!=='NONE'),'CUE_SELECTION');
   check(r.withHistory!=='BURST'||r.range>=47&&r.context!=='CONFINED','BURST_REACTION_BUDGET');
  }
  stationary=step>=0&&step<.025?Math.min(10,stationary+1):0;
 }
 check(emissions===3&&shots.length===3&&new Set(shots.map(x=>x.uuid)).size===3&&shots.every(x=>x.origin===actor.uuid&&x.owner===actor.uuid)&&shots.every(x=>rows.some(r=>r.emitted&&r.tick===x.tick)),'NATIVE_EMISSIONS');
 check(cues.length===3&&cues[2].history>=2&&cues[2].repeated===true&&rows.at(-1)?.history>=2&&rows.at(-1)?.repeated===true,'REPEATED_RESPONSE');
 const owned=[actor.uuid,actor.targetUuid,...shots.map(x=>x.uuid)];check(cleanup.length===owned.length&&new Set(cleanup.map(x=>x.uuid)).size===owned.length&&cleanup.every(x=>owned.includes(x.uuid)&&['DISCARDED_PRIVATE_ACTOR','ALREADY_REMOVED'].includes(x.status)),'OWNED_CLEANUP');
 return {status:failures.length?'FAIL':'PASS',scope:chargeResponseScope,failures:[...new Set(failures)],serverRows:rows.length,clientRows:clients.length,emissions,cues:cues.map(x=>({tick:x.tick,history:x.history,repeated:x.repeated,choice:x.choice,withHistory:x.withHistory,withoutHistory:x.withoutHistory}))};
}
