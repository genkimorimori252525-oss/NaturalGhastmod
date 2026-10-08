import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeClientSync,clientSyncScope} from './client-sync-results.mjs';
const names=['BURST','CURVE','LOB','BOMB','DEFLECTED_CURVE','STANDARD','GROUND'];
function fixture(){
 const request={nonce:'n'};
 const spawns=names.map((name,i)=>({name,uuid:'u'+i,owner:'boss',index:0,path:name.includes('CURVE')?'curve':i<4?name:'',normalized:false,inWater:false,x:0,y:0,z:0,vx:1,vy:0,vz:0,px:0,py:0,pz:0}));
 const joins=spawns.map(s=>({...s,renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer'}));
 const clientRows=spawns.flatMap(s=>Array.from({length:6},(_,i)=>({...s,frame:i+1,index:s.path?i+1:0,x:i+1,normalized:s.name==='DEFLECTED_CURVE'&&i>=2,owner:s.name==='DEFLECTED_CURVE'&&i>=2?'player':'boss',px:s.name==='DEFLECTED_CURVE'&&i>=2?.1:0}))).map(r=>r.name==='DEFLECTED_CURVE'&&r.normalized?{...r,index:2}:r);
 for(const row of clientRows)row.before={...row,x:row.x-1,index:row.path&&!row.normalized?row.index-1:row.index,dx:1,dy:0,dz:0};
 const serverRows=spawns.flatMap(s=>Array.from({length:6},(_,i)=>({...s,tick:10+i,index:s.path?i+1:0,x:i+1,alive:true})));
 const result={status:'PASS',scope:clientSyncScope,nonce:'n',errors:[],canonicalPlayer:true,overrides:false,samples:20,spawns,joins,clientRows,serverRows,deflection:{uuid:'u4',owner:'player',path:'curve',px:.1,py:0,pz:0,synthetic:true}};
 result.deflection.vx=1;result.deflection.vy=0;result.deflection.vz=0;
 let velocity=1,position=2;
 for(const row of clientRows.filter(r=>r.name==='DEFLECTED_CURVE'&&r.normalized)){
  row.before={...row,x:position,vx:velocity,vy:0,vz:0};position+=velocity;velocity=(velocity+.1)*.95;row.x=position;row.vx=velocity;row.vy=0;row.vz=0;
 }
 return {request,result};
}
test('accept bounded passive native spawn and deflection correlation',()=>{const f=fixture();assert.equal(analyzeClientSync(f.result,f.request).status,'PASS');});
test('allow known vanilla motion packet quantization after additional spawn data',()=>{const f=fixture();f.result.joins[0].vx+=.00012;assert.equal(analyzeClientSync(f.result,f.request).status,'PASS');});
for(const [label,mutate] of [
 ['wrong authority',f=>f.result.nonce='other'],['fake player',f=>f.result.canonicalPlayer=false],['override',f=>f.result.overrides=true],
 ['missing join',f=>f.result.joins.pop()],['duplicate join',f=>f.result.joins.push(f.result.joins[0])],
 ['path replacement',f=>f.result.clientRows[0].path='other'],['spawn clock reset',f=>f.result.joins[0].index=1],
 ['spawn velocity corruption',f=>f.result.joins[0].vx=4],['missing live continuation',f=>f.result.clientRows=f.result.clientRows.filter(r=>r.name!=='GROUND')],
 ['clock rewind',f=>f.result.clientRows[2].index=0],['invalid velocity',f=>f.result.clientRows[0].vx=NaN],
 ['stale normalization',f=>f.result.clientRows.filter(r=>r.name==='DEFLECTED_CURVE').forEach(r=>r.normalized=false)],
 ['stale owner',f=>f.result.clientRows.filter(r=>r.name==='DEFLECTED_CURVE').forEach(r=>r.owner='boss')],
 ['post-normalization special clock',f=>f.result.clientRows.filter(r=>r.name==='DEFLECTED_CURVE'&&r.normalized).at(-1).index++],
 ['stale acceleration',f=>f.result.clientRows.filter(r=>r.name==='DEFLECTED_CURVE').forEach(r=>r.px=0)],
 ['missing server entity',f=>f.result.serverRows=f.result.serverRows.filter(r=>r.name!=='BURST')],
 ['unregistered renderer',f=>f.result.joins[0].renderer='Unknown'],['native error',f=>f.result.errors.push('error')],
 ['unbounded rows',f=>f.result.clientRows=Array(2049).fill(f.result.clientRows[0])],
 ['missing native tick predecessor',f=>delete f.result.clientRows[0].before],['wrong native client displacement',f=>f.result.clientRows[0].before.dx=3],
 ['corrupt deflection velocity',f=>f.result.clientRows.filter(r=>r.normalized).forEach(r=>r.vx=4)],
 ['stale special velocity after deflection',f=>f.result.clientRows.filter(r=>r.normalized).forEach(r=>r.vy=.2)],
 ['wrong normalized displacement',f=>f.result.clientRows.find(r=>r.normalized).x+=1],
 ['self-consistent wrong redirect',f=>{const r=f.result.clientRows.find(r=>r.normalized);r.before.vy=.2;r.y=r.before.y+.2;r.vy=.2*Math.fround(.95);}],
 ['water changes native physics',f=>f.result.clientRows.find(r=>r.normalized).before.inWater=true]
])test('reject '+label,()=>{const f=fixture();mutate(f);assert.equal(analyzeClientSync(f.result,f.request).status,'FAIL');});
