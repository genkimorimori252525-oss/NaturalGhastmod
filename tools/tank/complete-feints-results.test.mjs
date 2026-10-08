import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeCompleteFeints,completeFeintsScope} from './complete-feints-results.mjs';
const phases={PASS_BY_FAKE:['TELEGRAPH','COMMIT','BRAKE','REVEAL','BRAKE','RETURN'],DOUBLE_FAKE:['TELEGRAPH','COMMIT','BRAKE','REVEAL','BRAKE','REVEAL','BRAKE','RETURN'],ABORT_FAKE:['TELEGRAPH','BRAKE','RETURN']};
function fixture(){
 const result={scope:completeFeintsScope,status:'PASS',nonce:'n',samples:160,canonicalPlayer:true,fixtureSubjectAbsent:true,canonicalOverrides:false,errors:[],cases:[],rows:[],joins:[],clientRows:[],cleanup:[]};
 for(const [i,name] of Object.keys(phases).entries()){
  const center={x:0,y:6,z:28},region={center,radii:{x:20,y:8,z:20},generation:1},c={name,uuid:name,id:100+i,position:center,region,privateControlledGoal:true,width:4,height:4,targetUuid:'target'+i,clearBody:true,wallRejected:true,unloadedRejected:true};result.cases.push(c);result.joins.push({...c,renderer:'com.genki.soutoughast.client.renderer.SoutouGhastRenderer'});for(let j=0;j<4;j++)result.clientRows.push({...c,tick:j});result.cleanup.push({uuid:name,status:'DISCARDED_PRIVATE_ACTOR'},{uuid:c.targetUuid,status:'DISCARDED_PRIVATE_ACTOR'});
  // Synthetic, continuous motion for analyzer contracts; not a native receipt.
  let position={...center},velocity={x:0,y:0,z:0},tick=0,reveals=0;
  for(const phase of phases[name]){
   if(phase==='REVEAL')reveals++;
   const waypoint=name==='PASS_BY_FAKE'?{x:phase==='REVEAL'?10:6,y:6,z:14}:{x:reveals===1?-4:4,y:6,z:28};
   const budget=phase==='TELEGRAPH'?24:phase==='BRAKE'?12:phase==='RETURN'?16:phase==='COMMIT'&&name==='DOUBLE_FAKE'?8:70;
   for(let j=0;j<budget;j++){
    const moving=['TELEGRAPH','COMMIT','REVEAL'].includes(phase),dx=waypoint.x-position.x,dz=waypoint.z-position.z,distance=Math.hypot(dx,dz);
    if(moving&&phase!=='TELEGRAPH'&&!(phase==='COMMIT'&&name==='DOUBLE_FAKE')&&distance<=.7)break;
    const before={...position};velocity=Object.fromEntries(Object.entries(velocity).map(([k,v])=>[k,Math.abs(v)<.003?0:v]));
    const velocityBefore={...velocity},speed=Math.hypot(velocity.x,velocity.z);
    if(!moving)velocity=speed<=.035?{x:0,y:0,z:0}:{x:velocity.x*(speed-.035)/speed,y:0,z:velocity.z*(speed-.035)/speed};
    else {
     const cap=phase==='TELEGRAPH'||phase==='COMMIT'&&name==='DOUBLE_FAKE'?.14:.32,wanted=Math.min(cap,Math.sqrt(.07*Math.max(0,distance-.4))),desired={x:dx/distance*wanted,z:dz/distance*wanted};
     if(speed<1e-9){const f=Math.min(1,.11/Math.max(wanted,1e-9));velocity={x:desired.x*f,y:0,z:desired.z*f};}
     else {const fx=velocity.x/speed,fz=velocity.z/speed,cx=desired.x-velocity.x,cz=desired.z-velocity.z,long=cx*fx+cz*fz,lx=cx-fx*long,lz=cz-fz*long,scale=Math.min(1,.045/Math.max(Math.hypot(lx,lz),1e-9)),thrust=Math.max(-.035,Math.min(.11,long));velocity={x:velocity.x+fx*thrust+lx*scale,y:0,z:velocity.z+fz*thrust+lz*scale};}
    }
    position={x:position.x+velocity.x,y:6,z:position.z+velocity.z};tick++;
    result.rows.push({...c,tick,goalTick:tick,position,before,velocity,velocityBefore,observedTarget:{x:0,y:0,z:18},targetNow:{x:0,y:0,z:18},waypoint,phase,committed:name!=='ABORT_FAKE'&&tick>24,completed:false,registered:true,loaded:true,blocked:false,speed:Math.hypot(velocity.x,velocity.z)});
   }
  }
  result.rows.at(-1).completed=true;
 }
 return {result,request:{nonce:'n'}};
}
test('accept explicit controlled composer scope with three complete sequences',()=>{const f=fixture();assert.deepEqual(analyzeCompleteFeints(f.result,f.request).failures,[]);});
test('native LivingEntity deadzone fixture actually exercises component zeroing',()=>{const f=fixture(),trace=f.result.rows.filter(x=>x.name==='PASS_BY_FAKE');assert(trace.some((r,i)=>i&&['x','y','z'].some(k=>trace[i-1].velocity[k]!==0&&Math.abs(trace[i-1].velocity[k])<.003&&r.velocityBefore[k]===0)));assert.equal(analyzeCompleteFeints(f.result,f.request).status,'PASS');});
test('reject losing a component outside the exact native deadzone',()=>{const f=fixture(),r=f.result.rows.find(x=>Math.abs(x.velocityBefore.x)>.01);r.velocityBefore={...r.velocityBefore,x:0};assert.equal(analyzeCompleteFeints(f.result,f.request).status,'FAIL');});
for(const [name,mutate] of [
 ['wrong authority',f=>f.result.nonce='bad'],['natural selection claim',f=>f.result.scope='NATURAL_SELECTION'],['canonical overrides',f=>f.result.canonicalOverrides=true],['canonical fixture boss present',f=>f.result.fixtureSubjectAbsent=false],['cleanup failure',f=>f.result.cleanup[0].status='FAIL'],['wrong body',f=>f.result.cases[0].width=1],['missing route negative',f=>f.result.cases[0].wallRejected=false],['missing native client',f=>f.result.joins.pop()],['wrong renderer',f=>f.result.joins[0].renderer='Other'],['missing live client continuation',f=>f.result.clientRows.pop()],['teleport',f=>f.result.rows[0].before.x=-999],['changed region',f=>f.result.rows[0].region={...f.result.rows[0].region,generation:2}],['live homing geometry',f=>f.result.rows[1].observedTarget={x:1,y:0,z:18}],['body unloaded',f=>f.result.rows[0].loaded=false],['timeout',f=>f.result.rows[0].goalTick=161],['wrong goal clock',f=>f.result.rows[1].goalTick=9],['missing reveal',f=>f.result.rows=f.result.rows.filter(x=>x.name!=='PASS_BY_FAKE'||x.phase!=='REVEAL')],['abort commits',f=>f.result.rows.find(x=>x.name==='ABORT_FAKE').committed=true],['pass never crosses',f=>{for(const r of f.result.rows.filter(x=>x.name==='PASS_BY_FAKE'))r.position={x:0,y:6,z:28};}],['false complete',f=>f.result.rows.at(-1).completed=false]
])test('reject '+name,()=>{const f=fixture();mutate(f);assert.equal(analyzeCompleteFeints(f.result,f.request).status,'FAIL');});
