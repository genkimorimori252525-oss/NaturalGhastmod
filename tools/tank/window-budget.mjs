/** Uses an already verified immutable lease snapshot; grants no authority or renewal. */
export function planOverheadWindow(preflight,now){
 const lease=preflight?.leaseCheck;
 if(preflight?.status!=='READY'||lease?.status!=='SUFFICIENT'||lease.requiredMs!==65000||!Number.isFinite(lease.remainingMs)||lease.remainingMs<65000||!Number.isSafeInteger(now))throw Error('OVERHEAD_WINDOW_BUDGET_NOT_ESTABLISHED');
 return Object.freeze({maxTicks:900,maxWallMs:50000,postDeathMaxTicks:200,wallDeadlineEpochMs:now+50000,reserveMs:15000});
}
export function remainingWindowBudget(plan,now){
 return {experimentMs:Math.max(0,plan.wallDeadlineEpochMs-now),finalizationMs:5000,cleanupMs:5000,marginMs:5000};
}
export function planGroundWindow(preflight,now){
 const lease=preflight?.leaseCheck;
 if(preflight?.status!=='READY'||lease?.status!=='SUFFICIENT'||lease.requiredMs!==30000||!Number.isFinite(lease.remainingMs)||lease.remainingMs<30000||!Number.isSafeInteger(now))throw Error('GROUND_WINDOW_BUDGET_NOT_ESTABLISHED');
 return Object.freeze({maxTicks:180,maxWallMs:15000,postDeathMaxTicks:0,wallDeadlineEpochMs:now+15000,reserveMs:15000});
}
export function planDomainWindow(preflight,now){
 const lease=preflight?.leaseCheck;
 if(preflight?.status!=='READY'||lease?.status!=='SUFFICIENT'||lease.requiredMs!==75000||!Number.isFinite(lease.remainingMs)||lease.remainingMs<75000||!Number.isSafeInteger(now))throw Error('DOMAIN_WINDOW_BUDGET_NOT_ESTABLISHED');
 return Object.freeze({maxTicks:1200,maxWallMs:60000,postDeathMaxTicks:200,wallDeadlineEpochMs:now+60000,reserveMs:15000});
}
/** Final roster shares the five-second finalization allocation, never cleanup/margin. */
export function finalRosterDeadline(plan,now){
 const cutoff=plan?.wallDeadlineEpochMs+5000;
 if(plan?.reserveMs!==15000||!Number.isSafeInteger(cutoff)||!Number.isSafeInteger(now)||now>=cutoff)throw Error('FINAL_ROSTER_BUDGET_EXHAUSTED');
 return Math.min(now+5000,cutoff);
}
