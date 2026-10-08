import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const launcher=fs.readFileSync(new URL('./run-lob-terminal.mjs',import.meta.url),'utf8'),probe=fs.readFileSync(new URL('./LobTerminalProbe.java',import.meta.url),'utf8');
test('private Forge Mod identity is valid and matches native annotation',()=>{
 const declared=launcher.match(/modId=(.*?)\\n/)[1].replaceAll('\\','').replaceAll('"','');
 assert.match(declared,/^[a-z][a-z0-9_]{1,63}$/);assert.equal(declared,probe.match(/@Mod\("([^"]+)"\)/)[1]);
});
test('native supplement dispatch and environment agree',()=>{
 assert.ok(launcher.includes("evidence/derived/lob-terminal")&&probe.includes('evidence/derived/lob-terminal'));
 for(const key of ['KNEEKURA_DEBUG_LOB_TERMINAL','KNEEKURA_DEBUG_LOB_TERMINAL_NONCE','KNEEKURA_DEBUG_LOB_TERMINAL_WORLD'])assert.ok(launcher.includes(key)&&probe.includes(key));
 assert.ok(launcher.includes('maxTicks:180')&&probe.includes('==180')&&launcher.includes('maxWallMs:18000')&&probe.includes('==18000'));
});
