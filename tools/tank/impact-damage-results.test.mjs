import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeImpactDamage} from './impact-damage-results.mjs';
function fixture(){
 const names=['LOB_BLOCK','BURST_MISS','STANDARD_COW','GROUND_COW','OWN_RETURN','DEFLECTION_RULES','ATTRIBUTION_REJECTION'];
 const cases=names.map((name,i)=>({name,uuid:'shot'+i,checks:{productionRules:true},impacts:[],damage:[],initialHealth:10,finalHealth:10,elapsedTicks:3,ended:true}));
 cases[0].impacts=[{type:'BLOCK',declared:true,canceled:false,impactResult:'DEFAULT',tick:110}];cases[0].explosions=[{stage:'START',canceled:false,tick:110,directUuid:'shot0'},{stage:'DETONATE',tick:110,directUuid:'shot0'}];cases[1].finiteEnd=true;
 for(const [i,base] of [[2,9],[3,3]]){cases[i].impacts=[{type:'ENTITY',victimUuid:'cow',canceled:false}];cases[i].victimUuid='cow';cases[i].damage=[{stage:'DAMAGE',kind:'FIREBALL',amount:base,directUuid:'shot'+i,ownerUuid:'boss',victimUuid:'cow'}];cases[i].finalHealth=10-base;}
 cases[4].initialHealth=100;cases[4].finalHealth=80;cases[4].victimUuid='boss';cases[4].impacts=[{type:'ENTITY',victimUuid:'boss',canceled:false}];cases[4].damage=[{stage:'DAMAGE',kind:'FIREBALL',amount:20,directUuid:'shot4',ownerUuid:'player',victimUuid:'boss'}];
 Object.assign(cases[4].checks,{delayedDuplicateRejected:true,cooldownAtDuplicate:0,ownDirectVictim:true,nonImmuneDuplicate:true});
 cases[6].checks.sourceImmunity=true;
 const request={nonce:'nonce',maxTicks:240,maxWallMs:20000,subjectUuid:'boss',playerUuid:'player'};
 const result={scope:'EXPLICIT_NATIVE_IMPACT_DAMAGE_NOT_HUMAN_MELEE_OR_BALANCE',nonce:'nonce',status:'PASS',failures:[],cases,startGameTime:100,endGameTime:160,canonicalPlayer:true};
 return {result,request};
}
test('seven native scopes are distinct',()=>{const f=fixture();assert.equal(analyzeImpactDamage(f.result,f.request).status,'PASS');});
test('last-index disappearance cannot substitute a declared collision',()=>{const f=fixture();f.result.cases[0].impacts=[];assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');});
test('reject canceled collision and impact during claimed miss',()=>{for(const mutate of [f=>f.result.cases[0].impacts[0].canceled=true,f=>f.result.cases[1].impacts.push({type:'BLOCK'})]){const f=fixture();mutate(f);assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');}});
test('reject wrong direct base, returned owner, health loss or double damage',()=>{for(const mutate of [f=>f.result.cases[2].damage[0].amount=8,f=>f.result.cases[4].damage[0].ownerUuid='boss',f=>f.result.cases[4].finalHealth=79,f=>f.result.cases[4].damage.push({...f.result.cases[4].damage[0],kind:'EXPLOSION',amount:1})]){const f=fixture();mutate(f);assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');}});
test('reject missing source rules, request bounds or actual health evidence',()=>{for(const mutate of [f=>f.result.cases[5].checks.productionRules=false,f=>f.request.maxTicks=400,f=>f.result.cases[3].finalHealth=10]){const f=fixture();mutate(f);assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');}});
test('skipped collision or absent execution cannot pass block-hit proof',()=>{for(const mutate of [f=>f.result.cases[0].impacts[0].impactResult='SKIP_ENTITY',f=>f.result.cases[0].explosions.pop()]){const f=fixture();mutate(f);assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');}});
test('cooldown-only protection cannot prove dedicated return deduplication',()=>{const f=fixture();f.result.cases[4].checks.cooldownAtDuplicate=10;assert.equal(analyzeImpactDamage(f.result,f.request).status,'FAIL');});
test('native ATTACK notification before immunity rejection is not applied damage',()=>{const f=fixture();f.result.cases[6].damage=[{stage:'ATTACK',kind:'FIREBALL',amount:20},{stage:'ATTACK',kind:'FIREBALL',amount:20}];assert.equal(analyzeImpactDamage(f.result,f.request).status,'PASS');});
