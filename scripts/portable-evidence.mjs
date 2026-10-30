import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile} from 'node:fs/promises';
import {hash,privateHeaders} from './acceptance-support.mjs';
import {lifecycleFixture} from './module-fixture.mjs';

async function upload(dm,root,bytes,preview=false){const cv=(await dm.get(root+'/module-packages')).campaignVersion,form=new FormData();if(!preview)form.append('command',new Blob([JSON.stringify({requestId:randomUUID(),expectedVersion:cv,createCopy:false,confirmLegacy:false})],{type:'application/json'}));form.append('file',new Blob([bytes],{type:'application/zip'}),'portable.zip');return(await dm.call(root+'/module-packages'+(preview?'/preview':''),{method:'POST',multipart:form})).json();}
async function join(dm,player,root){const invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:(await dm.get(root+'/members')).campaignVersion},201);await player.post('/invitations/accept',{token:invite.token});}
async function runtime(dm,player,root){const cards=await dm.get(root+'/characters');return {instance:await dm.get(root+'/module-instance'),scene:await dm.get(root+'/scene'),cards:await Promise.all(cards.characters.map(c=>dm.get(root+'/characters/'+c.id))),notes:await dm.get(root+'/notes/export?format=json'),publicNotes:await player.get(root+'/notes/export?format=json'),initiative:await dm.get(root+'/initiative'),combat:await dm.get(root+'/combat')};}
export async function preparePortable(dm,player){
 const library=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A03g library '+randomUUID()},201),root='/campaigns/'+library.id;await join(dm,player,root);
 const sheet=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet,pack=lifecycleFixture(sheet),imported=await upload(dm,root,pack.bytes);
 const copy=await dm.post(root+'/module-customizations',{requestId:randomUUID(),expectedVersion:imported.version,packageId:imported.id,name:'便携定义副本 A03g'}),path=root+'/module-customizations/'+copy.id;
 const live=await dm.post(path+'/campaigns',{requestId:randomUUID(),expectedVersion:0,name:'ZZZ A03g existing live '+randomUUID()}),liveRoot='/campaigns/'+live.id;await join(dm,player,liveRoot);
 const before=await dm.get(path),document=structuredClone(before.document),trigger=randomUUID(),start=randomUUID(),plain=randomUUID();
 const node=(id,kind,name)=>({id,kind,name,text:'原样文字 <literal>',visibility:'DM',nodeIds:[],assetIds:[],dependencyIds:[]});document.nodes.push(node(trigger,'TRIGGER','便携揭露和准备'),node(start,'TRIGGER','便携开始意图'),node(plain,'DOCUMENT','便携无图线索'));document.nodes.find(n=>n.id===pack.secret).assetIds=[pack.imageId];
 document.operations={formatVersion:1,definitions:[{nodeId:trigger,steps:[{kind:'REVEAL_TOKEN',placementId:pack.placement},{kind:'REVEAL_CHARACTER',unitId:pack.unit},{kind:'PUBLISH_TEXT',documentId:plain},{kind:'PUBLISH_MATERIAL',documentId:pack.secret},{kind:'PREPARE_ENCOUNTER',participants:[{unitId:pack.unit,kind:'MONSTER',group:'',surprised:false,advantage:[],disadvantage:[]}],reason:'准备仍需实例绑定'}]},{nodeId:start,steps:[{kind:'START_ENCOUNTER',reason:'不得携带源团先攻确认'}]}]};
 return {root,path,copy:copy.id,campaign:library.id,liveRoot,sourcePackage:imported.id,sourceSha:hash(pack.bytes),baseline:before.baseline,document,beforeRuntime:await runtime(dm,player,liveRoot)};
}
async function download(dm,root,id){const response=await dm.call(root+'/module-packages/'+id+'/content');privateHeaders(response);return Buffer.from(await response.arrayBuffer());}
export async function portableSnapshot(dm,player,e){return {detail:await dm.get(e.path),reopened:await dm.get(e.reopened),sourceSha:hash(await download(dm,e.root,e.sourcePackage)),templateSha:hash(await download(dm,e.root,e.template.id)),destinationSha:hash(await download(dm,e.destination,e.reimport.id)),runtime:await runtime(dm,player,e.liveRoot)};}
export async function finishPortable(dm,player,e,edit,ack,save,template){
 e.edit=edit;e.ack=ack;e.save=save;e.template=template;assert.deepEqual((await dm.get(e.path)).document,e.document);assert.deepEqual((await dm.get(e.path)).baseline,e.baseline);
 const bytes=await download(dm,e.root,template.id),preview=await upload(dm,e.root,bytes,true);assert.equal(preview.manifest.formatVersion,3);assert.deepEqual(preview.document,e.document);
 e.templateSha=hash(bytes);const destination=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A03g reimport '+randomUUID()},201);e.destination='/campaigns/'+destination.id;e.reimport=await upload(dm,e.destination,bytes);
 const reopened=await dm.post(e.destination+'/module-customizations',{requestId:randomUUID(),expectedVersion:e.reimport.version,packageId:e.reimport.id,name:'重新导入的便携副本'});e.reopened=e.destination+'/module-customizations/'+reopened.id;assert.notEqual(reopened.id,e.copy);assert.deepEqual((await dm.get(e.reopened)).baseline,e.document);
 const pending=await dm.post(e.path+'/campaigns',{requestId:randomUUID(),expectedVersion:1,name:'ZZZ A03h unbound portable '+randomUUID()});e.unboundRoot='/campaigns/'+pending.id;const bindings=await dm.get(e.unboundRoot+'/module-operation-bindings');assert.equal(bindings.bindings.length,e.document.operations.definitions.length);assert.ok(bindings.bindings.every(b=>b.binding.boundVersion===0&&b.operation===null));assert.equal((await dm.get(e.unboundRoot+'/module-operations')).length,0);assert.equal((await dm.get(e.unboundRoot+'/initiative')).status,'EMPTY');assert.equal((await dm.get(e.unboundRoot+'/combat')).status,'IDLE');
 for(const gate of ['update-impact','migration-impact']){const review=await dm.post(e.liveRoot+'/module-instance/'+gate,{expectedVersion:0,customizationVersion:1});assert.ok(review.blockers.includes('/portable-operations'));}
 assert.deepEqual(await runtime(dm,player,e.liveRoot),e.beforeRuntime);e.snapshot=await portableSnapshot(dm,player,e);return e;
}
export async function verifyPortable(dm,player,e){
 assert.deepEqual(await portableSnapshot(dm,player,e),e.snapshot);assert.deepEqual(await dm.post(e.path,e.edit),e.ack);assert.deepEqual(await dm.post(e.path+'/templates',e.save),e.template);assert.deepEqual(await portableSnapshot(dm,player,e),e.snapshot);
 privateHeaders(await player.call(e.path,{status:403}));await player.post(e.path,e.edit,403);privateHeaders(await player.call(e.root+'/module-packages/'+e.template.id+'/content',{status:403}));assert.deepEqual(e.snapshot.runtime,e.beforeRuntime);assert.equal(e.snapshot.sourceSha,e.sourceSha);assert.equal(e.snapshot.templateSha,e.templateSha);assert.equal(e.snapshot.destinationSha,e.templateSha);
 console.log('PORTABLE VERIFIED: v3 stable definitions/source baseline/ZIP image bytes, independent reimport and original edit/save receipts survive; no live execution or resource/history changes');
}
