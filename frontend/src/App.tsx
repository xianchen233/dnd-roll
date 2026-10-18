import React, { useCallback, useEffect, useState } from 'react';
import { Api, ApiError } from './api';
import type { Campaign, Identity } from './types';
import { LoginForm } from './LoginForm';
import { clearInviteFragment, InvitationPage, readInviteToken } from './InvitationPage';
import { Tabletop } from './Tabletop';
const defaultApi = new Api();
export function App({ api = defaultApi }: { api?: Api }) {
  const [identity, setIdentity] = useState<Identity>(), [campaigns, setCampaigns] = useState<Campaign[]>([]), [campaignId, setCampaignId] = useState('');
  const [error, setError] = useState(''), [busy, setBusy] = useState(false), [checking, setChecking] = useState(true), [invite, setInvite] = useState(readInviteToken);
  useEffect(() => {
    let disposed = false;
    void api.getCsrf().then(() => api.me()).then(user => { if (!disposed) setIdentity(user); }).catch(e => { if (!disposed && !(e instanceof ApiError && e.status === 401)) setError(e instanceof Error ? e.message : '会话检查失败'); }).finally(() => { if (!disposed) setChecking(false); });
    const onHash = () => setInvite(readInviteToken()); window.addEventListener('hashchange', onHash);
    return () => { disposed = true; window.removeEventListener('hashchange', onHash); };
  }, [api]);
  const refreshCampaigns = useCallback(async (preferred?: string) => {
    const list = await api.campaigns(); setCampaigns(list);
    setCampaignId(current => list.some(c => c.id === (preferred ?? current)) ? (preferred ?? current) : (list[0]?.id ?? ''));
  }, [api]);
  useEffect(() => {
    if (!identity || invite !== null) return;
    let active = true;
    void api.campaigns().then(list => { if (active) { setCampaigns(list); setCampaignId(current => list.some(c => c.id === current) ? current : (list[0]?.id ?? '')); } }).catch(e => { if (active) setError(e instanceof Error ? e.message : '读取战役列表失败'); });
    return () => { active = false; };
  }, [identity, invite, api]);
  async function joined(id: string) { await refreshCampaigns(id); clearInviteFragment(); setInvite(null); setError(''); }
  async function logout() {
    setBusy(true); setError('');
    try { await api.logout(); setIdentity(undefined); setCampaigns([]); setCampaignId(''); } catch { setError('退出失败，请重试'); } finally { setBusy(false); }
  }
  return <div className="app-shell"><header className="topbar"><a className="brand" href="/">D<span className="brand-die">20</span>ND <b>ROLL</b></a><span className="edition">2024 · 5R</span><div className="topbar-right">{identity ? <><strong>{identity.username}</strong><button className="quiet" disabled={busy} onClick={() => void logout()}>退出</button></> : <span className="muted">每一场冒险，都值得留下</span>}</div></header>
    {error && <p className="global-error" role="alert">{error}</p>}
    {checking ? <main className="session-check" role="status">检查会话…</main> : invite !== null ? <InvitationPage key={invite} api={api} token={invite} identity={identity} onIdentity={setIdentity} onJoined={joined} onCancel={() => { clearInviteFragment(); setInvite(null); }} /> : !identity ? <main className="login-layout"><div className="intro"><p className="eyebrow">YOUR NEXT CHAPTER</p><h1>故事在这里<br /><em>继续。</em></h1><p>与伙伴回到同一张桌面。<br />让每一步探索，都成为共同的冒险。</p><div className="intro-footer">DND ROLL <span>◇</span> 冒险桌面</div></div><LoginForm api={api} onLogin={setIdentity} /></main> : <Tabletop api={api} identity={identity} campaigns={campaigns} campaignId={campaignId} onCampaign={setCampaignId} onRefresh={refreshCampaigns} />}
  </div>;
}
