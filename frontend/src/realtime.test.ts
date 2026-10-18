import { afterEach, describe, expect, it, vi } from 'vitest';
import { connectRealtime } from './realtime';
import type { Api } from './api';
const harness = vi.hoisted(() => ({ client: undefined as any }));
vi.mock('@stomp/stompjs', () => ({ Client: class {
  activate = vi.fn(); deactivate = vi.fn().mockResolvedValue(undefined); subscribe = vi.fn(); connectHeaders = {};
  constructor(public options: any) { harness.client = this; }
} }));
afterEach(() => vi.useRealTimers());
describe('realtime invalidations', () => {
  it('subscribes only to the personal queue, filters campaigns and refreshes on reconnect and timer', async () => {
    vi.useFakeTimers();
    const api = { getCsrf: vi.fn().mockResolvedValue({ token: 'current', headerName: 'X-CSRF-TOKEN' }) }, refresh = vi.fn(), state = vi.fn();
    const stop = connectRealtime(api as unknown as Api, 'c', refresh, state);
    const client = harness.client;
    await client.options.beforeConnect(); expect(client.connectHeaders).toEqual({ 'X-CSRF-TOKEN': 'current' });
    client.options.onConnect(); expect(refresh).toHaveBeenCalledTimes(1);
    expect(client.subscribe.mock.calls[0][0]).toBe('/user/queue/scene-changes');
    const onMessage = client.subscribe.mock.calls[0][1];
    onMessage({ body: JSON.stringify({ type: 'SCENE_CHANGED', campaignId: 'other' }) });
    onMessage({ body: 'invalid-json' }); expect(refresh).toHaveBeenCalledTimes(1);
    onMessage({ body: JSON.stringify({ type: 'SCENE_CHANGED', campaignId: 'c' }) }); expect(refresh).toHaveBeenCalledTimes(2);
    client.options.onWebSocketClose(); expect(state).toHaveBeenLastCalledWith('reconnecting');
    client.options.onConnect(); expect(refresh).toHaveBeenCalledTimes(3);
    vi.advanceTimersByTime(15000); expect(refresh).toHaveBeenCalledTimes(4);
    stop(); expect(client.deactivate).toHaveBeenCalledOnce(); vi.advanceTimersByTime(15000); expect(refresh).toHaveBeenCalledTimes(4);
  });
});
