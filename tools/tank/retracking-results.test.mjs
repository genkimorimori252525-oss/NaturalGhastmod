import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeRetracking,retrackingScope} from './retracking-results.mjs';
const names=['BURST','CURVE','LOB','BOMB','DEFLECTED_CURVE','STANDARD','GROUND'];
function fixture(){
 const result={scope:retrackingScope,status:'PASS',nonce:'n',canonicalPlayer:true,overrides:false,samples:20,errors:[],cleanup:[],spawns:[],joins:[],clientRows:[],serverRows:[],stops:[],absences:[],readds:[],deflection:{uuid:'u4',owner:'player',path:'curve',px:.1,py:0,pz:0,vx:1,vy:0,vz:0,synthetic:true}};
 for(const [i,name] of names.entries()){
  const profile=i<5,norm=name==='DEFLECTED_CURVE';const base={name,uuid:'u'+i,id:100+i,owner:'boss',index:0,age:0,path:norm?'curve':profile?name:'',normalized:false,inWater:false,x:0,y:0,z:0,vx:1,vy:0,vz:0,px:0,py:0,pz:0};
  for(let age=1;age<=12;age++)result.serverRows.push({...base,tick:age,index:profile?(norm?Math.min(age,4):age):0,age,generation:age>=7?1:0,registered:true,alive:true,loaded:true,owner:norm&&age>=4?'player':'boss',normalized:norm&&age>=4,px:norm&&age>=4?.1:0});
  result.stops.push({...base,tick:5,epoch:100,owned:true,remaining:20,normalized:norm});result.absences.push({...base,epoch:110,oldRemoved:true,idAbsent:true});result.readds.push({...base,tick:7,epoch:120,registered:true});
  for(let generation=0;generation<2;generation++){
   const spawn={...base,generation,index:profile?(norm&&generation?4:generation?7:0):0,age:generation?7:0,tick:generation?7:0,epoch:generation?120:90,owner:norm&&generation?'player':'boss',normalized:norm&&generation===1,px:norm&&generation?.1:0};
   result.spawns.push(spawn);result.joins.push({...spawn,renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer',distinct:generation===1,epoch:generation?125:95});
   let velocity=1,position=0;
   const count=norm&&!generation?8:4;
   for(let j=0;j<count;j++){
    const normalized=norm&&(generation===1||j>=4),row={...spawn,frame:j+1,index:profile?(norm&&normalized?(generation?4:4):spawn.index+j+1):0,x:position+1,normalized,owner:normalized?'player':spawn.owner,px:normalized?.1:0,oldRemoved:generation===1};
    row.before={...row,x:position,index:profile&&!normalized?row.index-1:row.index,dx:1,dy:0,dz:0};
    if(normalized){row.before.vx=velocity;row.x=position+velocity;velocity=(velocity+.1)*Math.fround(.95);row.vx=velocity;}position=row.x;result.clientRows.push(row);
   }
  }
 }
 return {result,request:{nonce:'n'}};
}
test('accept actual removal, ticking server identity and one distinct native re-track',()=>{const f=fixture();assert.deepEqual(analyzeRetracking(f.result,f.request).failures,[]);});
for(const [name,mutate] of [
 ['wrong authority',f=>f.result.nonce='wrong'],['duplicate native readd',f=>f.result.readds.push(f.result.readds[0])],
 ['missing stop tracking',f=>f.result.stops.pop()],['unowned removal',f=>f.result.stops[0].owned=false],
 ['client old object alive',f=>f.result.absences[0].oldRemoved=false],['client native ID still present',f=>f.result.absences[0].idAbsent=false],
 ['readd precedes absence',f=>f.result.readds[0].epoch=105],['same client instance reused',f=>f.result.joins.find(x=>x.generation===1).distinct=false],
 ['changed server ID',f=>f.result.serverRows[0].id++],['server genuinely removed',f=>f.result.serverRows[0].registered=false],
 ['server clock rollback',f=>f.result.serverRows[3].age=0],['stale respawn clock',f=>f.result.spawns.find(x=>x.generation===1).index=0],
 ['hidden cleanup failure',f=>f.result.cleanup.push({status:'FAIL'})],['missing continuation',f=>f.result.clientRows=f.result.clientRows.filter(x=>!(x.name==='GROUND'&&x.generation===1))],
 ['path replacement',f=>f.result.joins.find(x=>x.generation===1).path='new'],['missing current state',f=>f.result.serverRows=f.result.serverRows.filter(x=>!(x.name==='CURVE'&&x.tick===7))],
 ['insufficient lifetime',f=>f.result.stops[0].remaining=3],['unloaded server entity',f=>f.result.serverRows[0].loaded=false],['old object revived',f=>f.result.clientRows.find(x=>x.generation===1).oldRemoved=false],
 ['stale current native acceleration',f=>f.result.serverRows.find(x=>x.name==='BURST'&&x.tick===7).px=.1],
 ['stale current native velocity',f=>f.result.serverRows.find(x=>x.name==='BURST'&&x.tick===7).vx=4],
 ['stale current native position',f=>f.result.serverRows.find(x=>x.name==='BURST'&&x.tick===7).x=4]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeRetracking(f.result,f.request).status,'FAIL');});
