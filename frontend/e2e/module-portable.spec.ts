import {test,expect} from '@playwright/test';
import {randomUUID,createHash} from 'node:crypto';
import {readFile,writeFile} from 'node:fs/promises';
import {Session,required} from '../../scripts/acceptance-support.mjs';
import {preparePortable,finishPortable} from '../../scripts/portable-evidence.mjs';
import {enter,login,click,noOverflow} from './p10-support';

test('explicit portable v3 editing retries the original saved document then downloads and reimports without executing live steps',async({browser,baseURL},info)=>{
 const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD')),e=await preparePortable(dm,player),context=await browser.newContext({baseURL});try{
  await login(context.request,'DM');const page=await context.newPage(),errors:string[]=[];page.on('pageerror',x=>errors.push(x.message));await enter(page,e.campaign);await click(page,'打开模组模板库');await click(page,'编辑 便携定义副本 A03g');await click(page,'启用便携组合格式 v3');
  const panel=page.getByRole('region',{name:'模组定制与战役实例',exact:true});await panel.getByLabel('定制内容 JSON',{exact:true}).fill(JSON.stringify(e.document,null,2));const bodies:string[]=[];let lost=false;
  await page.route('**'+e.path,async route=>{if(route.request().method()!=='POST'){await route.continue();return;}bodies.push(route.request().postData()!);if(!lost){lost=true;const response=await route.fetch();expect(response.status()).toBe(200);await route.abort('failed');}else await route.continue();});
  await click(page,'保存定制副本');await expect(panel.getByLabel('定制状态')).toContainText('结果未确认');await expect(panel.getByRole('button',{name:'添加便携操作定义',exact:true})).toBeDisabled();await click(page,'刷新定制与实例');await click(page,'重试同一定制操作');await expect(panel.getByText('编辑定制副本 · 版本 1',{exact:true})).toBeVisible();expect(bodies).toHaveLength(2);expect(bodies[0]).toBe(bodies[1]);await expect(panel.getByRole('button',{name:'从定制副本创建新战役'})).toBeEnabled();
  const edit=JSON.parse(bodies[0]),ack=await dm.post(e.path,edit);await panel.getByLabel('新模板身份',{exact:true}).fill(randomUUID());await panel.getByLabel('新模板版本',{exact:true}).fill('1.0.0');let save:any;page.on('request',r=>{if(r.method()==='POST'&&new URL(r.url()).pathname==='/api'+e.path+'/templates')save=r.postDataJSON();});
  await click(page,'另存为新模板');await expect(panel.getByLabel('定制状态')).toContainText('已另存新模板');expect(save).toBeTruthy();const template=await dm.post(e.path+'/templates',save);await click(page,'下载模组 便携定义副本 A03g');const download=page.waitForEvent('download');await page.getByRole('link',{name:'保存模组 ZIP',exact:true}).click();const bytes=await readFile((await(await download).path())!);await finishPortable(dm,player,e,edit,ack,save,template);expect(createHash('sha256').update(bytes).digest('hex')).toBe(e.templateSha);
  await noOverflow(page);await panel.screenshot({path:info.outputPath('a03g-portable-desktop.png')});await page.setViewportSize({width:390,height:844});await noOverflow(page);await panel.screenshot({path:info.outputPath('a03g-portable-mobile-layout.png')});expect(errors).toEqual([]);
  if(process.env.E2E_A03G_CAPTURE_FILE)await writeFile(process.env.E2E_A03G_CAPTURE_FILE,JSON.stringify({entries:[e]},null,2),{flag:'wx'});
 }finally{await context.close();}
});
