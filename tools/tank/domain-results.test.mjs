import test from 'node:test';
import assert from 'node:assert/strict';
import {analyzeDomain} from './domain-results.mjs';
const subject='boss',player='player';
function evidence(){
 const rows=[];let tick=0;
 for(const [phase,count] of [['START',59],['PLACING',5],['LANDING',10],['COMBAT',400],['ENDING',30],['TAKEOFF',20],['AIR',1]])for(let i=0;i<count;i++)rows.push({tick:tick++,uuid:subject,playerUuid:player,playerMode:'survival',playerHealth:20,targetUuid:player,noAI:false,width:4,height:4,radiusX:20,radiusY:8,radiusZ:20,regionGeneration:1,domainPhase:phase,domainTicks:phase==='START'?i+1:i,domainReason:phase==='AIR'?'EXIT_COMPLETE':'OBSERVED',domainOffenseAllowed:phase==='COMBAT',domainArmPlacement:phase==='PLACING'&&i===0,domainGrounded:phase==='COMBAT',groundFiredCount:0,domainJournals:{allVerifiedTerminal:['TAKEOFF','AIR'].includes(phase),originalsLoadedCurrent:['TAKEOFF','AIR'].includes(phase)}});
 return {rows,readiness:{verdict:'PASS',canonicalPlayer:true,subjectUuid:subject,playerUuid:player,plannedCells:16341,changedCells:2665,fullParticipantFit:true,freshFloorCommitted:true,admissionNotYetStarted:true},request:{maxTicks:1200,maxWallMs:60000,reserveMs:15000},end:{samples:rows.length,reason:'MAX_TICKS'},finalJournals:{allVerifiedTerminal:true,originalsLoadedCurrent:true}};
}
test('simulated complete supported cycle requires genuine identity and durable exit evidence',()=>{
 const result=analyzeDomain(evidence(),subject,player);assert.equal(result.status,'PASS');assert.equal(result.combatSamples,400);
});
test('a retained unresolved ending is partial coverage, not completed restoration',()=>{
 const input=evidence();input.rows=input.rows.filter(r=>!['TAKEOFF','AIR'].includes(r.domainPhase));input.end.samples=input.rows.length;input.finalJournals={allVerifiedTerminal:false,originalsLoadedCurrent:false};assert.equal(analyzeDomain(input,subject,player).status,'PARTIAL');
});
test('takeoff without positive terminal/original proof and cleanup offense are violations',()=>{
 for(const mutate of [input=>input.rows.find(r=>r.domainPhase==='TAKEOFF').domainJournals.allVerifiedTerminal=false,input=>input.rows.find(r=>r.domainPhase==='ENDING').domainOffenseAllowed=true,input=>input.rows.find(r=>r.domainPhase==='ENDING').groundFiredCount=1]){const input=evidence();mutate(input);assert.equal(analyzeDomain(input,subject,player).status,'FAIL');}
});
test('short combat or natural death cannot pass complete combat coverage',()=>{
 const input=evidence();input.rows=input.rows.filter(r=>r.domainPhase!=='COMBAT'||r.domainTicks<10);input.rows.forEach((r,i)=>r.tick=i);input.end.samples=input.rows.length;assert.equal(analyzeDomain(input,subject,player).status,'PARTIAL');
});
test('missing/late readiness, fake identity, widened region and nonmonotonic rows reject',()=>{
 for(const mutate of [input=>input.readiness=null,input=>input.readiness.admissionNotYetStarted=false,input=>input.rows[0].playerMode='creative',input=>input.rows[0].playerUuid='other',input=>input.rows[0].radiusX=10,input=>input.rows[1].tick=0]){const input=evidence();mutate(input);assert.equal(analyzeDomain(input,subject,player).status,'FAIL');}
});
