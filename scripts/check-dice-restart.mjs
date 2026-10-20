// seed -> actual packaged app + PostgreSQL restart -> verify with fresh sessions.
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
const [mode,evidencePath]=process.argv.slice(2);assert.ok(['seed','verify'].includes(mode)&&evidencePath,'Usage: check-dice-restart.mjs seed|verify <outside-repository.json>');
const required=n=>{assert.ok(process.env[n]?.trim(),`Missing ${n}`);return process.env[n];};const base=required('E2E_BASE_URL');
class Session{
 cookies=new Map();csrf;
 async call(path,{method='GET',body,status=200}={}){const headers={Cookie:[...this.cookies].map(([k,v])=>`${k}=${v}`).join('; ')};if(body!==undefined)headers['Content-Type']='application/json';if(method==='POST')headers[this.csrf.headerName]=this.csrf.token;const r=await fetch(new URL('/api'+path,base),{method,headers,cache:'no-store',...(body===undefined?{}:{body:JSON.stringify(body)})});for(const cookie of r.headers.getSetCookie()){const[k,...v]=cookie.split(';')[0].split('=');this.cookies.set(k,v.join('='));}assert.equal(r.status,status,`${method} ${path}`);return r;}
 async get(path){return(await this.call(path)).json();}
 async post(path,body,status=200){const r=await this.call(path,{method:'POST',body,status});return status===204?undefined:r.json();}
 async login(n,p){this.csrf=await this.get('/auth/csrf');await this.post('/auth/login',{username:n,password:p});this.csrf=await this.get('/auth/csrf');return this;}
}
const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
const roll=(characterId,expectedVersion,visibility='PUBLIC')=>({requestId:randomUUID(),expectedVersion,characterId,key:'initiative',expression:'',visibility,advantage:[],disadvantage:[]});
async function state(root){return{dmRolls:await dm.get(root+'/rolls'),playerRolls:await player.get(root+'/rolls'),dmInitiative:await dm.get(root+'/initiative'),playerInitiative:await player.get(root+'/initiative'),history:await dm.get(root+'/initiative/history')};}
if(mode==='seed'){
 const campaign=await dm.post('/campaigns',{requestId:randomUUID(),name:`ZZZ Dice restart ${randomUUID().slice(0,8)}`},201),root='/campaigns/'+campaign.id;
 const invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});
 const template=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet;const ids=[];let version=(await dm.get(root+'/characters')).campaignVersion;
 for(const name of ['Persistent PC one','Persistent PC two','SECRET_P05_RESTART']){const sheet=structuredClone(template);sheet.name=name;const card=await player.post(root+'/characters',{requestId:randomUUID(),expectedVersion:version++,sheet});ids.push(card.id);}
 await dm.post(root+'/characters/'+ids[2]+'/access',{requestId:randomUUID(),expectedVersion:0,controllers:[],hidden:true,shareFull:false,approveImport:true,reason:'Persistent hidden NPC'});
 const command=roll(ids[0],0),result=await player.post(root+'/rolls',command),secret=roll(ids[2],1,'DM'),secretResult=await dm.post(root+'/rolls',secret);
 const participants=ids.map((characterId,i)=>({characterId,expectedVersion:i===2?1:0,kind:i===2?'MONSTER':'PC',group:'',surprised:i===2,advantage:[],disadvantage:[]}));
 const generate={requestId:randomUUID(),expectedVersion:0,participants,reason:'Initial independent characters; surprise from DM'},generateAck=await dm.post(root+'/initiative',generate);
 const order=ids.toReversed(),confirm={requestId:randomUUID(),expectedVersion:1,order,reason:'Player tie choices and DM sourced exception'},confirmAck=await dm.post(root+'/initiative/confirm',confirm);
 const baseline=await state(root);assert.equal(baseline.playerRolls.rolls.length,1);assert.equal(baseline.playerInitiative.entries.length,2);assert.ok(!JSON.stringify(baseline.playerInitiative).includes('SECRET'));
 await writeFile(evidencePath,JSON.stringify({root,ids,command,result,secret,secretResult,generate,generateAck,confirm,confirmAck,baseline},null,2),{flag:'wx'});console.log('PASS: dice seed stores complete random/calculation snapshots, independent characters, surprise and confirmed order/receipts');
}else{
 const e=JSON.parse(await readFile(evidencePath,'utf8'));assert.deepEqual(await state(e.root),e.baseline);assert.deepEqual(await player.post(e.root+'/rolls',e.command),e.result);assert.deepEqual(await dm.post(e.root+'/rolls',e.secret),e.secretResult);assert.deepEqual(await dm.post(e.root+'/initiative',e.generate),e.generateAck);assert.deepEqual(await dm.post(e.root+'/initiative/confirm',e.confirm),e.confirmAck);assert.deepEqual(await state(e.root),e.baseline);
 await player.call(e.root+'/rolls/'+e.secretResult.id,{status:404});await player.call(e.root+'/initiative/history',{status:403});
 const cardPath=e.root+'/characters/'+e.ids[0],detail=await player.get(cardPath);const changed=structuredClone(detail.sheet);changed.name='Renamed after restart';await player.post(cardPath,{requestId:randomUUID(),expectedVersion:detail.version,sheet:changed,reason:'Snapshot immutability check'});assert.deepEqual(await player.get(e.root+'/rolls/'+e.result.id),e.result);assert.deepEqual(await player.post(e.root+'/rolls',e.command),e.result);
 const access=await dm.get(cardPath+'/access');await dm.post(cardPath+'/access',{requestId:randomUUID(),expectedVersion:access.version,controllers:[],hidden:true,shareFull:false,approveImport:true,reason:'Hide after restart'});await player.post(e.root+'/rolls',e.command,404);await player.call(e.root+'/rolls/'+e.result.id,{status:404});assert.equal((await player.get(e.root+'/initiative')).entries.length,1);
 const members=await dm.get(e.root+'/members');await dm.post(e.root+'/members/'+encodeURIComponent(required('E2E_PLAYER_USERNAME'))+'/remove',{requestId:randomUUID(),expectedVersion:members.campaignVersion},204);await player.call(e.root+'/rolls',{status:403});await player.call(e.root+'/initiative',{status:403});
 console.log('PASS: actual restart preserved exact dice, source/actor/name, history, confirmed order and receipts; old retries single-effect, current hiding/revocation enforced');
}
