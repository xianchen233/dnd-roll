import React,{useLayoutEffect,useRef,useState} from 'react';
import {ApiError} from './api';
import type {Api} from './api';
import type {Scene} from './types';
import type {RangeHint,RangeHintRequest} from './range-hints';
export interface RangeHintProps {api:Api;scene:Scene;isDm:boolean;revision:number;onDenied():void;onRefresh():void}
export function RangeHintWorkspace(props:RangeHintProps){
  // Remount before paint when any authoritative endpoint or permission scope changes.
  const scope=JSON.stringify([props.scene,props.isDm,props.revision]);
  return <RangeHintForm key={scope} {...props}/>;
}
function RangeHintForm({api,scene,onDenied,onRefresh}:RangeHintProps){
  const [source,setSource]=useState(''),[target,setTarget]=useState('');
  const [mode,setMode]=useState<'MELEE'|'RANGED'>('MELEE'),[normal,setNormal]=useState('5'),[long,setLong]=useState('5'),[reviewed,setReviewed]=useState(false);
  const [hint,setHint]=useState<RangeHint>(),[error,setError]=useState(''),[busy,setBusy]=useState(false),[denied,setDenied]=useState(false);
  const sequence=useRef(0),epoch=useRef(0);
  useLayoutEffect(()=>{epoch.current++;sequence.current++;setHint(undefined);setError('');setBusy(false);setDenied(false);return()=>{epoch.current++;sequence.current++;};},[api]);
  function edit(change:()=>void){sequence.current++;setHint(undefined);setError('');setBusy(false);change();}
  async function measure(){
    const a=scene.tokens.find(t=>t.id===source),b=scene.tokens.find(t=>t.id===target);if(!a||!b)return;
    const body:RangeHintRequest={sceneId:scene.id,sceneVersion:scene.version,sourceId:a.id,sourceVersion:a.version,targetId:b.id,targetVersion:b.version,profile:'GRID_5R_5FT_RANGE_V1',reviewed,mode,normalFeet:Number(normal),longFeet:mode==='MELEE'?Number(normal):Number(long)};
    const request=++sequence.current,scope=epoch.current;setHint(undefined);setError('');setBusy(true);
    try{const result=await api.previewRange(scene.campaignId,body);if(scope===epoch.current&&request===sequence.current)setHint(result);}
    catch(e){
      if(scope!==epoch.current)return;
      // Even an older request in the current scope can report revoked membership.
      if(e instanceof ApiError&&(e.status===401||e.status===403)){sequence.current++;setHint(undefined);setDenied(true);setError('范围查询权限已失效。');setBusy(false);onDenied();return;}
      if(request!==sequence.current)return;
      if(e instanceof ApiError&&(e.status===404||e.status===409)){setError('场景或目标已变化，请刷新后重新测量。');onRefresh();}
      else setError('范围查询失败，可重新测量。');
    }finally{if(scope===epoch.current&&request===sequence.current)setBusy(false);}
  }
  const bands={WITHIN_REACH:'在声明触及内',NORMAL_RANGE:'在声明普通射程内',LONG_RANGE:'远射程：通常具有劣势，最终优劣势由 DM 裁定',OUT_OF_RANGE:'超出声明范围'};
  const valid=Number.isInteger(Number(normal))&&Number(normal)>=0&&Number(normal)<=100000&&(mode==='MELEE'||Number.isInteger(Number(long))&&Number(long)>=Number(normal)&&Number(long)<=100000);
  return <section aria-label="范围提示"><h3>范围提示</h3><p className="hint">5R · SRD 5.2.1 · 5尺方格。只测当前可见棋子；遮蔽、视线、体型、高度、贴身远程与借机攻击需另行裁定。范围提示不代表可以攻击。</p>
    <form onSubmit={e=>{e.preventDefault();void measure();}}><fieldset disabled={denied}>
      <label>范围起点<select value={source} onChange={e=>edit(()=>setSource(e.target.value))}><option value="">选择可见棋子</option>{scene.tokens.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</select></label>
      <label>范围目标<select value={target} onChange={e=>edit(()=>setTarget(e.target.value))}><option value="">选择可见棋子</option>{scene.tokens.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</select></label>
      <label>范围方式<select value={mode} onChange={e=>edit(()=>setMode(e.target.value as typeof mode))}><option value="MELEE">触及</option><option value="RANGED">射程</option></select></label>
      <label>{mode==='MELEE'?'声明触及（尺）':'普通射程（尺）'}<input type="number" min="0" max="100000" step="1" required value={normal} onChange={e=>edit(()=>setNormal(e.target.value))}/></label>
      {mode==='RANGED'&&<label>最大射程（尺）<input type="number" min={normal} max="100000" step="1" required value={long} onChange={e=>edit(()=>setLong(e.target.value))}/></label>}
      <label><input type="checkbox" checked={reviewed} onChange={e=>edit(()=>setReviewed(e.target.checked))}/>已核对5尺方格、单格同平面及适用例外</label>
      <button disabled={!source||!target||!reviewed||!valid||busy}>计算范围提示</button>
    </fieldset></form>
    {error&&<p role="alert">{error}</p>}{hint&&<p role="status">{hint.status==='CANDIDATE'?`距离 ${hint.distanceFeet} 尺 · ${hint.band?bands[hint.band]:''}`:hint.reason==='GRID_REQUIRES_MANUAL'?'当前地图需人工测距。':'请先审阅方格、体型与规则例外。'}</p>}
  </section>;
}
