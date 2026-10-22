import {test,expect} from '@playwright/test';
import {randomUUID,createHash} from 'node:crypto';
import {lifecycleFixture,zipEntries} from '../../scripts/module-fixture.mjs';
import {emptySheet} from '../src/characters';
import {read,post,login,enter,click,noOverflow} from './p10-support';

test('DM compares baseline, saved local changes and incoming template without changing live resources or records',async({browser,baseURL},info)=>{
 const dm=await browser.newContext({baseURL});try{
  await login(dm.request,'DM');const c=await post(dm.request,'/api/campaigns',{requestId:randomUUID(),name:'ZZZ A03 preview '+randomUUID()},201),root='/api/campaigns/'+c.id;
  const pack=lifecycleFixture(emptySheet('A03 hidden unit'));
  async function upload(bytes:Buffer){const csrf=await read(dm.request,'/api/auth/csrf'),listing=await read(dm.request,root+'/module-packages');const response=await dm.request.post(root+'/module-packages',{headers:{[csrf.headerName]:csrf.token},multipart:{command:{name:'command.json',mimeType:'application/json',buffer:Buffer.from(JSON.stringify({requestId:randomUUID(),expectedVersion:listing.campaignVersion,createCopy:false,confirmLegacy:false}))},file:{name:'fixture.zip',mimeType:'application/zip',buffer:bytes}}});expect(response.status(),await response.text()).toBe(200);return response.json();}
  const original=await upload(pack.bytes),copy=await post(dm.request,root+'/module-customizations',{requestId:randomUUID(),expectedVersion:1,packageId:original.id,name:'A03 comparison'}),path=root+'/module-customizations/'+copy.id;
  const local=structuredClone(pack.definition);local.nodes.find((n:any)=>n.id===pack.document)!.text='A03_LOCAL_TEXT';await post(dm.request,path,{requestId:randomUUID(),expectedVersion:0,name:'A03 comparison',document:local});
  const target=await post(dm.request,path+'/campaigns',{requestId:randomUUID(),expectedVersion:1,name:'A03 ongoing party'}),targetRoot='/api/campaigns/'+target.id,instance=await read(dm.request,targetRoot+'/module-instance'),card=targetRoot+'/characters/'+instance.mapping.units[pack.unit];
  await post(dm.request,card+'/resources',{requestId:randomUUID(),expectedVersion:0,resourceId:'hp',delta:-1,reason:'Ongoing damage'});
  const message=await post(dm.request,targetRoot+'/messages',{requestId:randomUUID(),expectedVersion:0,characterId:'',kind:'OOC',text:'A03 preserve old record',replyTo:'',attachments:[]}),old=await read(dm.request,targetRoot+'/timeline/'+message.id),before=await read(dm.request,card),beforeCopy=await read(dm.request,path);
  const incoming=structuredClone(pack.definition);incoming.nodes.find((n:any)=>n.id===pack.document)!.text='A03_INCOMING_TEXT';const content=Buffer.from(JSON.stringify(incoming)),manifest={...pack.manifest,moduleVersion:'2.0.0',name:'A03 next template',content:{size:content.length,sha256:createHash('sha256').update(content).digest('hex')}};
  const candidate=await upload(zipEntries({'manifest.json':JSON.stringify(manifest),'content.json':content,['assets/'+pack.imageId]:pack.image}));
  const page=await dm.newPage(),errors:string[]=[];page.on('pageerror',e=>errors.push(e.message));await enter(page,c.id);await click(page,'打开模组模板库');await click(page,'编辑 A03 comparison');await page.getByLabel('更新候选模板',{exact:true}).selectOption(candidate.id);await click(page,'比较模板更新');const region=page.getByRole('region',{name:'模板三方差异',exact:true});await expect(region).toContainText('比较已完成');await region.locator('summary').filter({hasText:'双方修改冲突'}).click();for(const value of ['Synthetic original preparation','A03_LOCAL_TEXT','A03_INCOMING_TEXT'])await expect(region.getByText(value,{exact:true})).toBeVisible();
  await noOverflow(page);await region.screenshot({path:info.outputPath('a03-three-way-desktop.png')});await page.setViewportSize({width:390,height:844});await noOverflow(page);await region.screenshot({path:info.outputPath('a03-three-way-mobile-layout.png')});
  expect(await read(dm.request,path)).toEqual(beforeCopy);expect(await read(dm.request,card)).toEqual(before);expect(await read(dm.request,targetRoot+'/timeline/'+message.id)).toEqual(old);expect(await read(dm.request,targetRoot+'/module-instance')).toEqual(instance);expect(errors).toEqual([]);
 }finally{await dm.close();}
});
