import React, { useEffect, useRef, useState } from 'react';
import { ApiError, type Api } from './api';
import { rectangle } from './map-geometry';
import type { Cell, Fog, FogCommand } from './types';
type Props = { api: Api; campaignId: string; sceneId: string; width: number; height: number; revision: number; onChanged(): void; onDenied(): void; children(p: { revealed: Cell[]; locked: boolean; onRectangle(a: Cell, b: Cell, reveal: boolean): void }): React.ReactNode };
export function FogManagement(props: Props) { return <FogEditor key={`${props.campaignId}/${props.sceneId}`} {...props} />; }
function FogEditor({ api, campaignId, sceneId, width, height, revision, onChanged, onDenied, children }: Props) {
  const [draft, setDraft] = useState<Cell[]>([]), [ready, setReady] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const [coords, setCoords] = useState(['0', '0', '0', '0']);
  const active = useRef(true), readId = useRef(0), version = useRef<number | undefined>(undefined), dirty = useRef(false), pending = useRef<FogCommand | undefined>(undefined), afterAck = useRef<number | undefined>(undefined);
  function deny(e: unknown) {
    if (active.current && e instanceof ApiError && (e.status === 401 || e.status === 403)) { active.current = false; readId.current++; pending.current = undefined; afterAck.current = undefined; setDraft([]); setReady(false); setError(''); setMessage(''); onDenied(); return true; }
    return false;
  }
  async function load() {
    if (!active.current) return; const read = ++readId.current; setReady(false);
    try {
      const fog: Fog = await api.fog(campaignId, sceneId); if (!active.current || read !== readId.current) return;
      if (fog.sceneId !== sceneId) throw new Error('遮盖场景不匹配，请刷新。');
      setReady(true);
      if (!dirty.current) { setDraft(fog.revealed); version.current = fog.sceneVersion; }
      else if (version.current !== fog.sceneVersion) setMessage('场景已变化；遮盖草稿保留原版本，保存时将检查冲突。');
      if (afterAck.current !== undefined && read > afterAck.current) { setMessage('遮盖请求已确认；当前揭露状态已从服务器刷新。'); setError(''); }
    } catch (e) {
      if (deny(e) || !active.current || read !== readId.current) return;
      setMessage(''); setError(afterAck.current !== undefined ? '请求已确认；刷新失败，遮盖尚未同步。请刷新遮盖。' : e instanceof Error ? e.message : '读取遮盖失败');
    }
  }
  useEffect(() => { active.current = true; return () => { active.current = false; readId.current++; pending.current = undefined; }; }, []);
  useEffect(() => { void load(); }, [revision]);
  const locked = busy || !!pending.current;
  function edit(a: Cell, b: Cell, reveal: boolean) {
    if (locked || !ready || version.current === undefined) return;
    if (![a, b].every(c => Number.isInteger(c.x) && Number.isInteger(c.y) && c.x >= 0 && c.y >= 0 && c.x < width && c.y < height)) { setError('请填写网格内的整数坐标。'); return; }
    dirty.current = true; afterAck.current = undefined; setMessage('草稿尚未保存；玩家地图保持服务器已确认状态。'); setError(''); setDraft(cells => rectangle(cells, a, b, reveal));
  }
  async function save() {
    if (!active.current || busy || !ready || version.current === undefined) return;
    setBusy(true); setError(''); setMessage(''); afterAck.current = undefined;
    try {
      pending.current ??= { requestId: crypto.randomUUID(), expectedVersion: version.current, revealed: draft.map(c => ({ ...c })) };
      await api.saveFog(campaignId, sceneId, pending.current); if (!active.current) return;
      pending.current = undefined; dirty.current = false; afterAck.current = readId.current; onChanged(); await load();
    } catch (e) {
      if (deny(e) || !active.current) return;
      if (e instanceof ApiError && e.status === 409) { pending.current = undefined; dirty.current = false; await load(); if (active.current) { setMessage(''); setError('遮盖操作冲突，已请求最新状态；请检查后重新编辑。'); } }
      else { if (e instanceof ApiError && e.status < 500) pending.current = undefined; setError(`${e instanceof Error ? e.message : '遮盖保存失败'}${pending.current ? '；结果未确认，请重试同一遮盖操作。' : ''}`); }
    } finally { if (active.current) setBusy(false); }
  }
  function coordinates(reveal: boolean) {
    if (coords.some(v => !v.trim())) { setError('请填写网格内的整数坐标。'); return; }
    edit({ x: Number(coords[0]), y: Number(coords[1]) }, { x: Number(coords[2]), y: Number(coords[3]) }, reveal);
  }
  return <>{children({ revealed: draft, locked: locked || !ready, onRectangle: edit })}<section className="campaign-management fog-management" aria-label="地图遮盖"><div className="management-title"><h2>地图遮盖</h2><button disabled={busy} onClick={() => void load()}>刷新遮盖</button></div><p className="hint">矩形揭露或遮盖只修改草稿，保存后才影响玩家。单位隐藏独立管理。已公开并被保存的图片无法撤回。</p><p>已揭露 {draft.length} 格</p><fieldset disabled={locked}><legend>遮盖坐标</legend><div className="asset-calibration">{['起点 X', '起点 Y', '终点 X', '终点 Y'].map((label, i) => <label key={label}>{label}<input type="number" min="0" max={(i % 2 ? height : width) - 1} step="1" value={coords[i]} onChange={e => setCoords(v => v.map((old, n) => n === i ? e.target.value : old))} /></label>)}</div><button type="button" disabled={!ready} onClick={() => coordinates(true)}>加入揭露矩形</button> <button type="button" disabled={!ready} onClick={() => coordinates(false)}>加入遮盖矩形</button></fieldset><button disabled={locked || !ready} onClick={() => void save()}>保存地图遮盖</button>{pending.current && <button disabled={busy || !ready} onClick={() => void save()}>重试同一遮盖操作</button>}{error && <p role="alert" className="error">{error}</p>}{message && <p role="status" className="hint">{message}</p>}</section></>;
}
