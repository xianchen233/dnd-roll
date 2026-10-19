import type { Asset, Assets, Campaign, CreatedInvitation, Csrf, Identity, Invitations, InvitePreview, MapConfiguration, Members, Move, Scene, SceneMap, Scenes, SceneReceipt, Token, VersionedCommand } from './types';
import type { AuthoredTokens, TokenCommand, TokenReceipt, MapView, Fog, FogCommand } from './types';
export class ApiError extends Error { constructor(public status: number, message: string) { super(message); } }
export class Api {
  async mapView(id: string): Promise<MapView> {
    const response = await this.request(`/api/campaigns/${encodeURIComponent(id)}/scene/map-view`, { credentials: 'include', cache: 'no-store' });
    await this.check(response); return response.json();
  }
  async mapImage(id: string, sceneId: string, version: number): Promise<Blob> {
    const response = await this.request(`/api/campaigns/${encodeURIComponent(id)}/scene/map-image?sceneId=${encodeURIComponent(sceneId)}&version=${version}`, { credentials: 'include', cache: 'no-store' });
    await this.check(response); return response.blob();
  }
  async fog(id: string, sceneId: string): Promise<Fog> {
    const response = await this.request(`/api/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/fog`, { credentials: 'include', cache: 'no-store' });
    await this.check(response); return response.json();
  }
  saveFog(id: string, sceneId: string, body: FogCommand): Promise<Fog> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/fog`, body); }
  private csrf?: Csrf;
  constructor(private request: typeof fetch = globalThis.fetch.bind(globalThis)) {}
  private async call<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
    const response = await this.request(`/api${path}`, { method, credentials: 'include', headers: { ...(body === undefined ? {} : { 'Content-Type': 'application/json' }), ...(method === 'POST' && this.csrf ? { [this.csrf.headerName]: this.csrf.token } : {}) }, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
    await this.check(response);
    return response.status === 204 ? undefined as T : response.json();
  }
  private async check(response: Response): Promise<void> {
    if (!response.ok) {
      let message = `请求失败 (${response.status})`;
      try { const data = await response.json(); if (typeof data.message === 'string') message = data.message; } catch { /* Proxies may return HTML. */ }
      throw new ApiError(response.status, message);
    }
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
  assets(id: string): Promise<Assets> { return this.call(`/campaigns/${encodeURIComponent(id)}/assets`); }
  async uploadAsset(id: string, command: VersionedCommand, file: File): Promise<{ asset: Asset; campaignVersion: number }> {
    if (!this.csrf) await this.getCsrf();
    const body = new FormData(); body.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' })); body.append('file', file);
    const response = await this.request(`/api/campaigns/${encodeURIComponent(id)}/assets`, { method: 'POST', credentials: 'include', headers: { [this.csrf!.headerName]: this.csrf!.token }, body });
    await this.check(response); return response.json();
  }
  async assetContent(id: string, assetId: string): Promise<Blob> {
    const response = await this.request(`/api/campaigns/${encodeURIComponent(id)}/assets/${encodeURIComponent(assetId)}/content`, { credentials: 'include', cache: 'no-store' });
    await this.check(response); return response.blob();
  }
  sceneMap(id: string, sceneId: string): Promise<SceneMap> { return this.call(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/map`); }
  saveSceneMap(id: string, sceneId: string, body: VersionedCommand & { map: MapConfiguration | null }): Promise<SceneMap> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/map`, body); }
  async move(campaignId: string, tokenId: string, move: Move): Promise<Token> {
    if (!this.csrf) await this.getCsrf();
    return this.call(`/campaigns/${encodeURIComponent(campaignId)}/tokens/${encodeURIComponent(tokenId)}/moves`, 'POST', move);
  }
  tokens(id: string, sceneId: string): Promise<AuthoredTokens> { return this.call(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/tokens`); }
  createToken(id: string, sceneId: string, body: TokenCommand): Promise<TokenReceipt> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/tokens`, body); }
  updateToken(id: string, sceneId: string, tokenId: string, body: TokenCommand): Promise<TokenReceipt> { return this.post(`/campaigns/${encodeURIComponent(id)}/scenes/${encodeURIComponent(sceneId)}/tokens/${encodeURIComponent(tokenId)}`, body); }
}
