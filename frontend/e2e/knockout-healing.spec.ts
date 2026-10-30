import {randomUUID} from 'node:crypto';
import {writeFile} from 'node:fs/promises';
import {test,expect,type APIRequestContext} from '@playwright/test';
import {emptySheet} from '../src/characters';
const required=(n:string)=>{const v=process.env[n];if(!v)throw Error('Missing '+n);return v;};
async function get(r:APIRequestContext,path:string){const res=await r.get(path);expect(res.status()).toBe(200);return res.json();}
async function post(r:APIRequestContext,path:string,data:unknown,status=200){const csrf=await get(r,'/api/auth/csrf'),res=await r.post(path,{headers:{[csrf.headerName]:csrf.token},data});expect(res.status()).toBe(status);return res.json();}

test('healing both knockout origins is explicit, atomic and durable through lost responses',async({browser,baseURL},info)=>{
 const dc=await browser.newContext({baseURL}),pc=await browser.newContext({baseURL}),dm=await dc.newPage(),errors:string[]=[];dm.on('pageerror',e=>errors.push(e.message));
 try{
  await post(dc.request,'/api/auth/login',{username:required('E2E_DM_USERNAME'),password:required('E2E_DM_PASSWORD')});await post(pc.request,'/api/auth/login',{username:required('E2E_PLAYER_USERNAME'),password:required('E2E_PLAYER_PASSWORD')});
  const c=await post(dc.request,'/api/campaigns',{requestId:randomUUID(),name:'ZZZ A13 '+randomUUID().slice(0,8)},201),root='/api/campaigns/'+c.id,invite=await post(dc.request,root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await post(pc.request,'/api/invitations/accept',{token:invite.token});
  const ids:string[]=[],posts:any[]=[];
  for(const name of ['治疗来源','短棒目标','匕首目标','重启后目标']){const sheet=emptySheet(name);sheet.abilities.STR=30;sheet.combat.maxHp=100;sheet.combat.hp=name==='治疗来源'?100:2;sheet.combat.tempHp=1;ids.push((await post(dc.request,root+'/characters',{requestId:randomUUID(),expectedVersion:(await get(dc.request,root+'/characters')).campaignVersion,sheet})).id);}
  for(const entryId of ['916bf8b1-a24e-5dea-be38-2caee47dad54','e1086d6d-e523-53d9-93b9-7aa24c718e39'])await post(dc.request,root+'/characters/'+ids[0]+'/content',{requestId:randomUUID(),expectedVersion:(await get(dc.request,root+'/characters/'+ids[0])).version,policyVersion:(await get(dc.request,root+'/characters')).campaignVersion,entryId,ability:'STR',castingId:'',reason:'Trusted origin'});
  async function knocked(targetId:string,dagger:boolean){
   const source=await get(dc.request,root+'/characters/'+ids[0]),target=await get(dc.request,root+'/characters/'+targetId);let p:any;
   for(let n=0;n<20;n++){
    const body={requestId:randomUUID(),characterId:source.id,sourceVersion:source.version,targetId,targetVersion:target.version,attackId:'content:'+(dagger?'e1086d6d-e523-53d9-93b9-7aa24c718e39':'916bf8b1-a24e-5dea-be38-2caee47dad54'),reviewed:true,exceptions:[],advantage:[],disadvantage:[],immunities:[],resistances:[],vulnerabilities:[]};
    const ack=await post(dc.request,root+'/attack-previews',body);p=await get(dc.request,root+'/attack-previews/'+ack.id);if(p.evidence.resolution.damage>=3)break;
   }
   expect(p.evidence.resolution.damage).toBeGreaterThanOrEqual(3);const path=root+'/attack-previews/'+p.id;
   await post(dc.request,path+(dagger?'/dagger-knockout-confirmations':'/knockout-confirmations'),{requestId:randomUUID(),expectedVersion:p.version,previewHash:p.hash,treatment:'KNOCK_OUT_MELEE',reviewed:true,reason:'Original reviewed melee knockout'});
   p=await get(dc.request,path);await post(dc.request,path+'/events',{requestId:randomUUID(),expectedVersion:p.version,previewHash:p.hash,kind:'APPLY',changes:[],correctionOf:'',reason:'Apply original knockout',summary:''});return get(dc.request,path);
  }
  const origins=[await knocked(ids[1],false),await knocked(ids[2],true)];
  await dm.goto('/');await expect(dm.getByRole('combobox',{name:'选择战役'})).not.toHaveValue('');await dm.getByRole('combobox',{name:'选择战役'}).selectOption(c.id);await expect(dm.locator('.connection')).toHaveText('● 实时连接');await dm.getByRole('button',{name:'打开手动战斗与更正',exact:true}).click();const area=dm.getByRole('region',{name:'武器结算预览'});
  let lost=false;const sent:any[]=[];dm.on('request',req=>{if(req.method()==='POST'&&req.url().endsWith('/knockout-healings'))sent.push({path:new URL(req.url()).pathname.slice(4),body:req.postDataJSON()});});
  await dm.route('**/attack-previews/*/knockout-healings',async route=>{if(!lost){lost=true;const committed=await route.fetch();expect(committed.status()).toBe(200);await route.abort('connectionfailed');}else await route.continue();});
  for(let i=0;i<origins.length;i++){
   const p=origins[i],path=root+'/attack-previews/'+p.id,targetId=p.evidence.target.id,before=await get(dc.request,root+'/characters/'+targetId),rolls=await get(dc.request,root+'/rolls'),actions=await get(dc.request,root+'/actions');
   await expect(area.getByRole('combobox',{name:'已保存预览'})).toBeEnabled();await area.getByRole('combobox',{name:'已保存预览'}).selectOption(p.id);await expect(area.getByRole('button',{name:'预览治疗与解除'})).toBeDisabled();await area.getByLabel('已裁定治疗量',{exact:true}).fill('4');await area.getByLabel('我已裁定治疗量及资源消耗，确认没有治疗阻碍，并会继续处理倒地等关联效果').check();await area.getByRole('button',{name:'预览治疗与解除'}).click();await expect(area.getByText('治疗方案：实际恢复 4，生命变为 5，临时生命 0；执行时移除这次击昏记录，醒来仍倒地。',{exact:true})).toBeVisible();expect(await get(dc.request,root+'/characters/'+targetId)).toEqual(before);expect(await get(dc.request,root+'/attack-previews/'+p.id)).toEqual(p);
   await post(pc.request,path+'/knockout-healing-effect',{expectedVersion:p.version,previewHash:p.hash,targetVersion:before.version,amount:4,reviewed:true},403);
   if(i===0){await area.screenshot({path:info.outputPath('a13-dm-plan.png')});await dm.setViewportSize({width:390,height:844});await dm.getByRole('tab',{name:'战斗记录',exact:true}).click();await area.screenshot({path:info.outputPath('a13-dm-390.png')});await dm.setViewportSize({width:1440,height:1000});}
   await area.getByLabel('结算理由',{exact:true}).fill('PRIVATE healing adjudicated, remains prone');await area.getByRole('button',{name:'执行治疗并解除击昏记录'}).click();
   if(i===0){await expect(area.getByRole('button',{name:'重试同一结算请求'})).toBeVisible();await area.getByRole('button',{name:'重试同一结算请求'}).click();}
   await expect(area.getByText('治疗已执行、击昏记录已解除；醒来仍倒地，其他关联效果由 DM 继续处理。',{exact:true})).toBeVisible();
   const after=await get(dc.request,root+'/characters/'+targetId);expect(after.version).toBe(before.version+1);expect(after.sheet.combat.hp).toBe(5);expect(after.sheet.conditions).toHaveLength(0);expect(after.sheet.resources).toEqual(before.sheet.resources);expect(await get(dc.request,root+'/rolls')).toEqual(rolls);expect(await get(dc.request,root+'/actions')).toEqual(actions);expect((await get(dc.request,path)).evidence).toEqual(p.evidence);expect(await get(pc.request,root+'/attack-results')).toHaveLength(0);
   // Old receipts must not repeat the healing or erase an independently restored condition.
   await post(dc.request,root+'/characters/'+targetId,{requestId:randomUUID(),expectedVersion:after.version,sheet:before.sheet,reason:'Later explicit correction'});const latest=await get(dc.request,root+'/characters/'+targetId);
   for(const e of sent.filter(v=>v.path.includes(p.id)))posts.push({...e,ack:await post(dc.request,'/api'+e.path,e.body)});expect(await get(dc.request,root+'/characters/'+targetId)).toEqual(latest);
  }
  expect(sent[1]).toEqual(sent[0]);expect(sent[0].body).not.toHaveProperty('hp');expect(sent[0].body).not.toHaveProperty('changes');
  const pending=await knocked(ids[3],true),target=await get(dc.request,root+'/characters/'+ids[3]),review={expectedVersion:pending.version,previewHash:pending.hash,targetVersion:target.version,amount:7,reviewed:true},plan=await post(dc.request,root+'/attack-previews/'+pending.id+'/knockout-healing-effect',review);
  expect(plan.status).toBe('CANDIDATE');const snapshot:any={};for(const suffix of ['/attack-previews','/rolls','/actions'])snapshot[suffix]=await get(dc.request,root+suffix);snapshot.cards=[];for(const id of ids)snapshot.cards.push({card:await get(dc.request,root+'/characters/'+id),history:await get(dc.request,root+'/characters/'+id+'/history')});snapshot.public=await get(pc.request,root+'/attack-results');
  const file=process.env.E2E_A13_CAPTURE_FILE;if(file)await writeFile(file,JSON.stringify({root:root.slice(4),ids,posts,snapshot,pending,review,plan},null,2),{flag:'wx'});expect(errors).toEqual([]);
 }finally{await dc.close();await pc.close();}
});
