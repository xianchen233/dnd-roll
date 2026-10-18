import { describe, it, expect, vi } from 'vitest';
import { SceneStore } from './scene-store';
import { ApiError } from './api';
import type { Scene } from './types';
describe('membership loss', () => {
  it('invalidates every in-flight snapshot when an older refresh reports revoked membership', async () => {
    let rejectDenied!: (error: Error) => void, resolveStale!: (scene: Scene) => void;
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockImplementationOnce(() => new Promise<Scene>((_resolve, reject) => { rejectDenied = reject; })).mockImplementationOnce(() => new Promise<Scene>(resolve => { resolveStale = resolve; })), move: vi.fn() };
    const store = new SceneStore(api); await store.open('c');
    const denied = store.refresh(), stale = store.refresh();
    rejectDenied(new ApiError(403, '成员已移除')); await denied;
    resolveStale(snapshot(1, 6)); await stale;
    expect(store.getSnapshot().scene).toBeUndefined();
    expect(store.getSnapshot().message).toContain('已失去战役访问权限');
  });
  it.each([401, 403])('clears the scene and a retryable command on refresh %i', async status => {
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockRejectedValueOnce(new ApiError(status, '无权访问')), move: vi.fn().mockRejectedValue(new Error('network')) };
    const store = new SceneStore(api);
    await store.open('c'); await store.move('hero', 4, 4);
    expect(store.canRetry()).toBe(true);
    await store.refresh();
    expect(store.getSnapshot().scene).toBeUndefined();
    expect(store.canRetry()).toBe(false);
    await store.retry();
    expect(api.move).toHaveBeenCalledTimes(1);
  });
});
const snapshot = (version = 0, x = 2): Scene => ({ id: 'scene', campaignId: 'c', name: '营地', width: 24, height: 16, gridSize: 48, version, tokens: [{ id: 'hero', name: '游侠', x, y: 3, version, canMove: true }, { id: 'ally', name: '队友', x: 6, y: 3, version, canMove: false }] });
const deferred = <T,>() => { let resolve!: (v: T) => void; const promise = new Promise<T>(r => { resolve = r; }); return { resolve, promise }; };
describe('authoritative scene state', () => {
  it('keeps the move pending until the authoritative snapshot arrives', async () => {
    const confirmed = deferred<Scene>();
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockReturnValueOnce(confirmed.promise), move: vi.fn().mockResolvedValue({ x: 4 }) };
    const store = new SceneStore(api); await store.open('c');
    const moving = store.move('hero', 4, 5); await Promise.resolve();
    expect(store.getSnapshot()).toMatchObject({ status: 'pending' });
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(2);
    confirmed.resolve(snapshot(1, 4)); await moving;
    expect(store.getSnapshot().status).toBe('confirmed');
  });
  it('clears a terminal permission rejection and does not offer unsafe retries', async () => {
    const api = { scene: vi.fn().mockResolvedValue(snapshot()), move: vi.fn().mockRejectedValue(new ApiError(403, '权限已撤销')) };
    const store = new SceneStore(api); await store.open('c'); await store.move('hero', 4, 5);
    expect(store.canRetry()).toBe(false);
    expect(store.getSnapshot()).toMatchObject({ status: 'failed' });
    expect(store.getSnapshot().scene).toBeUndefined();
  });
  it('ignores an in-flight move result after switching campaigns', async () => {
    const move = deferred<unknown>();
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockResolvedValueOnce({ ...snapshot(0, 9), campaignId: 'other' }), move: vi.fn().mockReturnValue(move.promise) };
    const store = new SceneStore(api); await store.open('c'); const moving = store.move('hero', 4, 5);
    await store.open('other'); move.resolve({}); await moving;
    expect(store.getSnapshot().scene?.campaignId).toBe('other');
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(9);
  });
  it('retains confirmed position after network failure and retries the same request ID', async () => {
    const api = { scene: vi.fn().mockResolvedValue(snapshot()), move: vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce({}) };
    const store = new SceneStore(api);
    await store.open('c');
    await store.move('hero', 4, 5);
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(2);
    expect(store.getSnapshot().status).toBe('failed');
    api.scene.mockResolvedValue(snapshot(1, 4));
    await store.retry();
    expect(api.move.mock.calls[0][2]).toEqual(api.move.mock.calls[1][2]);
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(4);
    expect(store.getSnapshot().status).toBe('confirmed');
  });
  it('refreshes after 409 and reports conflict without optimistic success', async () => {
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockResolvedValueOnce(snapshot(1, 7)), move: vi.fn().mockRejectedValue(new ApiError(409, 'version')) };
    const store = new SceneStore(api); await store.open('c'); await store.move('hero', 4, 5);
    expect(store.getSnapshot()).toMatchObject({ status: 'conflict', scene: { version: 1 } });
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(7);
  });
  it('rejects unauthorized and out-of-bounds local moves without a POST', async () => {
    const api = { scene: vi.fn().mockResolvedValue(snapshot()), move: vi.fn() };
    const store = new SceneStore(api); await store.open('c');
    await store.move('ally', 3, 4); await store.move('hero', 24, 3); await store.move('hero', 2.5, 3);
    expect(api.move).not.toHaveBeenCalled();
  });
  it('ignores an old refresh that completes after a newer snapshot', async () => {
    const old = deferred<Scene>();
    const api = { scene: vi.fn().mockResolvedValueOnce(snapshot()).mockReturnValueOnce(old.promise).mockResolvedValueOnce(snapshot(2, 8)), move: vi.fn() };
    const store = new SceneStore(api); await store.open('c');
    const first = store.refresh(); await store.refresh(); old.resolve(snapshot(1, 5)); await first;
    expect(store.getSnapshot().scene?.tokens[0].x).toBe(8);
  });
  it('does not accept a previous campaign response after switching', async () => {
    const old = deferred<Scene>();
    const api = { scene: vi.fn().mockReturnValueOnce(old.promise).mockResolvedValueOnce({ ...snapshot(), campaignId: 'other' }), move: vi.fn() };
    const store = new SceneStore(api); const first = store.open('c'); await store.open('other'); old.resolve(snapshot()); await first;
    expect(store.getSnapshot().scene?.campaignId).toBe('other');
  });
});
