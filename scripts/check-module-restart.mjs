import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,hash,privateHeaders} from './acceptance-support.mjs';
import {moduleFixture} from './module-fixture.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify'].includes(mode));assert.ok(file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
async function upload(root,command,bytes,suffix=''){const form=new FormData();if(command)form.append('command',new Blob([JSON.stringify(command)],{type:'application/json'}));form.append('file',new Blob([bytes],{type:'application/zip'}),'ignored.zip');return(await dm.call(root+'/module-packages'+suffix,{method:'POST',multipart:form})).json();}
if(mode==='seed'){
 const campaign=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A01 restart '+randomUUID().slice(0,8)},201),root='/campaigns/'+campaign.id,invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});
 const version=(await dm.get(root+'/members')).campaignVersion,pack=moduleFixture({name:'SECRET_A01_RESTART'}),command={requestId:randomUUID(),expectedVersion:version,createCopy:false,confirmLegacy:false},preview=await upload(root,null,pack.bytes,'/preview');assert.equal(preview.missingDependencies.length,1);
 const before=await dm.get(root+'/scene'),ack=await upload(root,command,pack.bytes),copyCommand={...command,requestId:randomUUID(),expectedVersion:ack.version,createCopy:true},copyAck=await upload(root,copyCommand,pack.bytes),listing=await dm.get(root+'/module-packages');assert.equal(listing.packages.length,2);assert.notEqual(ack.id,copyAck.id);assert.deepEqual(await dm.get(root+'/scene'),before);
 await writeFile(file,JSON.stringify({root,command,ack,copyCommand,copyAck,listing,bytes:pack.bytes.toString('base64'),sha:hash(pack.bytes),before},null,2),{flag:'wx'});
 console.log('MODULE VERIFIED: seed stores private inert ZIP/images, exact missing dependency, distinct copies and original receipts');
}else{
 const saved=JSON.parse(await readFile(file,'utf8')),bytes=Buffer.from(saved.bytes,'base64');assert.deepEqual(await dm.get(saved.root+'/module-packages'),saved.listing);assert.deepEqual(await upload(saved.root,saved.command,bytes),saved.ack);assert.deepEqual(await upload(saved.root,saved.copyCommand,bytes),saved.copyAck);assert.deepEqual(await dm.get(saved.root+'/scene'),saved.before);
 const download=await dm.call(saved.root+'/module-packages/'+saved.ack.id+'/content');privateHeaders(download);assert.equal(hash(Buffer.from(await download.arrayBuffer())),saved.sha);
 for(const suffix of ['', '/'+saved.ack.id+'/content']){const denied=await player.call(saved.root+'/module-packages'+suffix,{status:403});privateHeaders(denied);}
 assert.doesNotMatch(JSON.stringify(await player.get(saved.root+'/scene')),/SECRET_A01_RESTART|moduleId|missingDependencies/);
 console.log('MODULE VERIFIED: fresh sessions preserve exact ZIP/images/dependencies, copies, original receipts and DM-only access after real restart/restore');
}
