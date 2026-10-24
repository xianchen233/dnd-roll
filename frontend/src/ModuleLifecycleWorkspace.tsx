import {useLayoutEffect,useRef,useState} from 'react';
import {ApiError,type Api} from './api';
import {emptySheet} from './characters';
import {ModuleUpdatePreview} from './ModuleUpdatePreview';
import {ModuleSelectiveUpdate} from './ModuleSelectiveUpdate';
import {ModuleLiveUpdate} from './ModuleLiveUpdate';
import {ModuleOperationsWorkspace} from './ModuleOperationsWorkspace';
import type {Customization,CustomizationListing,ModuleDefinition,ModuleInstance,ModuleSummary} from './modules';
type Props={api:Api;campaignId:string;revision:number;packages:ModuleSummary[];onDenied():void;onCreated?(id:string):void|Promise<void>;onTemplates?():void|Promise<unknown>};
type Op={kind:'create';body:Parameters<Api['createModuleCustomization']>[1]}|{kind:'edit';id:string;body:Parameters<Api['editModuleCustomization']>[2]}|{kind:'template';id:string;body:Parameters<Api['saveModuleTemplate']>[2]}|{kind:'launch';id:string;body:Parameters<Api['launchModuleCampaign']>[2]}|{kind:'progress';body:Parameters<Api['saveModuleProgress']>[1]};
export function ModuleLifecycleWorkspace({api,campaignId,revision,packages,onDenied,onCreated,onTemplates}:Props){
 const [listing,setListing]=useState<CustomizationListing>(),[detail,setDetail]=useState<Customization>(),[name,setName]=useState(''),[document,setDocument]=useState(''),[dirty,setDirty]=useState(false),[source,setSource]=useState(''),[copyName,setCopyName]=useState('新定制副本'),[templateId,setTemplateId]=useState(''),[templateVersion,setTemplateVersion]=useState('1.0.0'),[campaignName,setCampaignName]=useState('新的冒险'),[created,setCreated]=useState(''),[instance,setInstance]=useState<ModuleInstance>(),[progress,setProgress]=useState('{}'),[busy,setBusy]=useState(false),[loadingDetail,setLoadingDetail]=useState(false),[denied,setDenied]=useState(false),[pending,setPending]=useState<Op>(),[message,setMessage]=useState('正在读取定制与实例…');
 const scope=useRef(0),reads=useRef(0),details=useRef(0),blocked=useRef(false),lock=useRef(false),uncertain=useRef<Op|undefined>(undefined),progressDirty=useRef(false);
 function valid(s:number){return s===scope.current&&!blocked.current;}
 function waiting(op?:Op){uncertain.current=op;setPending(op);}
 function deny(){scope.current++;blocked.current=true;setDenied(true);setListing(undefined);setDetail(undefined);setDocument('');setName('');setInstance(undefined);setProgress('{}');setCreated('');waiting();setBusy(false);setMessage('定制权限已失效。');onDenied();}
 async function observe<T>(promise:Promise<T>,s:number){try{return await promise;}catch(e){if(valid(s)&&e instanceof ApiError&&(e.status===401||e.status===403))deny();throw e;}}
 async function refresh(){const s=scope.current,seq=++reads.current;
  const results=await Promise.allSettled([observe(api.moduleCustomizations(campaignId),s),observe(api.moduleInstance(campaignId),s)]);if(!valid(s)||seq!==reads.current)return;
  if(results[0].status==='fulfilled')setListing(results[0].value);else setMessage(uncertain.current?'结果未确认；读取失败，仍可重试原操作。':'定制列表读取失败，请刷新。');
  if(results[1].status==='fulfilled'){if(!progressDirty.current&&uncertain.current?.kind!=='progress'){setInstance(results[1].value);setProgress(JSON.stringify(results[1].value.progress,null,2));}}
  else if(results[1].reason instanceof ApiError&&results[1].reason.status===404)setInstance(undefined);else if(!uncertain.current)setMessage('实例读取失败，请刷新后继续。');
 }
 async function load(id:string){const s=scope.current,seq=++details.current;setLoadingDetail(true);
  try{const d=await observe(api.moduleCustomization(campaignId,id),s);if(!valid(s)||seq!==details.current)return false;setDetail(d);setName(d.name);setDocument(JSON.stringify(d.document,null,2));setDirty(false);setTemplateId(crypto.randomUUID());setTemplateVersion('1.0.0');return true;}
  catch{if(valid(s))setMessage('副本读取失败；原草稿保留，请重试读取。');return false;}finally{if(valid(s)&&seq===details.current)setLoadingDetail(false);}
 }
 useLayoutEffect(()=>{scope.current++;reads.current++;details.current++;blocked.current=false;lock.current=false;progressDirty.current=false;setDenied(false);setListing(undefined);setDetail(undefined);setName('');setDocument('');setDirty(false);setSource('');setCopyName('新定制副本');setTemplateId('');setTemplateVersion('1.0.0');setCampaignName('新的冒险');setInstance(undefined);setProgress('{}');setCreated('');waiting();setBusy(false);setLoadingDetail(false);setMessage('模板原件、定制副本和进行中状态分别保存。');return()=>{scope.current++;};},[api,campaignId]);
 useLayoutEffect(()=>{void refresh();},[api,campaignId,revision]);
 async function send(op:Op){if(lock.current||blocked.current)return;lock.current=true;setBusy(true);waiting(op);const s=scope.current;
  try{
   const result=await observe(op.kind==='create'?api.createModuleCustomization(campaignId,op.body):op.kind==='edit'?api.editModuleCustomization(campaignId,op.id,op.body):op.kind==='template'?api.saveModuleTemplate(campaignId,op.id,op.body):op.kind==='launch'?api.launchModuleCampaign(campaignId,op.id,op.body):api.saveModuleProgress(campaignId,op.body),s);
   if(!valid(s))return;waiting();
   if(op.kind==='launch'){setCreated(result.id);setMessage('独立新战役已创建；可进入后邀请成员。');}
   else if(op.kind==='template'){setMessage('已另存新模板；当前团与旧记录保持。');void onTemplates?.();}
   else if(op.kind==='progress'){progressDirty.current=false;setMessage('剧情进度已确认。');}
   else{const loaded=await load(result.id);if(valid(s))setMessage(loaded?'定制副本已确认；已从服务器读取。':'定制副本已确认；详情尚未确认同步，原草稿保留。');}
   if(valid(s))await refresh();
  }catch(e){if(!valid(s))return;if(e instanceof ApiError&&e.status<500){waiting();setMessage(e.status===409?'版本、依赖或请求冲突；草稿保留，请检查服务器版本后重新载入。':'操作被拒绝，请检查定义、权限与规则版本。');await refresh();}else setMessage('结果未确认；可重试同一定制操作。');}
  finally{if(valid(s)){lock.current=false;setBusy(false);}}
 }
 function changeDoc(d:ModuleDefinition){setDocument(JSON.stringify(d,null,2));setDirty(true);}
 function add(kind:'SCENE'|'UNIT'){try{const d=JSON.parse(document) as ModuleDefinition,id=crypto.randomUUID();d.nodes.push({id,kind,name:kind==='SCENE'?'新场景':'新单位',text:'',visibility:'DM',nodeIds:[],assetIds:[],dependencyIds:[]});if(kind==='SCENE')d.play.scenes.push({nodeId:id,width:24,height:16,gridSize:48,map:null});else d.play.units.push({nodeId:id,sheet:emptySheet('新单位'),hidden:true,shareFull:false});changeDoc(d);}catch{setMessage('JSON 无法读取，请先修正格式。');}}
 function parse<T>(value:string,action:(data:T)=>void){try{action(JSON.parse(value) as T);}catch{setMessage('JSON 无法读取，请先修正格式。');}}
 const disabled=busy||loadingDetail||!!pending||denied;
 return <section className="module-lifecycle" aria-label="模组定制与战役实例"><h3>定制副本与战役实例</h3><p aria-label="定制状态" aria-live="polite">{message}</p>{!denied&&<>
 <button disabled={busy} onClick={()=>void refresh()}>刷新定制与实例</button>{pending&&!busy&&<button onClick={()=>void send(pending)}>重试同一定制操作</button>}
 <fieldset disabled={disabled}><legend>从模板创建独立定制</legend><label>来源模板<select aria-label="来源模板" value={source} onChange={e=>setSource(e.target.value)}><option value="">选择库中模板</option>{packages.map(p=><option key={p.id} value={p.id}>{p.manifest.name} · {p.id}</option>)}</select></label><label>新副本名称<input value={copyName} onChange={e=>setCopyName(e.target.value)}/></label><button disabled={!source||!listing||!copyName.trim()} onClick={()=>listing&&void send({kind:'create',body:{requestId:crypto.randomUUID(),expectedVersion:listing.campaignVersion,packageId:source,name:copyName}})}>创建可编辑定制副本</button></fieldset>
 <div className="module-copy-list">{listing?.customizations.map(c=><button key={c.id} disabled={disabled} onClick={()=>{setCreated('');void load(c.id);}}>编辑 {c.name}</button>)}</div>
 {detail&&<fieldset disabled={disabled}><legend>编辑定制副本 · 版本 {detail.version}</legend><p>原件 {detail.sourcePackageId} · SHA-256 {detail.sourceSha256}</p>{detail.missingDependencies.map(d=><p key={d.id}>未就绪依赖：{d.id} · {d.version} · {d.rules}；补齐前不能开团。</p>)}<label>定制名称<input value={name} onChange={e=>{setName(e.target.value);setDirty(true);}}/></label><p>可完整编辑场景与地图、角色卡与资源初值、预摆棋子、剧情资料、规则准入及依赖选择。相同单位的多个棋子引用同一 unitId。便携模板中的遭遇、奖励、触发仍保存为描述；本团实况可另行编辑并手动触发受支持的操作组合。</p><div><button onClick={()=>add('SCENE')}>添加场景定义</button><button onClick={()=>add('UNIT')}>添加单位定义</button></div><label>定制内容 JSON<textarea aria-label="定制内容 JSON" rows={18} spellCheck={false} value={document} onChange={e=>{setDocument(e.target.value);setDirty(true);}}/></label><button onClick={()=>parse<ModuleDefinition>(document,d=>void send({kind:'edit',id:detail.id,body:{requestId:crypto.randomUUID(),expectedVersion:detail.version,name,document:d}}))}>保存定制副本</button><button onClick={()=>void load(detail.id)}>重新载入所选副本</button>{dirty&&<p>包含未保存修改；另存模板或开团前请先保存。</p>}<details><summary>查看不可变编辑基线</summary><pre>{JSON.stringify(detail.baseline,null,2)}</pre></details>
 <label>新模板身份<input value={templateId} onChange={e=>setTemplateId(e.target.value)}/></label><label>新模板版本<input value={templateVersion} onChange={e=>setTemplateVersion(e.target.value)}/></label><button disabled={dirty||!templateId||!templateVersion} onClick={()=>void send({kind:'template',id:detail.id,body:{requestId:crypto.randomUUID(),expectedVersion:detail.version,moduleId:templateId,moduleVersion:templateVersion,name}})}>另存为新模板</button><p>另存仅包含已保存定义与原件素材，不包含当前团的生命、进度、权限或历史。可从上方模板库下载 ZIP。</p>
 <label>新战役名称<input value={campaignName} onChange={e=>setCampaignName(e.target.value)}/></label><button disabled={dirty||!campaignName.trim()||!!detail.missingDependencies.length} onClick={()=>void send({kind:'launch',id:detail.id,body:{requestId:crypto.randomUUID(),expectedVersion:detail.version,name:campaignName}})}>从定制副本创建新战役</button></fieldset>}
 {created&&<p>新战役 {created} <button onClick={()=>{const s=scope.current;void Promise.resolve(onCreated?.(created)).catch(()=>{if(valid(s))setMessage('战役已创建；列表读取失败，请刷新战役列表。');});}}>进入新战役</button></p>}
 {instance&&<fieldset disabled={disabled}><legend>本团独立剧情进度</legend><p>来源 {instance.source.manifest.name} · 定制版本 {instance.source.customizationVersion} · 进度版本 {instance.version}。角色生命与资源在原角色卡工作区维护。</p><label>剧情进度 JSON<textarea aria-label="剧情进度 JSON" rows={5} value={progress} onChange={e=>{progressDirty.current=true;setProgress(e.target.value);}}/></label><button onClick={()=>parse<Record<string,string>>(progress,p=>void send({kind:'progress',body:{requestId:crypto.randomUUID(),expectedVersion:instance.version,progress:p}}))}>保存剧情进度</button><details><summary>查看实例定义与独立身份映射</summary><pre>{JSON.stringify({document:instance.document,mapping:instance.mapping},null,2)}</pre></details></fieldset>}
 {detail&&<><ModuleUpdatePreview api={api} campaignId={campaignId} copyId={detail.id} version={detail.version} packageIds={packages.map(p=>({id:p.id,name:p.manifest.name+' · '+p.manifest.moduleVersion}))} onDenied={deny}/><ModuleSelectiveUpdate api={api} campaignId={campaignId} copyId={detail.id} version={detail.version} revision={revision} disabled={disabled||dirty} packageIds={packages.map(p=>({id:p.id,name:p.manifest.name+' · '+p.manifest.moduleVersion}))} onDenied={deny} onApplied={async()=>{await load(detail.id);await refresh();}}/></>}
 {instance&&<><ModuleLiveUpdate api={api} campaignId={campaignId} instance={instance} revision={revision} disabled={disabled} onDenied={deny} onApplied={refresh} onCreated={onCreated}/><ModuleOperationsWorkspace api={api} campaignId={campaignId} revision={revision} onDenied={deny}/></>}
 </>}</section>;
}
