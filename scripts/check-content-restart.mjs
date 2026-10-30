// Run seed -> actually restart PostgreSQL and packaged application -> verify.
// Credentials come only from the operator's environment. Evidence stays outside Git.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath, 'Usage: check-content-restart.mjs seed|verify <outside-repository.json>');
const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL');
class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', body, status = 200 } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ') };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL('/api' + path, base), { method, headers, cache: 'no-store', ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
    for (const cookie of response.headers.getSetCookie()) { const [key, ...value] = cookie.split(';')[0].split('='); this.cookies.set(key, value.join('=')); }
    assert.equal(response.status, status, `${method} ${path}`); return response;
  }
  async get(path) { return (await this.call(path)).json(); }
  async post(path, body, status = 200) { const r = await this.call(path, { method: 'POST', body, status }); return status === 204 ? undefined : r.json(); }
  async login(username, password) { this.csrf = await this.get('/auth/csrf'); await this.post('/auth/login', { username, password }); this.csrf = await this.get('/auth/csrf'); return this; }
}
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
const player = await new Session().login(required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
const actor = required('E2E_PLAYER_USERNAME');
async function state(c,id){return {catalog:await player.get(`/campaigns/${c}/content`),detail:await player.get(`/campaigns/${c}/characters/${id}`),history:await player.get(`/campaigns/${c}/characters/${id}/history`)};}
if(mode==='seed'){
 const campaign=await dm.post('/campaigns',{requestId:randomUUID(),name:`ZZZ Content restart ${randomUUID().slice(0,8)}`},201),c=campaign.id,root=`/campaigns/${c}`;
 const invite=await dm.post(root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await player.post('/invitations/accept',{token:invite.token});
 const sheet=JSON.parse(await readFile(new URL('../contracts/character.example.json',import.meta.url),'utf8')).sheet;sheet.name='Persistent content selection';
 const card=await player.post(root+'/characters',{requestId:randomUUID(),expectedVersion:(await player.get(root+'/content')).campaignVersion,sheet}),path=root+'/characters/'+card.id;
 const publish={requestId:randomUUID(),expectedVersion:(await player.get(root+'/content')).campaignVersion,entry:{name:'Versioned original',aliases:['reusable'],type:'FEATURE',source:{book:'Campaign original',revision:'v1',rules:'2014',detail:'Explicit compatibility selection'},description:'Persistent original description',spellLevel:0,dice:'',damageType:'',rights:'Original fixture permitted for campaign sharing'}};
 const custom=await player.post(root+'/content/custom',publish);await player.post(path+'/content-preview',{expectedVersion:0,entryId:custom.id,ability:'STR',castingId:''},403);
 const settings={requestId:randomUUID(),expectedVersion:custom.version,mode:'REVIEW',approvedIds:[custom.id]},policyAck=await dm.post(root+'/content/settings',settings);
 const preview=await player.post(path+'/content-preview',{expectedVersion:0,entryId:custom.id,ability:'STR',castingId:''});
 const fill={requestId:randomUUID(),expectedVersion:preview.expectedVersion,policyVersion:preview.policyVersion,entryId:custom.id,ability:'STR',castingId:'',reason:'Explicit reviewed selection'},fillAck=await player.post(path+'/content',fill);
 const newer=structuredClone(publish);newer.requestId=randomUUID();newer.expectedVersion=(await player.get(root+'/content')).campaignVersion;newer.entry.source.revision='v2';newer.entry.description='A new independent revision';await player.post(root+'/content/custom',newer);
 const baseline=await state(c,card.id);assert.equal(baseline.detail.sheet.features.at(-1).description,'Persistent original description');
 await writeFile(evidencePath,JSON.stringify({c,id:card.id,publish,custom,settings,policyAck,fill,fillAck,baseline},null,2));console.log('PASS: content seed includes original/new revision, reviewed legacy fill, receipts and unchanged snapshots');
}else{
 const e=JSON.parse(await readFile(evidencePath,'utf8')),root=`/campaigns/${e.c}`,path=root+'/characters/'+e.id;assert.deepEqual(await state(e.c,e.id),e.baseline);
 assert.deepEqual(await player.post(root+'/content/custom',e.publish),e.custom);assert.deepEqual(await dm.post(root+'/content/settings',e.settings),e.policyAck);assert.deepEqual(await player.post(path+'/content',e.fill),e.fillAck);assert.deepEqual(await state(e.c,e.id),e.baseline);
 const review=await dm.post(root+'/content/settings',{requestId:randomUUID(),expectedVersion:e.baseline.catalog.campaignVersion,mode:'REVIEW',approvedIds:[]});await player.post(path+'/content',e.fill,403);assert.deepEqual((await player.get(path)).sheet,e.baseline.detail.sheet);
 await dm.post(root+'/content/settings',{requestId:randomUUID(),expectedVersion:review.version,mode:'REVIEW',approvedIds:[e.custom.id]});
 const members=await dm.get(root+'/members');await dm.post(root+'/members/'+encodeURIComponent(actor)+'/remove',{requestId:randomUUID(),expectedVersion:members.campaignVersion},204);
 await player.post(path+'/content',e.fill,403);await player.call(root+'/content',{status:403});
 console.log('PASS: real restart retained immutable content, admission, source snapshots/history/receipts; replay is single-effect and revocation-safe');
}
