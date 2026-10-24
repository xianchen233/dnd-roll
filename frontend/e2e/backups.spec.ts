import {test,expect,type APIRequestContext} from '@playwright/test';
import {readFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
function required(name:string){const v=process.env[name];if(!v)throw Error(`Missing ${name}`);return v;}
async function login(request:APIRequestContext,player=false){const csrf=await (await request.get('/api/auth/csrf')).json();expect((await request.post('/api/auth/login',{headers:{[csrf.headerName]:csrf.token},data:{username:required(player?'E2E_PLAYER_USERNAME':'E2E_DM_USERNAME'),password:required(player?'E2E_PLAYER_PASSWORD':'E2E_DM_PASSWORD')}})).status()).toBe(200);}
test('operator creates and downloads a complete archive while another campaign DM/player has no deployment access',async({browser,baseURL},info)=>{
 const owner=await browser.newContext({baseURL}),other=await browser.newContext({baseURL});
 try{
  await login(owner.request);await login(other.request,true);const page=await owner.newPage();await page.goto('/');await page.getByRole('button',{name:'打开部署备份管理'}).click();const panel=page.getByRole('region',{name:'部署备份管理'});await expect(panel.getByText(/自动备份保留最近 7 份/)).toBeVisible();
  const before=(await (await owner.request.get('/api/admin/backups')).json()).jobs.map((j:{id:string})=>j.id);await expect(panel.getByRole('button',{name:'生成手动备份'})).toBeEnabled();await panel.getByRole('button',{name:'生成手动备份'}).click();
  let id='';await expect.poll(async()=>{const status=await (await owner.request.get('/api/admin/backups')).json();const job=status.jobs.find((j:{id:string})=>!before.includes(j.id));id=job?.id??'';return job?.state;}).toBe('COMPLETE');
  const link=panel.getByRole('link',{name:'下载备份 '+id});await expect(link).toBeVisible();const downloading=page.waitForEvent('download');await link.click();const download=await downloading,bytes=await readFile((await download.path())!);expect(bytes.subarray(0,2).toString()).toBe('PK');const direct=await owner.request.get('/api/admin/backups/'+id+'/content');expect(direct.status()).toBe(200);expect(createHash('sha256').update(bytes).digest('hex')).toBe(createHash('sha256').update(await direct.body()).digest('hex'));
  expect((await other.request.get('/api/admin/backups/'+id+'/content')).status()).toBe(403);const player=await other.newPage();await player.goto('/');await player.getByRole('button',{name:'打开部署备份管理'}).click();await expect(player.getByText(/当前账号无部署备份权限/)).toBeVisible();await expect(player.getByRole('region',{name:'部署备份管理'}).getByRole('link')).toHaveCount(0);
  await page.reload();await page.getByRole('button',{name:'打开部署备份管理'}).click();await expect(page.getByRole('link',{name:'下载备份 '+id})).toBeVisible();await panel.screenshot({path:info.outputPath('backups-operator.png')});
 }finally{await owner.close();await other.close();}
});
