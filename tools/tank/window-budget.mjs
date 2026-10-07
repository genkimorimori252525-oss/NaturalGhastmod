/** Uses an already verified immutable lease snapshot; grants no authority or renewal. */
export function planOverheadWindow(preflight,now){
 const lease=preflight?.leaseCheck;
 if(preflight?.status!=='READY'||lease?.status!=='SUFFICIENT'||lease.requiredMs!==65000||!Number.isFinite(lease.remainingMs)||lease.remainingMs<65000||!Number.isSafeInteger(now))throw Error('OVERHEAD_WINDOW_BUDGET_NOT_ESTABLISHED');
 return Object.freeze({maxTicks:900,maxWallMs:50000,postDeathMaxTicks:200,wallDeadlineEpochMs:now+50000,reserveMs:15000});
}
export function remainingWindowBudget(plan,now){
 return {experimentMs:Math.max(0,plan.wallDeadlineEpochMs-now),finalizationMs:5000,cleanupMs:5000,marginMs:5000};
}
