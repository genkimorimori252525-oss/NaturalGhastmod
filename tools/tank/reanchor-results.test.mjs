import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeReanchor,reanchorScope} from './reanchor-results.mjs';
const add=(a,b)=>Object.fromEntries(['x','y','z'].map(k=>[k,a[k]+b[k]])),scale=(a,n)=>Object.fromEntries(['x','y','z'].map(k=>[k,a[k]*n])),sub=(a,b)=>add(a,scale(b,-1)),norm=v=>Math.hypot(v.x,v.y,v.z),dot=(a,b)=>a.x*b.x+a.y*b.y+a.z*b.z,limited=(v,n)=>norm(v)>n?scale(v,n/norm(v)):v,zero=()=>({x:0,y:0,z:0});
function fixture(){
 const old={center:{x:26,y:230,z:53.35},radii:{x:20,y:8,z:20},generation:1,reason:'ACQUIRED'},observedTarget={x:26,y:224,z:26},start={x:26,y:230,z:38},overhead={x:26,y:242.15,z:38},beyond={x:26,y:242.15,z:18},candidateCenter={x:26,y:230,z:18},destination={x:26,y:236.8,z:18},heading={x:0,y:0,z:-1};
 const actors={uuid:'boss',id:22,position:start,targetUuid:'cow',privateControlledGoal:true,width:4,height:4,observedTarget,targetTop:225.4,routeLength:37.5,budget:311,region:old};
 const r={scope:reanchorScope,nonce:'n',status:'PASS',samples:1,canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,errors:[],actors,rows:[],joins:[{uuid:'boss',id:22,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'}],clientRows:Array.from({length:4},(_,tick)=>({uuid:'boss',id:22,tick})),cleanup:[{uuid:'boss',status:'DISCARDED_PRIVATE_ACTOR'},{uuid:'cow',status:'DISCARDED_PRIVATE_ACTOR'}]};
 const aligned=(yaw,pitch,d,limit)=>Math.abs(turn(yaw,-Math.atan2(d.x,d.z)*180/Math.PI,360)-yaw)<=limit&&Math.abs(turn(pitch,-Math.atan2(d.y,Math.hypot(d.x,d.z))*180/Math.PI,360)-pitch)<=limit;
 const radius=p=>Math.hypot((p.x-candidateCenter.x)/20,(p.y-candidateCenter.y)/8,(p.z-candidateCenter.z)/20);
 let phase='TELL',ticks=0,p=start,v=zero(),yaw=0,pitch=0,normal=0,newRegion=null;
 for(let tick=1;tick<=340;tick++){
  v=Object.fromEntries(['x','y','z'].map(k=>[k,Math.abs(v[k])<.003?0:v[k]]));const before={...p},velocityBefore={...v};
  const yawBefore=yaw,pitchBefore=pitch,observedLook=sub(observedTarget,p);
  if(phase==='CLIMB'&&norm(sub(p,overhead))<=.7){phase='ALIGN';ticks=0;}if(phase==='ALIGN'&&aligned(yaw,pitch,heading,15)){phase='CROSS';ticks=0;}
  if(phase==='CROSS'&&norm(sub(p,beyond))<=.7){phase='BRAKE';ticks=0;}if(phase==='BRAKE'&&norm(v)<=.04){phase='TURN';ticks=0;}
  if(phase==='TURN'&&aligned(yaw,pitch,observedLook,8)){phase='DESCEND';ticks=0;}
  const commit=phase==='DESCEND'&&radius(p)<=.95,regionBefore=newRegion??old;if(commit){phase='HANDOFF';newRegion={center:candidateCenter,radii:old.radii,generation:2,reason:'TACTICAL_RELOCATION'};}
  const point=['TELL','CLIMB','ALIGN'].includes(phase)?overhead:['CROSS','BRAKE','TURN'].includes(phase)?beyond:destination;
  const look=['TURN','DESCEND','HANDOFF','NORMAL'].includes(phase)?observedLook:heading;
  if(['TELL','CLIMB','CROSS','DESCEND','HANDOFF','NORMAL'].includes(phase)){
   const offset=phase==='NORMAL'?{x:.2,y:-1,z:0}:sub(point,p),speed=phase==='NORMAL'?.08:Math.min(phase==='TELL'?.1:phase==='CROSS'?.24:.12,Math.sqrt(2*.035*Math.max(0,norm(offset)-.4))),desired=norm(offset)?scale(offset,speed/norm(offset)):zero();
   if(norm(v)<1e-9)v=limited(desired,.11);else{const forward=scale(v,1/norm(v)),correction=sub(desired,v),long=dot(correction,forward),lateral=limited(sub(correction,scale(forward,long)),.045);v=limited(add(add(v,scale(forward,Math.max(-.035,Math.min(.11,long)))),lateral),Math.max(.65,norm(v)));}
  }else v=norm(v)<=.035?zero():scale(v,(norm(v)-.035)/norm(v));p=add(p,v);
  yaw=turn(yaw,-Math.atan2(look.x,look.z)*180/Math.PI,4);pitch=turn(pitch,-Math.atan2(look.y,Math.hypot(look.x,look.z))*180/Math.PI,3);
  r.rows.push({uuid:'boss',id:22,tick,goalTick:tick,phase,commit,visible:true,before,velocityBefore,position:{...p},velocity:{...v},yawBefore,pitchBefore,yaw,pitch,bodyYaw:yaw,look,observedLook,regionBefore,region:newRegion??old,overhead,beyond,destination,candidateCenter,targetNow:observedTarget,targetHealth:10,registered:true,loaded:true,blocked:false});
  ticks++;if(phase==='TELL'&&ticks>=12){phase='CLIMB';ticks=0;}if(commit)phase='NORMAL';else if(phase==='NORMAL'&&++normal===12)break;
 }
 r.clientRows=r.rows.map(x=>({uuid:'boss',id:22,tick:x.tick,position:x.position,yaw:x.yaw,pitch:x.pitch,bodyYaw:x.bodyYaw,targetEye:observedTarget}));r.samples=r.rows.length+1;return {result:r,request:{nonce:'n'}};
}
function turn(current,desired,limit){let diff=(desired-current)%360;if(diff>=180)diff-=360;if(diff< -180)diff+=360;return current+Math.max(-limit,Math.min(limit,diff));}
test('accept high travel-facing passage, behind-turn, gentle descent and moving handoff',()=>{const f=fixture();assert.deepEqual(analyzeReanchor(f.result,f.request).failures,[]);});
for(const [name,mutate] of [
 ['authority',f=>f.result.canonicalOverrides=true],['scope',f=>f.result.scope='NATURAL'],['nonce',f=>f.result.nonce='bad'],['unbounded route',f=>f.result.actors.routeLength=30],['private goal',f=>f.result.actors.privateControlledGoal=false],['client missing',f=>f.result.joins=[]],['renderer',f=>f.result.joins[0].renderer='OTHER'],['body unloaded',f=>f.result.rows[0].loaded=false],['clock',f=>f.result.rows[1].goalTick=9],['teleport',f=>f.result.rows[5].position.x+=3],['motion snap',f=>f.result.rows[5].velocityBefore.x=1],['hidden target',f=>f.result.rows[3].visible=false],['frozen route changed',f=>f.result.rows[20].destination={x:27,y:232.15,z:18}],['old region changed early',f=>f.result.rows[2].region={...f.result.actors.region,generation:2}],['low crossing',f=>f.result.rows.find(x=>x.phase==='CROSS').before.y=225],['wrong reason',f=>f.result.rows.at(-1).region={...f.result.rows.at(-1).region,reason:'SPACE_BLOCKED'}],['new narrow region',f=>f.result.rows.at(-1).region={...f.result.rows.at(-1).region,radii:{x:5,y:5,z:5}}],['stopped handoff',f=>f.result.rows.find(x=>x.commit).velocityBefore={x:0,y:0,z:0}],['no commit',f=>f.result.rows.find(x=>x.commit).commit=false],['cleanup missing',f=>f.result.cleanup.pop()],['moonwalk intent',f=>{const x=f.result.rows.find(x=>x.phase==='CROSS');x.look=x.observedLook;}],['travel rotation missing',f=>f.result.rows.find(x=>x.phase==='CROSS').yaw=0],['client moonwalk',f=>f.result.clientRows.forEach(x=>x.bodyYaw=0)],['ordinary resume missing',f=>f.result.rows=f.result.rows.filter(x=>x.phase!=='NORMAL')]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeReanchor(f.result,f.request).status,'FAIL');});
