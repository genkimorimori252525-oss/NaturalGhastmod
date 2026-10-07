import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeSwimming} from './swimming-results.mjs';
const trace=()=>Array.from({length:600},(_,tick)=>{
 const angle=tick*.012,x=12*Math.cos(angle),z=12*Math.sin(angle),y=6+2*Math.sin(angle);
 const previous=tick===0?{x,y,z}:{x:12*Math.cos(angle-.012),y:6+2*Math.sin(angle-.012),z:12*Math.sin(angle-.012)};
 const vx=x-previous.x,vy=y-previous.y,vz=z-previous.z;
 return {tick,x,y,z,vx,vy,vz,speed:Math.hypot(vx,vy,vz),noAI:false,collisionFree:true,width:4,height:4,health:10,
  targetUuid:'player',targetType:'minecraft:player',lineOfSight:true,intent:'MOVE',primitive:'DRIFT',context:'OPEN_AIR',
  regionGeneration:1,regionReason:'ACQUIRED',regionX:0,regionY:6,regionZ:0,radiusX:20,radiusY:8,radiusZ:20,inCombatRegion:true};
});
test('continuous broad physical swimming passes its separate scope',()=>assert.equal(analyzeSwimming(trace(),'player').status,'PASS'));
test('tiny drift and vertical stillness do not establish broad swimming',()=>{
 const rows=trace().map(r=>({...r,x:0,y:6,z:r.z*.1,vx:0,vy:0,vz:r.vz*.1,speed:Math.abs(r.vz*.1)}));
 assert(analyzeSwimming(rows,'player').failures.includes('SWIMMING_SPAN_INSUFFICIENT'));
 assert(analyzeSwimming(rows,'player').failures.includes('VERTICAL_SWIMMING_NOT_OBSERVED'));
});
test('region drift within one generation is rejected',()=>{const rows=trace();rows[100].regionX=.1;assert(analyzeSwimming(rows,'player').failures.includes('REGION_MOVED_WITHOUT_RESELECTION'));});
test('changed region dimensions cannot silently pass',()=>{const rows=trace();rows[100].radiusX=5;assert(analyzeSwimming(rows,'player').failures.includes('REGION_SIZE_MISMATCH'));});
test('collision gaps and invented motion are rejected',()=>{
 for(const change of [r=>{r.collisionFree=false;},r=>{r.tick+=2;},r=>{r.speed=.6;},r=>{r.vx=NaN;}]){
  const rows=trace();change(rows[100]);assert.equal(analyzeSwimming(rows,'player').status,'FAIL');
 }
});
test('labels without actual movement cannot pass',()=>{
 const rows=trace().map(r=>({...r,x:0,y:6,z:0,vx:0,vy:0,vz:0,speed:0}));assert.equal(analyzeSwimming(rows,'player').status,'FAIL');
});
test('missing target or region cannot pass',()=>{
 assert.equal(analyzeSwimming(trace().map(r=>({...r,targetUuid:null})),'player').status,'FAIL');
 assert.equal(analyzeSwimming(trace().map(r=>({...r,regionGeneration:undefined})),'player').status,'FAIL');
});
