import React, { useRef, useState } from 'react';
import { ApiError, type Api } from './api';
export function CampaignCreate({ api, onCreated }: { api: Api; onCreated(id: string): Promise<void> }) {
  const [name, setName] = useState(''), [busy, setBusy] = useState(false), [error, setError] = useState('');
  const command = useRef<{ requestId: string; name: string } | undefined>(undefined);
  async function submit(event: React.FormEvent) {
    event.preventDefault(); setBusy(true); setError('');
    command.current ??= { requestId: crypto.randomUUID(), name: name.trim() };
    try { const created = await api.createCampaign(command.current); command.current = undefined; setName(''); await onCreated(created.id); }
    catch (e) { if (e instanceof ApiError && e.status < 500) command.current = undefined; setError(e instanceof Error ? e.message : '创建战役失败'); }
    finally { setBusy(false); }
  }
  return <form className="campaign-create" onSubmit={event => void submit(event)}><label>战役名称<input required maxLength={100} value={name} disabled={busy || !!command.current} onChange={e => setName(e.target.value)} /></label><button disabled={busy || (!command.current && !name.trim())}>{command.current ? '重试创建战役' : '创建战役'}</button>{error && <p role="alert" className="error">{error}{command.current ? '；可重试同一请求。' : ''}</p>}</form>;
}
