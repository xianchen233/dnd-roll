import {randomUUID} from 'node:crypto';
import {test,expect,type APIRequestContext} from '@playwright/test';
const required=(name:string)=>{const value=process.env[name];if(!value)throw Error('Missing '+name);return value;};
async function get(r:APIRequestContext,path:string){const response=await r.get(path);expect(response.status()).toBe(200);return response.json();}
async function post(r:APIRequestContext,path:string,data:unknown,status=200){const csrf=await get(r,'/api/auth/csrf'),response=await r.post(path,{headers:{[csrf.headerName]:csrf.token},data});expect(response.status()).toBe(status);return response.json();}
test('visible uncontrolled endpoints give bounded range hints and hidden targets stay private',async({browser,baseURL},info)=>{
 const dc=await browser.newContext({baseURL}),pc=await browser.newContext({baseURL}),dm=await dc.newPage(),player=await pc.newPage(),errors:string[]=[];
 for(const p of [dm,player])p.on('pageerror',e=>errors.push(e.message));
 try{
  await post(dc.request,'/api/auth/login',{username:required('E2E_DM_USERNAME'),password:required('E2E_DM_PASSWORD')});await post(pc.request,'/api/auth/login',{username:required('E2E_PLAYER_USERNAME'),password:required('E2E_PLAYER_PASSWORD')});
  const campaign=await post(dc.request,'/api/campaigns',{requestId:randomUUID(),name:'ZZZ A07 browser '+randomUUID().slice(0,8)},201),root='/api/campaigns/'+campaign.id;
  const invite=await post(dc.request,root+'/invitations',{requestId:randomUUID(),expectedVersion:0},201);await post(pc.request,'/api/invitations/accept',{token:invite.token});
  const first=await get(dc.request,root+'/scene'),tokenRoute=root+'/scenes/'+first.id+'/tokens';
  async function create(name:string,x:number,y:number,hidden=false){return (await post(dc.request,tokenRoute,{requestId:randomUUID(),expectedVersion:(await get(dc.request,root+'/scene')).version,name,x,y,hidden,controllers:[]})).token;}
  const a=await create('测距起点',1,1),b=await create('测距目标',4,3),secret=await create('PRIVATE A07 endpoint',9,9,true);
  const before=await get(dc.request,root+'/scene'),beforeRolls=await get(dc.request,root+'/rolls'),beforeCombat=await get(dc.request,root+'/combat');
  for(const p of [player,dm]){await p.goto('/');await expect(p.getByRole('combobox',{name:'选择战役'})).not.toHaveValue('');await p.getByRole('combobox',{name:'选择战役'}).selectOption(campaign.id);await expect(p.locator('.connection')).toHaveText('● 实时连接');}
  const area=player.getByRole('region',{name:'范围提示'});await expect(area.getByLabel('范围目标')).not.toContainText('PRIVATE A07');
  const writes:string[]=[];player.on('request',r=>{const url=new URL(r.url());if(r.method()==='POST'&&url.origin===new URL(baseURL!).origin&&url.pathname.startsWith('/api/'))writes.push(url.pathname);});
  await area.getByLabel('范围起点').selectOption(a.id);await area.getByLabel('范围目标').selectOption(b.id);await area.getByLabel('范围方式').selectOption('RANGED');await area.getByLabel('普通射程（尺）').fill('10');await area.getByLabel('最大射程（尺）').fill('15');await area.getByLabel('已核对5尺方格、单格同平面及适用例外').check();await area.getByRole('button',{name:'计算范围提示'}).click();await expect(area.getByRole('status')).toContainText('距离 15 尺');await expect(area.getByRole('status')).toContainText('远射程');
  await area.getByLabel('普通射程（尺）').fill('15');await expect(area.getByRole('status')).toHaveCount(0);await area.getByRole('button',{name:'计算范围提示'}).click();await expect(area.getByRole('status')).toContainText('普通射程');
  expect(writes).toEqual([root+'/range-hints/preview',root+'/range-hints/preview']);expect(await get(dc.request,root+'/scene')).toEqual(before);expect(await get(dc.request,root+'/rolls')).toEqual(beforeRolls);expect(await get(dc.request,root+'/combat')).toEqual(beforeCombat);
  const body={sceneId:before.id,sceneVersion:before.version,sourceId:a.id,sourceVersion:a.version,targetId:secret.id,targetVersion:secret.version,profile:'GRID_5R_5FT_RANGE_V1',reviewed:true,mode:'RANGED',normalFeet:10,longFeet:15};
  const csrf=await get(pc.request,'/api/auth/csrf');const response=await pc.request.post(root+'/range-hints/preview',{headers:{[csrf.headerName]:csrf.token},data:body});expect(response.status()).toBe(404);expect(response.headers()['cache-control']).toBe('no-store, private');expect(await response.text()).not.toMatch(/PRIVATE|distanceFeet|9/);
  const da=dm.getByRole('region',{name:'范围提示'});await da.getByLabel('范围起点').selectOption(a.id);await da.getByLabel('范围目标').selectOption(secret.id);await da.getByLabel('声明触及（尺）').fill('40');await da.getByLabel('已核对5尺方格、单格同平面及适用例外').check();await da.getByRole('button',{name:'计算范围提示'}).click();await expect(da.getByRole('status')).toContainText('距离 40 尺');await da.screenshot({path:info.outputPath('a07-dm-desktop.png')});
  await player.setViewportSize({width:390,height:844});await area.screenshot({path:info.outputPath('a07-player-390.png')});expect(errors).toEqual([]);
  // A real DM hide invalidates the player's measured result through existing realtime refresh.
  await post(dc.request,tokenRoute+'/'+b.id,{requestId:randomUUID(),expectedVersion:b.version,name:b.name,x:b.x,y:b.y,hidden:true,controllers:[]});await expect(area.getByLabel('范围目标')).not.toContainText('测距目标');await expect(area.getByRole('status')).toHaveCount(0);
  expect(await post(pc.request,root+'/range-hints/preview',{...body,targetId:b.id,targetVersion:b.version},404)).toEqual({code:'NOT_FOUND',message:'Token not found'});
 }finally{await dc.close();await pc.close();}
});
