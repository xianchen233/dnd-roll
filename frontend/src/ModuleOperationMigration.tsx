import {useLayoutEffect,useRef,useState} from 'react';
import type {Api} from './api';
import type {ModuleInstance,ModuleOperationOptions,ModuleOperationChoice,ModuleOperationMigrationImpact,ModuleOperationMigrationReview} from './modules';
import {useModuleTask} from './useModuleTask';
type Props={api:Api;campaignId:string;instance:ModuleInstance;revision:number;disabled?:boolean;onDenied():void;onApplied():void|Promise<unknown>};
export function ModuleOperationMigration(props:Props){return <Panel key={props.campaignId} {...props}/>;}
function Panel({api,campaignId:c,instance,revision,disabled,onDenied,onApplied}:Props){
 const [options,setOptions]=useState<ModuleOperationOptions>(),[choices,setChoices]=useState<Record<string,ModuleOperationChoice['choice']>>({}),[review,setReview]=useState<{impact:ModuleOperationMigrationImpact;body:ModuleOperationMigrationReview}>(),[confirmed,setConfirmed]=useState<string[]>([]),[reading,setReading]=useState(false);
 const reads=useRef(0);
 function clear(){reads.current++;setOptions(undefined);setChoices({});setReview(undefined);setConfirmed([]);setReading(false);}
 const task=useModuleTask(api,c,onDenied,clear);
 useLayoutEffect(()=>{clear();},[instance.version,revision]);
 async function load(){const g=task.capture(),seq=++reads.current;setReview(undefined);setConfirmed([]);setReading(true);
  try{const source=await task.observe(api.moduleCustomization(instance.source.campaignId,instance.source.customizationId),g);if(!task.valid(g)||seq!==reads.current)return;
   const value=await task.observe(api.moduleOperationOptions(c,{expectedVersion:instance.version,customizationVersion:source.version}),g);if(task.valid(g)&&seq===reads.current){setOptions(value);setChoices({});task.setMessage('请逐个选择保留、采用来源或退役；没有默认决定。');}}
  catch{if(task.valid(g)&&seq===reads.current)task.setMessage('组合选择读取失败，请重新读取当前来源和本团版本。');}finally{if(task.valid(g)&&seq===reads.current)setReading(false);}}
 async function inspect(){if(!options||!task.canStart())return;const g=task.capture(),seq=++reads.current;setReading(true);setReview(undefined);setConfirmed([]);
  const body={expectedVersion:options.version,customizationVersion:options.customizationVersion,campaignVersion:options.campaignVersion,selections:options.options.map(o=>({nodeId:o.nodeId,choice:choices[o.nodeId],bindingVersion:o.binding?.version??0,operationVersion:o.operation?.version??0}))};
  try{const impact=await task.observe(api.moduleOperationMigrationImpact(c,body),g);if(task.valid(g)&&seq===reads.current)setReview({impact,body});}catch{if(task.valid(g)&&seq===reads.current)task.setMessage('组合迁移预览失败；核对本团编辑、引用、依赖与版本后重新读取。');}finally{if(task.valid(g)&&seq===reads.current)setReading(false);}}
 const locked=!!disabled||reading||task.busy||task.pending||task.denied;
 const confirmations=review?.impact.migration.confirmations??[];
 function apply(){if(!review||!task.canStart())return;const body=structuredClone({...review.body,requestId:crypto.randomUUID(),impactHash:review.impact.migration.impactHash,confirmations:[...confirmed]});void task.send({send:()=>api.migrateModuleOperations(c,body),done:async()=>{clear();await onApplied();}});}
 return <section aria-label="便携组合选择迁移"><h4>便携组合选择迁移</h4><p>保留本团编辑、采用来源或明确退役。采用来源后须另行绑定当前目标；退役保留身份与历史。迁移先创建独立恢复团，其中的便携组合均未绑定，不继承原团版本或先攻确认。</p><p aria-live="polite">{task.message}</p>{task.pending&&!task.busy&&<button onClick={()=>void task.retry()}>重试同一组合迁移</button>}{!task.denied&&<><button disabled={task.busy} onClick={()=>void load()}>读取便携组合更新选择</button><fieldset disabled={locked}><legend>逐个操作决定</legend>{options?.options.map(o=><article key={o.nodeId}><p>{o.binding?.name??o.incomingName} · {o.nodeId}{o.retired?' · 已退役':''}</p>{o.operation&&o.operation.version!==o.binding?.boundVersion&&<p>已有本团编辑；采用来源将保留当前前像并等待重新绑定。</p>}<details><summary>查看本团前像与新来源</summary><pre>{JSON.stringify(o,null,2)}</pre></details><label>本项组合决定<select aria-label={`组合决定 ${o.nodeId}`} value={choices[o.nodeId]??''} onChange={e=>{reads.current++;setReview(undefined);setConfirmed([]);setChoices(old=>({...old,[o.nodeId]:e.target.value as ModuleOperationChoice['choice']}));}}><option value="">尚未决定</option><option value="KEEP">保留本团定义</option><option value="INCOMING" disabled={!o.incoming}>采用来源，另行绑定</option><option value="RETIRE">退役，保留身份与历史</option></select></label></article>)}{options&&<button disabled={!options.options.every(o=>choices[o.nodeId])} onClick={()=>void inspect()}>预览组合选择迁移</button>}
 {review&&<><details open><summary>完整选择、实况前像与候选定义</summary><pre>{JSON.stringify(review.impact,null,2)}</pre></details>{review.impact.migration.blockers.map(b=><p key={b}>阻断：{b}</p>)}{confirmations.map(value=><label key={value}><input type="checkbox" aria-label={`确认组合迁移 ${value}`} checked={confirmed.includes(value)} onChange={e=>setConfirmed(old=>e.target.checked?[...old,value]:old.filter(x=>x!==value))}/>{value.startsWith('OPERATION ')?'确认此组合决定，保留历史且不执行。':'确认此结构变化与独立恢复要求。'}<code>{value}</code></label>)}<button disabled={!!review.impact.migration.blockers.length||confirmations.length>500||confirmations.some(x=>!confirmed.includes(x))} onClick={apply}>确认组合选择迁移</button></>}
 </fieldset></>}</section>;
}
