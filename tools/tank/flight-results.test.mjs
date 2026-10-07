import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeFlight} from './flight-results.mjs';
test('idle evidence cannot establish flight or ordinary player acquisition',()=>{
 const rows=Array.from({length:120},(_,tick)=>({tick,noAI:false,collisionFree:true,x:9.5,y:230,z:9.5,vx:0,vy:0,vz:0,speed:0,targetUuid:null,targetType:null,range:6,primitive:'HOLD',context:'CONFINED'}));
 const result=analyzeFlight(rows,'player');assert.equal(result.status,'FAIL');assert(result.failures.includes('TARGET_ACQUISITION_NOT_OBSERVED'));assert(result.failures.includes('FLIGHT_DISPLACEMENT_INSUFFICIENT'));
});
test('unsafe, discontinuous or missing trace never passes',()=>{
 const rows=[{tick:1,noAI:false,collisionFree:false,x:1,y:2,z:3,vx:1,vy:0,vz:0,speed:1,targetUuid:'player',targetType:'minecraft:player',range:28,primitive:'DRIFT',context:'OPEN_AIR'},
 {tick:4,noAI:false,collisionFree:true,x:20,y:2,z:3,vx:0,vy:0,vz:0,speed:0,targetUuid:'player',targetType:'minecraft:player',range:28,primitive:'HOLD',context:'OPEN_AIR'}];
 const result=analyzeFlight(rows,'player');assert.equal(result.status,'FAIL');assert(result.failures.includes('BODY_COLLISION_OR_INACTIVE'));assert(result.failures.includes('TRACE_GAP'));assert(result.failures.includes('SPEED_CAP_EXCEEDED'));
 assert.throws(()=>analyzeFlight([],null));
});
