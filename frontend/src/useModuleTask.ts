import {useLayoutEffect,useRef,useState} from 'react';
import {ApiError,type Api} from './api';

// A domain lifetime is independent of read ordering. Even an obsolete read's
// authorization failure revokes this domain, but never a later campaign.
export function useModuleTask(api:Api,campaignId:string,onDenied:()=>void,onClear:()=>void,onReject=onClear){
 const generation=useRef(0),blocked=useRef(false),mounted=useRef(false),locked=useRef(false);
 const clear=useRef(onClear),rejected=useRef(onReject),denyCallback=useRef(onDenied);clear.current=onClear;rejected.current=onReject;denyCallback.current=onDenied;
 const [denied,setDenied]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState('');
 type Pending={send:()=>Promise<unknown>;done:(value:any)=>void|Promise<unknown>};
 const pendingRef=useRef<Pending|undefined>(undefined),[pending,setPending]=useState<Pending>();
 function waiting(value?:Pending){pendingRef.current=value;setPending(value);}
 const capture=()=>generation.current;
 const valid=(g:number)=>mounted.current&&!blocked.current&&g===generation.current;
 useLayoutEffect(()=>{mounted.current=true;generation.current++;blocked.current=false;locked.current=false;setDenied(false);setBusy(false);waiting();setMessage('');clear.current();return()=>{mounted.current=false;generation.current++;};},[api,campaignId]);
 function deny(g:number){if(!valid(g))return;blocked.current=true;generation.current++;waiting();locked.current=false;setBusy(false);setDenied(true);clear.current();setMessage('模组权限已失效，请刷新战役列表。');denyCallback.current();}
 async function observe<T>(promise:Promise<T>,g:number){try{return await promise;}catch(e){if(e instanceof ApiError&&(e.status===401||e.status===403))deny(g);throw e;}}
 async function send(operation:Pending){
  if(locked.current||blocked.current||!mounted.current)return;
  locked.current=true;setBusy(true);waiting(operation);const g=capture();setMessage('等待服务器确认…');
  let value:unknown;
  try{value=await observe(operation.send(),g);}
  catch(e){if(valid(g)){if(e instanceof ApiError&&e.status<500){waiting();rejected.current();setMessage(e.status===409?'版本、依赖或请求冲突；请刷新并重新预览。':'请求被拒绝；请刷新并检查目标与定义。');}else setMessage('结果未确认；请重试同一请求。');}if(valid(g)){locked.current=false;setBusy(false);}return;}
  if(!valid(g))return;waiting();setMessage('请求已确认；请以服务器刷新结果为准。');
  try{await operation.done(value);}catch{if(valid(g))setMessage('请求已确认；刷新失败，请重新读取，勿重复提交。');}
  finally{if(valid(g)){locked.current=false;setBusy(false);}}
 }
 return {capture,valid,observe,send,retry:()=>pendingRef.current&&send(pendingRef.current),pending:!!pending,busy,denied,message,setMessage,canStart:()=>!locked.current&&!pendingRef.current&&!blocked.current&&mounted.current};
}
