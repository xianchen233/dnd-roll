import {useLayoutEffect,useRef,useState} from 'react';
import type {Api} from './api';
import type {ModuleInstance,ModuleLiveEvidence,ModuleLiveImpact,ModuleMigrationEvidence,ModuleMigrationImpact} from './modules';
import {useModuleTask} from './useModuleTask';
type Props={api:Api;campaignId:string;instance:ModuleInstance;revision:number;disabled?:boolean;onDenied():void;onApplied():void|Promise<unknown>;onCreated?(id:string):void|Promise<unknown>};
type Review={kind:'text';value:ModuleLiveImpact}|{kind:'structure';value:ModuleMigrationImpact};
function confirmationText(value:string){
 const labels:Record<string,string>={APPLY:'采用此项定义变化。',RETIRE_SCENE:'退役此场景并保留行与历史；必要时切换当前场景。',RETAIN_COMBAT_IDENTITY:'保留原参战身份、生命、资源、状态和回合；替代单位不会自动参战。',CREATE_DM_ONLY_RECOVERY:'创建仅归 DM 的独立恢复战役，保留实况资源，地图全盖；历史与战斗仍在原团。',RECOVER_EXTRA_SCENE:'把额外实况场景加入独立恢复副本；原场景仍由本团维护。',RECOVER_EXTRA_MAP:'复制额外地图及校验过的独立图片；恢复地图全盖。',RECOVER_EXTRA_UNIT:'恢复额外角色及实况资源，分配独立身份和仅 DM 控制；不重放战斗或历史。',RECOVER_EXTRA_PLACEMENT:'恢复额外棋子的实况位置与隐藏状态，独立身份且仅 DM 控制。'};
 return labels[value.split(/[ ;]/)[0]]??'确认此项实况变化。';
}
export function ModuleLiveUpdate(props:Props){return <Panel key={props.campaignId} {...props}/>;}
function Panel({api,campaignId:c,instance,revision,disabled,onDenied,onApplied,onCreated}:Props){
 const [review,setReview]=useState<Review>(),[confirmed,setConfirmed]=useState<string[]>([]),[reading,setReading]=useState(false),[texts,setTexts]=useState<ModuleLiveEvidence[]>(),[migrations,setMigrations]=useState<ModuleMigrationEvidence[]>();
 const reads=useRef(0),historyReads=useRef(0);
 function clear(){reads.current++;historyReads.current++;setReview(undefined);setConfirmed([]);setReading(false);setTexts(undefined);setMigrations(undefined);}
 const task=useModuleTask(api,c,onDenied,clear);
 useLayoutEffect(()=>{reads.current++;setReview(undefined);setConfirmed([]);setReading(false);},[instance.version,revision]);
 async function inspect(kind:Review['kind']){if(!task.canStart())return;const g=task.capture(),seq=++reads.current;setReview(undefined);setConfirmed([]);setReading(true);
  try{const source=await task.observe(api.moduleCustomization(instance.source.campaignId,instance.source.customizationId),g);if(!task.valid(g)||seq!==reads.current)return;
   const body={expectedVersion:instance.version,customizationVersion:source.version};
   const value=await task.observe(kind==='text'?api.liveModuleImpact(c,body):api.moduleMigrationImpact(c,body),g);
   if(task.valid(g)&&seq===reads.current){setReview(kind==='text'?{kind,value:value as ModuleLiveImpact}:{kind,value:value as ModuleMigrationImpact});task.setMessage('实况影响已读取；请检查变化、阻断与恢复要求。');}
  }catch{if(task.valid(g)&&seq===reads.current)task.setMessage('实况影响读取失败；请核对来源副本、依赖和当前版本。');}finally{if(task.valid(g)&&seq===reads.current)setReading(false);}
 }
 async function evidence(){const g=task.capture(),seq=++historyReads.current;const results=await Promise.allSettled([task.observe(api.liveModuleUpdates(c),g),task.observe(api.moduleMigrations(c),g)]);if(!task.valid(g)||seq!==historyReads.current)return;
  if(results[0].status==='fulfilled')setTexts(results[0].value);if(results[1].status==='fulfilled')setMigrations(results[1].value);if(results.some(x=>x.status==='rejected'))task.setMessage('部分实况历史读取失败；可重新读取。');
 }
 function apply(){if(!review||!task.canStart())return;const g=task.capture(),{value}=review,body={requestId:crypto.randomUUID(),expectedVersion:value.version,customizationVersion:value.customizationVersion,impactHash:value.impactHash};
  const command=review.kind==='text'?body:{...body,confirmations:[...confirmed]};const kind=review.kind;
  void task.send({send:()=>kind==='text'?api.applyLiveModule(c,command):api.migrateModule(c,command as typeof body&{confirmations:string[]}),done:async()=>{setReview(undefined);setConfirmed([]);await onApplied();if(task.valid(g))await evidence();}});
 }
 const locked=!!disabled||reading||task.busy||task.pending||task.denied;
 const confirmations=review?.kind==='structure'?review.value.confirmations:[];
 const blocked=!!review?.value.blockers.length||confirmations.length>500;
 return <section aria-label="实况模组更新"><h4>本团备团与实况迁移</h4><p>备团文字更新不改实况对象。结构迁移需逐项确认，并先保存独立的可玩恢复战役；生命、资源、剧情进度、回合和历史保留。</p><p aria-label="实况更新状态" aria-live="polite">{task.message}</p>{task.pending&&!task.busy&&<button onClick={()=>void task.retry()}>重试同一实况更新</button>}{!task.denied&&<>
 <fieldset disabled={locked}><legend>从来源定制副本检查更新</legend><button onClick={()=>void inspect('text')}>预览备团文字更新</button><button onClick={()=>void inspect('structure')}>预览实况结构迁移</button>
 {review&&<><p>本团版本 {review.value.version} · 来源定制版本 {review.value.customizationVersion}</p>{review.value.blockers.map(x=><p key={x}>阻断：<span>{x}</span></p>)}{confirmations.length>500&&<p>确认超过 500 项，不能迁移；请拆分来源更新。</p>}{review.value.changes.map(x=><article key={x.path}><p>{x.path}</p><dl><dt>更新前</dt><dd><pre>{typeof x.current==='string'?x.current:JSON.stringify(x.current,null,2)}</pre></dd><dt>拟采用</dt><dd><pre>{typeof x.incoming==='string'?x.incoming:JSON.stringify(x.incoming,null,2)}</pre></dd></dl></article>)}
 {review.kind==='structure'&&<><details><summary>查看迁移前实况与恢复范围</summary><pre>{JSON.stringify(review.value.runtime,null,2)}</pre></details><p>恢复副本拥有独立身份，仅归迁移 DM 控制；原团历史与战斗留在原团。恢复副本不会撤销后续游玩。</p>{confirmations.map(x=><div key={x}><label><input type="checkbox" aria-label={`确认 ${x}`} checked={confirmed.includes(x)} onChange={e=>setConfirmed(old=>e.target.checked?[...old,x]:old.filter(s=>s!==x))}/><span>{confirmationText(x)}</span></label><details><summary>查看本项路径与精确确认标识</summary><code>{x}</code></details></div>)}</>}
 <p>影响校验 {review.value.impactHash}</p><button disabled={blocked||(review.kind==='structure'&&confirmations.some(x=>!confirmed.includes(x)))} onClick={apply}>{review.kind==='text'?'应用备团文字更新':'确认并迁移实况'}</button></>}
 </fieldset><button onClick={()=>void evidence()}>读取实况更新历史</button>{texts?.map(h=><details key={'t'+h.version}><summary>文字更新版本 {h.version} · {h.actor}</summary><pre>{JSON.stringify(h,null,2)}</pre></details>)}{migrations?.map(h=><article key={h.version}><p>结构迁移版本 {h.version} · {h.actor} · 恢复战役 {h.recoveryCampaignId}</p>{onCreated&&<button onClick={()=>void Promise.resolve(onCreated(h.recoveryCampaignId)).catch(()=>{if(task.valid(task.capture()))task.setMessage('恢复战役入口读取失败，请刷新战役列表。');})}>进入恢复战役 {h.version}</button>}<details><summary>查看迁移证据与前像 {h.version}</summary><pre>{JSON.stringify(h,null,2)}</pre></details></article>)}
 </>}</section>;
}
