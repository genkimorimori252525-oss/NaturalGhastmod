/** Finite selected-subject supplement, never whole-room or subjective feint acceptance. */
export function analyzeTactics(rows){
 if(!Array.isArray(rows)||!rows.length)throw Error('Measured tactics trace required');
 const failures=[],require=(ok,message)=>{if(!ok)failures.push(message);};
 const phases=['TELEGRAPH','COMMIT','REVEAL','BRAKE','RETURN'],actions=['DRIFT','FALSE_APPROACH','FALSE_RETREAT','LATERAL_FAKE','VERTICAL_FAKE'];
 require(rows.length>=300,'TRACE_TOO_SHORT');
 require(rows.every(r=>Number.isSafeInteger(r.tick)&&actions.includes(r.tacticalAction)&&['IDLE',...phases].includes(r.tacticalPhase)&&typeof r.tacticalCommitted==='boolean'),'TACTICAL_STATE_INVALID');
 require(rows.slice(1).every((r,i)=>r.tick===rows[i].tick+1),'TRACE_GAP');
 require(rows.every(r=>r.tacticalCommitted===['COMMIT','REVEAL','BRAKE','RETURN'].includes(r.tacticalPhase)),'COMMIT_FLAG_MISMATCH');
 require(rows.every(r=>r.tacticalTiming===(r.tacticalPhase==='IDLE'?'AT_ANCHOR':r.tacticalPhase==='TELEGRAPH'?'BEFORE_COMMIT':r.tacticalPhase==='COMMIT'?'ON_COMMIT':r.tacticalPhase==='RETURN'?'ON_RETURN':'AFTER_REVEAL')),'TIMING_SLOT_MISMATCH');
 require(rows.every(r=>(r.tacticalAction==='DRIFT')===(r.tacticalPhase==='IDLE')),'RECIPE_PHASE_MISMATCH');
 const ordinary=rows.filter(r=>r.tacticalPhase==='IDLE'),active=rows.filter(r=>r.tacticalPhase!=='IDLE');
 require(ordinary.length>rows.length*.5,'NORMAL_SWIMMING_NOT_DOMINANT');
 require(active.every(r=>r.inCombatRegion===true&&r.context!=='GROUND_FORCED'),'MANEUVER_OUTSIDE_FEASIBLE_REGION');
 require(new Set(rows.filter(r=>r.regionGeneration>0).map(r=>r.regionGeneration)).size===1,'REGION_NOT_RETAINED');
 let completed=0,group=[];const recipes=[];
 function inspect(){
  if(group.length===84&&phases.every((phase,i)=>group.filter(r=>r.tacticalPhase===phase).length===[24,8,24,12,16][i])&&
    group.every((r,i)=>r.tacticalPhase===phases[i<24?0:i<32?1:i<56?2:i<68?3:4])&&new Set(group.map(r=>r.tacticalAction)).size===1){
   if(group.filter(r=>r.tacticalPhase==='REVEAL'&&Number.isFinite(r.speed)&&r.speed>.005).length>=12&&group.slice(0,24).every(r=>r.lineOfSight===true)){
    completed++;recipes.push({action:group[0].tacticalAction,startTick:group[0].tick,endTick:group.at(-1).tick});
   }
  }
  group=[];
 }
 for(const row of rows){if(row.tacticalPhase==='IDLE')inspect();else group.push(row);}inspect();
 require(completed>0,'COMPLETE_MOVING_RECIPE_NOT_OBSERVED');
 return {status:failures.length?'FAIL':'PASS',failures,samples:rows.length,ordinarySamples:ordinary.length,maneuverSamples:active.length,completedRecipes:completed,recipes,phases,
  scope:'STATIC_PLAYER_OBSERVED_TACTICAL_MOVEMENT',limitations:['Only measured recipes are covered; subjective deception/readability, moving Player, LOS cancellation, ground, attacks and multiplayer require separate trials.']};
}
