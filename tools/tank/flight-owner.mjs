/** Journal completion and owner next-action publication are separate observations. */
export async function waitForFlightAction({inspect,selectedActionId,timeoutMs=5000,pollMs=50}){
 const deadline=Date.now()+timeoutMs;
 do{
  const state=await inspect();
  if(state.nextActionId===selectedActionId)return state;
  await new Promise(resolve=>setTimeout(resolve,pollMs));
 }while(Date.now()<deadline);
 throw Error('OWNER_NEXT_ACTION_NOT_READY:'+selectedActionId);
}
