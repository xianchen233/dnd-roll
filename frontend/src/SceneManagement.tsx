import React, { useEffect, useRef, useState } from 'react';
import { ApiError, type Api } from './api';
import type { Scenes, VersionedCommand } from './types';
type Command = { kind: 'create'; body: VersionedCommand & { name: string } } | { kind: 'activate'; target: string; body: VersionedCommand };
export function SceneManagement({ api, campaignId, revision, onChanged, onDenied }: { api: Api; campaignId: string; revision: number; onChanged(): Promise<void>; onDenied(): void }) {
  const [data, setData] = useState<Scenes>(), [name, setName] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const pending = useRef<Command | undefined>(undefined), active = useRef(true), readId = useRef(0);
  function denial(e: unknown) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) { active.current = false; pending.current = undefined; setData(undefined); onDenied(); return true; }
    return false;
  }
  async function load() {
    const read = ++readId.current;
    try { const next = await api.scenes(campaignId); if (!active.current || read !== readId.current) return; setData(next); return next; }
    catch (e) { if (active.current) denial(e); throw e; }
  }
  useEffect(() => { active.current = true; return () => { active.current = false; readId.current++; }; }, [api, campaignId]);
  useEffect(() => { void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '读取场景列表失败'); }); }, [api, campaignId, revision]);
  async function run(kind: 'create' | 'activate', target?: string) {
    if (busy || !active.current) return;
    const trimmed = name.trim();
    if (!pending.current && kind === 'create' && (!trimmed || [...trimmed].length > 100)) { setError('场景名称需为 1–100 个字符。'); return; }
    setBusy(true); setError(''); setMessage('');
    try {
      if (!pending.current) {
        // P01 invitations and scene commands share this version. Read it again at
        // the point of a new action; explicit uncertain retries keep their body.
        const latest = await load(); if (!latest || !active.current) return;
        const body = { requestId: crypto.randomUUID(), expectedVersion: latest.campaignVersion };
        pending.current = kind === 'create' ? { kind, body: { ...body, name: trimmed } } : { kind, target: target!, body };
      }
      const cmd = pending.current;
      if (cmd.kind === 'create') await api.createScene(campaignId, cmd.body);
      else await api.activateScene(campaignId, cmd.target, cmd.body);
      if (!active.current) return;
      pending.current = undefined;
      // Receipts can describe a historical activation. Only reads establish current.
      await load(); if (!active.current) return;
      await onChanged(); if (!active.current) return;
      setMessage(cmd.kind === 'create' ? '空场景已创建；全团当前场景保持不变。' : '切换请求已确认；全团当前场景以最新服务器快照为准。');
      if (cmd.kind === 'create') setName('');
    } catch (e) {
      if (!active.current || denial(e)) return;
      if (e instanceof ApiError && e.status === 409) {
        pending.current = undefined;
        try { await load(); if (active.current) { await onChanged(); if (active.current) setError('操作冲突，已刷新场景列表。请检查当前状态后重新操作。'); } }
        catch { if (active.current) setError('操作冲突；场景列表刷新失败，请手动刷新。'); }
      } else {
        if (e instanceof ApiError && e.status < 500) pending.current = undefined;
        setError(`${e instanceof Error ? e.message : '场景操作失败'}${pending.current ? '；结果未确认，可安全重试同一场景操作。' : ''}`);
      }
    } finally { if (active.current) setBusy(false); }
  }
  return <section className="campaign-management" aria-label="场景管理"><div className="management-title"><h2>场景管理</h2><button className="quiet" disabled={busy} onClick={() => { setError(''); void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '刷新场景列表失败'); }); }}>刷新场景列表</button></div>{error && <p className="error" role="alert">{error}</p>}{message && <p role="status" className="hint">{message}</p>}{data && <><p>当前：{data.scenes.find(s => s.id === data.currentSceneId)?.name ?? '读取中'}</p><ul>{data.scenes.map(s => <li key={s.id}><span>{s.name}{s.id === data.currentSceneId ? ' · 全团当前场景' : ''}</span><button aria-label={`切换到 ${s.name}`} disabled={busy || !!pending.current || s.id === data.currentSceneId} onClick={() => void run('activate', s.id)}>切换全团场景</button></li>)}</ul></>}<form onSubmit={e => { e.preventDefault(); void run('create'); }}><label>场景名称<input value={name} disabled={busy || !!pending.current} onChange={e => setName(e.target.value)} required /></label><button disabled={busy || !data || !!pending.current}>创建空场景</button></form>{pending.current && <button disabled={busy} onClick={() => void run(pending.current!.kind)}>重试同一场景操作</button>}<p className="hint">新建空场景不会切换全团；切换后所有成员使用同一场景。地图原图与校准可在下方备团区管理；棋子可在下方配置；地图遮盖将在后续阶段提供。</p></section>;
}
