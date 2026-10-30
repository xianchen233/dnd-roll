import assert from 'node:assert/strict';
import {writeFile} from 'node:fs/promises';
import {randomUUID} from 'node:crypto';
import {Session,required,privateHeaders} from './acceptance-support.mjs';
const [file]=process.argv.slice(2);assert.ok(file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
const before=await dm.get('/admin/backups');assert.equal(before.paused,false);
const body={requestId:randomUUID(),expectedVersion:before.version};const ack=await dm.post('/admin/backups/migration',body);assert.deepEqual(await dm.post('/admin/backups/migration',body),ack);
let job;const until=Date.now()+60000;
while(Date.now()<until){const state=await dm.get('/admin/backups');assert.equal(state.paused,true);job=state.jobs.find(j=>j.id===ack.id);if(job?.state==='COMPLETE'||job?.state==='FAILED')break;await new Promise(r=>setTimeout(r,250));}
assert.equal(job?.state,'COMPLETE');const response=await dm.call('/admin/backups/'+ack.id+'/content');privateHeaders(response);await writeFile(file,Buffer.from(await response.arrayBuffer()),{flag:'wx'});
const paused=await dm.call('/campaigns',{method:'POST',json:{requestId:randomUUID(),name:'Must not be created while fenced'},status:503});assert.equal((await paused.json()).code,'WRITE_PAUSED');
await writeFile(file+'.evidence.json',JSON.stringify({ack,kind:job.kind,state:job.state,paused:true},null,2),{flag:'wx'});
console.log('M3 NATIVE ARCHIVE COMPLETE: actual P09 migration job/download, exact retry, private headers and durable source write fence verified');
