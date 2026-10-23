import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
import {parseArgs} from 'node:util';
import {pathToFileURL} from 'node:url';

/** Natural is a platform client. Importing/preparing never starts Minecraft. */
export async function runManualRally({installation,profileFile,launch=false}) {
 const profile=JSON.parse(await fs.readFile(profileFile,'utf8'));
 assert(['LIFECYCLE','MANUAL_RALLY'].includes(profile.mode),'NATURAL_MANUAL_PROFILE_REQUIRED');
 assert(profile.mods.some(mod=>mod.id==='soutou_ghast'),'NATURAL_PRODUCT_REQUIRED');
 assert(typeof profile.sourceRevision==='string'&&/^[a-f0-9]{40}$/.test(profile.sourceRevision),'NATURAL_SOURCE_PIN_REQUIRED');
 const {prepare,commandRun}=await import(pathToFileURL(path.join(installation,'native-host/tank.mjs')));
 const prepared=await prepare(installation,profile);
 console.log(JSON.stringify(prepared));
 if(!launch)return prepared;
 // Same installed launch/status/STOP/evidence path as every other target MOD.
 return commandRun('start',prepared.trial);
}
if(process.argv[1]&&import.meta.url===pathToFileURL(path.resolve(process.argv[1])).href){
 const {values}=parseArgs({options:{installation:{type:'string'},profile:{type:'string'},launch:{type:'boolean',default:false}}});
 try{
  assert(values.installation&&values.profile,'--installation and --profile required');
  console.log(JSON.stringify(await runManualRally({installation:path.resolve(values.installation),profileFile:path.resolve(values.profile),launch:values.launch})));
 }catch(error){console.error(JSON.stringify({status:'FAIL',error:error.message}));process.exitCode=1;}
}
