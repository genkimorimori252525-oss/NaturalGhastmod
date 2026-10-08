import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeDelayedOwner,delayedOwnerScope} from './delayed-owner-results.mjs';
function fixture(){
 const owner={uuid:'boss',id:100,epoch:10,tick:1},result={scope:delayedOwnerScope,status:'PASS',nonce:'n',canonicalPlayer:true,overrides:false,samples:30,errors:[],cleanup:[],ownerInitial:owner,ownerStop:{...owner,epoch:20,tick:2,owned:true},ownerAbsence:{...owner,epoch:30,oldRemoved:true,idAbsent:true},ownerReadd:{...owner,epoch:80,tick:8,registered:true},ownerJoin:{...owner,epoch:90,distinct:true},deflection:{uuid:'RETURNED_CURVE',owner:'player',synthetic:true,epoch:15},bossReturn:{uuid:'RETURNED_CURVE',owner:'boss',synthetic:true,epoch:40},spawns:[],joins:[],clientRows:[],serverRows:[]};
 for(const name of ['STANDARD','GROUND','CURVE','RETURNED_CURVE']){
  const returned=name==='RETURNED_CURVE',profile=name.includes('CURVE'),base={name,uuid:name,id:200+result.spawns.length,owner:'boss',path:profile?'curve':'',index:0,normalized:returned,inWater:false,x:0,y:0,z:0,vx:name==='GROUND'?1.9:1,vy:0,vz:0,px:profile&&!returned||name==='GROUND'?0:.1,py:0,pz:0};
  result.spawns.push({...base,epoch:returned?11:40});result.joins.push({...base,owner:returned?'boss':'',epoch:returned?12:45,renderer:'net.minecraft.client.renderer.entity.ThrownItemRenderer'});
  if(returned)for(let i=0;i<4;i++)result.clientRows.push({...base,phase:'PLAYER',owner:'player',epoch:16+i,frame:i+1});
  let v=base.vx,x=0;
  for(let j=0;j<8;j++){
   const hidden=j<4,pre={...base,index:profile&&!returned?j:0,x,vx:v,dx:1,dy:0,dz:0};
   if(!profile||returned){x+=v;v=(v+base.px)*(name==='GROUND'?1:Math.fround(.95));}else x++;
   result.clientRows.push({...base,phase:hidden?'HIDDEN':'RESOLVED',owner:hidden?'':'boss',ownerAbsent:hidden,epoch:hidden?50+j:100+j,frame:j+5,index:profile&&!returned?j+1:0,x,vx:v,before:pre});
  }
  for(let j=0;j<12;j++)result.serverRows.push({...base,tick:j+3,age:j,index:profile&&!returned?j:0,registered:true,loaded:true,alive:true});
 }
 return {result,request:{nonce:'n',playerUuid:'player'}};
}
test('accept controlled delayed availability and authoritative replacement',()=>{const f=fixture();assert.deepEqual(analyzeDelayedOwner(f.result,f.request).failures,[]);});
for(const [name,mutate] of [
 ['wrong nonce',f=>f.result.nonce='x'],['late-join claim',f=>f.result.scope='LATE_JOIN'],['hidden cleanup fault',f=>f.result.cleanup=[{status:'FAIL'}]],
 ['missing owner stop',f=>f.result.ownerStop=null],['unowned stop',f=>f.result.ownerStop.owned=false],['old client alive',f=>f.result.ownerAbsence.oldRemoved=false],['ID still present',f=>f.result.ownerAbsence.idAbsent=false],['early readd',f=>f.result.ownerReadd.epoch=25],['same owner instance',f=>f.result.ownerJoin.distinct=false],['owner identity changed',f=>f.result.ownerJoin.uuid='other'],
 ['insufficient absent rows',f=>f.result.clientRows=f.result.clientRows.filter(x=>!(x.name==='STANDARD'&&x.phase==='HIDDEN'&&x.frame===5))],['stale Player cached while hidden',f=>f.result.clientRows.find(x=>x.name==='RETURNED_CURVE'&&x.phase==='HIDDEN').owner='player'],['unresolved after owner arrival',f=>f.result.clientRows.find(x=>x.phase==='RESOLVED').owner=''],['Player reappears',f=>f.result.clientRows.find(x=>x.name==='RETURNED_CURVE'&&x.phase==='RESOLVED').owner='player'],
 ['spawn before owner absent',f=>f.result.spawns.find(x=>x.name==='GROUND').epoch=21],['synthetic return hidden',f=>f.result.bossReturn.synthetic=false],['missing Player normalization',f=>f.result.deflection=null],['missing actual Player rows',f=>f.result.clientRows=f.result.clientRows.filter(x=>x.phase!=='PLAYER')],['changed path',f=>f.result.clientRows.find(x=>x.phase==='RESOLVED').path='different'],['unintended Ground decay',f=>f.result.clientRows.find(x=>x.name==='GROUND'&&x.phase==='HIDDEN').vx*=.95],['corrupt motion',f=>f.result.clientRows.find(x=>x.phase==='HIDDEN').x=999],['unloaded server entity',f=>f.result.serverRows[0].loaded=false],['server clock reset',f=>f.result.serverRows[2].age=0],['stale client index',f=>f.result.clientRows.find(x=>x.name==='CURVE'&&x.phase==='RESOLVED').index=0]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeDelayedOwner(f.result,f.request).status,'FAIL');});
