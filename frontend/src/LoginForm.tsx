import React, { useState } from 'react';
import type { Api } from './api';
import type { Identity } from './types';
export function LoginForm({ api, checking = false, invitation = false, onLogin }: { api: Api; checking?: boolean; invitation?: boolean; onLogin(identity: Identity): Promise<void> | void }) {
  const [username, setUsername] = useState(''), [password, setPassword] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('');
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    try { const user = await api.login(username.trim(), password); setPassword(''); await onLogin(user); }
    catch (e) { setError(e instanceof Error ? e.message : '登录失败，请重试'); }
    finally { setBusy(false); }
  }
  return <form className="login-card" onSubmit={event => void submit(event)}><p className="eyebrow">欢迎回来</p><h2>{invitation ? '已有固定账号' : '进入你的冒险'}</h2><p className="muted">使用固定账号登录。新账号需通过主持人的邀请注册。</p><label>账号<input autoComplete="username" value={username} onChange={e => setUsername(e.target.value)} required /></label><label>密码<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></label>{error && <p className="error" role="alert">{error}</p>}<button className="primary" disabled={busy || checking}>{checking ? '检查会话…' : busy ? '正在登录…' : invitation ? '登录并接受邀请' : '进入冒险'}</button></form>;
}
