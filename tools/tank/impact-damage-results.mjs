export const impactScope='EXPLICIT_NATIVE_IMPACT_DAMAGE_NOT_HUMAN_MELEE_OR_BALANCE';
const names=['LOB_BLOCK','BURST_MISS','STANDARD_COW','GROUND_COW','OWN_RETURN','DEFLECTION_RULES','ATTRIBUTION_REJECTION'];
/** Coverage only; caller separately attests pinned native material, artifacts and clean closure. */
export function analyzeImpactDamage(result,request){
 const failures=[];const check=(ok,why)=>{if(!ok)failures.push(why);};
 check(request?.maxTicks===240&&request?.maxWallMs===20000,'REQUEST_SCOPE');
 check(result?.scope===impactScope&&result?.nonce===request?.nonce&&result?.status==='PASS'&&result?.failures?.length===0&&result?.canonicalPlayer===true,'NATIVE_RESULT');
 const cases=result?.cases??[];check(cases.length===7&&new Set(cases.map(c=>c.name)).size===7&&names.every(n=>cases.some(c=>c.name===n)),'CASES');
 check(Number.isSafeInteger(result?.startGameTime)&&Number.isSafeInteger(result?.endGameTime)&&result.endGameTime>=result.startGameTime&&result.endGameTime-result.startGameTime<240,'TICK_WINDOW');
 for(const c of cases){check(c.ended===true&&c.checks?.productionRules===true&&Number.isFinite(c.initialHealth)&&Number.isFinite(c.finalHealth)&&c.finalHealth>=0&&c.finalHealth<=c.initialHealth&&c.impacts?.length<=4&&c.damage?.length<=24,'CASE_'+c.name);check(c.impacts?.every(i=>i.canceled===false),'CANCELED_'+c.name);}
 const get=n=>cases.find(c=>c.name===n),lob=get('LOB_BLOCK'),miss=get('BURST_MISS');
 check(lob?.impacts?.length===1&&lob.impacts[0].type==='BLOCK'&&lob.impacts[0].declared===true,'DECLARED_BLOCK_IMPACT');
 const executed=lob?.explosions??[],impact=lob?.impacts?.[0];
 check(impact?.impactResult==='DEFAULT'&&executed.some(e=>e.stage==='START'&&e.canceled===false&&e.tick===impact.tick&&e.directUuid===lob.uuid)&&executed.some(e=>e.stage==='DETONATE'&&e.tick===impact.tick&&e.directUuid===lob.uuid),'PERMITTED_BLOCK_EXECUTION');
 check(miss?.impacts?.length===0&&miss.finiteEnd===true,'FINITE_MISS');
 for(const [n,base] of [['STANDARD_COW',9],['GROUND_COW',3],['OWN_RETURN',20]]){
  const c=get(n),ds=c?.damage?.filter(e=>e.stage==='DAMAGE')??[],direct=ds.filter(e=>e.kind==='FIREBALL');
  check(c?.impacts?.length===1&&c.impacts[0].type==='ENTITY'&&c.impacts[0].victimUuid===c.victimUuid,'ENTITY_IMPACT_'+n);
  check(direct.length===1&&direct[0].amount===base&&direct[0].directUuid===c?.uuid&&direct[0].victimUuid===c?.victimUuid&&direct[0].ownerUuid===(n==='OWN_RETURN'?request.playerUuid:request.subjectUuid),'DIRECT_DAMAGE_'+n);
  check(c&&c.finalHealth<c.initialHealth&&ds.every(e=>Number.isFinite(e.amount)&&e.amount>0&&e.directUuid===c.uuid&&e.victimUuid===c.victimUuid&&e.ownerUuid===(n==='OWN_RETURN'?request.playerUuid:request.subjectUuid)),'APPLIED_DAMAGE_'+n);
  if(n==='OWN_RETURN'){check(c?.victimUuid===request.subjectUuid&&ds.length===1&&Math.abs(c.initialHealth-c.finalHealth-20)<1e-5,'RETURN_ONCE_NO_DOUBLE_HIT');check(c?.checks?.delayedDuplicateRejected===true&&c.checks.cooldownAtDuplicate===0&&c.checks.ownDirectVictim===true&&c.checks.nonImmuneDuplicate===true,'DEDICATED_RETURN_SUPPRESSION_AFTER_COOLDOWN');}
  else {const amount=ds.reduce((sum,e)=>sum+e.amount,0);check(c&&Math.abs(c.finalHealth-Math.max(0,c.initialHealth-amount))<1e-5,'HEALTH_BOOKKEEPING_'+n);}
 }
 for(const n of ['DEFLECTION_RULES','ATTRIBUTION_REJECTION']){const c=get(n);check(c?.impacts?.length===0&&(n==='ATTRIBUTION_REJECTION'?c.checks?.sourceImmunity===true&&c.damage?.every(e=>e.stage==='ATTACK'):c.damage?.length===0)&&c.initialHealth===c.finalHealth,'REJECTION_STATE_'+n);}
 return {status:failures.length?'FAIL':'PASS',scope:impactScope,cases:cases.length,failures,limitations:['Explicit native collisions and synthetic production API sources; not human melee/natural rally.','Normal direct base damage and explosion/total health are distinct; this is not balance acceptance.','Native provenance, current originals and closure require independent receipts; no restart/client/visual acceptance.']};
}
