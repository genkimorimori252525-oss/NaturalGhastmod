import test from 'node:test';
import assert from 'node:assert/strict';
import {waitForFlightAction} from './flight-owner.mjs';
test('verified journal does not bypass the independently published next-action state',async()=>{
 const states=['open-00','open-00','open-01'];let reads=0;
 const state=await waitForFlightAction({inspect:async()=>({nextActionId:states[Math.min(reads++,2)]}),selectedActionId:'open-01',timeoutMs:100,pollMs:1});
 assert.equal(state.nextActionId,'open-01');assert.equal(reads,3);
});
test('unreconciled owner state times out without dispatching another operation',async()=>{
 await assert.rejects(waitForFlightAction({inspect:async()=>({nextActionId:'old'}),selectedActionId:'next',timeoutMs:5,pollMs:1}),/OWNER_NEXT_ACTION_NOT_READY/);
});
