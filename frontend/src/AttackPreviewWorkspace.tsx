import React,{useCallback,useEffect,useRef,useState} from 'react';
import {Api,ApiError} from './api';
import type {CharacterDetail,CharacterList} from './characters';
import {changeFrom,type CombatChange} from './combat';
import type {AttackPreview,AttackPreviewCreate,AttackPreviewCommand,AttackPublicResult,AttackFactor} from './attack-previews';
export type AttackPreviewProps={api:Api;campaignId:string;isDm:boolean;revision:number;onDenied():void};
const scopes=new WeakMap<Api,number>();let scopeId=0;
export function AttackPreviewWorkspace(props:AttackPreviewProps){if(!scopes.has(props.api))scopes.set(props.api,++scopeId);return <Workspace key={`${scopes.get(props.api)}:${props.campaignId}:${props.isDm}`} {...props}/>;}
const statuses={MANUAL_REQUIRED:'需人工裁定',PREVIEW:'待确认',CONFIRMED:'已确认，尚未应用',APPLIED:'已应用'};
const factorFields=[['exceptions','未支持例外'],['advantage','优势依据'],['disadvantage','劣势依据'],['immunities','该类型免疫依据'],['resistances','该类型抗性依据'],['vulnerabilities','该类型易伤依据']] as const;
function Workspace({api,campaignId:c,isDm,revision,onDenied}:AttackPreviewProps){
 const [feed,setFeed]=useState<AttackPreview[]>([]),[results,setResults]=useState<AttackPublicResult[]>([]),[cards,setCards]=useState<CharacterList>();
 const [selected,setSelected]=useState<AttackPreview>(),[source,setSource]=useState<CharacterDetail>(),[target,setTarget]=useState<CharacterDetail>(),[attack,setAttack]=useState(''),[reviewed,setReviewed]=useState(false),[factorText,setFactorText]=useState<Record<string,string>>({});
 const [changes,setChanges]=useState<CombatChange[]>([]),[checked,setChecked]=useState(false),[reason,setReason]=useState(''),[summary,setSummary]=useState(''),[correction,setCorrection]=useState(''),[correctionReady,setCorrectionReady]=useState(false),[readId,setReadId]=useState('');
 const [message,setMessage]=useState(''),[loading,setLoading]=useState(true),[busy,setBusy]=useState(false),[denied,setDenied]=useState(false),[needsRead,setNeedsRead]=useState(false),[pending,setPending]=useState<{send:()=>Promise<{id:string;version:number}>}>();
 const live=useRef(true),blocked=useRef(false),lock=useRef(false),seq=useRef(0),selection=useRef(0),sourceSeq=useRef(0),targetSeq=useRef(0),selectedId=useRef('');
 useEffect(()=>()=>{live.current=false;},[]);
 const observe=useCallback(async<T,>(promise:Promise<T>):Promise<T>=>{try{return await promise;}catch(e){if(live.current&&!blocked.current&&e instanceof ApiError&&(e.status===401||e.status===403)){blocked.current=true;selection.current++;setDenied(true);setFeed([]);setResults([]);setCards(undefined);setSelected(undefined);setSource(undefined);setTarget(undefined);setChanges([]);setReason('');setSummary('');setFactorText({});setPending(undefined);onDenied();}throw e;}},[onDenied]);
 const refresh=useCallback(async()=>{
  const id=++seq.current;setLoading(true);
  try{const [rs,ps,cs]=await Promise.all([observe(api.attackResults(c)),isDm?observe(api.attackPreviews(c)):Promise.resolve([]),isDm?observe(api.characters(c)):Promise.resolve(undefined)]);if(!live.current||blocked.current||id!==seq.current)return;setResults(rs);setFeed(ps);setCards(cs);}
  catch{if(live.current&&!blocked.current&&id===seq.current)setMessage('读取结算失败；请刷新结算。');}finally{if(live.current&&id===seq.current)setLoading(false);}
 },[api,c,isDm,observe]);
 useEffect(()=>{void refresh();},[refresh,revision]);
 function show(p:AttackPreview){setSelected(p);selectedId.current=p.id;setReason('');setSummary('');setChecked(false);setCorrection('');setCorrectionReady(false);setNeedsRead(false);setReadId('');setChanges(p.status==='APPLIED'?[]:[{characterId:p.evidence.target.id,expectedVersion:p.evidence.target.version,hp:p.evidence.target.sheet.combat.hp,tempHp:p.evidence.target.sheet.combat.tempHp,resources:p.evidence.target.sheet.resources.map(r=>({id:r.id,current:r.current}))}]);}
 async function choose(id:string){const n=++selection.current;setSelected(undefined);selectedId.current=id;setChanges([]);setChecked(false);if(!id)return;try{const p=await observe(api.attackPreview(c,id));if(live.current&&!blocked.current&&n===selection.current)show(p);}catch{if(live.current&&!blocked.current&&n===selection.current)setMessage('读取此预览失败。');}}
 async function chooseCard(id:string,which:'source'|'target'){
  const tracker=which==='source'?sourceSeq:targetSeq,n=++tracker.current;setReviewed(false);if(which==='source'){setSource(undefined);setAttack('');}else setTarget(undefined);if(!id)return;
  try{const d=await observe(api.character(c,id));if(!live.current||blocked.current||tracker.current!==n)return;if(which==='source'){setSource(d);setAttack('');}else setTarget(d);}catch{if(live.current&&!blocked.current&&tracker.current===n)setMessage('读取角色失败，请重新选择。');}
 }
 async function mutate(send:()=>Promise<{id:string;version:number}>){
  if(lock.current||blocked.current||!live.current)return;lock.current=true;setBusy(true);setPending({send});setMessage('等待服务器确认…');let acknowledged=false;
  try{const ack=await observe(send());acknowledged=true;if(!live.current||blocked.current)return;setPending(undefined);setNeedsRead(true);setReadId(ack.id);setMessage('请求已确认，正在读取。');
   const p=await observe(api.attackPreview(c,ack.id));if(!live.current||blocked.current)return;show(p);setMessage('结算已从服务器读取。');await refresh();
  }catch(e){if(!live.current||blocked.current)return;if(acknowledged){setPending(undefined);setNeedsRead(true);setMessage('请求已确认，但读取失败；请重新读取此预览。');}
   else if(e instanceof ApiError&&e.status<500){setPending(undefined);setChecked(false);setNeedsRead(!!selectedId.current);if(!selectedId.current){setSource(undefined);setTarget(undefined);setAttack('');setReviewed(false);}setMessage(e.status===409?'版本或证据冲突；请重新读取。原角色变化时须新建预览，不能重基旧确认。':e.message);}
   else setMessage('结果未确认；请重试同一结算请求。');
  }finally{lock.current=false;if(live.current)setBusy(false);}
 }
 const locked=loading||busy||!!pending||denied||needsRead;
 function create(){if(!source||!target||!attack)return;const factors=(key:string):AttackFactor[]=>factorText[key]?.trim()?[{reason:factorText[key].trim(),source:{book:'SRD 5.2.1',revision:'5.2.1',rules:'5R',detail:'DM review of applicability; '+key}}]:[];
  const body:AttackPreviewCreate={requestId:crypto.randomUUID(),characterId:source.id,sourceVersion:source.version,targetId:target.id,targetVersion:target.version,attackId:attack,reviewed,exceptions:factors('exceptions'),advantage:factors('advantage'),disadvantage:factors('disadvantage'),immunities:factors('immunities'),resistances:factors('resistances'),vulnerabilities:factors('vulnerabilities')};void mutate(()=>api.createAttackPreview(c,body));
 }
 function command(kind:AttackPreviewCommand['kind']){if(!selected)return;const body:AttackPreviewCommand={requestId:crypto.randomUUID(),expectedVersion:selected.version,kind,previewHash:selected.hash,changes:kind==='CONFIRM'||kind==='CORRECT'?structuredClone(changes):[],correctionOf:kind==='CORRECT'?correction:'',reason,summary:kind==='PUBLISH'?summary:''};const id=selected.id;void mutate(()=>api.attackPreviewCommand(c,id,body));}
 async function currentValues(){if(!selected)return;const n=++selection.current;setChecked(false);setCorrectionReady(false);setChanges([]);setBusy(true);try{const ds=await Promise.all([...new Set([selected.evidence.target.id,selected.evidence.source.id])].map(id=>observe(api.character(c,id))));if(live.current&&!blocked.current&&n===selection.current){setChanges(ds.map(changeFrom));setCorrectionReady(true);}}catch{if(live.current&&!blocked.current)setMessage('读取当前值失败；未准备更正。');}finally{if(live.current)setBusy(false);}}
 function edit(id:string,patch:Partial<CombatChange>){setChecked(false);setChanges(old=>old.map(v=>v.characterId===id?{...v,...patch}:v));}
 if(denied)return <section aria-label="武器结算预览"><p>结算权限已失效。</p></section>;
 return <section className="combat-workspace attack-preview-workspace" aria-label="武器结算预览"><h2>武器结算预览</h2><p>支持已审阅的 SRD 5.2.1 普通单目标武器；未支持能力与例外转人工裁定。保存暗骰预览不会扣血或消耗资源。</p>
 <button disabled={busy} onClick={()=>void refresh()}>刷新结算</button><p role="status">{message}</p>{needsRead&&readId&&<button disabled={busy} onClick={()=>void choose(readId)}>重新读取已确认预览</button>}{pending&&<button disabled={busy} onClick={()=>void mutate(pending.send)}>重试同一结算请求</button>}
 {isDm&&<><fieldset disabled={locked}><legend>生成新的持久预览</legend><label>预览行动角色<select aria-label="预览行动角色" value={source?.id??''} onChange={e=>void chooseCard(e.target.value,'source')}><option value="">选择角色与版本</option>{cards?.characters.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
 <label>预览武器<select aria-label="预览武器" value={attack} onChange={e=>{setAttack(e.target.value);setReviewed(false);}}><option value="">选择已保存攻击</option>{source?.sheet.attacks.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
 <label>预览目标<select aria-label="预览目标" value={target?.id??''} onChange={e=>void chooseCard(e.target.value,'target')}><option value="">选择目标与版本</option>{cards?.characters.map(x=><option key={x.id} value={x.id}>{x.name}</option>)}</select></label>
 {factorFields.map(([key,label])=><label key={key}>{label}<input aria-label={label} maxLength={1000} value={factorText[key]??''} onChange={e=>{setFactorText(v=>({...v,[key]:e.target.value}));setReviewed(false);}}/></label>)}
 <label><input type="checkbox" checked={reviewed} onChange={e=>setReviewed(e.target.checked)}/>我已审阅射程、合法性、角色特性、状态与所有适用例外</label><button disabled={!source||!target||!attack} onClick={create}>保存暗骰预览</button></fieldset>
 <label>已保存预览<select aria-label="已保存预览" value={selectedId.current} disabled={locked} onChange={e=>void choose(e.target.value)}><option value="">选择保存记录</option>{feed.map(p=><option key={p.id} value={p.id}>{p.evidence.source.sheet.name} → {p.evidence.target.sheet.name} · {statuses[p.status]} · {p.id}</option>)}</select></label>
 {selected&&<article><h3>{statuses[selected.status]}</h3><button disabled={busy||!!pending} onClick={()=>void choose(selected.id)}>重新读取此预览</button><p>证据哈希：{selected.hash}</p>
 {selected.evidence.resolution?<><p>候选命中：{selected.evidence.resolution.review.candidate.verdict}</p><p>候选伤害：{selected.evidence.resolution.damage}</p><p>候选仅供审阅；最终生命、临时生命、资源、0HP 与其他效果须由 DM 明确裁定。</p></>:<p>需人工裁定：{selected.evidence.manualReasons.join('；')}</p>}
 <details><summary>展开私有骰子、来源与计算快照</summary><p>{selected.evidence.source.sheet.name} → {selected.evidence.target.sheet.name} · {selected.actor} · {selected.at}</p>
 {[selected.evidence.attackRoll,selected.evidence.damageRoll].filter(r=>r!==null).map(r=><div key={r.id}><p>{r.key.startsWith('attack:')?'攻击':'伤害'}：{r.result.expression} = {r.result.total}</p><p>逐骰：{r.result.dice.map((d,i)=><span key={i}>d{d.sides} {d.value}（{d.kept?'保留':'舍弃'}） </span>)}</p>{r.calculation.components.map((x,i)=><p key={i}>{x.label} {x.value} · {x.source.book} {x.source.revision} · {x.source.detail}</p>)}</div>)}
 {selected.evidence.resolution?.explanation.map((e,i)=><p key={i}>{e.calculation}<br/><small>{e.source.book} · {e.source.revision} · {e.source.detail}</small></p>)}
 <details><summary>完整角色、目录、情境与来源快照</summary><pre>{JSON.stringify(selected.evidence,null,2)}</pre></details></details>
 <fieldset disabled={locked}><legend>明确最终值与更正</legend><label>结算理由<textarea aria-label="结算理由" maxLength={1000} value={reason} onChange={e=>setReason(e.target.value)}/></label>
 {selected.status==='PREVIEW'&&<button onClick={()=>void currentValues()}>读取双方当前值</button>}
 {selected.status==='APPLIED'&&<><button onClick={()=>void currentValues()}>读取当前值准备更正</button><label>更正所关联的应用<select aria-label="更正所关联的应用" value={correction} onChange={e=>setCorrection(e.target.value)}><option value="">选择原记录</option>{selected.events.filter(e=>e.kind==='APPLY'||e.kind==='CORRECT').map(e=><option key={e.id} value={e.id}>{e.kind} · {e.id}</option>)}</select></label></>}
 {changes.map(v=><div key={v.characterId}><p>{v.characterId} · 版本 {v.expectedVersion}</p><label>最终生命值<input aria-label={`最终生命值 ${v.characterId}`} type="number" min="0" value={v.hp} onChange={e=>edit(v.characterId,{hp:Number(e.target.value)})}/></label><label>最终临时生命<input aria-label={`最终临时生命 ${v.characterId}`} type="number" min="0" value={v.tempHp} onChange={e=>edit(v.characterId,{tempHp:Number(e.target.value)})}/></label>{v.resources.map(r=><label key={r.id}>资源 {r.id}<input aria-label={`最终资源 ${v.characterId} ${r.id}`} type="number" min="0" value={r.current} onChange={e=>edit(v.characterId,{resources:v.resources.map(x=>x.id===r.id?{...x,current:Number(e.target.value)}:x)})}/></label>)}</div>)}
 <label><input type="checkbox" checked={checked} onChange={e=>setChecked(e.target.checked)}/>我已核对最终值及未支持效果</label>
 {selected.status==='PREVIEW'&&<button disabled={!checked||!reason.trim()||!changes.length} onClick={()=>command('CONFIRM')}>确认最终值</button>}
 {selected.status==='CONFIRMED'&&<><p>即将应用已保存的确认值：{JSON.stringify(selected.events.find(e=>e.kind==='CONFIRM')?.changes)}</p><button disabled={!checked||!reason.trim()} onClick={()=>command('APPLY')}>原子应用已确认结果</button></>}
 {selected.status==='APPLIED'&&<><button disabled={!correctionReady||!checked||!reason.trim()||!correction||!changes.length} onClick={()=>command('CORRECT')}>追加当前值更正</button><label>单独公开的摘要<textarea aria-label="单独公开的摘要" maxLength={1000} value={summary} onChange={e=>setSummary(e.target.value)}/></label><button disabled={!reason.trim()||!summary.trim()} onClick={()=>command('PUBLISH')}>确认公开摘要</button></>}
 </fieldset><details><summary>展开确认、应用与更正历史</summary>{selected.events.map(e=><article key={e.id}><p>{{CONFIRM:'确认最终值',APPLY:'应用结果',CORRECT:'更正当前值',PUBLISH:'单独公开摘要'}[e.kind]} · {e.actor} · {e.at}</p><p>{e.reason}</p>{e.correctionOf&&<p>关联原记录：{e.correctionOf}</p>}{e.changes.map(v=><p key={v.characterId}>{v.characterId===selected.evidence.source.id?selected.evidence.source.sheet.name:selected.evidence.target.sheet.name}：生命 {v.hp} · 临时生命 {v.tempHp} · 资源 {v.resources.map(r=>`${r.id}: ${r.current}`).join('，')}</p>)}{e.summary&&<p>{e.summary}</p>}</article>)}</details></article>}</>}
 <h3>公开结算摘要</h3>{results.length?results.map(r=><article key={r.id}><strong>{r.name} → {r.targetName}</strong><p>{r.summary}</p><p>{r.actor} · {r.at}</p></article>):<p>尚无公开结算摘要。</p>}</section>;
}
