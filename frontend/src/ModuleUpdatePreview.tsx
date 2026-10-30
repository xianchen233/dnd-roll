import {useLayoutEffect,useRef,useState} from 'react';
import {ApiError,type Api} from './api';
import type {ModuleUpdatePreviewResult} from './modules';
const display=(value:unknown)=>typeof value==='string'?value:JSON.stringify(value,null,2);
export function ModuleUpdatePreview({api,campaignId,copyId,version,packageIds,onDenied}:{api:Api;campaignId:string;copyId:string;version:number;packageIds:{id:string;name:string}[];onDenied():void}){
 const [candidate,setCandidate]=useState(''),[result,setResult]=useState<ModuleUpdatePreviewResult>(),[busy,setBusy]=useState(false),[denied,setDenied]=useState(false),[message,setMessage]=useState('');
 const epoch=useRef(0),locked=useRef(false);
 useLayoutEffect(()=>{epoch.current++;locked.current=false;setCandidate('');setResult(undefined);setBusy(false);setDenied(false);setMessage('');return()=>{epoch.current++;};},[api,campaignId,copyId,version]);
 async function compare(){if(locked.current||denied||!candidate)return;locked.current=true;setBusy(true);setResult(undefined);const e=++epoch.current;
  try{const value=await api.previewModuleUpdate(campaignId,copyId,{expectedVersion:version,packageId:candidate});if(e!==epoch.current)return;setResult(value);setMessage(value.changes.length?'比较已完成；尚未应用更新。':'定义字段无差异；尚未应用更新。');}
  catch(error){if(e!==epoch.current)return;if(error instanceof ApiError&&(error.status===401||error.status===403)){setDenied(true);setCandidate('');setMessage('更新比较权限已失效。');onDenied();}
   else setMessage(error instanceof ApiError&&error.status===409?'副本版本或模板身份冲突，请重新载入副本。':'比较读取失败，请重试。');
  }finally{if(e===epoch.current){locked.current=false;setBusy(false);}}
 }
 return <section aria-label="模板三方差异"><h4>模板更新比较</h4><p role="status">{message}</p>{!denied&&<>
 <label>更新候选模板<select aria-label="更新候选模板" disabled={busy} value={candidate} onChange={e=>{epoch.current++;setCandidate(e.target.value);setResult(undefined);setMessage('');}}><option value="">选择模板版本</option>{packageIds.map(p=><option key={p.id} value={p.id}>{p.name}</option>)}</select></label>
 <button disabled={busy||!candidate} onClick={()=>void compare()}>比较模板更新</button>
 {result&&<><p>副本版本 {result.version} · 候选 {result.packageId} · SHA-256 {result.sha256}</p>{result.missingDependencies.map(d=><p key={d.id}>未就绪依赖：{d.id} · {d.version} · {d.rules}</p>)}<div className="module-update-changes">{result.changes.map(c=><details key={c.path}><summary>{c.path} · <span>{c.conflict?'双方修改冲突':'单方或一致修改'}</span></summary><dl><dt>原始基线</dt><dd><pre>{display(c.baseline)}</pre></dd><dt>当前定制</dt><dd><pre>{display(c.current)}</pre></dd><dt>新模板</dt><dd><pre>{display(c.incoming)}</pre></dd></dl></details>)}</div></>}
 </>}</section>;
}
