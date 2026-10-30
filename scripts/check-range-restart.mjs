import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
const player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
async function snapshot(e){return {dm:await dm.get(e.root+'/scene'),player:await player.get(e.root+'/scene'),combat:await dm.get(e.root+'/combat'),rolls:await dm.get(e.root+'/rolls')};}
async function verify(e){
 assert.deepEqual(await snapshot(e),e.snapshot);
 for(const entry of e.measurements){const session=entry.actor==='dm'?dm:player;const response=await session.call(e.root+'/range-hints/preview',{method:'POST',json:entry.body,status:entry.status});privateHeaders(response);assert.deepEqual(await response.json(),entry.result);}
 assert.deepEqual(await snapshot(e),e.snapshot);
 console.log('RANGE VERIFIED: fresh sessions recompute exact visible ranges, private DM endpoints, hidden rejection and stale versions without changing scene, dice or combat');
}
if(mode==='seed'){
 const c=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A07 restart '+randomUUID().slice(0,8)},201),root='/campaigns/'+c.id;
 const invitation=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invitation.token});
 const scene=await dm.get(root+'/scene');
 async function token(name,x,y,hidden){return (await dm.post(root+'/scenes/'+scene.id+'/tokens',{requestId:randomUUID(),expectedVersion:(await dm.get(root+'/scene')).version,name,x,y,hidden,controllers:[]})).token;}
 const a=await token('Range source',1,1,false),b=await token('Range target',4,3,false),secret=await token('PRIVATE range endpoint',9,9,true);
 const current=await player.get(root+'/scene'),body={sceneId:scene.id,sceneVersion:current.version,sourceId:a.id,sourceVersion:a.version,targetId:b.id,targetVersion:b.version,profile:'GRID_5R_5FT_RANGE_V1',reviewed:true,mode:'RANGED',normalFeet:10,longFeet:15};
 const measurements=[];
 for(const [actor,request,status]of [['player',body,200],['dm',{...body,targetId:secret.id},200],['player',{...body,targetId:secret.id},404],['player',{...body,sceneVersion:current.version-1},409]]){
  const session=actor==='dm'?dm:player,result=await session.post(root+'/range-hints/preview',request,status);measurements.push({actor,body:request,status,result});
 }
 const e={root,measurements};e.snapshot=await snapshot(e);await writeFile(file,JSON.stringify(e,null,2),{flag:'wx'});await verify(e);
}else await verify(JSON.parse(await readFile(file,'utf8')));
