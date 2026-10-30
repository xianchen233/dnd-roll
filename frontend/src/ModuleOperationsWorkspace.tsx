import {useLayoutEffect,useRef,useState} from 'react';
import type {Api} from './api';
import type {ModuleOperation,ModuleOperationExecution,ModuleOperationImpact,ModuleOperationStep} from './modules';
import {ModuleOperationSteps,operationLabels,type OperationTargets} from './ModuleOperationSteps';
import {useModuleTask} from './useModuleTask';
type Props={api:Api;campaignId:string;revision:number;onDenied():void};
export function ModuleOperationsWorkspace(props:Props){return <Panel key={props.campaignId} {...props}/>;}
function Panel({api,campaignId:c,revision,onDenied}:Props){
 const [listing,setListing]=useState<ModuleOperation[]>([]),[targets,setTargets]=useState<OperationTargets>(),[selected,setSelected]=useState<ModuleOperation>(),[name,setName]=useState(''),[steps,setSteps]=useState<ModuleOperationStep[]>([]),[dirty,setDirty]=useState(false),[impact,setImpact]=useState<ModuleOperationImpact>(),[executions,setExecutions]=useState<ModuleOperationExecution[]>(),[receipt,setReceipt]=useState<ModuleOperationExecution>(),[loading,setLoading]=useState(false),[reviewing,setReviewing]=useState(false);
 const reads=useRef(0),reviews=useRef(0),historyReads=useRef(0);
 function clear(){reads.current++;reviews.current++;historyReads.current++;setListing([]);setTargets(undefined);setSelected(undefined);setName('');setSteps([]);setDirty(false);setImpact(undefined);setExecutions(undefined);setReceipt(undefined);setLoading(false);setReviewing(false);}
 const task=useModuleTask(api,c,onDenied,clear,()=>{reviews.current++;setImpact(undefined);});
 async function refresh(){const g=task.capture(),seq=++reads.current;reviews.current++;setImpact(undefined);setReviewing(false);setLoading(true);setTargets(undefined);
  try{const [definitions,cards,scene,notes,initiative,combat]=await Promise.all([task.observe(api.moduleOperations(c),g),task.observe(api.characters(c),g),task.observe(api.scene(c),g),task.observe(api.notes(c,{q:'',visibility:'DM'}),g),task.observe(api.initiative(c),g),task.observe(api.combat(c),g)]);
   if(!task.valid(g)||seq!==reads.current)return;setListing(definitions);const tokens=await task.observe(api.tokens(c,scene.id),g);if(task.valid(g)&&seq===reads.current)setTargets({cards,scene,notes,initiative,combat,tokens});
  }catch{if(task.valid(g)&&seq===reads.current)task.setMessage('操作或目标读取失败；请刷新后继续，草稿保留。');}finally{if(task.valid(g)&&seq===reads.current)setLoading(false);}
 }
 useLayoutEffect(()=>{void refresh();},[api,c,revision]);
 function edit(value:ModuleOperation){reviews.current++;historyReads.current++;setSelected(structuredClone(value));setName(value.name);setSteps(structuredClone(value.steps));setDirty(false);setImpact(undefined);setExecutions(undefined);setReceipt(undefined);}
 function change(value:ModuleOperationStep[]){reviews.current++;setSteps(value);setDirty(true);setImpact(undefined);}
 function add(kind:ModuleOperationStep['kind']){if(!targets)return;let step:ModuleOperationStep;
  switch(kind){case 'REVEAL_TOKEN':step={kind,sceneId:targets.scene.id,tokenId:'',expectedVersion:0};break;case 'REVEAL_CHARACTER':step={kind,characterId:'',expectedVersion:0};break;case 'PUBLISH_TEXT':case 'PUBLISH_MATERIAL':step={kind,noteId:'',expectedVersion:0};break;case 'PREPARE_ENCOUNTER':step={kind,expectedVersion:targets.initiative.version??0,participants:[],reason:''};break;case 'START_ENCOUNTER':step={kind,expectedVersion:targets.combat.version??0,initiativeVersion:targets.initiative.version??0,reason:''};}
  change([...steps,step]);
 }
 async function review(){if(!selected||!targets||dirty||!task.canStart())return;const g=task.capture(),seq=++reviews.current;setReviewing(true);setImpact(undefined);
  try{const value=await task.observe(api.moduleOperationImpact(c,selected.id,{expectedVersion:selected.version,campaignVersion:targets.cards.campaignVersion}),g);if(task.valid(g)&&seq===reviews.current){setImpact(value);task.setMessage('组合影响已由服务器核验；尚未触发。');}}
  catch{if(task.valid(g)&&seq===reviews.current)task.setMessage('组合预览失败；请检查目标版本、依赖、资料图片及已确认先攻。');}finally{if(task.valid(g)&&seq===reviews.current)setReviewing(false);}
 }
 function save(){if(!selected||!task.canStart())return;const id=selected.id,body=structuredClone({requestId:crypto.randomUUID(),expectedVersion:selected.version,name,steps});void task.send({send:()=>api.saveModuleOperation(c,id,body),done:async(value:ModuleOperation)=>{edit(value);await refresh();}});}
 function execute(){if(!selected||!impact||dirty||!task.canStart())return;const id=selected.id,body={requestId:crypto.randomUUID(),expectedVersion:impact.version,campaignVersion:impact.campaignVersion,impactHash:impact.impactHash};void task.send({send:()=>api.executeModuleOperation(c,id,body),done:async(value:ModuleOperationExecution)=>{setReceipt(value);setImpact(undefined);await refresh();}});}
 async function history(){if(!selected)return;const g=task.capture(),seq=++historyReads.current;try{const value=await task.observe(api.moduleOperationExecutions(c,selected.id),g);if(task.valid(g)&&seq===historyReads.current)setExecutions(value);}catch{if(task.valid(g))task.setMessage('执行证据读取失败，请重试。');}}
 async function moreNotes(){if(!targets?.notes.next)return;const g=task.capture(),seq=reads.current;try{const value=await task.observe(api.notes(c,{q:'',visibility:'DM'},targets.notes.next),g);if(task.valid(g)&&seq===reads.current)setTargets(old=>old?{...old,notes:{...value,notes:[...old.notes.notes,...value.notes.filter(n=>!old.notes.notes.some(x=>x.id===n.id))]}}:old);}catch{if(task.valid(g))task.setMessage('更早资料读取失败，请重试。');}}
 const locked=loading||reviewing||task.busy||task.pending||task.denied;
 const seen=new Set<string>();const validSteps=steps.length>0&&steps.length<=20&&steps.every(s=>{const note=s.kind==='PUBLISH_TEXT'||s.kind==='PUBLISH_MATERIAL';const key=s.kind==='REVEAL_TOKEN'?'token:'+s.tokenId:s.kind==='REVEAL_CHARACTER'?'card:'+s.characterId:note?'note:'+s.noteId:'encounter';if(seen.has(key))return false;seen.add(key);return s.kind==='REVEAL_TOKEN'?!!s.tokenId:s.kind==='REVEAL_CHARACTER'?!!s.characterId:note?!!s.noteId:!!s.reason.trim()&&(s.kind==='START_ENCOUNTER'||s.participants.length>0);});
 const saveSize=new TextEncoder().encode(JSON.stringify({requestId:'00000000-0000-4000-8000-000000000000',expectedVersion:selected?.version??0,name,steps})).length;
 const canSave=validSteps&&name.trim()&&Array.from(name).length<=100&&saveSize<=65536;
 const encounter=steps.some(s=>s.kind==='PREPARE_ENCOUNTER'||s.kind==='START_ENCOUNTER');
 return <section aria-label="模组手动组合"><h4>手动操作组合</h4><p>仅在本团保存与触发，最多 20 步、100 个组合。保存不执行；先核验全部目标，再由 DM 明确触发。不会执行脚本、自动条件、攻击或消耗资源。</p><p aria-label="组合操作状态" aria-live="polite">{task.message}</p>{task.pending&&!task.busy&&<button onClick={()=>void task.retry()}>重试同一组合请求</button>}{!task.denied&&<>
 <button disabled={task.busy} onClick={()=>void refresh()}>刷新操作组合与目标</button><fieldset disabled={locked}><legend>选择组合</legend>{listing.map(x=><button key={x.id} onClick={()=>edit(x)}>编辑组合 {x.name}</button>)}<button disabled={listing.length>=100} onClick={()=>{edit({id:crypto.randomUUID(),version:0,name:'新操作组合',steps:[]});setDirty(true);}}>新建操作组合</button></fieldset>
 {selected&&<fieldset disabled={locked}><legend>编辑组合 · 已载入版本 {selected.version}</legend><label>组合名称<input aria-label="组合名称" value={name} onChange={e=>{setName(e.target.value);setDirty(true);reviews.current++;setImpact(undefined);}}/></label><button disabled={!listing.some(x=>x.id===selected.id)} onClick={()=>{const value=listing.find(x=>x.id===selected.id);if(value)edit(value);}}>重新载入所选组合</button>{dirty&&<p>包含未保存修改；保存后才能预览和触发。</p>}{(Object.keys(operationLabels) as ModuleOperationStep['kind'][]).map(kind=><button key={kind} disabled={!targets||steps.length>=20||((kind==='PREPARE_ENCOUNTER'||kind==='START_ENCOUNTER')&&encounter)} onClick={()=>add(kind)}>添加{operationLabels[kind]}</button>)}
 <ModuleOperationSteps steps={steps} targets={targets} onChange={change}/>{targets?.notes.next&&<button onClick={()=>void moreNotes()}>读取更早 DM 资料</button>}{!validSteps&&<p>请选择目标，补齐遭遇理由和参战者；同一目标不可重复，每个组合最多一个遭遇步骤。</p>}{saveSize>65536&&<p>完整保存请求超过 64 KiB，请缩减组合。</p>}<button disabled={!canSave} onClick={save}>保存操作组合</button><button disabled={dirty||selected.version===0||!targets} onClick={()=>void review()}>预览组合影响</button>
 {impact&&<section aria-label="组合影响"><p>将按已保存顺序执行 {impact.definition.name}；所有步骤一起成功或回滚。</p><p>影响校验 {impact.impactHash}</p><details><summary>查看逐步目标与实际影响</summary><pre>{JSON.stringify(impact,null,2)}</pre></details><button onClick={execute}>确认触发组合</button></section>}
 </fieldset>}{selected&&selected.version>0&&<button onClick={()=>void history()}>读取组合执行证据</button>}{receipt&&<details open><summary>触发回执 · {receipt.actor} · {receipt.at}</summary><pre>{JSON.stringify(receipt,null,2)}</pre></details>}{executions?.map(x=><details key={x.id}><summary>执行 {x.actor} · {x.at}</summary><pre>{JSON.stringify(x,null,2)}</pre></details>)}
 </>}</section>;
}
