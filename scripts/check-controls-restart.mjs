import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {Session,required} from './acceptance-support.mjs';
import {controlsSnapshot} from './module-controls-evidence.mjs';
const [file]=process.argv.slice(2);assert.ok(file);
const entries=JSON.parse(await readFile(file,'utf8'));assert.equal(entries.length,2);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
for(const e of entries){
 const snapshot=()=>controlsSnapshot(path=>dm.get(path.slice(4)),async path=>Buffer.from(await(await dm.call(path.slice(4))).arrayBuffer()),e);
 assert.deepEqual(await snapshot(),e.snapshot);
 assert.ok(e.commands.length>=2);
 for(const command of e.commands)assert.deepEqual(await dm.post(command.path.slice(4),command.body),command.result);
 assert.deepEqual(await snapshot(),e.snapshot);
}
console.log('CONTROLS VERIFIED: browser-created selective/live migrations, recovery ZIP/maps/cards and five typed operations retain exact snapshots and original receipts; later damage, hiding, stopped combat, progress and RP survive fresh-session restart/isolated restore');
