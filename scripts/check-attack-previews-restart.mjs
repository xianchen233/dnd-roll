import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required,privateHeaders} from './acceptance-support.mjs';

const [mode,file]=process.argv.slice(2);assert.ok(['seed','verify','resume'].includes(mode)&&file);
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD'));
const player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
async function snapshot(e){const out={};for(const suffix of ['/attack-previews','/rolls','/actions'])out[suffix]=await dm.get(e.root+suffix);out.cards=[];for(const id of e.ids)out.cards.push({card:await dm.get(e.root+'/characters/'+id),history:await dm.get(e.root+'/characters/'+id+'/history')});out.public=await player.get(e.root+'/attack-results');return out;}
async function command(root,p,kind,changes=[],correctionOf='',summary=''){
 const body={requestId:randomUUID(),expectedVersion:p.version,kind,previewHash:p.hash,changes,correctionOf,reason:'Private acceptance DM review',summary},path=root+'/attack-previews/'+p.id+'/events',ack=await dm.post(path,body);return {path,body,ack};
}
if(mode==='seed'){
 const created=await dm.post('/campaigns',{requestId:randomUUID(),name:'ZZZ A05 restart '+randomUUID().slice(0,8)},201),root='/campaigns/'+created.id;
 const invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});
 const sample=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet,ids=[];
 for(const name of ['A05 source','A05 target']){const sheet=structuredClone(sample);sheet.name=name;sheet.attacks=[];sheet.combat.maxHp=20;sheet.combat.hp=20;sheet.combat.tempHp=5;const ack=await dm.post(root+'/characters',{requestId:randomUUID(),expectedVersion:(await dm.get(root+'/characters')).campaignVersion,sheet});ids.push(ack.id);}
 const entry=(await dm.get(root+'/content')).entries.find(e=>e.type==='ATTACK');await dm.post(root+'/characters/'+ids[0]+'/content',{requestId:randomUUID(),expectedVersion:0,policyVersion:(await dm.get(root+'/characters')).campaignVersion,entryId:entry.id,ability:'STR',castingId:'',reason:'Trusted SRD catalog'});
 const posts=[];
 async function create(reviewed=true){const s=await dm.get(root+'/characters/'+ids[0]),t=await dm.get(root+'/characters/'+ids[1]),body={requestId:randomUUID(),characterId:s.id,sourceVersion:s.version,targetId:t.id,targetVersion:t.version,attackId:s.sheet.attacks[0].id,reviewed,exceptions:[],advantage:[],disadvantage:[],immunities:[],resistances:[],vulnerabilities:[]},path=root+'/attack-previews',ack=await dm.post(path,body);posts.push({path,body,ack});return dm.get(path+'/'+ack.id);}
 let p=await create();assert.equal(p.status,'PREVIEW');const evidence=structuredClone(p.evidence),change=(version,hp)=>[{characterId:ids[1],expectedVersion:version,hp,tempHp:0,resources:[]}];
 posts.push(await command(root,p,'CONFIRM',change(0,18)));p=await dm.get(root+'/attack-previews/'+p.id);posts.push(await command(root,p,'APPLY'));p=await dm.get(root+'/attack-previews/'+p.id);const original=p.events.at(-1).id;
 await dm.post(root+'/characters/'+ids[1]+'/resources',{requestId:randomUUID(),expectedVersion:1,resourceId:'hp',delta:-2,reason:'Later independent damage'});
 posts.push(await command(root,p,'CORRECT',change(2,17),original));p=await dm.get(root+'/attack-previews/'+p.id);assert.deepEqual(p.evidence,evidence);posts.push(await command(root,p,'PUBLISH',[],'','Explicit published summary'));
 const pub=await player.get(root+'/attack-results');assert.equal(pub.length,1);assert.deepEqual(Object.keys(pub[0]).sort(),['actor','at','id','name','summary','targetName']);assert.equal((await player.get(root+'/rolls')).rolls.length,0);
 const confirmed=await create();posts.push(await command(root,confirmed,'CONFIRM',change(3,16)));await create();const manual=await create(false);assert.equal(manual.status,'MANUAL_REQUIRED');
 const e={root,ids,posts};e.snapshot=await snapshot(e);for(const post of posts)assert.deepEqual(await dm.post(post.path,post.body),post.ack);assert.deepEqual(await snapshot(e),e.snapshot);await writeFile(file,JSON.stringify(e,null,2),{flag:'wx'});
 console.log('ATTACK PREVIEWS VERIFIED: pending and confirmed previews, saved dark rolls, explicit application, linked correction, separate summary and exact retries survive later independent damage without repeated effects');
}else{
 const e=JSON.parse(await readFile(file,'utf8'));assert.deepEqual(await snapshot(e),e.snapshot);
 for(const p of e.posts)assert.deepEqual(await dm.post(p.path,p.body),p.ack);
 privateHeaders(await player.call(e.root+'/attack-previews',{status:403}));for(const p of e.snapshot['/attack-previews'])privateHeaders(await player.call(e.root+'/attack-previews/'+p.id,{status:403}));
 assert.deepEqual(await snapshot(e),e.snapshot);
 if(mode==='resume'){
  const pending=e.snapshot['/attack-previews'].find(p=>p.status==='PREVIEW');assert.ok(pending);const target=await dm.get(e.root+'/characters/'+pending.evidence.target.id);assert.equal(target.version,pending.evidence.target.version);const beforeRolls=await dm.get(e.root+'/rolls');
  const change=[{characterId:target.id,expectedVersion:target.version,hp:Math.max(0,target.sheet.combat.hp-1),tempHp:target.sheet.combat.tempHp,resources:[]}];
  const confirm=await command(e.root,pending,'CONFIRM',change);const confirmed=await dm.get(e.root+'/attack-previews/'+pending.id);const apply=await command(e.root,confirmed,'APPLY');
  for(const p of [confirm,apply])assert.deepEqual(await dm.post(p.path,p.body),p.ack);
  const applied=await dm.get(e.root+'/attack-previews/'+pending.id),after=await dm.get(e.root+'/characters/'+target.id);assert.equal(applied.status,'APPLIED');assert.deepEqual(applied.evidence,pending.evidence);assert.equal(applied.hash,pending.hash);assert.equal(after.version,target.version+1);assert.equal(after.sheet.combat.hp,change[0].hp);assert.deepEqual(await dm.get(e.root+'/rolls'),beforeRolls);
  console.log('ATTACK RESUMED VERIFIED: an actually restored pending preview confirms and applies once using original saved dice/evidence/hash and versions; retries neither reroll nor spend twice');
 }else console.log('ATTACK PREVIEWS VERIFIED: fresh sessions after actual restart/isolated restore preserve exact evidence/hash, all rolls/actions/card histories, pending/confirmed status, actors, correction/publication and original receipts; retries neither reroll nor overwrite later state');
}
