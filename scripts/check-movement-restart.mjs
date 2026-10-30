import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify','resume'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
const endpoint=e=>`${e.root}/tokens/${e.token}/movement`;
async function snapshot(e){return {dm:await dm.get(endpoint(e)),player:await player.get(endpoint(e)),scene:await dm.get(e.root+'/scene'),card:await dm.get(e.root+'/characters/'+e.card),combat:await dm.get(e.root+'/combat')};}
const body=(v,path)=>({requestId:randomUUID(),expectedVersion:v.token.version,sceneVersion:v.sceneVersion,characterVersion:v.characterVersion,budgetVersion:v.budgetVersion,turnKey:v.turnKey,reviewed:true,path,override:false,cost:0,reason:''});
if(mode==='seed'){
 const campaign=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A06 restart '+randomUUID().slice(0,8)},201),root='/campaigns/'+campaign.id,invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});
 const sheet=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet;sheet.name='A06 restart walker';sheet.combat.speed=30;
 const card=await player.post(root+'/characters',{requestId:randomUUID(),expectedVersion:(await player.get(root+'/characters')).campaignVersion,sheet}),scene=await dm.get(root+'/scene'),placed=await dm.post(root+'/scenes/'+scene.id+'/tokens',{requestId:randomUUID(),expectedVersion:scene.version,name:sheet.name,x:1,y:1,hidden:false,controllers:[required('E2E_PLAYER_USERNAME')]}),token=placed.token.id;
 await dm.post(root+'/characters/'+card.id+'/placements',{requestId:randomUUID(),expectedVersion:0,tokenId:token,tokenVersion:0,reason:'Placement'});const version=(await dm.get(root+'/characters/'+card.id)).version;
 await dm.post(root+'/initiative',{requestId:randomUUID(),expectedVersion:0,participants:[{characterId:card.id,expectedVersion:version,kind:'PC',group:'',surprised:false,advantage:[],disadvantage:[]}],reason:'Roster'});await dm.post(root+'/initiative/confirm',{requestId:randomUUID(),expectedVersion:1,order:[card.id],reason:'Confirm'});await dm.post(root+'/combat',{requestId:randomUUID(),expectedVersion:0,kind:'START',characterId:'',order:[],reason:'Start'});
 const e={root,token,card:card.id,posts:[]},steps=[{x:2,y:2,mode:'WALK',difficult:false},{x:2,y:1,mode:'CRAWL',difficult:true},{x:1,y:1,mode:'WALK',difficult:false}],d=body(await player.get(endpoint(e)),steps);assert.equal((await player.post(endpoint(e)+'/preview',d)).cost,25);assert.equal((await player.get(endpoint(e))).used,0);
 const ack=await player.post(endpoint(e)+'/paths',d);e.posts.push({path:endpoint(e)+'/paths',body:d,ack,actor:'player'});
 const v=await dm.get(endpoint(e)),a={requestId:randomUUID(),expectedVersion:v.token.version,sceneVersion:v.sceneVersion,characterVersion:v.characterVersion,budgetVersion:v.budgetVersion,turnKey:v.turnKey,used:10,allowance:60,reason:'Private DM final budget correction'};
 e.posts.push({path:endpoint(e)+'/adjustments',body:a,ack:await dm.post(endpoint(e)+'/adjustments',a),actor:'dm'});e.snapshot=await snapshot(e);assert.equal(e.snapshot.player.used,10);assert.equal(e.snapshot.player.allowance,60);assert.deepEqual(e.snapshot.player.history,[]);
 e.draft=body(e.snapshot.player,[{x:2,y:1,mode:'WALK',difficult:false}]);await writeFile(file,JSON.stringify(e,null,2),{flag:'wx'});
 console.log('MOVEMENT VERIFIED: actual declared loop and mixed terrain/modes, shared character budget, private DM final correction and original receipts captured');
}else{
 const e=JSON.parse(await readFile(file,'utf8'));assert.deepEqual(await snapshot(e),e.snapshot);
 for(const p of e.posts)assert.deepEqual(await (p.actor==='dm'?dm:player).post(p.path,p.body),p.ack);
 assert.deepEqual(await snapshot(e),e.snapshot);privateHeaders(await dm.call(endpoint(e)));privateHeaders(await player.call(endpoint(e)));assert.deepEqual(e.snapshot.player.history,[]);assert.ok(!JSON.stringify(e.snapshot.player).includes('Private DM'));
 if(mode==='resume'){
  const before=await player.get(endpoint(e)),d=e.draft??body(before,[{x:before.token.x+1,y:before.token.y,mode:'WALK',difficult:false}]);const p=await player.post(endpoint(e)+'/preview',d),result=await player.post(endpoint(e)+'/paths',d);assert.deepEqual(await player.post(endpoint(e)+'/paths',d),result);assert.equal((await player.get(endpoint(e))).used,before.used+p.cost);
  console.log('MOVEMENT RESUMED VERIFIED: restored budget accepts original current-context draft once, retains DM allowance and single-effect receipt');
 }else console.log('MOVEMENT VERIFIED: fresh sessions retain exact position, card/source, turn, complete spent budget, private history and all original receipts after restart/isolated restore');
}
