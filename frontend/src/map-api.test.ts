import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';
describe('safe map HTTP client', () => {
  it('fetches only the current safe view with credentials and without browser caches', async () => {
    const request = vi.fn().mockResolvedValue(Response.json({ sceneId: 's', sceneVersion: 9, map: { distancePerCell: 5 } }));
    expect(await new Api(request).mapView('c/x')).toEqual({ sceneId: 's', sceneVersion: 9, map: { distancePerCell: 5 } });
    expect(request).toHaveBeenCalledWith('/api/campaigns/c%2Fx/scene/map-view', expect.objectContaining({ credentials: 'include', cache: 'no-store' }));
  });
  it('fetches a fresh authorized safe PNG bound to exact scene/version, never the original asset', async () => {
    const request = vi.fn().mockResolvedValue(new Response('safe', { headers: { 'Content-Type': 'image/png' } }));
    expect((await new Api(request).mapImage('c/x', 's/x', 9)).type).toBe('image/png');
    expect(request).toHaveBeenCalledWith('/api/campaigns/c%2Fx/scene/map-image?sceneId=s%2Fx&version=9', expect.objectContaining({ credentials: 'include', cache: 'no-store' }));
  });
  it('observes an image authorization denial rather than creating a blob', async () => {
    const request = vi.fn().mockResolvedValue(Response.json({ message: 'denied' }, { status: 403 }));
    await expect(new Api(request).mapImage('c', 's', 9)).rejects.toEqual(new ApiError(403, 'denied'));
  });
});
