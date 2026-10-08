import {analyzeCoverLob,coverLobScope} from './cover-lob-results.mjs';
export const lobTerminalScope='NEW_PRIVATE_INTEGER_LOB_NATIVE_TERMINAL_NOT_PLAYER_INPUT_OR_PRODUCTION_GOAL';
const finite=v=>v&&['x','y','z'].every(k=>Number.isFinite(v[k])),distance=(a,b)=>finite(a)&&finite(b)?Math.hypot(a.x-b.x,a.y-b.y,a.z-b.z):Infinity,same=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const cells=(a,b)=>['x','z'].reduce((n,k)=>n*(Math.floor(Math.max(a[k],b[k])+.5-1e-7)-Math.floor(Math.min(a[k],b[k])-.5+1e-7)+1),1);
export function analyzeLobTerminal(result,request){
 // Reuse exact cover contracts with explicit schema adaptation; terminal acceptance adds stricter native evidence.
 const base=analyzeCoverLob({...result,scope:coverLobScope},request),failures=[...base.failures],check=(ok,label)=>{if(!ok)failures.push(label);};
 check(result?.scope===lobTerminalScope,'TERMINAL_SCOPE');
 const shot=result?.shots?.[0],raw=shot?.rawPath?.points,points=shot?.path?.points,impacts=result?.impacts??[],explosions=result?.explosions??[];
 const valid=Array.isArray(raw)&&raw.length>=2&&raw.length<97&&raw.every(finite)&&Array.isArray(points)&&points.length===raw.length+1&&points.length<=97&&points.every(finite);
 check(valid&&shot?.rawPath?.kind==='LOB','ONE_BOUNDED_TERMINAL_SPLIT');
 if(valid){
  const from=raw.at(-2),end=raw.at(-1),split=points.at(-2),t=(split.y-from.y)/(end.y-from.y);
  check(raw.slice(0,-1).every((p,i)=>same(p,points[i]))&&same(end,points.at(-1)),'UNCHANGED_ORIGINAL_WORLD_ARC_ENDPOINT');
  const onLine=Object.fromEntries(['x','y','z'].map(k=>[k,from[k]+(end[k]-from[k])*t]));
  check(t>0&&t<1&&distance(onLine,split)<1e-7&&split.y>Math.floor(end.y)+1+1e-7,'SPLIT_BEFORE_NATIVE_FLOOR_CONTACT');
  check(shot.rawTerminalCells===cells(from,end)&&shot.rawTerminalCells>4&&cells(split,end)<=4,'ACTUAL_TERMINAL_FOOTPRINT_REDUCTION');
 }
 check(impacts.length===1&&explosions.length===1,'ONE_NATIVE_BLOCK_IMPACT_EXPLOSION');
 const hit=impacts[0],explosion=explosions[0];
 if(valid&&hit&&explosion){
  const terminal=shot.declaredTerminal;
  check(Array.isArray(terminal)&&terminal.length>0&&terminal.length<=4&&terminal.some(t=>same(t.position,hit.block)&&t.state===hit.state),'EXACT_DECLARED_NATIVE_COLLIDER');
  check(hit.uuid===shot.uuid&&hit.canceled===false&&hit.kind==='BLOCK'&&hit.index===points.length-1&&hit.tick===shot.tick+points.length-1,'ACTUAL_LAST_SEGMENT_NATIVE_IMPACT');
  const before=points.at(-2),end=points.at(-1),t=finite(hit.hit)?(hit.hit.y-before.y)/(end.y-before.y):-1;
  const expected=Object.fromEntries(['x','y','z'].map(k=>[k,before[k]+(end[k]-before[k])*t]));
  check(t>=0&&t<=1&&distance(hit.position,before)<1e-6&&distance(hit.hit,expected)<1e-6&&Math.abs(hit.hit.y-(Math.floor(end.y)+1))<1e-6,'ACTUAL_NATIVE_FINAL_RAY');
  const previous=result.rows.find(r=>r.tick===hit.tick-1)?.projectiles?.find(p=>p.uuid===shot.uuid);
  check(previous?.index===points.length-2&&distance(previous?.position,before)<1e-6,'REGISTERED_PREIMPACT_NATIVE_POSITION');
  check(explosion.uuid===shot.uuid&&explosion.tick===hit.tick&&distance(explosion.position,before)<1e-6&&distance(explosion.explosionPosition,before)<1e-6,'NATIVE_EXPLOSION_SOURCE_CLOCK_POSITION');
  const state=result.rows.find(r=>r.tick===hit.tick)?.endShots?.find(s=>s.uuid===shot.uuid);
  check(state?.removed===true&&state?.registered===false,'NATIVE_REMOVAL_AFTER_IMPACT');
 }
 return {...base,status:failures.length?'FAIL':'PASS',scope:lobTerminalScope,failures:[...new Set(failures)],rawPoints:raw?.length,refinedPoints:points?.length,impactTick:hit?.tick,impactBlock:hit?.block,nativeImpactScope:'LOWEST_UNCANCELED_FORGE_BLOCK_RAY_PLUS_NATIVE_EXPLOSION_AND_END_REMOVAL'};
}
