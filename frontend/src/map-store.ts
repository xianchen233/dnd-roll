import { ApiError } from './api';
import type { MapView } from './types';
interface MapApi { mapView(id: string): Promise<MapView>; mapImage(id: string, sceneId: string, version: number): Promise<Blob> }
interface Resources { create(blob: Blob): string; revoke(url: string): void }
export interface MapState { url?: string; view?: MapView; loading: boolean; message: string }
// Own object URLs here, textures in MapCanvas. No global Pixi asset cache is used.
export class MapStore {
  private state: MapState = { loading: false, message: '' };
  private listeners = new Set<() => void>();
  private generation = 0;
  private owner = 0;
  private denied = false;
  private campaignScope?: string;
  constructor(private api: MapApi, private onDenied: () => void, private resources: Resources = { create: b => URL.createObjectURL(b), revoke: u => URL.revokeObjectURL(u) }) {}
  getSnapshot = () => this.state;
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  private update(state: MapState) { const previous = this.state.url; this.state = state; this.listeners.forEach(f => f()); if (previous && previous !== state.url) this.resources.revoke(previous); }
  invalidate = () => { this.generation++; this.update({ loading: false, message: '' }); };
  reset = () => { this.owner++; this.denied = false; this.campaignScope = undefined; this.invalidate(); };
  async load(campaignId: string, sceneId: string, version: number, onConflict: () => void) {
    if (this.campaignScope !== campaignId) { this.reset(); this.campaignScope = campaignId; }
    if (this.denied) return;
    this.invalidate(); const generation = this.generation, owner = this.owner;
    this.update({ loading: true, message: '正在读取安全地图…' });
    try {
      const view = await this.api.mapView(campaignId);
      if (this.denied || generation !== this.generation) return;
      if (view.sceneId !== sceneId || view.sceneVersion !== version) { this.update({ loading: false, message: '地图版本已变化，正在刷新场景。' }); onConflict(); return; }
      if (!view.map) { this.update({ view, loading: false, message: '当前场景未配置地图。' }); return; }
      const blob = await this.api.mapImage(campaignId, sceneId, version);
      if (this.denied || generation !== this.generation) return;
      this.update({ url: this.resources.create(blob), view, loading: false, message: '安全地图已同步。' });
    } catch (e) {
      // Every authorization denial in this owner scope wins, even an older read.
      if (owner === this.owner && e instanceof ApiError && (e.status === 401 || e.status === 403)) { this.denied = true; this.invalidate(); this.onDenied(); return; }
      if (generation !== this.generation) return;
      this.update({ loading: false, message: e instanceof Error ? e.message : '安全地图读取失败' });
      if (e instanceof ApiError && e.status === 409) onConflict();
    }
  }
}
