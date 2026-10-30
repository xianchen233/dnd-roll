import React, { useEffect, useState } from 'react';
import type { Api } from './api';
import type { Identity, InvitePreview } from './types';
import { LoginForm } from './LoginForm';
export function readInviteToken() { return new URLSearchParams(location.hash.slice(1)).get('invite'); }
export function clearInviteFragment() { history.replaceState(null, '', location.pathname + location.search); }
export function InvitationPage({ api, token, identity, onIdentity, onJoined, onCancel }: { api: Api; token: string; identity?: Identity; onIdentity(user: Identity): void; onJoined(campaignId: string): Promise<void>; onCancel(): void }) {
  const [preview, setPreview] = useState<InvitePreview>(), [error, setError] = useState(''), [busy, setBusy] = useState(false), [username, setUsername] = useState(''), [password, setPassword] = useState('');
  const [reload, setReload] = useState(0), [registered, setRegistered] = useState(false);
  useEffect(() => { let active = true; setError(''); void api.previewInvitation(token).then(value => { if (active) setPreview(value); }).catch(e => { if (active) setError(`无法读取邀请：${e instanceof Error ? e.message : '请求失败'}`); }); return () => { active = false; }; }, [api, token, reload]);
  async function accept(user: Identity) {
    onIdentity(user); setError('');
    try { const result = await api.acceptInvitation(token); await onJoined(result.campaignId); }
    catch (e) { setError(e instanceof Error ? e.message : '接受邀请失败，请重试'); }
  }
  async function register(event: React.FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    let accountCreated = false;
    try {
      if (!/^[a-z][a-z0-9_-]{2,31}$/.test(username)) throw new Error('账号需为 3–32 位，以小写字母开头，仅含小写字母、数字、下划线或连字符。');
      if ([...password].length < 12 || new TextEncoder().encode(password).length > 72) throw new Error('密码至少 12 个字符，且不超过 72 个 UTF-8 字节。');
      const result = await api.registerInvitation(token, username, password); accountCreated = true; setRegistered(true);
      const user = await api.login(username, password); setPassword(''); onIdentity(user); await onJoined(result.campaignId);
    } catch (e) { setError(`${accountCreated ? '账号已创建；如无法继续，请使用下方固定账号登录恢复。' : ''}${e instanceof Error ? e.message : '注册失败，请重试或使用已有账号登录恢复'}`); }
    finally { setBusy(false); }
  }
  return <main className="invitation-page"><div className="invitation-heading"><p className="eyebrow">伙伴的邀请</p><h1>{preview ? `受邀加入「${preview.campaignName}」` : error ? '邀请信息暂不可用' : '读取邀请…'}</h1>{preview && <p className="muted">邀请有效期至 {new Date(preview.expiresAt).toLocaleString()}，以玩家身份加入。</p>}{error && <p role="alert" className="error">{error}</p>}<button className="quiet" onClick={onCancel}>返回桌面</button>{!preview && <button onClick={() => setReload(value => value + 1)}>重试读取邀请</button>}</div>{(identity ? <section className="login-card"><h2>以 {identity.username} 加入</h2><button className="primary" disabled={busy} onClick={() => { setBusy(true); setError(''); void accept(identity).catch(e => setError(e instanceof Error ? e.message : '接受邀请失败')).finally(() => setBusy(false)); }}>接受邀请</button><p className="hint">若注册已成功但登录失败，请退出后使用刚创建的固定账号登录恢复。</p></section> : <div className="invite-forms">{preview && <form className="login-card" onSubmit={event => void register(event)}><h2>创建固定账号</h2><p className="hint">此账号可在以后继续登录。注册仅限此邀请。</p><label>新账号<input value={username} autoComplete="username" required pattern="[a-z][a-z0-9_-]{2,31}" onChange={e => setUsername(e.target.value)} /></label><label>新密码<input value={password} type="password" autoComplete="new-password" required minLength={12} onChange={e => setPassword(e.target.value)} /></label><p className="hint">密码至少 12 个字符，不超过 72 个 UTF-8 字节；空格会原样保留。</p><button className="primary" disabled={busy || registered}>注册并加入战役</button>{registered && <p role="status">账号已创建。若登录失败，请使用右侧登录恢复。</p>}</form>}<LoginForm api={api} invitation onLogin={accept} /></div>)}</main>;
}
