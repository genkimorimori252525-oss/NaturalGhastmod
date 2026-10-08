/** Finite supplementary lifecycle analysis. Simulated tests never become native acceptance. */
export function analyzeDomain(input,subjectUuid,playerUuid){
 const {rows=[],readiness,request,end,finalJournals}=input??{},failures=[],missing=[];
 const reject=(condition,reason)=>{if(!condition)failures.push(reason);};
 reject(request?.maxTicks===1200&&request?.maxWallMs===60000&&request?.reserveMs===15000,'DOMAIN_WINDOW_CONTRACT');
 reject(rows.length>0&&rows.length<=1200&&end?.samples===rows.length,'DOMAIN_BOUNDED_ROWS');
 reject(readiness?.verdict==='PASS'&&readiness.canonicalPlayer===true&&readiness.subjectUuid===subjectUuid&&readiness.playerUuid===playerUuid&&readiness.plannedCells===16341&&readiness.changedCells<=4096&&readiness.fullParticipantFit===true&&readiness.freshFloorCommitted===true&&readiness.admissionNotYetStarted===true,'DOMAIN_NATIVE_READINESS_MISSING_OR_LATE');
 const phases=new Set(['AIR','START','PLACING','LANDING','COMBAT','ENDING','TAKEOFF','EXIT_HOLD']);
 let lastTick=-Infinity,lastCount=null,generation=null;
 for(const row of rows){
  reject(Number.isSafeInteger(row.tick)&&row.tick>lastTick,'DOMAIN_ROW_ORDER');lastTick=row.tick;
  reject(row.uuid===subjectUuid&&row.playerUuid===playerUuid&&row.playerMode==='survival'&&row.noAI===false&&row.width===4&&row.height===4,'DOMAIN_REAL_PARTICIPANT_IDENTITY');
  reject(phases.has(row.domainPhase),'DOMAIN_UNKNOWN_PHASE');
  if(row.domainPhase!=='AIR'){
   reject(row.radiusX===20&&row.radiusY===8&&row.radiusZ===20,'DOMAIN_RETAINED_REGION_CHANGED');
   if(generation===null)generation=row.regionGeneration;else reject(row.regionGeneration===generation,'DOMAIN_REGION_REANCHORED_DURING_ACTION');
  }
  if(['START','PLACING','LANDING','ENDING','TAKEOFF','EXIT_HOLD'].includes(row.domainPhase)){
   reject(row.domainOffenseAllowed===false,'DOMAIN_CLEANUP_OR_ENTRY_OFFENSE');
   if(lastCount!==null)reject(row.groundFiredCount===lastCount,'DOMAIN_RELEASE_DURING_SUPPRESSED_PHASE');
  }
  if(Number.isSafeInteger(row.groundFiredCount))lastCount=row.groundFiredCount;else failures.push('DOMAIN_GROUND_COUNT_UNKNOWN');
  if(row.domainPhase==='TAKEOFF')reject(row.domainJournals?.allVerifiedTerminal===true&&row.domainJournals.originalsLoadedCurrent===true,'DOMAIN_TAKEOFF_WITHOUT_DURABLE_ORIGINAL_PROOF');
  if(row.domainPhase==='COMBAT')reject(row.domainGrounded===true&&row.targetUuid===playerUuid&&row.playerHealth>0,'DOMAIN_COMBAT_WITHOUT_SUPPORTED_LIVE_PARTICIPANT');
 }
 const observed=new Set(rows.map(r=>r.domainPhase));
 for(const phase of ['START','PLACING','LANDING','COMBAT','ENDING','TAKEOFF'])if(!observed.has(phase))missing.push('PHASE_'+phase+'_NOT_OBSERVED');
 const startRows=rows.filter(r=>r.domainPhase==='START'),placing=rows.find(r=>r.domainPhase==='PLACING');
 if(placing)reject(startRows.length>=59&&startRows[0]?.domainTicks===1&&startRows.at(-1)?.domainTicks===59&&placing.domainArmPlacement===true,'DOMAIN_COMPLETE_RESERVED_TELL_NOT_OBSERVED');
 const combatSamples=rows.filter(r=>r.domainPhase==='COMBAT').length;
 if(combatSamples<400)missing.push('FULL_400TICK_COMBAT_NOT_OBSERVED');
 if(!rows.some(r=>r.domainPhase==='AIR'&&r.domainReason==='EXIT_COMPLETE'))missing.push('RETURN_TO_AIR_NOT_OBSERVED');
 if(finalJournals?.allVerifiedTerminal!==true||finalJournals.originalsLoadedCurrent!==true)missing.push('DURABLE_FINAL_ORIGINALS_NOT_PROVEN');
 return {status:failures.length?'FAIL':missing.length?'PARTIAL':'PASS',scope:'DEVELOPMENT_NATURAL_DOMAIN_LIFECYCLE',samples:rows.length,combatSamples,failures:[...new Set(failures)],missing,limitations:['Static survival Player; natural injury/death.','Supplementary rows and same-Player frame are not fixed-cardinal visibility or genuine-input proof.','Moving counterplay, cues, balance, reload and late-client sync remain separate.']};
}
