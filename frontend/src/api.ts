import type { Campaign, CreatedInvitation, Csrf, Identity, Invitations, InvitePreview, Members, Move, Scene, Scenes, SceneReceipt, Token, VersionedCommand } from './types';
export class ApiError extends Error { constructor(public status: number, message: string) { super(message); } }
export class Api {
  private csrf?: Csrf;
  constructor(private request: typeof fetch = globalThis.fetch.bind(globalThis)) {}
  private async call<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
    const response = await this.request(`/api${path}`, { method, credentials: 'include', headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(method === 'POST' && this.csrf ? { [this.csrf.headerName]: this.csrf.token } : {}) }, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
    if (!response.ok) {
      let message = `请求失败 (${response.status})`;
      try { const data = await response.json(); if (typeof data.message === 'string') message = data.message; } catch { /* Proxies may return HTML. */ }
      throw new ApiError(response.status, message);
    }
    return response.status === 204 ? undefined as T : response.json();
  }
  async getCsrf(): Promise<Csrf> { this.csrf = await this.call<Csrf>('/auth/csrf'); return this.csrf; }
  async login(username: string, password: string): Promise<Identity> {
    await this.getCsrf();
    const identity = await this.call<Identity>('/auth/login', 'POST', { username, password });
    this.csrf = undefined;
    await this.getCsrf();
    return identity;
  }
  me(): Promise<Identity> { return this.call('/auth/me'); }
  async logout(): Promise<void> { await this.getCsrf(); await this.call('/auth/logout', 'POST'); this.csrf = undefined; await this.getCsrf(); }
  campaigns(): Promise<Campaign[]> { return this.call('/campaigns'); }
  private async post<T>(path: string, body: unknown): Promise<T> {
    if (!this.csrf) await this.getCsrf();
    return this.call(path, 'POST', body);
  }
  createCampaign(body: { requestId: string; name: string }): Promise<Campaign> { return this.post('/campaigns', body); }
  members(id: string): Promise<Members> { return this.call(`/campaigns/${encodeURIComponent(id)}/members`); }
  invitations(id: string): Promise<Invitations> { return this.call(`/campaigns/${encodeURIComponent(id)}/invitations`); }
  createInvitation(id: string, body: VersionedCommand): Promise<CreatedInvitation> { return this.post(`/campaigns/${encodeURIComponent(id)}/invitations`, body); }
  revokeInvitation(id: string, invitationId: string, body: VersionedCommand): Promise<void> { return this.post(`/campaigns/${encodeURIComponent(id)}/invitations/${encodeURIComponent(invitationId)}/revoke`, body); }
  removeMember(id: string, username: string, body: VersionedCommand): Promise<void> { return this.post(`/campaigns/${encodeURIComponent(id)}/members/${encodeURIComponent(username)}/remove`, body); }
  previewInvitation(token: string): Promise<InvitePreview> { return this.post('/invitations/preview', { token }); }
  acceptInvitation(token: string): Promise<{ campaignId: string }> { return this.post('/invitations/accept', { token }); }
  registerInvitation(token: string, username: string, password: string): Promise<{ username: string; campaignId: string }> { return this.post('/invitations/register', { token, username, password }); }
  scene(campaignId: string): Promise<Scene> { return this.call(`/campaigns/${encodeURIComponent(campaignId)}/scene`); }
  scenes(id: string): Promise<Scenes> { return this.call(`/campaigns/${encodeURIComponent(id)}/scenes`); }
  createScene(id: string, body: VersionedCommand & { name: string }): Promise<SceneReceipt> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes`, body); }
  activateScene(id: string, sceneId: string, body: VersionedCommand): Promise<SceneReceipt> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/activate`, body); }
  async move(campaignId: string, tokenId: string, move: Move): Promise<Token> {
    if (!this.csrf) await this.getCsrf();
    return this.call(`/campaigns/${encodeURIComponent(campaignId)}/tokens/${encodeURIComponent(tokenId)}/moves`, 'POST', move);
  }
}
