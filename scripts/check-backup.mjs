import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';

const [file,kind='migration']=process.argv.slice(2);assert.ok(file);assert.ok(['manual','migration'].includes(kind));
const operator=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
const player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
const path='/admin/backups'+(kind==='migration'?'/migration':''),version=(await operator.get('/admin/backups')).version;
const command={requestId:randomUUID(),expectedVersion:version},ack=await operator.post(path,command);
assert.deepEqual(await operator.post(path,command),ack);await operator.post(path,{...command,expectedVersion:version+1},409);
await player.call('/admin/backups',{status:403});await player.post(path,command,403);
let job;for(let i=0;i<60;i++){const status=await operator.get('/admin/backups');job=status.jobs.find(j=>j.id===ack.id);if(['COMPLETE','FAILED','INTERRUPTED'].includes(job?.state))break;await new Promise(r=>setTimeout(r,1000));}
assert.equal(job?.state,'COMPLETE','logical archive must complete');
const response=await operator.call('/admin/backups/'+ack.id+'/content');privateHeaders(response);assert.match(response.headers.get('content-disposition'),/attachment/);
const bytes=Buffer.from(await response.arrayBuffer());assert.equal(bytes.subarray(0,2).toString(),'PK');assert.equal(bytes.length,job.bytes);
await writeFile(file,bytes,{flag:'wx'});await player.call('/admin/backups/'+ack.id+'/content',{status:403});
if(kind==='migration'){assert.equal((await operator.get('/admin/backups')).paused,true);await operator.post('/campaigns',{requestId:randomUUID(),name:'must remain stopped'},503);}
console.log('PASS: complete database/assets deployment archive downloaded; operator-only access, exact receipts and persistent source write fence verified');
