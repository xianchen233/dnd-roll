import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';
import {lifecycleFixture} from './module-fixture.mjs';

const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
const player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
const fresh=(expectedVersion,name)=>({requestId:randomUUID(),expectedVersion,name});
async function snapshot(e){
 const root=e.root,card=root+'/characters/'+e.character;
 const operations=await dm.get(root+'/module-operations'),history={};
 for(const op of operations)history[op.id]=await dm.get(root+'/module-operations/'+op.id+'/executions');
 return {operations,history,instance:await dm.get(root+'/module-instance'),scene:await dm.get(root+'/scene'),tokens:await dm.get(root+'/scenes/'+e.scene+'/tokens'),card:await dm.get(card),access:await dm.get(card+'/access'),cardHistory:await dm.get(card+'/history'),initiative:await dm.get(root+'/initiative'),initiativeHistory:await dm.get(root+'/initiative/history'),combat:await dm.get(root+'/combat'),turnHistory:await dm.get(root+'/combat/history'),notes:await dm.get(root+'/notes/export?format=json'),timeline:await dm.get(root+'/timeline/export?format=json'),source:await dm.get(e.source)};
}
async function review(root,id,version){
 const campaignVersion=(await dm.get(root+'/scenes')).campaignVersion;
 const impact=await dm.post(root+'/module-operations/'+id+'/impact',{expectedVersion:version,campaignVersion});
 return {requestId:randomUUID(),expectedVersion:version,campaignVersion,impactHash:impact.impactHash};
}
if(mode==='seed'){
 const library=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ manual combinations '+randomUUID().slice(0,8)},201),source='/campaigns/'+library.id;
 const sheet=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet;
 sheet.name='Manual persistent guard';sheet.combat.maxHp=20;sheet.combat.hp=20;
 sheet.resources=[{id:'uses',name:'Manual uses',kind:'CUSTOM',current:3,max:3,recovery:'Manual',source:{book:'Original fixture',revision:'1',rules:'CUSTOM',detail:''}}];
 const pack=lifecycleFixture(sheet),body=new FormData();body.append('command',new Blob([JSON.stringify({requestId:randomUUID(),expectedVersion:0,createCopy:false,confirmLegacy:false})],{type:'application/json'}));body.append('file',new Blob([pack.bytes],{type:'application/zip'}),'fixture.zip');
 const imported=await(await dm.call(source+'/module-packages',{method:'POST',multipart:body})).json();
 const copy=await dm.post(source+'/module-customizations',{...fresh(imported.version,'Manual source'),packageId:imported.id});
 const launched=await dm.post(source+'/module-customizations/'+copy.id+'/campaigns',fresh(0,'Manual operation party')),root='/campaigns/'+launched.id;
 const invitation=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:(await dm.get(root+'/members')).campaignVersion},201);await player.post('/invitations/accept',{token:invitation.token});
 const instance=await dm.get(root+'/module-instance'),character=instance.mapping.units[pack.unit],scene=instance.mapping.scenes[pack.scene],token=instance.mapping.placements[pack.placement],note=instance.mapping.documents[pack.secret],card=root+'/characters/'+character;
 await dm.post(card+'/resources',{requestId:randomUUID(),expectedVersion:0,resourceId:'hp',delta:-3,reason:'Preexisting damage'});
 await dm.post(root+'/module-instance',{requestId:randomUUID(),expectedVersion:0,progress:{[pack.scene]:'Played before manual trigger'}});
 await dm.post(root+'/messages',{requestId:randomUUID(),expectedVersion:(await dm.get(root+'/scenes')).campaignVersion,characterId:'',kind:'OOC',text:'Old RP survives combinations',replyTo:'',attachments:[]});
 const id=randomUUID(),path=root+'/module-operations/'+id;
 const save={...fresh(0,'揭露守卫、公开文字与准备先攻'),steps:[{kind:'REVEAL_TOKEN',sceneId:scene,tokenId:token,expectedVersion:0},{kind:'REVEAL_CHARACTER',characterId:character,expectedVersion:1},{kind:'PUBLISH_TEXT',noteId:note,expectedVersion:0},{kind:'PREPARE_ENCOUNTER',expectedVersion:0,participants:[{characterId:character,expectedVersion:1,kind:'MONSTER',group:'',surprised:false,advantage:[],disadvantage:[]}],reason:'Explicit manual preparation'}]};
 const saved=await dm.post(path,save),beforeSheet=(await dm.get(card)).sheet;
 assert.equal((await player.get(root+'/scene')).tokens.length,0);const beforeNotes=(await player.get(root+'/notes')).notes.length;
 const command=await review(root,id,1);assert.equal((await dm.get(root+'/initiative')).status,'EMPTY');
 const result=await dm.post(path+'/executions',command);assert.equal(result.actor,required('E2E_DM_USERNAME'));assert.equal(result.results.length,4);
 assert.equal((await dm.get(root+'/initiative')).status,'DRAFT');assert.equal((await dm.get(root+'/combat')).status,'IDLE');assert.deepEqual((await dm.get(card)).sheet,beforeSheet);
 assert.equal((await player.get(root+'/scene')).tokens.length,1);assert.equal((await player.get(root+'/notes')).notes.length,beforeNotes+1);assert.equal((await dm.get(root+'/notes/'+note)).visibility,'DM');
 const draft=await dm.get(root+'/initiative');await dm.post(root+'/initiative/confirm',{requestId:randomUUID(),expectedVersion:draft.version,order:draft.entries.map(e=>e.characterId),reason:'Explicit DM order confirmation'});
 const startId=randomUUID(),startPath=root+'/module-operations/'+startId;
 const startSave={...fresh(0,'启动已确认遭遇'),steps:[{kind:'START_ENCOUNTER',expectedVersion:0,initiativeVersion:2,reason:'Explicit confirmed start'}]};
 const startSaved=await dm.post(startPath,startSave),start=await review(root,startId,1),started=await dm.post(startPath+'/executions',start);assert.equal((await dm.get(root+'/combat')).status,'ACTIVE');
 await dm.post(root+'/combat',{requestId:randomUUID(),expectedVersion:1,kind:'STOP',characterId:'',order:[],reason:'Later independent stop'});
 await dm.post(root+'/scenes/'+scene+'/tokens/'+token,{requestId:randomUUID(),expectedVersion:1,name:'Later hidden guard',x:7,y:8,hidden:true,controllers:[required('E2E_DM_USERNAME')]});
 await dm.post(card+'/access',{requestId:randomUUID(),expectedVersion:2,controllers:[required('E2E_DM_USERNAME')],hidden:true,shareFull:false,approveImport:false,reason:'Later hide'});
 await dm.post(card+'/resources',{requestId:randomUUID(),expectedVersion:3,resourceId:'hp',delta:-2,reason:'Later damage'});
 const edit={...fresh(1,'重新编辑后的组合'),steps:[{kind:'REVEAL_TOKEN',sceneId:scene,tokenId:token,expectedVersion:2}]},edited=await dm.post(path,edit);
 const e={root,source:source+'/module-customizations/'+copy.id,scene,character,id,startId,save,saved,command,result,startSave,startSaved,start,started,edit,edited};
 e.snapshot=await snapshot(e);
 for(const [ep,b,ack]of [[path,save,saved],[path,edit,edited],[path+'/executions',command,result],[startPath,startSave,startSaved],[startPath+'/executions',start,started]])assert.deepEqual(await dm.post(ep,b),ack);
 assert.deepEqual(await snapshot(e),e.snapshot);await writeFile(file,JSON.stringify(e,null,2),{flag:'wx'});
 console.log('OPERATIONS VERIFIED: editable typed combinations use original reveal/text/initiative/turn services, retain damaged sheets/progress/RP and replay once after later edits/hiding/stopped combat');
}else{
 const e=JSON.parse(await readFile(file,'utf8'));assert.deepEqual(await snapshot(e),e.snapshot);
 const path=e.root+'/module-operations/'+e.id,startPath=e.root+'/module-operations/'+e.startId;
 for(const [ep,b,ack]of [[path,e.save,e.saved],[path,e.edit,e.edited],[path+'/executions',e.command,e.result],[startPath,e.startSave,e.startSaved],[startPath+'/executions',e.start,e.started]])assert.deepEqual(await dm.post(ep,b),ack);
 for(const ep of [e.root+'/module-operations',path+'/executions',startPath+'/executions'])privateHeaders(await player.call(ep,{status:403}));
 await player.call(path+'/executions',{method:'POST',json:e.command,status:403});
 assert.equal((await player.get(e.root+'/scene')).tokens.length,0);assert.equal((await player.get(e.root+'/characters')).characters.length,0);
 assert.deepEqual(await snapshot(e),e.snapshot);
 console.log('OPERATIONS VERIFIED: fresh sessions after actual restart/isolated restore retain exact definitions/evidence/actor/receipts and all later resources/hiding/progress/RP; old retries cannot reveal, reroll, republish or restart');
}
