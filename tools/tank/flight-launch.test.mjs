import test from 'node:test';
import assert from 'node:assert/strict';
import {privateFlightLaunchArgs} from './flight-launch.mjs';
test('reused private launch template replaces one host option and its final private init only',()=>{
 const template=['--project-dir','old-host','-Pforge_version=1.20.1-47.4.10','runClient','--console=plain','--offline','--init-script','base.gradle','--init-script','old/native.init.gradle'];
 const args=privateFlightLaunchArgs(template,'selected-host','new/native.init.gradle');
 assert.deepEqual(args,['--project-dir','selected-host','-Pforge_version=1.20.1-47.4.10','runClient','--console=plain','--init-script','base.gradle','--init-script','new/native.init.gradle']);
 assert.equal(template[1],'old-host');
});
