import type { Move, Scene } from './types';
import { ApiError } from './api';
interface SceneApi { scene(campaignId: string): Promise<Scene>; move(campaignId: string, tokenId: string, move: Move): Promise<unknown> }
export interface SceneState { scene?: Scene; status: 'idle' | 'loading' | 'confirmed' | 'pending' | 'failed' | 'conflict'; message: string }
export class SceneStore {
  private state: SceneState = { status: 'idle', message: '' };
  private listeners = new Set<() => void>();
  private campaignId?: string;
  private epoch = 0;
  private refreshId = 0;
  private acceptedMoveAfterRefresh?: number;
  private command?: { tokenId: string; body: Move };
  constructor(private api: SceneApi) {}
  getSnapshot = (): SceneState => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private update(patch: Partial<SceneState>) { this.state = { ...this.state, ...patch }; this.listeners.forEach(listener => listener()); }
  reset() { this.epoch++; this.refreshId++; this.campaignId = undefined; this.command = undefined; this.acceptedMoveAfterRefresh = undefined; this.state = { status: 'idle', message: '' }; this.listeners.forEach(listener => listener()); }
  private denyAccess() {
    this.epoch++; this.refreshId++; this.command = undefined; this.acceptedMoveAfterRefresh = undefined;
    this.update({ scene: undefined, status: 'failed', message: '已失去战役访问权限或会话已失效，地图已清空。请刷新战役列表。' });
  }
  async open(campaignId: string) { this.reset(); this.campaignId = campaignId; this.update({ status: 'loading', message: '读取场景…' }); await this.refresh(); }
  async refresh(): Promise<boolean> {
    if (!this.campaignId) return false;
    const campaignId = this.campaignId, epoch = this.epoch, refreshId = ++this.refreshId;
    try {
      const scene = await this.api.scene(campaignId);
      if (epoch !== this.epoch || refreshId !== this.refreshId) return false;
      if (scene.campaignId !== campaignId || (this.state.scene?.id === scene.id && scene.version < this.state.scene.version)) return false;
      // A post-acknowledgment read may come from the move, realtime or the timer.
      // Reads started before the POST acknowledgment cannot confirm that move.
      const confirmsMove = this.acceptedMoveAfterRefresh !== undefined && refreshId > this.acceptedMoveAfterRefresh;
      if (confirmsMove) this.acceptedMoveAfterRefresh = undefined;
      this.update({ scene, ...(confirmsMove ? { status: 'confirmed' as const, message: '移动已保存，场景已同步' } : this.state.status === 'loading' || this.state.status === 'idle' ? { status: 'confirmed' as const, message: '已读取服务器状态' } : {}) });
      return true;
    } catch (error) {
      // Any denial in this campaign epoch invalidates all in-flight successes.
      if (epoch === this.epoch && error instanceof ApiError && (error.status === 401 || error.status === 403)) { this.denyAccess(); return false; }
      if (epoch === this.epoch && refreshId === this.refreshId) this.update(this.acceptedMoveAfterRefresh !== undefined ? { status: 'failed', message: '服务器已接受移动；场景刷新失败，请刷新查看最新位置' } : { message: `场景刷新失败；保留上次确认状态。${error instanceof Error ? error.message : ''}`, ...(this.state.scene ? {} : { status: 'failed' as const }) });
      return false;
    }
  }
  async move(tokenId: string, x: number, y: number) {
    const scene = this.state.scene, token = scene?.tokens.find(t => t.id === tokenId);
    if (!scene || !token?.canMove || this.command || this.state.status === 'pending' || !Number.isInteger(x) || !Number.isInteger(y) || x < 0 || y < 0 || x >= scene.width || y >= scene.height) return;
    this.acceptedMoveAfterRefresh = undefined;
    this.command = { tokenId, body: { requestId: crypto.randomUUID(), expectedVersion: token.version, x, y } };
    await this.send();
  }
  async retry() { if (this.command && this.state.status !== 'pending') await this.send(); }
  private async send() {
    const command = this.command, campaignId = this.campaignId, epoch = this.epoch;
    if (!command || !campaignId) return;
    this.update({ status: 'pending', message: '等待服务器确认；地图仍显示已确认位置…' });
    try {
      await this.api.move(campaignId, command.tokenId, command.body);
      if (epoch !== this.epoch) return;
      this.command = undefined;
      this.acceptedMoveAfterRefresh = this.refreshId;
      const moveRefreshId = this.refreshId + 1;
      const refreshed = await this.refresh();
      if (epoch !== this.epoch) return;
      // A superseding read owns the synchronization result; never overwrite it.
      if (!refreshed && moveRefreshId === this.refreshId && this.acceptedMoveAfterRefresh !== undefined) this.update({ status: 'failed', message: '服务器已接受移动；场景刷新失败，请刷新查看最新位置' });
    } catch (error) {
      if (epoch !== this.epoch) return;
      if (error instanceof ApiError && (error.status === 401 || error.status === 403)) { this.denyAccess(); return; }
      if (error instanceof ApiError && error.status === 409) {
        this.command = undefined; const refreshed = await this.refresh();
        if (epoch !== this.epoch) return;
        this.update({ status: 'conflict', message: refreshed ? '操作冲突，已读取最新位置，请重新选择目标' : '操作冲突；最新场景读取失败，请刷新后重试' });
      } else {
        if (error instanceof ApiError && error.status < 500) this.command = undefined;
        this.update({ status: 'failed', message: this.command ? '未收到移动确认，保留已确认位置；可安全重试同一请求' : `移动未保存：${error instanceof Error ? error.message : '请求失败'}` });
      }
    }
  }
  canRetry() { return !!this.command; }
}
