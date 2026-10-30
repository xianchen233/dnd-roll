import assert from 'node:assert/strict';
import {randomUUID,createHash} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import {lifecycleFixture} from './module-fixture.mjs';
import {sourcePng,privateHeaders} from './acceptance-support.mjs';

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
export async function materialSnapshot(dm,player,e){
 const notes=await dm.get(e.root+'/notes/export?format=json'),publicNotes=await player.get(e.root+'/notes/export?format=json'),images={};
 for(const image of notes.attachments){const r=await dm.call(e.root+'/note-attachments/'+image.id+'/content');privateHeaders(r);images[image.id]=sha(Buffer.from(await r.arrayBuffer()));}
 for(const image of publicNotes.attachments){const r=await player.call(e.root+'/note-attachments/'+image.id+'/content');privateHeaders(r);assert.equal(sha(Buffer.from(await r.arrayBuffer())),images[image.id]);}
 for(const image of e.originalImages)privateHeaders(await player.call(e.root+'/note-attachments/'+image+'/content',{status:404}));
 return {notes,publicNotes,images,instance:await dm.get(e.root+'/module-instance'),scene:await dm.get(e.root+'/scene'),operations:await dm.get(e.root+'/module-operations'),executions:await dm.get(e.path+'/executions'),sourceHistory:await dm.get(e.root+'/notes/'+e.note+'/history')};
}
export async function prepareMaterial(dm,player){
 const library=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A03f library '+randomUUID()},201),source='/campaigns/'+library.id;
 const card=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet;
 const pack=lifecycleFixture(card),body=new FormData();body.append('command',new Blob([JSON.stringify({requestId:randomUUID(),expectedVersion:0,createCopy:false,confirmLegacy:false})],{type:'application/json'}));body.append('file',new Blob([pack.bytes],{type:'application/zip'}),'fixture.zip');
 const imported=await(await dm.call(source+'/module-packages',{method:'POST',multipart:body})).json();const copy=await dm.post(source+'/module-customizations',{requestId:randomUUID(),expectedVersion:imported.version,packageId:imported.id,name:'Illustrated preparation'});
 const party=await dm.post(source+'/module-customizations/'+copy.id+'/campaigns',{requestId:randomUUID(),expectedVersion:0,name:'ZZZ A03f party '+randomUUID()}),root='/campaigns/'+party.id;
 const invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});const cv=(await dm.get(root+'/members')).campaignVersion;
 const image=sourcePng(),upload=new FormData();upload.append('command',new Blob([JSON.stringify({requestId:randomUUID(),expectedVersion:cv,visibility:'DM'})],{type:'application/json'}));upload.append('file',new Blob([image],{type:'image/png'}),'synthetic.png');
 const attachment=await(await dm.call(root+'/note-attachments',{method:'POST',multipart:upload})).json();
 const note=await dm.post(root+'/notes',{requestId:randomUUID(),expectedVersion:cv,visibility:'DM',title:'附图线索 A03f',text:'明确公开的当前文字 <literal>',attachments:[attachment.id]});
 const id=randomUUID(),path=root+'/module-operations/'+id,save={requestId:randomUUID(),expectedVersion:0,name:'公开附图线索 A03f',steps:[{kind:'PUBLISH_MATERIAL',noteId:note.id,expectedVersion:0}]};await dm.post(path,save);
 return {root,path,id,note:note.id,campaign:party.id,originalImages:[attachment.id],sourceSha:sha(image),save};
}
export async function finishMaterial(dm,player,e,command,result){
 e.command=command;e.result=result;const all=(await player.get(e.root+'/notes')).notes;const published=all.find(n=>n.title==='附图线索 A03f');assert.ok(published);assert.equal(published.attachments.length,1);assert.ok(!e.originalImages.includes(published.attachments[0].id));
 e.published=published.id;const bytes=await player.call(e.root+'/note-attachments/'+published.attachments[0].id+'/content');assert.equal(sha(Buffer.from(await bytes.arrayBuffer())),e.sourceSha);
 await dm.post(e.root+'/notes/'+e.note,{requestId:randomUUID(),expectedVersion:0,title:'Later private material',text:'LATER_PRIVATE_A03F_TEXT',attachments:e.originalImages,reason:'PRIVATE_A03F_REASON'});
 const pendingId=randomUUID();e.pendingPath=e.root+'/module-operations/'+pendingId;await dm.post(e.pendingPath,{requestId:randomUUID(),expectedVersion:0,name:'恢复后显式公开',steps:[{kind:'PUBLISH_MATERIAL',noteId:e.note,expectedVersion:1}]});
 const cv=(await dm.get(e.root+'/members')).campaignVersion,review=await dm.post(e.pendingPath+'/impact',{expectedVersion:1,campaignVersion:cv});e.pending={requestId:randomUUID(),expectedVersion:1,campaignVersion:cv,impactHash:review.impactHash};
 e.snapshot=await materialSnapshot(dm,player,e);assert.ok(!JSON.stringify(e.snapshot.publicNotes).includes('LATER_PRIVATE_A03F'));
 assert.deepEqual(await dm.post(e.path+'/executions',command),result);assert.deepEqual(await materialSnapshot(dm,player,e),e.snapshot);return e;
}
export async function verifyMaterial(dm,player,e){
 assert.deepEqual(await materialSnapshot(dm,player,e),e.snapshot);assert.deepEqual(await dm.post(e.path+'/executions',e.command),e.result);assert.deepEqual(await materialSnapshot(dm,player,e),e.snapshot);
 await player.call(e.path+'/executions',{status:403});await player.post(e.path+'/executions',e.command,403);
 console.log('MATERIAL VERIFIED: independent public image bytes, private source/history, ordered note snapshots and original receipt survive; replay cannot republish later private text');
}
