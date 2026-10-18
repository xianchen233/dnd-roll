import React, { useEffect, useRef, useState } from 'react';
import { ApiError, type Api } from './api';
import type { Assets, MapConfiguration, SceneMap, VersionedCommand } from './types';
type Command = { kind: 'upload'; body: VersionedCommand; file: File } | { kind: 'map'; body: VersionedCommand & { map: MapConfiguration | null } };
const defaults = { assetId: '', gridSize: '48', offsetX: '0', offsetY: '0', distancePerCell: '5' };
export function AssetManagement({ api, campaignId, sceneId, revision, onChanged, onDenied }: { api: Api; campaignId: string; sceneId: string; revision: number; onChanged(): void; onDenied(): void }) {
  const [assets, setAssets] = useState<Assets>(), [configuration, setConfiguration] = useState<SceneMap>(), [fields, setFields] = useState(defaults), [file, setFile] = useState<File>();
  const [busy, setBusy] = useState(false), [preview, setPreview] = useState(''), [error, setError] = useState(''), [message, setMessage] = useState('');
  const active = useRef(true), readId = useRef(0), contentId = useRef(0), previewUrl = useRef(''), pending = useRef<Command | undefined>(undefined);
  function clearPreview() { contentId.current++; if (previewUrl.current) URL.revokeObjectURL(previewUrl.current); previewUrl.current = ''; setPreview(''); }
  function denial(e: unknown) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) {
      active.current = false; readId.current++; pending.current = undefined; setFile(undefined); setAssets(undefined); setConfiguration(undefined); setFields(defaults); clearPreview(); onDenied(); return true;
    }
    return false;
  }
  async function observe<T>(request: Promise<T>): Promise<T> { try { return await request; } catch (e) { if (active.current) denial(e); throw e; } }
  async function load() {
    const read = ++readId.current;
    const [nextAssets, nextMap] = await Promise.all([observe(api.assets(campaignId)), observe(api.sceneMap(campaignId, sceneId))]);
    if (!active.current || read !== readId.current) return;
    setAssets(nextAssets); setConfiguration(nextMap);
    const map = nextMap.map;
    setFields(map ? { assetId: map.assetId, gridSize: String(map.gridSize), offsetX: String(map.offsetX), offsetY: String(map.offsetY), distancePerCell: String(map.distancePerCell) } : defaults);
    return { assets: nextAssets, configuration: nextMap };
  }
  useEffect(() => {
    active.current = true;
    return () => { active.current = false; readId.current++; contentId.current++; pending.current = undefined; if (previewUrl.current) URL.revokeObjectURL(previewUrl.current); previewUrl.current = ''; };
  }, []);
  useEffect(() => { void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '读取素材失败'); }); }, [revision]);
  async function content(assetId: string, download = false) {
    if (!active.current) return;
    const read = ++contentId.current;
    try {
      const blob = await observe(api.assetContent(campaignId, assetId));
      if (!active.current || read !== contentId.current) return;
      const url = URL.createObjectURL(blob);
      if (download) {
        // Downloads use the same authorized fetch as previews; no permanent URL
        // or browser-cache capability is handed to another session.
        const anchor = document.createElement('a'); anchor.href = url; anchor.download = `map-${assetId}.${blob.type === 'image/jpeg' ? 'jpg' : 'png'}`; anchor.click(); URL.revokeObjectURL(url);
      } else { if (previewUrl.current) URL.revokeObjectURL(previewUrl.current); previewUrl.current = url; setPreview(url); }
    } catch (e) { if (active.current) setError(e instanceof Error ? e.message : '读取素材内容失败'); }
  }
  async function run(kind: 'upload' | 'map' | 'detach') {
    if (busy || !active.current) return;
    setBusy(true); setError(''); setMessage(''); let acknowledged = false;
    try {
      if (!pending.current) {
        if (kind === 'upload' && (!file || !['image/png', 'image/jpeg'].includes(file.type) || file.size === 0 || file.size > 10485760)) throw new Error('请选择不超过 10 MiB 的 PNG 或 JPEG；服务器还会校验真实图片。');
        const latest = await load(); if (!latest || !active.current) return;
        if (kind === 'upload') pending.current = { kind, file: file!, body: { requestId: crypto.randomUUID(), expectedVersion: latest.assets.campaignVersion } };
        else {
          const map: MapConfiguration | null = kind === 'detach' ? null : { assetId: fields.assetId, gridSize: Number(fields.gridSize), offsetX: Number(fields.offsetX), offsetY: Number(fields.offsetY), distancePerCell: Number(fields.distancePerCell) };
          if (map && (!map.assetId || !Number.isInteger(map.gridSize) || map.gridSize < 8 || map.gridSize > 512 || !Number.isInteger(map.offsetX) || Math.abs(map.offsetX) > 8192 || !Number.isInteger(map.offsetY) || Math.abs(map.offsetY) > 8192 || !Number.isFinite(map.distancePerCell) || map.distancePerCell < .01 || map.distancePerCell > 10000)) throw new Error('请选择素材，并检查校准数值范围。');
          pending.current = { kind: 'map', body: { requestId: crypto.randomUUID(), expectedVersion: latest.configuration.sceneVersion, map } };
        }
      }
      const command = pending.current;
      if (command.kind === 'upload') await observe(api.uploadAsset(campaignId, command.body, command.file));
      else await observe(api.saveSceneMap(campaignId, sceneId, command.body));
      if (!active.current) return;
      acknowledged = true; pending.current = undefined; onChanged();
      // The receipt can be historical. Its map must never overwrite current state.
      const current = await load(); if (!current || !active.current) return;
      setMessage(command.kind === 'upload' ? '上传请求已确认；素材列表已刷新。' : '地图请求已确认；当前配置已从服务器刷新。');
    } catch (e) {
      if (!active.current || denial(e)) return;
      if (acknowledged) { setError('请求已确认；刷新失败，当前配置尚未同步。请刷新素材与地图。'); return; }
      if (e instanceof ApiError && e.status === 409) {
        pending.current = undefined;
        try { await load(); if (active.current) setError('操作冲突，已刷新素材与地图配置。请检查后重新操作。'); }
        catch { if (active.current) setError('操作冲突；刷新失败，请刷新素材与地图。'); }
      } else {
        if (e instanceof ApiError && e.status < 500) pending.current = undefined;
        setError(`${e instanceof Error ? e.message : '素材操作失败'}${pending.current ? '；结果未确认，可重试同一素材操作。' : ''}`);
      }
    } finally { if (active.current) setBusy(false); }
  }
  const disabled = busy || !!pending.current;
  return <section className="campaign-management asset-management" aria-label="地图备团"><div className="management-title"><h2>地图备团</h2><button className="quiet" disabled={busy} onClick={() => { setError(''); void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '刷新素材失败'); }); }}>刷新素材与地图</button></div><p className="hint">仅 DM 可查看原图和配置，用于备团。此阶段共享探索网格保持不变；玩家地图展示与遮盖将在后续阶段提供。</p>{error && <p role="alert" className="error">{error}</p>}{message && <p role="status" className="hint">{message}</p>}<form className="asset-upload" onSubmit={e => { e.preventDefault(); void run('upload'); }}><label>上传地图文件<input type="file" accept="image/png,image/jpeg" disabled={disabled} onChange={e => setFile(e.target.files?.[0])} /></label><button disabled={disabled || !assets || !file}>上传素材</button></form><p className="hint">PNG / JPEG，最大 10 MiB；原始文件名不会保存。</p><ul>{assets?.assets.map(a => <li key={a.id}><span>{a.id}<small>{a.width} × {a.height} · {a.mediaType} · {a.byteSize} 字节</small></span><div className="asset-actions"><button aria-label={`预览 ${a.id}`} onClick={() => void content(a.id)}>预览</button><button aria-label={`下载 ${a.id}`} onClick={() => void content(a.id, true)}>下载</button></div></li>)}</ul>{preview && <div className="asset-preview"><img src={preview} alt="DM 私密地图预览" /><button className="quiet" onClick={clearPreview}>关闭预览</button></div>}<form onSubmit={e => { e.preventDefault(); void run('map'); }}><label>当前场景地图素材<select aria-label="当前场景地图素材" value={fields.assetId} disabled={disabled || !configuration} onChange={e => setFields(v => ({ ...v, assetId: e.target.value }))}><option value="">请选择素材</option>{assets?.assets.map(a => <option value={a.id} key={a.id}>{a.id} · {a.width} × {a.height}</option>)}</select></label><div className="asset-calibration">{([{ key: 'gridSize', label: '图片每格像素', min: 8, max: 512, step: 1 }, { key: 'offsetX', label: '图片偏移 X', min: -8192, max: 8192, step: 1 }, { key: 'offsetY', label: '图片偏移 Y', min: -8192, max: 8192, step: 1 }, { key: 'distancePerCell', label: '每格距离', min: .01, max: 10000, step: 'any' }] as const).map(f => <label key={f.key}>{f.label}<input type="number" min={f.min} max={f.max} step={f.step} required disabled={disabled} value={fields[f.key]} onChange={e => setFields(v => ({ ...v, [f.key]: e.target.value }))} /></label>)}</div><div className="asset-actions"><button disabled={disabled || !configuration || !fields.assetId}>保存地图配置</button><button type="button" disabled={disabled || !configuration?.map} onClick={() => void run('detach')}>移除地图配置</button></div></form>{pending.current && <button disabled={busy} onClick={() => void run(pending.current!.kind)}>重试同一素材操作</button>}</section>;
}
