import test from 'node:test';
import assert from 'node:assert/strict';
import {planOverheadWindow,remainingWindowBudget} from './window-budget.mjs';
const ready={status:'READY',leaseCheck:{status:'SUFFICIENT',requiredMs:65000,remainingMs:80000}};
test('fixed900tick/50second window reserves15seconds and never renews or extends',()=>{
 const plan=planOverheadWindow(ready,1000);assert.equal(plan.maxTicks,900);assert.equal(plan.wallDeadlineEpochMs,51000);
 assert.deepEqual(remainingWindowBudget(plan,21000),{experimentMs:30000,finalizationMs:5000,cleanupMs:5000,marginMs:5000});
 assert.equal(remainingWindowBudget(plan,61000).experimentMs,0);assert.equal(plan.wallDeadlineEpochMs,51000);
});
test('unknown, insufficient, mismatched or nonready lease cannot start the window',()=>{
 for(const input of [{}, {...ready,status:'BLOCKED'}, {...ready,leaseCheck:{...ready.leaseCheck,remainingMs:64000}}, {...ready,leaseCheck:{...ready.leaseCheck,remainingMs:null}}, {...ready,leaseCheck:{...ready.leaseCheck,requiredMs:25000}}])assert.throws(()=>planOverheadWindow(input,1000));
});
