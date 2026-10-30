import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,hash} from './acceptance-support.mjs';
import {prepareMaterial,finishMaterial,verifyMaterial} from './material-evidence.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify','resume'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
if(mode==='seed'){
 const e=await prepareMaterial(dm,player),cv=(await dm.get(e.root+'/members')).campaignVersion,impact=await dm.post(e.path+'/impact',{expectedVersion:1,campaignVersion:cv}),command={requestId:randomUUID(),expectedVersion:1,campaignVersion:cv,impactHash:impact.impactHash};
 const result=await dm.post(e.path+'/executions',command);await finishMaterial(dm,player,e,command,result);await writeFile(file,JSON.stringify({entries:[e]},null,2),{flag:'wx'});await verifyMaterial(dm,player,e);
}else{
 const {entries}=JSON.parse(await readFile(file,'utf8'));for(const e of entries){await verifyMaterial(dm,player,e);if(mode==='resume'){
  const before=(await player.get(e.root+'/notes')).notes.length,result=await dm.post(e.pendingPath+'/executions',e.pending);assert.deepEqual(await dm.post(e.pendingPath+'/executions',e.pending),result);assert.equal((await player.get(e.root+'/notes')).notes.length,before+1);assert.equal((await dm.get(e.pendingPath+'/executions')).length,1);
  const note=await player.get(e.root+'/notes/'+result.results[0].id);assert.equal(note.text,'LATER_PRIVATE_A03F_TEXT');assert.equal(note.attachments.length,1);assert.ok(!e.originalImages.includes(note.attachments[0].id));const image=await player.call(e.root+'/note-attachments/'+note.attachments[0].id+'/content');assert.equal(hash(Buffer.from(await image.arrayBuffer())),e.sourceSha);
  console.log('MATERIAL RESUMED VERIFIED: restored exact reviewed publication commits once with original public image bytes');
 }}
}
