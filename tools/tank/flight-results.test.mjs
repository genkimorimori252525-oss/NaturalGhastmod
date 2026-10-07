import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeFlight} from './flight-results.mjs';
test('contiguous visible frontal arrival, gradual braking and moving drift establish the stated scope',()=>{
 let z=6;
 const rows=Array.from({length:240},(_,tick)=>{
  const speed=tick<=5?tick*.1:tick<=40?.5:tick<=55?Math.max(0,.5-(tick-40)*.035):tick>=100&&tick<=107?.08:0;
  z+=speed;
  return {tick,noAI:false,collisionFree:true,x:0,y:6,z,vx:0,vy:0,vz:speed,speed,
   targetUuid:'player',targetType:'minecraft:player',range:Math.hypot(6,z),lineOfSight:true,inFrontalVolume:z>=22,
   primitive:tick>=100&&tick<=107?'DRIFT':tick>40&&tick<=55?'BRAKE':speed?'WITHDRAW':'HOLD',context:'OPEN_AIR'};
 });
 const result=analyzeFlight(rows,'player');assert.equal(result.status,'PASS',result.failures.join(','));
});
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
test('unseen rearward movement, instant stop and a stationary DRIFT label do not prove behavior',()=>{
 const rows=Array.from({length:600},(_,tick)=>{const moving=tick<=40,z=-6-Math.min(tick,40)*.5;
  return {tick,noAI:false,collisionFree:true,x:0,y:6,z,vx:0,vy:0,vz:moving?-.5:0,speed:moving?.5:0,
   targetUuid:'player',targetType:'minecraft:player',range:Math.hypot(6,z),lineOfSight:false,inFrontalVolume:false,
   primitive:tick===599?'DRIFT':moving?'RETURN':'HOLD',context:'OPEN_AIR'};});
 const result=analyzeFlight(rows,'player');assert.equal(result.status,'FAIL');
 assert(result.failures.includes('VISIBLE_FRONTAL_ARRIVAL_NOT_OBSERVED'));
 assert(result.failures.includes('CONTINUOUS_BRAKING_NOT_OBSERVED'));
 assert(result.failures.includes('ACTUAL_DRIFT_NOT_OBSERVED'));
});
