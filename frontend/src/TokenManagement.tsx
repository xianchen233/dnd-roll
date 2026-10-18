import React, { useEffect, useRef, useState } from 'react';
import { ApiError, type Api } from './api';
import type { AuthoredToken, AuthoredTokens, Members, TokenCommand } from './types';
type Props = { api: Api; campaignId: string; sceneId: string; width: number; height: number; revision: number; membersRevision: number; onChanged(): void; onDenied(): void };
type Fields = { name: string; x: string; y: string; hidden: boolean; controllers: string[] };
const empty: Fields = { name: '', x: '0', y: '0', hidden: false, controllers: [] };
function fieldsFor(t: AuthoredToken): Fields { return { name: t.name, x: String(t.x), y: String(t.y), hidden: t.hidden, controllers: [...t.controllers] }; }
// Key the private session by both scopes so old promises retain a disposed owner.
export function TokenManagement(props: Props) { return <TokenEditor key={`${props.campaignId}/${props.sceneId}`} {...props} />; }
function TokenEditor({ api, campaignId, sceneId, width, height, revision, membersRevision, onChanged, onDenied }: Props) {
  const [tokens, setTokens] = useState<AuthoredTokens>(), [members, setMembers] = useState<Members>(), [selected, setSelected] = useState<string>(), [fields, setFields] = useState<Fields>(empty);
  const [ready, setReady] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const dirty = useRef(false), editingVersion = useRef<number | undefined>(undefined);
  const active = useRef(true), readId = useRef(0), editing = useRef<string | undefined>(undefined), pending = useRef<{ tokenId?: string; body: TokenCommand } | undefined>(undefined), afterAck = useRef<number | undefined>(undefined);
  function deny(e: unknown) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) {
      active.current = false; readId.current++; pending.current = undefined; afterAck.current = undefined; editing.current = undefined;
      setTokens(undefined); setMembers(undefined); setSelected(undefined); setFields(empty); setReady(false); setMessage(''); setError(''); onDenied(); return true;
    }
    return false;
  }
  async function observe<T>(promise: Promise<T>): Promise<T> { try { return await promise; } catch (e) { if (active.current) deny(e); throw e; } }
  async function load() {
    if (!active.current) return;
    const read = ++readId.current; setReady(false);
    try {
      const [t, m] = await Promise.all([observe(api.tokens(campaignId, sceneId)), observe(api.members(campaignId))]);
      if (!active.current || read !== readId.current) return;
      if (t.sceneId !== sceneId) throw new Error('场景列表不匹配，请刷新棋子与控制者。');
      setTokens(t); setMembers(m); setReady(true);
      const current = t.tokens.find(token => token.id === editing.current);
      if (editing.current) {
        if (current && !dirty.current) { setFields(fieldsFor(current)); editingVersion.current = current.version; }
        else if (current && current.version !== editingVersion.current) setMessage('当前棋子已变化；草稿已保留，保存时将检查原版本。');
        else if (!current) { editing.current = undefined; editingVersion.current = undefined; dirty.current = false; setSelected(undefined); setFields(empty); }
      }
      setFields(v => ({ ...v, controllers: v.controllers.filter(c => m.members.some(member => member.username === c)) }));
      if (afterAck.current !== undefined && read > afterAck.current) { setMessage('棋子请求已确认；当前列表已从服务器刷新。'); setError(''); }
      return { tokens: t, members: m };
    } catch (e) {
      if (active.current && read === readId.current) {
        setMessage(''); setError(afterAck.current !== undefined && read > afterAck.current ? '请求已确认；刷新失败，棋子列表尚未同步。请刷新棋子与控制者。' : e instanceof Error ? e.message : '读取棋子失败');
      }
      throw e;
    }
  }
  useEffect(() => { active.current = true; return () => { active.current = false; readId.current++; pending.current = undefined; afterAck.current = undefined; }; }, []);
  useEffect(() => { void load().catch(() => {}); }, [revision, membersRevision]);
  function choose(t?: AuthoredToken) { if (busy || pending.current) return; dirty.current = false; editingVersion.current = t?.version; editing.current = t?.id; setSelected(t?.id); setFields(t ? fieldsFor(t) : empty); setMessage(''); setError(''); }
  async function run() {
    if (!active.current || busy || !ready || !tokens || !members) return;
    setBusy(true); setError(''); setMessage(''); afterAck.current = undefined; let acknowledged = false;
    try {
      if (!pending.current) {
        const name = fields.name.trim(), x = Number(fields.x), y = Number(fields.y);
        if (!name || Array.from(name).length > 100 || !fields.x || !fields.y || !Number.isInteger(x) || !Number.isInteger(y) || x < 0 || y < 0 || x >= width || y >= height) throw new Error('请检查名称及网格内的整数坐标。');
        const token = tokens.tokens.find(t => t.id === selected);
        if (selected && !token) throw new Error('棋子已变化，请刷新后重新选择。');
        pending.current = { tokenId: selected, body: { requestId: crypto.randomUUID(), expectedVersion: token ? editingVersion.current! : tokens.sceneVersion, name, x, y, hidden: fields.hidden, controllers: [...fields.controllers].sort() } };
      }
      const command = pending.current;
      if (command.tokenId) await observe(api.updateToken(campaignId, sceneId, command.tokenId, command.body));
      else await observe(api.createToken(campaignId, sceneId, command.body));
      if (!active.current) return;
      acknowledged = true; pending.current = undefined; dirty.current = false; afterAck.current = readId.current;
      onChanged(); await load();
    } catch (e) {
      if (!active.current || deny(e) || acknowledged) return;
      if (e instanceof ApiError && e.status === 409) {
        pending.current = undefined; dirty.current = false;
        try { await load(); if (active.current) setError('操作冲突，已刷新棋子与控制者。请检查后重新操作。'); }
        catch { if (active.current) setError('操作冲突；刷新失败，请刷新棋子与控制者。'); }
      } else {
        if (e instanceof ApiError && e.status < 500) pending.current = undefined;
        setError(`${e instanceof Error ? e.message : '棋子操作失败'}${pending.current ? '；结果未确认，可重试同一棋子操作。' : ''}`);
      }
    } finally { if (active.current) setBusy(false); }
  }
  function edit(next: Partial<Fields>) { dirty.current = true; setFields(v => ({ ...v, ...next })); }
  const locked = busy || !!pending.current, canSubmit = ready && !!tokens && !!members;
  return <section className="campaign-management token-management" aria-label="棋子配置"><div className="management-title"><h2>棋子配置</h2><button className="quiet" disabled={busy} onClick={() => { void load().catch(() => {}); }}>刷新棋子与控制者</button></div><p className="hint">仅 DM 可编辑名称、位置、隐藏与共享控制。隐藏单位不会发送给玩家；DM 始终可以控制。</p>{error && <p role="alert" className="error">{error}</p>}{message && <p role="status" className="hint">{message}</p>}<ul className="authored-tokens">{tokens?.tokens.map(t => <li key={t.id}><span><strong>{t.name}</strong><small>({t.x}, {t.y}) · 版本 {t.version} · {t.hidden ? '隐藏' : '公开'} · 控制者：{t.controllers.join('、') || '无'}</small></span><button disabled={locked || !canSubmit} aria-label={`编辑棋子 ${t.id}`} onClick={() => choose(t)}>编辑</button></li>)}</ul><button type="button" disabled={locked} onClick={() => choose()}>新建棋子</button><form onSubmit={e => { e.preventDefault(); void run(); }}><label>棋子名称<input required value={fields.name} disabled={locked} onChange={e => edit({ name: e.target.value })} /></label><div className="coordinates"><label>棋子 X<input type="number" min="0" max={width - 1} step="1" required value={fields.x} disabled={locked} onChange={e => edit({ x: e.target.value })} /></label><label>棋子 Y<input type="number" min="0" max={height - 1} step="1" required value={fields.y} disabled={locked} onChange={e => edit({ y: e.target.value })} /></label></div><label className="token-checkbox"><input type="checkbox" checked={fields.hidden} disabled={locked} onChange={e => edit({ hidden: e.target.checked })} />棋子隐藏</label><fieldset disabled={locked || !canSubmit}><legend>共享控制者</legend>{members?.members.map(m => <label className="token-checkbox" key={m.username}><input type="checkbox" aria-label={`控制者 ${m.username}`} checked={fields.controllers.includes(m.username)} onChange={e => edit({ controllers: e.target.checked ? [...fields.controllers, m.username] : fields.controllers.filter(c => c !== m.username) })} />{m.username} · {m.role === 'DM' ? '主持人' : '玩家'}</label>)}</fieldset><button disabled={locked || !canSubmit}>{selected ? '保存棋子配置' : '创建棋子'}</button></form>{pending.current && <button disabled={busy || !canSubmit} onClick={() => void run()}>重试同一棋子操作</button>}</section>;
}
