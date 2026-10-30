import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';
const [mode,file]=process.argv.slice(2);assert.ok(['verify','resume'].includes(mode)&&file);
const e=JSON.parse(await readFile(file,'utf8')),dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
async function snapshot(){const out={};for(const suffix of ['/attack-previews','/rolls','/actions'])out[suffix]=await dm.get(e.root+suffix);out.cards=[];for(const id of e.ids)out.cards.push({card:await dm.get(e.root+'/characters/'+id),history:await dm.get(e.root+'/characters/'+id+'/history')});out.public=await player.get(e.root+'/attack-results');return out;}
assert.deepEqual(await snapshot(),e.snapshot);for(const p of e.posts)assert.deepEqual(await dm.post(p.path,p.body),p.ack);assert.deepEqual(await snapshot(),e.snapshot);
const path=e.root+'/attack-previews/'+e.pending.id;privateHeaders(await player.call(path+'/damage-effect',{status:403}));assert.deepEqual(await dm.get(path+'/damage-effect'),e.pendingPlan);
if(mode==='resume'){
 const p=e.pending,target=await dm.get(e.root+'/characters/'+p.evidence.target.id),rolls=await dm.get(e.root+'/rolls'),body={requestId:randomUUID(),expectedVersion:p.version,kind:'APPLY',previewHash:p.hash,changes:[],correctionOf:'',reason:'Continue saved damage effect after real restart',summary:''};const ack=await dm.post(path+'/events',body);assert.deepEqual(await dm.post(path+'/events',body),ack);
 const after=await dm.get(e.root+'/characters/'+target.id);assert.equal(after.version,target.version+1);assert.equal(after.sheet.combat.hp,e.pendingPlan.hp);assert.equal(after.sheet.combat.tempHp,e.pendingPlan.tempHp);assert.deepEqual(after.sheet.resources,target.sheet.resources);assert.deepEqual(after.sheet.conditions,target.sheet.conditions);assert.deepEqual(await dm.get(e.root+'/rolls'),rolls);
 for(const post of e.posts)assert.deepEqual(await dm.post(post.path,post.body),post.ack);assert.deepEqual(await dm.get(e.root+'/characters/'+target.id),after);assert.deepEqual((await dm.get(path)).evidence,p.evidence);await writeFile(file+'.resumed.json',JSON.stringify(await snapshot(),null,2),{flag:'wx'});
 console.log('DAMAGE EFFECT RESUMED VERIFIED: saved server confirmation applies once after restart with unchanged resources, conditions and dice; old receipts cannot overwrite it');
}else console.log('DAMAGE EFFECT VERIFIED: fresh sessions preserve browser-created evidence, derived changes, correction, private dice, pending confirmation and all original receipts');
