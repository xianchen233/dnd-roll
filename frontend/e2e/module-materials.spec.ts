import {test,expect} from '@playwright/test';
import {readFile,writeFile} from 'node:fs/promises';
import {createHash} from 'node:crypto';
import {Session,required} from '../../scripts/acceptance-support.mjs';
import {prepareMaterial,finishMaterial} from '../../scripts/material-evidence.mjs';
import {login,enter,click,noOverflow} from './p10-support';

test('explicit illustrated publication copies public image bytes once after a real lost acknowledgment and keeps the private original',async({browser,baseURL},info)=>{
 const dm=await new Session().login(required('E2E_DM_USERNAME'),required('E2E_DM_PASSWORD')),player=await new Session().login(required('E2E_PLAYER_USERNAME'),required('E2E_PLAYER_PASSWORD'));
 const e=await prepareMaterial(dm,player),dmContext=await browser.newContext({baseURL}),playerContext=await browser.newContext({baseURL});try{
  await login(dmContext.request,'DM');await login(playerContext.request,'PLAYER');const page=await dmContext.newPage(),publicPage=await playerContext.newPage(),errors:string[]=[],privateRequests:string[]=[];
  page.on('pageerror',x=>errors.push(x.message));publicPage.on('pageerror',x=>errors.push(x.message));publicPage.on('request',r=>{if(/\/module-(operations|instance|customizations|packages)/.test(r.url()))privateRequests.push(r.url());});
  await enter(publicPage,e.campaign);await enter(page,e.campaign);await click(page,'打开模组模板库');await click(page,'编辑组合 公开附图线索 A03f');
  const panel=page.getByRole('region',{name:'模组手动组合',exact:true});await expect(panel).toContainText('独立的团队笔记和公开图片副本');await expect(panel.getByRole('combobox',{name:'步骤 1 资料'})).toHaveValue(e.note);
  await click(page,'预览组合影响');const bodies:string[]=[];let lost=false;
  await page.route('**'+e.path+'/executions',async route=>{if(route.request().method()!=='POST'){await route.continue();return;}bodies.push(route.request().postData()!);if(!lost){lost=true;const committed=await route.fetch();expect(committed.status()).toBe(200);await route.abort('failed');}else await route.continue();});
  await click(page,'确认触发组合');await expect(panel.getByLabel('组合操作状态')).toContainText('结果未确认');await expect(panel.getByRole('button',{name:'添加公开资料及附图',exact:true})).toBeDisabled();
  await click(page,'刷新操作组合与目标');await click(page,'重试同一组合请求');await expect(panel.getByLabel('组合操作状态')).toContainText('已确认');expect(bodies).toHaveLength(2);expect(bodies[0]).toBe(bodies[1]);
  const command=JSON.parse(bodies[0]),result=await dm.post(e.path+'/executions',command);await finishMaterial(dm,player,e,command,result);
  await click(publicPage,'打开团队笔记与 DM 资料');await expect(publicPage.getByRole('heading',{name:'附图线索 A03f',exact:true})).toBeVisible();const note=(await player.get(e.root+'/notes/'+e.published)),image=note.attachments[0];
  await click(publicPage,'查看笔记图片 '+image.id);const picture=publicPage.getByRole('img',{name:'笔记图片 '+image.id});await expect(picture).toBeVisible();expect(await picture.evaluate((i:HTMLImageElement)=>i.naturalWidth)).toBe(16);
  const download=publicPage.waitForEvent('download');await publicPage.getByRole('link',{name:'下载笔记图片',exact:true}).click();const bytes=await readFile((await(await download).path())!);expect(createHash('sha256').update(bytes).digest('hex')).toBe(e.sourceSha);
  await noOverflow(page);await panel.screenshot({path:info.outputPath('a03f-material-desktop.png')});await page.setViewportSize({width:390,height:844});await noOverflow(page);await panel.screenshot({path:info.outputPath('a03f-material-mobile-layout.png')});
  expect(privateRequests).toEqual([]);expect(errors).toEqual([]);if(process.env.E2E_A03F_CAPTURE_FILE)await writeFile(process.env.E2E_A03F_CAPTURE_FILE,JSON.stringify({entries:[e]},null,2),{flag:'wx'});
 }finally{await dmContext.close();await playerContext.close();}
});
