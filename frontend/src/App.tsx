import React, { useEffect, useMemo, useState, useSyncExternalStore } from 'react';
import { Api } from './api';
import { SceneStore } from './scene-store';
import type { Campaign, Identity } from './types';
import { MapCanvas } from './MapCanvas';
import { connectRealtime, type Connection } from './realtime';
const defaultApi = new Api();
export function App({ api = defaultApi }: { api?: Api }) {
  const store = useMemo(() => new SceneStore(api), [api]);
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot);
  const [identity, setIdentity] = useState<Identity>(), [campaigns, setCampaigns] = useState<Campaign[]>([]), [campaignId, setCampaignId] = useState('');
  const [username, setUsername] = useState(''), [password, setPassword] = useState(''), [error, setError] = useState(''), [busy, setBusy] = useState(false), [checking, setChecking] = useState(true);
  const [selected, setSelected] = useState<string>(), [x, setX] = useState(''), [y, setY] = useState(''), [connection, setConnection] = useState<Connection>('connecting');
  useEffect(() => {
    let disposed = false;
    void api.getCsrf().then(() => api.me()).then(user => { if (!disposed) setIdentity(user); }).catch(() => {}).finally(() => { if (!disposed) setChecking(false); });
    return () => { disposed = true; store.reset(); };
  }, [api, store]);
  useEffect(() => {
    if (!identity) return;
    let disposed = false;
    void api.campaigns().then(list => { if (!disposed) { setCampaigns(list); setCampaignId(list[0]?.id ?? ''); } }).catch(e => { if (!disposed) setError(e.message); });
    return () => { disposed = true; };
  }, [identity, api]);
  useEffect(() => { if (!campaignId) return; setSelected(undefined); void store.open(campaignId); return () => store.reset(); }, [campaignId, store]);
  useEffect(() => { if (!identity || !campaignId) return; return connectRealtime(api, campaignId, () => { void store.refresh(); }, setConnection); }, [api, campaignId, identity, store]);
  const token = state.scene?.tokens.find(t => t.id === selected && t.canMove);
  const disabled = state.status === 'pending' || store.canRetry();
  const campaign = campaigns.find(c => c.id === campaignId);
  async function login(event: React.FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try { setIdentity(await api.login(username.trim(), password)); setPassword(''); } catch (e) { setError(e instanceof Error ? e.message : '登录失败'); } finally { setBusy(false); }
  }
  async function logout() {
    setBusy(true); setError('');
    try { await api.logout(); setIdentity(undefined); setCampaigns([]); setCampaignId(''); store.reset(); } catch { setError('退出失败，请重试'); } finally { setBusy(false); }
  }
  return <div className="app-shell"><header className="topbar"><a className="brand" href="/">D<span className="brand-die">20</span>ND <b>ROLL</b></a><span className="edition">2024 · 5R</span><div className="topbar-right">{identity ? <><span className={`connection ${connection}`}>● {connection === 'online' ? '实时连接' : connection === 'connecting' ? '正在连接' : '离线 · 正在重连'}</span><strong>{identity.username}</strong><button className="quiet" disabled={busy} onClick={() => void logout()}>退出</button></> : <span className="muted">每一场冒险，都值得留下</span>}</div></header>
    {!identity ? <main className="login-layout"><div className="intro"><p className="eyebrow">YOUR NEXT CHAPTER</p><h1>故事在这里<br /><em>继续。</em></h1><p>与伙伴回到同一张桌面。<br />让每一步探索，都成为共同的冒险。</p><div className="intro-footer">DND ROLL <span>◇</span> 冒险桌面</div></div><form className="login-card" onSubmit={event => void login(event)}><p className="eyebrow">欢迎回来</p><h2>进入你的冒险</h2><p className="muted">使用主持人提供的固定账号登录。</p><label>账号<input autoComplete="username" value={username} onChange={e => setUsername(e.target.value)} required /></label><label>密码<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></label>{error && <p className="error" role="alert">{error}</p>}<button className="primary" disabled={busy || checking}>{checking ? '检查会话…' : busy ? '正在登录…' : '进入冒险'}</button><p className="scope-note">当前版本：账号登录、战役场景与探索移动。<br />账号注册与邀请将于后续阶段开放。</p></form></main> : <><main className="tabletop"><aside className="sidebar"><p className="eyebrow">当前战役</p><label className="sr-only" htmlFor="campaign">选择战役</label><select id="campaign" value={campaignId} onChange={e => setCampaignId(e.target.value)} disabled={state.status === 'pending'}>{campaigns.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select><span className="role-tag">{campaign?.role === 'DM' ? '主持人 DM' : '玩家'}</span><div className="section-heading"><h2>场景单位</h2><span>{state.scene?.tokens.length ?? '—'}</span></div><p className="hint">选择可控单位，再点击地图上的目标格。</p><div className="unit-list">{state.scene?.tokens.map(t => <button className={`unit ${selected === t.id ? 'selected' : ''}`} key={t.id} disabled={!t.canMove || disabled} onClick={() => setSelected(t.id)}><span className="avatar">{t.name.slice(0, 1)}</span><span><strong>{t.name}</strong><small>{t.canMove ? '可控制' : '仅查看'} · ({t.x}, {t.y})</small></span>{t.canMove && <span className="unit-arrow">›</span>}</button>)}</div><div className="sidebar-bottom"><span>探索模式</span><p>移动不计算战斗速度或路径预算。</p></div></aside>
      <section className="map-panel"><div className="scene-header"><div><p className="eyebrow">EXPLORATION</p><h1>{state.scene?.name ?? '等待场景'}</h1></div><button className="quiet" onClick={() => void store.refresh()}>↻ 刷新场景</button></div>{state.scene ? <><div className="map-meta"><span>{state.scene.width} × {state.scene.height} 方格</span><span>服务器版本 {state.scene.version}</span></div><MapCanvas scene={state.scene} selected={token?.id} disabled={disabled} onSelect={setSelected} onMove={(nextX, nextY) => { if (token) void store.move(token.id, nextX, nextY); }} /><div className="map-caption"><span>◇ {token ? `正在控制：${token.name}` : '先选择左侧可控单位'}</span><span>坐标从 (0, 0) 开始</span></div></> : <div className="empty-state">{campaigns.length ? '正在读取战役场景…' : '尚未加入战役。请联系主持人添加成员。'}</div>}</section>
      <aside className="inspector"><p className="eyebrow">单位控制</p><h2>{token?.name ?? '未选择单位'}</h2><p className="hint">{token ? `已确认位置 (${token.x}, ${token.y}) · 版本 ${token.version}` : '只有获授权的单位可以移动。'}</p><form onSubmit={event => { event.preventDefault(); if (token) void store.move(token.id, Number(x), Number(y)); }}><div className="coordinates"><label>目标 X<input type="number" min="0" max={(state.scene?.width ?? 24) - 1} step="1" required value={x} onChange={e => setX(e.target.value)} /></label><label>目标 Y<input type="number" min="0" max={(state.scene?.height ?? 16) - 1} step="1" required value={y} onChange={e => setY(e.target.value)} /></label></div><button className="primary" disabled={!token || disabled}>确认移动</button></form><div className={`save-status ${state.status}`} aria-live="polite"><strong>{({ idle: '等待场景', loading: '正在读取', confirmed: '已确认', pending: '待确认', failed: '操作未完成', conflict: '状态冲突' })[state.status]}</strong><p>{state.message}</p>{state.status === 'failed' && store.canRetry() && <button onClick={() => void store.retry()}>重试同一操作</button>}</div><div className="scope-note">此阶段提供探索移动。角色卡、战斗、掷骰与 RP 正在后续开发计划中。</div></aside></main>{error && <p className="global-error" role="alert">{error}</p>}<footer className="statusbar"><span>DND ROLL / 首个可运行切片</span><span>已确认状态由服务器保存 · 每 15 秒补充同步</span></footer></>}
  </div>;
}
