import {useLayoutEffect,useRef,useState} from 'react';
import {ApiError,type Api} from './api';
import {ModuleLifecycleWorkspace} from './ModuleLifecycleWorkspace';
import type {ModuleCommand,ModuleDependency,ModuleListing,ModulePreview} from './modules';
type Props={api:Api;campaignId:string;isDm:boolean;revision:number;onDenied():void;onCreated?(id:string):void|Promise<void>};
type Operation={command:ModuleCommand;file:File};
export function ModuleWorkspace({api,campaignId,isDm,revision,onDenied,onCreated}:Props){
 const [listing,setListing]=useState<ModuleListing>(),[preview,setPreview]=useState<ModulePreview>(),[file,setFile]=useState<File>();
 const [legacy,setLegacy]=useState(false),[copy,setCopy]=useState(false),[busy,setBusy]=useState(false),[loading,setLoading]=useState(true),[denied,setDenied]=useState(false),[pending,setPending]=useState<Operation>(),[message,setMessage]=useState('正在读取模组库…'),[download,setDownload]=useState('');
 const scope=useRef(0),reads=useRef(0),previews=useRef(0),downloads=useRef(0),blocked=useRef(false),lock=useRef(false),url=useRef(''),pendingRef=useRef<Operation|undefined>(undefined),confirmedAfter=useRef<number|undefined>(undefined);
 function revoke(){if(url.current)URL.revokeObjectURL(url.current);url.current='';setDownload('');}
 function uncertain(next?:Operation){pendingRef.current=next;setPending(next);}
 function valid(s:number){return s===scope.current&&!blocked.current;}
 function deny(){blocked.current=true;scope.current++;reads.current++;previews.current++;downloads.current++;setDenied(true);setListing(undefined);setPreview(undefined);setFile(undefined);uncertain();revoke();setBusy(false);setMessage('模组权限已失效，请刷新战役列表。');onDenied();}
 async function observe<T>(promise:Promise<T>,s:number){try{return await promise;}catch(error){if(valid(s)&&error instanceof ApiError&&(error.status===401||error.status===403))deny();throw error;}}
 async function refresh(){const s=scope.current,seq=++reads.current;downloads.current++;setLoading(true);setListing(undefined);revoke();
  try{const next=await observe(api.modulePackages(campaignId),s);if(!valid(s)||seq!==reads.current)return false;setListing(next);
   if(pendingRef.current)setMessage('结果未确认；可重试同一模组导入。');else if(confirmedAfter.current!==undefined){if(seq>confirmedAfter.current){confirmedAfter.current=undefined;setMessage('导入已确认；模组库已从服务器刷新。');}}else if(!lock.current)setMessage('模组库已从服务器刷新。');return true;
  }catch{if(valid(s)&&seq===reads.current)setMessage(confirmedAfter.current!==undefined?'导入已确认；列表尚未确认同步，请刷新。':pendingRef.current?'结果未确认；列表读取失败，可重试同一模组导入。':'模组库读取失败，请刷新。');return false;}
  finally{if(valid(s)&&seq===reads.current)setLoading(false);}
 }
 useLayoutEffect(()=>{scope.current++;reads.current++;previews.current++;downloads.current++;blocked.current=!isDm;lock.current=false;setDenied(false);setListing(undefined);setPreview(undefined);setFile(undefined);setLegacy(false);setCopy(false);setBusy(false);uncertain();confirmedAfter.current=undefined;revoke();return()=>{scope.current++;if(url.current)URL.revokeObjectURL(url.current);url.current='';};},[api,campaignId,isDm]);
 useLayoutEffect(()=>{if(!isDm||blocked.current)return;previews.current++;downloads.current++;setPreview(undefined);revoke();void refresh();},[api,campaignId,isDm,revision]);
 async function inspect(){if(!file||lock.current||blocked.current)return;lock.current=true;setBusy(true);setPreview(undefined);const s=scope.current,seq=++previews.current;
  try{const next=await observe(api.previewModule(campaignId,file),s);if(valid(s)&&seq===previews.current){setPreview(next);setLegacy(false);setCopy(false);setMessage('校验通过；请检查规则版本、依赖与资料后导入。');}}
  catch(error){if(valid(s))setMessage(error instanceof ApiError&&error.status===409?'该模组身份与版本已有不同内容，请使用新版本。':'模组校验失败，请检查格式、引用和素材完整性。');}
  finally{if(valid(s)){lock.current=false;setBusy(false);}}
 }
 async function send(operation:Operation){if(lock.current||blocked.current)return;lock.current=true;setBusy(true);const s=scope.current;uncertain(operation);
  try{await observe(api.importModule(campaignId,operation.command,operation.file),s);if(!valid(s))return;uncertain();setFile(undefined);setPreview(undefined);confirmedAfter.current=reads.current;setMessage('导入已确认；列表尚未确认同步，请刷新。');await refresh();}
  catch(error){if(!valid(s))return;if(error instanceof ApiError&&error.status<500){uncertain();setPreview(undefined);if(error.status===409){await refresh();if(valid(s))setMessage('模组版本或请求冲突；请重新校验预览，未自动覆盖。');}else setMessage('模组导入被拒绝，请重新校验与确认规则版本。');}else setMessage('结果未确认；可重试同一模组导入。');}
  finally{if(valid(s)){lock.current=false;setBusy(false);}}
 }
 async function fetchDownload(id:string){const s=scope.current,seq=++downloads.current;try{const blob=await observe(api.moduleDownload(campaignId,id),s);if(!valid(s)||seq!==downloads.current)return;revoke();url.current=URL.createObjectURL(blob);setDownload(url.current);}catch{if(valid(s))setMessage('模组下载失败或权限已改变，请刷新。');}}
 function dependencies(values:ModuleDependency[]){return values.map(d=><p key={d.id}>缺失依赖：{d.id} · {d.version} · {d.rules}。保存在库中仍不可执行；请补充精确版本。</p>);}
 if(!isDm)return null;
 const disabled=busy||!!pending||denied,canImport=!!preview&&!!listing&&(!preview.requiresLegacyConfirmation||legacy)&&!disabled&&!loading;
 return <section className="notes-workspace module-workspace" aria-label="模组模板库"><h2>模组模板库</h2><p>先校验与预览，再明确导入。模板原件与素材保持；可创建定制副本、另存模板和独立战役。不执行包内代码；本团手动组合须先保存、预览再明确触发。</p><p role="status">{message}</p><button disabled={denied} onClick={()=>{previews.current++;setPreview(undefined);void refresh();}}>刷新模组库</button>{pending&&!busy&&<button onClick={()=>void send(pending)}>重试同一模组导入</button>}
 {!denied&&<><fieldset disabled={disabled}><legend>导入模组</legend><label>模组 ZIP 文件<input key={`${campaignId}-${file?'selected':'empty'}`} aria-label="模组 ZIP 文件" type="file" accept=".zip,application/zip" onChange={e=>{previews.current++;setFile(e.target.files?.[0]);setPreview(undefined);setLegacy(false);setCopy(false);}}/></label><p>ZIP 最大 10 MiB；需要 manifest.json、content.json 及清单内 PNG/JPEG。</p><button disabled={!file||disabled} onClick={()=>void inspect()}>校验与预览模组</button>
 {preview&&<section aria-label="模组预览"><h3>{preview.manifest.name}</h3><p>{preview.manifest.moduleId} · {preview.manifest.moduleVersion} · {preview.manifest.rules}</p><p>分享声明：{preview.manifest.rights}</p><p>素材 {preview.manifest.assets.length}；内容 {preview.nodes.length}；SHA-256 {preview.sha256}</p>{preview.existingId&&<p>相同模组与版本已存在；默认复用，可明确创建独立副本。</p>}{preview.manifest.dependencies.map(d=><p key={`declared-${d.id}`}>规则依赖：{d.id} · {d.version} · {d.rules}</p>)}{dependencies(preview.missingDependencies)}{preview.document&&<details><summary>查看完整结构定义</summary><pre>{JSON.stringify(preview.document,null,2)}</pre></details>}{preview.nodes.map(n=><article key={n.id}><strong>{n.name} · {n.kind} · {n.visibility}</strong><p className="timeline-text">{n.text}</p></article>)}</section>}
 <label><input type="checkbox" checked={legacy} onChange={e=>setLegacy(e.target.checked)}/>明确保留旧版或自定义规则</label><label><input type="checkbox" checked={copy} onChange={e=>setCopy(e.target.checked)}/>创建独立模组副本</label><button disabled={!canImport} onClick={()=>{if(file&&listing)void send({file,command:{requestId:crypto.randomUUID(),expectedVersion:listing.campaignVersion,createCopy:copy,confirmLegacy:legacy}});}}>导入模板库</button></fieldset>
 {listing?.packages.map(p=><article key={p.id}><h3>{p.manifest.name}</h3><p>{p.manifest.moduleVersion} · {p.manifest.rules} · 库记录 {p.id}</p>{dependencies(p.missingDependencies)}<button disabled={disabled} onClick={()=>void fetchDownload(p.id)}>下载模组 {p.manifest.name}</button></article>)}{download&&<a href={download} download="module.zip">保存模组 ZIP</a>}</>}
 {!denied&&<ModuleLifecycleWorkspace api={api} campaignId={campaignId} revision={revision} packages={listing?.packages??[]} onDenied={deny} onCreated={onCreated} onTemplates={refresh}/>} </section>;
}
