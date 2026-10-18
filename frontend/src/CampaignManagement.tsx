import React, { useEffect, useRef, useState } from 'react';
import { ApiError, type Api } from './api';
import type { Invitations, Members, VersionedCommand } from './types';
type Command = { kind: 'create' | 'revoke' | 'remove'; target?: string; body: VersionedCommand };
export function CampaignManagement({ api, campaignId, revision = 0, onChanged, onDenied }: { api: Api; campaignId: string; revision?: number; onChanged?(): void; onDenied?(): void }) {
  const [members, setMembers] = useState<Members>(), [invitations, setInvitations] = useState<Invitations>(), [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState(''), [link, setLink] = useState('');
  const pending = useRef<Command | undefined>(undefined), active = useRef(true);
  function handleError(e: unknown) {
    if (e instanceof ApiError && (e.status === 401 || e.status === 403)) { active.current = false; pending.current = undefined; setMembers(undefined); setInvitations(undefined); setLink(''); onDenied?.(); return true; }
    return false;
  }
  async function observeAuthorization<T>(request: Promise<T>): Promise<T> {
    try { return await request; }
    catch (e) {
      // Each read must observe denial even if its peer already rejected the
      // aggregate with a transport error. Do not wait for the peer to settle.
      if (active.current) handleError(e);
      throw e;
    }
  }
  async function load() {
    try {
      const [m, i] = await Promise.all([observeAuthorization(api.members(campaignId)), observeAuthorization(api.invitations(campaignId))]);
      if (!active.current) return;
      setMembers(m); setInvitations(i);
      if (m.campaignVersion !== i.campaignVersion) throw new Error('管理列表版本已变化，请刷新成员与邀请后重试。');
    } catch (e) { if (active.current) handleError(e); throw e; }
  }
  useEffect(() => { active.current = true; void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '读取管理列表失败'); }); return () => { active.current = false; }; }, [api, campaignId]);
  useEffect(() => { if (revision) void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '读取管理列表失败'); }); }, [revision]);
  async function run(kind: Command['kind'], target?: string) {
    if (!members || members.campaignVersion !== invitations?.campaignVersion) return;
    setBusy(true); setError(''); setMessage(''); setLink('');
    pending.current ??= { kind, target, body: { requestId: crypto.randomUUID(), expectedVersion: members.campaignVersion } };
    const cmd = pending.current;
    try {
      if (cmd.kind === 'create') {
        const result = await api.createInvitation(campaignId, cmd.body);
        if (!active.current) return;
        if (result.token !== null) { setLink(`${location.origin}/#invite=${encodeURIComponent(result.token)}`); setMessage('请立即复制并私下分享；链接仅在本次生成时显示。'); }
        else setMessage('邀请链接仅首次返回，本次重试不会再次显示。若链接丢失，请撤销该邀请后重新生成。');
      } else if (cmd.kind === 'revoke') await api.revokeInvitation(campaignId, cmd.target!, cmd.body);
      else await api.removeMember(campaignId, cmd.target!, cmd.body);
      if (!active.current) return;
      pending.current = undefined;
      onChanged?.();
      await load();
    } catch (e) {
      if (!active.current || handleError(e)) return;
      if (e instanceof ApiError && e.status === 409) {
        pending.current = undefined;
        try { await load(); setError('操作冲突，已刷新成员与邀请。请检查最新状态后重试。'); } catch { setError('操作冲突；刷新管理列表失败，请手动刷新后重试。'); }
      } else { if (e instanceof ApiError && e.status < 500) pending.current = undefined; setError(`${e instanceof Error ? e.message : '管理操作失败'}${pending.current ? '；可安全重试同一操作。' : ''}`); }
    } finally { if (active.current) setBusy(false); }
  }
  const disabled = busy || !members || members.campaignVersion !== invitations?.campaignVersion;
  const statuses = { ACTIVE: '有效', EXPIRED: '已过期', REVOKED: '已撤销', USED: '已使用' };
  return <section className="campaign-management" aria-label="战役管理"><div className="management-title"><h2>成员与邀请</h2><button className="quiet" disabled={busy} onClick={() => { setError(''); void load().catch(e => { if (active.current) setError(e instanceof Error ? e.message : '刷新失败'); }); }}>刷新成员与邀请</button></div>{error && <p className="error" role="alert">{error}</p>}{message && <p role="status" className="hint">{message}</p>}{pending.current ? <button disabled={busy} onClick={() => void run(pending.current!.kind, pending.current!.target)}>重试同一管理操作</button> : members && invitations && <button disabled={disabled} onClick={() => void run('create')}>生成邀请链接</button>}{link && <div className="invite-link"><label>一次性邀请链接<input readOnly value={link} onFocus={e => e.target.select()} /></label><button onClick={() => { void navigator.clipboard.writeText(link).then(() => setMessage('邀请链接已复制。')).catch(() => setError('复制失败，请选中链接手动复制。')); }}>复制邀请链接</button></div>}<h3>战役成员</h3><ul>{members?.members.map(m => <li key={m.username}><span>{m.username} · {m.role === 'DM' ? '主持人' : '玩家'}</span>{m.role === 'PLAYER' && <button disabled={disabled || !!pending.current} aria-label={`移除 ${m.username}`} onClick={() => void run('remove', m.username)}>移除</button>}</li>)}</ul><h3>邀请记录</h3><ul>{invitations?.invitations.map(i => <li key={i.id}><span>{statuses[i.status]} · {new Date(i.expiresAt).toLocaleString()}{i.usedBy && ` · ${i.usedBy}`}</span>{i.status === 'ACTIVE' && <button aria-label={`撤销邀请 ${i.id}`} disabled={disabled || !!pending.current} onClick={() => void run('revoke', i.id)}>撤销邀请</button>}</li>)}</ul><p className="hint">邀请 7 天后过期。撤销邀请不移除已加入的成员；请单独移除玩家。地图备团和棋子配置仅对主持人开放。</p></section>;
}
