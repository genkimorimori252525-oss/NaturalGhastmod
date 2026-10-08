import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeReanchor,reanchorScope} from './reanchor-results.mjs';
const add=(a,b)=>Object.fromEntries(['x','y','z'].map(k=>[k,a[k]+b[k]])),scale=(a,n)=>Object.fromEntries(['x','y','z'].map(k=>[k,a[k]*n])),sub=(a,b)=>add(a,scale(b,-1)),norm=v=>Math.hypot(v.x,v.y,v.z),dot=(a,b)=>a.x*b.x+a.y*b.y+a.z*b.z,limited=(v,n)=>norm(v)>n?scale(v,n/norm(v)):v,zero=()=>({x:0,y:0,z:0});
function fixture(){
 const old={center:{x:26,y:230,z:53.35},radii:{x:20,y:8,z:20},generation:1,reason:'ACQUIRED'},observedTarget={x:26,y:224,z:26},start={x:26,y:230,z:38},overhead={x:26,y:232.15,z:38},destination={x:26,y:232.15,z:18};
 const actors={uuid:'boss',id:22,position:start,targetUuid:'cow',privateControlledGoal:true,width:4,height:4,observedTarget,targetTop:225.4,routeLength:22.15,region:old};
 const r={scope:reanchorScope,nonce:'n',status:'PASS',samples:1,canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,errors:[],actors,rows:[],joins:[{uuid:'boss',id:22,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'}],clientRows:Array.from({length:4},(_,tick)=>({uuid:'boss',id:22,tick})),cleanup:[{uuid:'boss',status:'DISCARDED_PRIVATE_ACTOR'},{uuid:'cow',status:'DISCARDED_PRIVATE_ACTOR'}]};
 let phase='TELL',ticks=0,p=start,v=zero();
 for(let tick=1;tick<=160;tick++){
  v=Object.fromEntries(['x','y','z'].map(k=>[k,Math.abs(v[k])<.003?0:v[k]]));const before={...p},velocityBefore={...v};
  if(phase==='CLIMB'&&norm(sub(p,overhead))<=.7){phase='CROSS';ticks=0;}if(phase==='CROSS'&&norm(sub(p,destination))<=.7){phase='BRAKE';ticks=0;}
  const commit=phase==='BRAKE'&&norm(v)<=.04;if(commit)phase='DONE';const point=phase==='CROSS'?destination:overhead;
  if(['TELL','CLIMB','CROSS'].includes(phase)){
   const offset=sub(point,p),speed=Math.min(phase==='TELL'?.1:.32,Math.sqrt(2*.035*Math.max(0,norm(offset)-.4))),desired=norm(offset)?scale(offset,speed/norm(offset)):zero();
   if(norm(v)<1e-9)v=limited(desired,.11);else{const forward=scale(v,1/norm(v)),correction=sub(desired,v),long=dot(correction,forward),lateral=limited(sub(correction,scale(forward,long)),.045);v=limited(add(add(v,scale(forward,Math.max(-.035,Math.min(.11,long)))),lateral),Math.max(.65,norm(v)));}
  }else v=norm(v)<=.035?zero():scale(v,(norm(v)-.035)/norm(v));p=add(p,v);
  r.rows.push({uuid:'boss',id:22,tick,goalTick:tick,phase,commit,visible:true,before,velocityBefore,position:{...p},velocity:{...v},regionBefore:old,region:commit?{center:before,radii:old.radii,generation:2,reason:'TACTICAL_RELOCATION'}:old,overhead,destination,targetNow:observedTarget,targetHealth:10,registered:true,loaded:true,blocked:false});
  ticks++;if(phase==='TELL'&&ticks>=12){phase='CLIMB';ticks=0;}if(commit)break;
 }
 r.samples=r.rows.length+1;return {result:r,request:{nonce:'n'}};
}
test('accept finite physically braked transit and one guarded region publication',()=>{const f=fixture();assert.deepEqual(analyzeReanchor(f.result,f.request).failures,[]);});
for(const [name,mutate] of [
 ['authority',f=>f.result.canonicalOverrides=true],['scope',f=>f.result.scope='NATURAL'],['nonce',f=>f.result.nonce='bad'],['unbounded route',f=>f.result.actors.routeLength=30],['private goal',f=>f.result.actors.privateControlledGoal=false],['client missing',f=>f.result.joins=[]],['renderer',f=>f.result.joins[0].renderer='OTHER'],['body unloaded',f=>f.result.rows[0].loaded=false],['clock',f=>f.result.rows[1].goalTick=9],['teleport',f=>f.result.rows[5].position.x+=3],['motion snap',f=>f.result.rows[5].velocityBefore.x=1],['hidden target',f=>f.result.rows[3].visible=false],['frozen route changed',f=>f.result.rows[20].destination={x:27,y:232.15,z:18}],['old region changed early',f=>f.result.rows[2].region={...f.result.actors.region,generation:2}],['low crossing',f=>f.result.rows.find(x=>x.phase==='CROSS').before.y=225],['wrong reason',f=>f.result.rows.at(-1).region={...f.result.rows.at(-1).region,reason:'SPACE_BLOCKED'}],['new narrow region',f=>f.result.rows.at(-1).region={...f.result.rows.at(-1).region,radii:{x:5,y:5,z:5}}],['unbraked commit',f=>f.result.rows.at(-1).velocityBefore.z=.2],['no commit',f=>f.result.rows.at(-1).commit=false],['cleanup missing',f=>f.result.cleanup.pop()]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeReanchor(f.result,f.request).status,'FAIL');});
