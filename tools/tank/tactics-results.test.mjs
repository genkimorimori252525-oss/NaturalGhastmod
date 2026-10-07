import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeTactics} from './tactics-results.mjs';
const phases=['TELEGRAPH','COMMIT','REVEAL','BRAKE','RETURN'];
function trace(){return Array.from({length:600},(_,tick)=>{
 const age=tick-250,phase=age<0||age>=84?'IDLE':age<24?'TELEGRAPH':age<32?'COMMIT':age<56?'REVEAL':age<68?'BRAKE':'RETURN';
 return {tick,tacticalAction:phase==='IDLE'?'DRIFT':'LATERAL_FAKE',tacticalPhase:phase,tacticalCommitted:age>=24&&age<84,
 tacticalTiming:phase==='IDLE'?'AT_ANCHOR':phase==='TELEGRAPH'?'BEFORE_COMMIT':phase==='COMMIT'?'ON_COMMIT':phase==='RETURN'?'ON_RETURN':'AFTER_REVEAL',
 speed:phase==='BRAKE'?.01:.1,inCombatRegion:true,lineOfSight:true,regionGeneration:1,context:'OPEN_AIR'};
});}
test('one contiguous finite committed recipe amid ordinary swimming supports only scoped tactics',()=>{
 const result=analyzeTactics(trace());assert.equal(result.status,'PASS');assert.equal(result.completedRecipes,1);
 assert.equal(result.ordinarySamples,516);assert.deepEqual(result.phases,phases);
});
test('idle-only, fabricated phase/commit, constant feints and stopped reveals cannot pass',()=>{
 for(const mutate of [rows=>rows.map(r=>({...r,tacticalAction:'DRIFT',tacticalPhase:'IDLE',tacticalCommitted:false,tacticalTiming:'AT_ANCHOR'})),
 rows=>rows.map(r=>({...r,tacticalCommitted:false})),rows=>rows.map(r=>({...r,tacticalPhase:'REVEAL'})),
 rows=>rows.map(r=>({...r,speed:0})),rows=>rows.map(r=>({...r,regionGeneration:r.tick}))])
  assert.equal(analyzeTactics(mutate(trace())).status,'FAIL');
});
