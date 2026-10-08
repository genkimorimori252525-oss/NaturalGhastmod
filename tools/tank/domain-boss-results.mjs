const scope='EXPLICIT_ACTUAL_BOSS_DOMAIN_ABORT_INTEGRATION_NOT_NATURAL_ADMISSION';
/** Coverage checker only; caller must separately prove native source/material/closure provenance. */
export function analyzeDomainBoss(rows,result,request){
 const failures=[];const check=(ok,reason)=>{if(!ok)failures.push(reason);};
 check(request.maxTicks===1200&&request.maxWallMs===60000&&request.abortCombatTicks===40,'REQUEST_SCOPE');
 check(result?.scope===scope&&result?.nonce===request.nonce&&result?.status==='PASS'&&result?.failures?.length===0,'NATIVE_RESULT');
 check(rows.length>0&&rows.length<=1200&&rows.length===result?.samples,'SAMPLE_COUNT');
 check(rows.every((r,i)=>Number.isSafeInteger(r.tick)&&(!i||r.tick>rows[i-1].tick)),'MONOTONIC_TICKS');
 check(rows.every(r=>r.subjectUuid===request.subjectUuid&&r.playerUuid===request.playerUuid&&r.canonicalConnectedPlayer===true&&r.noAI===false&&r.width===4&&r.height===4),'PARTICIPANT_IDENTITY');
 check(rows.every(r=>[r.x,r.y,r.z,r.speed,r.playerHealth,r.groundShots].every(Number.isFinite)&&r.x>=2&&r.x<=50&&r.z>=2&&r.z<=50&&r.y>=224-.12&&r.y+4<=248&&r.speed>=0),'FINITE_ROOM_BODY');
 const combat=rows.filter(r=>r.phase==='COMBAT');
 check(combat.length===40&&combat.every(r=>r.supported===true&&r.groundPhase==='GROUNDED'&&r.playerHealth>0),'SUPPORTED_LIVE_40_COMBAT');
 const proof=j=>j?.allVerifiedTerminal===true&&j?.originalsLoadedCurrent===true&&Array.isArray(j.slots)&&j.slots.length>0&&j.slots.every(s=>s.phase==='VERIFIED_TERMINAL'&&s.originalsLoadedCurrent===true);
 check(rows.filter(r=>r.phase==='TAKEOFF').every(r=>proof(r.journals)),'DURABLE_BEFORE_TAKEOFF');
 check(rows.every((r,i)=>!['ENDING','EXIT_HOLD','TAKEOFF'].includes(r.phase)||r.offenseAllowed===false&&(!i||r.groundShots===rows[i-1].groundShots)),'CLEANUP_OFFENSE');
 check(rows.every(r=>['START','PLACING','LANDING','COMBAT','ENDING','EXIT_HOLD','TAKEOFF'].includes(r.phase)?r.majorActive===true:true),'SHARED_MAJOR_EXCLUSION');
 let cursor=-1;for(const phase of ['START','PLACING','LANDING','COMBAT','ENDING','TAKEOFF','AIR']){const next=rows.findIndex((r,i)=>i>cursor&&r.phase===phase);check(next>=0,'PHASE_'+phase);if(next>=0)cursor=next;}
 check(result?.entered===true&&result?.abortRequested===true&&result?.duplicateRejected===true&&result?.sawGround===true&&result?.sawTakeoff===true&&result?.combatTicks===40,'ABORT_AND_DUPLICATE_BOUNDARY');
 const last=rows.at(-1);check(last?.phase==='AIR'&&last?.groundPhase==='AIR'&&last?.majorActive===false&&proof(result?.journals),'RESTORED_EXIT');
 check(last?.tick===result?.endGameTime&&rows[0]?.tick===result?.startGameTime&&last?.tick-rows[0]?.tick<1200,'WINDOW_TICKS');
 check(combat.length>0&&last?.y>Math.min(...combat.map(r=>r.y))+2&&rows.some(r=>['LANDING','TAKEOFF'].includes(r.phase)&&r.speed>.02),'REAL_MOVEMENT');
 return {status:failures.length?'FAIL':'PASS',scope,samples:rows.length,combatSamples:combat.length,failures,limitations:['Explicit reliability entry/40tick abort only; natural selection/400tick combat/input/balance remain unaccepted.','Native provenance, shutdown and current original-world preservation require separate positive receipts.']};
}
