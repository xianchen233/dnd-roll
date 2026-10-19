import { describe, expect, it, vi } from 'vitest';
import { ApiError } from './api';
import { MapStore } from './map-store';
const view = { sceneId: 's', sceneVersion: 8, map: { distancePerCell: 5 } };
function deferred<T>() { let resolve!: (v: T) => void, reject!: (e: unknown) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function setup() {
  const api = { mapView: vi.fn().mockResolvedValue(view), mapImage: vi.fn().mockResolvedValue(new Blob(['safe'])) }, denied = vi.fn(), resources = { create: vi.fn().mockReturnValue('blob:safe'), revoke: vi.fn() }, conflict = vi.fn();
  return { api, resources, denied, conflict, store: new MapStore(api, denied, resources) };
}
describe('safe map lifecycle', () => {
  it('removes and revokes public pixels synchronously before requesting a covered replacement', async () => {
    const { api, store, resources, conflict } = setup(); await store.load('c', 's', 8, conflict);
    expect(store.getSnapshot().url).toBe('blob:safe'); const next = deferred<typeof view>(); api.mapView.mockReturnValueOnce(next.promise);
    const loading = store.load('c', 's', 9, conflict); expect(store.getSnapshot().url).toBeUndefined(); expect(resources.revoke).toHaveBeenCalledWith('blob:safe');
    next.resolve({ ...view, sceneVersion: 9 }); await loading; expect(api.mapImage).toHaveBeenLastCalledWith('c', 's', 9);
  });
  it('cannot restore a late image after scene switch or unmount and revokes the live URL on reset', async () => {
    const { api, store, resources, conflict } = setup(), image = deferred<Blob>(); api.mapImage.mockReturnValueOnce(image.promise);
    const old = store.load('c', 's', 8, conflict); await Promise.resolve(); store.reset(); image.resolve(new Blob(['old'])); await old;
    expect(resources.create).not.toHaveBeenCalled(); await store.load('other', 's', 8, conflict); store.reset(); expect(resources.revoke).toHaveBeenCalledWith('blob:safe');
  });
  it.each([401, 403])('a concurrent old image %s denial disposes a newer successful image and all pending state', async status => {
    const { api, store, resources, denied, conflict } = setup(), old = deferred<Blob>(); api.mapImage.mockReturnValueOnce(old.promise);
    const first = store.load('c', 's', 8, conflict); await Promise.resolve(); await store.load('c', 's', 8, conflict);
    expect(store.getSnapshot().url).toBe('blob:safe'); old.reject(new ApiError(status, 'denied')); await first;
    expect(denied).toHaveBeenCalledOnce(); expect(store.getSnapshot().url).toBeUndefined(); expect(resources.revoke).toHaveBeenCalledOnce();
    await store.load('c', 's', 8, conflict); expect(resources.create).toHaveBeenCalledOnce();
  });
  it('does not let a previous campaign denial invalidate the new campaign', async () => {
    const { api, store, denied, conflict } = setup(), old = deferred<Blob>(); api.mapImage.mockReturnValueOnce(old.promise);
    const first = store.load('c', 's', 8, conflict); await Promise.resolve(); store.reset(); await store.load('other', 's', 8, conflict);
    old.reject(new ApiError(403, 'old denied')); await first; expect(denied).not.toHaveBeenCalled(); expect(store.getSnapshot().url).toBe('blob:safe');
  });
  it('an old scene image denial still revokes a newer scene in the same campaign', async () => {
    const { api, store, denied, resources, conflict } = setup(), old = deferred<Blob>(); api.mapImage.mockReturnValueOnce(old.promise);
    const first = store.load('c', 's', 8, conflict); await Promise.resolve(); api.mapView.mockResolvedValue({ ...view, sceneId: 'new' });
    await store.load('c', 'new', 8, conflict); expect(store.getSnapshot().url).toBe('blob:safe');
    old.reject(new ApiError(403, 'membership revoked')); await first;
    expect(denied).toHaveBeenCalledOnce(); expect(store.getSnapshot().url).toBeUndefined(); expect(resources.revoke).toHaveBeenCalledWith('blob:safe');
  });
  it('requests a fresh authoritative scene on mismatched map-view and stale image, displaying neither', async () => {
    const { api, store, conflict } = setup(); api.mapView.mockResolvedValueOnce({ ...view, sceneVersion: 9 }); await store.load('c', 's', 8, conflict);
    expect(api.mapImage).not.toHaveBeenCalled(); expect(conflict).toHaveBeenCalledOnce(); api.mapImage.mockRejectedValueOnce(new ApiError(409, 'stale'));
    await store.load('c', 's', 8, conflict); expect(conflict).toHaveBeenCalledTimes(2); expect(store.getSnapshot().url).toBeUndefined();
  });
});
