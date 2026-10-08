import test from 'node:test';
import assert from 'node:assert/strict';
import {planOverheadWindow,planGroundWindow,planDomainWindow,remainingWindowBudget,finalRosterDeadline} from './window-budget.mjs';
const ready={status:'READY',leaseCheck:{status:'SUFFICIENT',requiredMs:65000,remainingMs:80000}};
test('Domain1200tick AND60second request retains closure reserve and rejects unknown/short lease',()=>{
 const input={status:'READY',leaseCheck:{status:'SUFFICIENT',requiredMs:75000,remainingMs:75000}};
 const plan=planDomainWindow(input,1000);assert.equal(plan.maxTicks,1200);assert.equal(plan.maxWallMs,60000);assert.equal(plan.reserveMs,15000);assert.equal(plan.wallDeadlineEpochMs,61000);
 assert.equal(plan.postDeathMaxTicks,200);assert(Object.isFrozen(plan));
 for(const bad of [{...input,status:'BLOCKED'},{...input,leaseCheck:{...input.leaseCheck,remainingMs:74999}},{...input,leaseCheck:{...input.leaseCheck,remainingMs:null}},ready])assert.throws(()=>planDomainWindow(bad,1000));
 assert.throws(()=>planDomainWindow(input,NaN));assert.equal(finalRosterDeadline(plan,61500),66000);
 assert.equal(remainingWindowBudget(plan,71000).experimentMs,0);assert.equal(plan.wallDeadlineEpochMs,61000);
});
test('Ground180ticks AND15seconds retains15second closure reserve',()=>{
 const input={...ready,leaseCheck:{...ready.leaseCheck,requiredMs:30000,remainingMs:40000}};
 const plan=planGroundWindow(input,1000);assert.equal(plan.maxTicks,180);assert.equal(plan.maxWallMs,15000);
 assert.equal(plan.wallDeadlineEpochMs,16000);assert.equal(plan.postDeathMaxTicks,0);assert.equal(plan.reserveMs,15000);
 assert.throws(()=>planGroundWindow({...input,leaseCheck:{...input.leaseCheck,remainingMs:29000}},1000));
 assert.throws(()=>planGroundWindow(ready,1000));
});
test('final roster shares5second finalization deadline and preserves cleanup+margin10seconds',()=>{
 const plan={wallDeadlineEpochMs:16000,reserveMs:15000};
 assert.equal(finalRosterDeadline(plan,17000),21000);
 assert.equal(finalRosterDeadline(plan,20500),21000);
 assert.throws(()=>finalRosterDeadline(plan,21000));
});
test('fixed900tick/50second window reserves15seconds and never renews or extends',()=>{
 const plan=planOverheadWindow(ready,1000);assert.equal(plan.maxTicks,900);assert.equal(plan.wallDeadlineEpochMs,51000);
 assert.deepEqual(remainingWindowBudget(plan,21000),{experimentMs:30000,finalizationMs:5000,cleanupMs:5000,marginMs:5000});
 assert.equal(remainingWindowBudget(plan,61000).experimentMs,0);assert.equal(plan.wallDeadlineEpochMs,51000);
});
test('unknown, insufficient, mismatched or nonready lease cannot start the window',()=>{
 for(const input of [{}, {...ready,status:'BLOCKED'}, {...ready,leaseCheck:{...ready.leaseCheck,remainingMs:64000}}, {...ready,leaseCheck:{...ready.leaseCheck,remainingMs:null}}, {...ready,leaseCheck:{...ready.leaseCheck,requiredMs:25000}}])assert.throws(()=>planOverheadWindow(input,1000));
});
