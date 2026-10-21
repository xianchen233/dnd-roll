import type { Asset, Assets, Campaign, CreatedInvitation, Csrf, Identity, Invitations, InvitePreview, MapConfiguration, Members, Move, Scene, SceneMap, Scenes, SceneReceipt, Token, VersionedCommand } from './types';
import type { AuthoredTokens, TokenCommand, TokenReceipt, MapView, Fog, FogCommand } from './types';
import type { CharacterList, CharacterDetail, CharacterAck, CharacterAccess, CharacterHistory, CharacterDocument, Sheet } from './characters';
import type { Catalog,ContentData,ContentFilters,ContentChoice,ContentPreview,ContentFill } from './content';
import type {DiceCommand,DiceTextCommand,Roll,RollList,Initiative,InitiativeGenerate,InitiativeConfirm,InitiativeHistory} from './dice';
export class ApiError extends Error { constructor(public status: number, message: string) { super(message); } }
export class Api {
  modulePackages(c:string):Promise<import('./modules').ModuleListing>{return this.call(`/campaigns/${encodeURIComponent(c)}/module-packages`);}
  async previewModule(c:string,file:File):Promise<import('./modules').ModulePreview>{return this.moduleUpload(c,file);}
  async importModule(c:string,command:import('./modules').ModuleCommand,file:File):Promise<{id:string;version:number}>{return this.moduleUpload(c,file,command);}
  private async moduleUpload<T>(c:string,file:File,command?:import('./modules').ModuleCommand):Promise<T>{
    if(!this.csrf)await this.getCsrf();const body=new FormData();if(command)body.append('command',new Blob([JSON.stringify(command)],{type:'application/json'}));body.append('file',file);
    const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/module-packages${command?'':'/preview'}`,{method:'POST',credentials:'include',headers:{[this.csrf!.headerName]:this.csrf!.token},body});await this.check(response);return response.json();
  }
  async moduleDownload(c:string,id:string):Promise<Blob>{const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/module-packages/${encodeURIComponent(id)}/content`,{credentials:'include',cache:'no-store'});await this.check(response);return response.blob();}
  backups():Promise<import('./backups').BackupStatus>{return this.call('/admin/backups');}
  createBackup(body:VersionedCommand,migration=false):Promise<{id:string;version:number}>{return this.post(`/admin/backups${migration?'/migration':''}`,body);}
  notes(c:string,filters:import('./notes').NoteFilters,before=''):Promise<import('./notes').NoteListing>{return this.call(`/campaigns/${encodeURIComponent(c)}/notes?${new URLSearchParams({...filters,...(before?{before}:{})})}`);}
  note(c:string,id:string):Promise<import('./notes').Note>{return this.call(`/campaigns/${encodeURIComponent(c)}/notes/${encodeURIComponent(id)}`);}
  createNote(c:string,body:import('./notes').NoteCreate):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/notes`,body);}
  editNote(c:string,id:string,body:import('./notes').NoteEdit):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/notes/${encodeURIComponent(id)}`,body);}
  noteHistory(c:string,id:string):Promise<import('./notes').NoteRevision[]>{return this.call(`/campaigns/${encodeURIComponent(c)}/notes/${encodeURIComponent(id)}/history`);}
  async uploadNoteAttachment(c:string,command:VersionedCommand&{visibility:import('./notes').NoteVisibility},file:File):Promise<Asset>{if(!this.csrf)await this.getCsrf();const body=new FormData();body.append('command',new Blob([JSON.stringify(command)],{type:'application/json'}));body.append('file',file);const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/note-attachments`,{method:'POST',credentials:'include',headers:{[this.csrf!.headerName]:this.csrf!.token},body});await this.check(response);return response.json();}
  async noteAttachmentContent(c:string,id:string):Promise<Blob>{const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/note-attachments/${encodeURIComponent(id)}/content`,{credentials:'include',cache:'no-store'});await this.check(response);return response.blob();}
  async exportNotes(c:string,filters:import('./notes').NoteFilters,format:'markdown'|'json'):Promise<Blob>{const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/notes/export?${new URLSearchParams({...filters,format})}`,{credentials:'include',cache:'no-store'});await this.check(response);return response.blob();}
  timeline(c:string,filters:import('./timeline').TimelineFilters,before=''):Promise<import('./timeline').TimelineListing>{return this.call(`/campaigns/${encodeURIComponent(c)}/timeline?${new URLSearchParams({...filters,...(before?{before}:{})})}`);}
  timelineEntry(c:string,id:string):Promise<import('./timeline').TimelineEntry>{return this.call(`/campaigns/${encodeURIComponent(c)}/timeline/${encodeURIComponent(id)}`);}
  sendMessage(c:string,body:import('./timeline').TimelineSend):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/messages`,body);}
  editMessage(c:string,id:string,body:import('./timeline').TimelineEdit):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/messages/${encodeURIComponent(id)}`,body);}
  messageHistory(c:string,id:string):Promise<import('./timeline').TimelineRevision[]>{return this.call(`/campaigns/${encodeURIComponent(c)}/messages/${encodeURIComponent(id)}/history`);}
  timelineSessions(c:string):Promise<import('./timeline').TimelineSessions>{return this.call(`/campaigns/${encodeURIComponent(c)}/timeline/sessions`);}
  timelineSession(c:string,body:VersionedCommand&{kind:string;name:string}):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/timeline/sessions`,body);}
  async uploadAttachment(c:string,command:VersionedCommand,file:File):Promise<Asset>{
    if(!this.csrf)await this.getCsrf();const body=new FormData();body.append('command',new Blob([JSON.stringify(command)],{type:'application/json'}));body.append('file',file);
    const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/attachments`,{method:'POST',credentials:'include',headers:{[this.csrf!.headerName]:this.csrf!.token},body});await this.check(response);return response.json();
  }
  async attachmentContent(c:string,id:string):Promise<Blob>{const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/attachments/${encodeURIComponent(id)}/content`,{credentials:'include',cache:'no-store'});await this.check(response);return response.blob();}
  async exportTimeline(c:string,filters:import('./timeline').TimelineFilters,format:'markdown'|'json'):Promise<Blob>{const response=await this.request(`/api/campaigns/${encodeURIComponent(c)}/timeline/export?${new URLSearchParams({...filters,format})}`,{credentials:'include',cache:'no-store'});await this.check(response);return response.blob();}
  actions(c:string,before=''):Promise<{actions:import('./combat').CombatAction[];next:string}>{return this.call(`/campaigns/${encodeURIComponent(c)}/actions${before?`?before=${encodeURIComponent(before)}`:''}`);}
  action(c:string,id:string):Promise<import('./combat').CombatAction>{return this.call(`/campaigns/${encodeURIComponent(c)}/actions/${encodeURIComponent(id)}`);}
  declareAction(c:string,body:import('./combat').CombatDeclaration):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/actions`,body);}
  actionEvent(c:string,id:string,body:import('./combat').CombatCommand):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/actions/${encodeURIComponent(id)}/events`,body);}
  combat(c:string):Promise<import('./combat').CombatState>{return this.call(`/campaigns/${encodeURIComponent(c)}/combat`);}
  combatCommand(c:string,body:import('./combat').TurnCommand):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(c)}/combat`,body);}
  combatHistory(c:string):Promise<import('./combat').TurnHistory[]>{return this.call(`/campaigns/${encodeURIComponent(c)}/combat/history`);}
  rolls(id:string,before=''):Promise<RollList>{return this.call(`/campaigns/${encodeURIComponent(id)}/rolls${before?`?before=${encodeURIComponent(before)}`:''}`);}
  roll(id:string,body:DiceCommand):Promise<Roll>{return this.post(`/campaigns/${encodeURIComponent(id)}/rolls`,body);}
  rollCommand(id:string,body:DiceTextCommand):Promise<Roll>{return this.post(`/campaigns/${encodeURIComponent(id)}/roll-commands`,body);}
  initiative(id:string):Promise<Initiative>{return this.call(`/campaigns/${encodeURIComponent(id)}/initiative`);}
  generateInitiative(id:string,body:InitiativeGenerate):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(id)}/initiative`,body);}
  confirmInitiative(id:string,body:InitiativeConfirm):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(id)}/initiative/confirm`,body);}
  initiativeHistory(id:string):Promise<InitiativeHistory[]>{return this.call(`/campaigns/${encodeURIComponent(id)}/initiative/history`);}
  content(id:string,filters:ContentFilters):Promise<Catalog>{return this.call(`/campaigns/${encodeURIComponent(id)}/content?${new URLSearchParams(filters)}`);}
  publishContent(id:string,body:VersionedCommand & {entry:ContentData}):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(id)}/content/custom`,body);}
  contentSettings(id:string,body:VersionedCommand & {mode:string;approvedIds:string[]}):Promise<CharacterAck>{return this.post(`/campaigns/${encodeURIComponent(id)}/content/settings`,body);}
  previewContent(id:string,characterId:string,body:ContentChoice):Promise<ContentPreview>{return this.post(`${this.characterPath(id,characterId)}/content-preview`,body);}
  fillContent(id:string,characterId:string,body:ContentFill):Promise<CharacterAck>{return this.post(`${this.characterPath(id,characterId)}/content`,body);}
  characters(id: string): Promise<CharacterList> { return this.call(`/campaigns/${encodeURIComponent(id)}/characters`); }
  character(id: string, characterId: string): Promise<CharacterDetail> { return this.call(this.characterPath(id, characterId)); }
  createCharacter(id: string, body: VersionedCommand & { sheet: Sheet }): Promise<CharacterAck> { return this.post(`/campaigns/${encodeURIComponent(id)}/characters`, body); }
  updateCharacter(id: string, characterId: string, body: VersionedCommand & { sheet: Sheet; reason: string }): Promise<CharacterAck> { return this.post(this.characterPath(id, characterId), body); }
  characterHistory(id: string, characterId: string): Promise<CharacterHistory[]> { return this.call(`${this.characterPath(id, characterId)}/history`); }
  characterAccess(id: string, characterId: string): Promise<CharacterAccess> { return this.call(`${this.characterPath(id, characterId)}/access`); }
  saveCharacterAccess(id: string, characterId: string, body: VersionedCommand & { controllers: string[]; hidden: boolean; shareFull: boolean; approveImport: boolean; reason: string }): Promise<CharacterAck> { return this.post(`${this.characterPath(id, characterId)}/access`, body); }
  characterResource(id: string, characterId: string, body: VersionedCommand & { resourceId: string; delta: number; reason: string }): Promise<CharacterAck> { return this.post(`${this.characterPath(id, characterId)}/resources`, body); }
  characterCondition(id: string, characterId: string, body: VersionedCommand & { action: string; conditionId: string | null; reason: string }): Promise<CharacterAck> { return this.post(`${this.characterPath(id, characterId)}/conditions`, body); }
  copyCharacter(id: string, characterId: string, body: VersionedCommand & { name: string }): Promise<CharacterAck> { return this.post(`${this.characterPath(id, characterId)}/copy`, body); }
  exportCharacter(id: string, characterId: string): Promise<CharacterDocument> { return this.call(`${this.characterPath(id, characterId)}/export`); }
  importCharacter(id: string, body: VersionedCommand & { document: CharacterDocument }): Promise<CharacterAck> { return this.post(`/campaigns/${encodeURIComponent(id)}/characters/import`, body); }
  characterPlacements(id: string, characterId: string): Promise<{ tokenId: string; sceneId: string; tokenVersion: number }[]> { return this.call(`${this.characterPath(id, characterId)}/placements`); }
  linkCharacter(id: string, characterId: string, body: VersionedCommand & { tokenId: string; tokenVersion: number; reason: string }): Promise<CharacterAck> { return this.post(`${this.characterPath(id, characterId)}/placements`, body); }
  private characterPath(id: string, characterId: string) { return `/campaigns/${encodeURIComponent(id)}/characters/${encodeURIComponent(characterId)}`; }
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
