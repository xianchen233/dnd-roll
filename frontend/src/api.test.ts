import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';

describe('session HTTP client', () => {
  it('uses multipart JSON command Blob, authenticated binary reads and all five asset operations', async () => {
    const map = { assetId: 'a', gridSize: 64, offsetX: 0, offsetY: 0, distancePerCell: 5 };
    const request = vi.fn().mockResolvedValueOnce(Response.json({ campaignVersion: 2, assets: [] }))
      .mockResolvedValueOnce(Response.json({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ asset: { id: 'a' }, campaignVersion: 3 }))
      .mockResolvedValueOnce(new Response('png', { headers: { 'Content-Type': 'image/png' } }))
      .mockResolvedValueOnce(Response.json({ sceneId: 's', sceneVersion: 0, map: null }))
      .mockResolvedValueOnce(Response.json({ sceneId: 's', sceneVersion: 1, map }));
    const api = new Api(request), body = { requestId: 'r', expectedVersion: 2 }, file = new File(['png'], 'private.png', { type: 'image/png' });
    await api.assets('c/id'); await api.uploadAsset('c/id', body, file);
    expect((await api.assetContent('c/id', 'a/id')).type).toBe('image/png');
    await api.sceneMap('c/id', 's/id'); await api.saveSceneMap('c/id', 's/id', { ...body, map });
    expect(request.mock.calls.map(c => c[0])).toEqual(['/api/campaigns/c%2Fid/assets', '/api/auth/csrf', '/api/campaigns/c%2Fid/assets', '/api/campaigns/c%2Fid/assets/a%2Fid/content', '/api/campaigns/c%2Fid/scenes/s%2Fid/map', '/api/campaigns/c%2Fid/scenes/s%2Fid/map']);
    const init = request.mock.calls[2][1]; expect(init.credentials).toBe('include'); expect(init.headers).toEqual({ 'X-CSRF-TOKEN': 'csrf' }); expect(init.body).toBeInstanceOf(FormData);
    expect([...init.body.keys()]).toEqual(['command', 'file']); expect(init.body.get('command').type).toBe('application/json');
    const commandText = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => resolve(String(reader.result)); reader.onerror = reject; reader.readAsText(init.body.get('command')); });
    expect(JSON.parse(commandText)).toEqual(body);
    expect(init.body.get('file').name).toBe('private.png');
    expect(request.mock.calls[3][1]).toMatchObject({ credentials: 'include' }); expect(JSON.parse(request.mock.calls[5][1].body)).toEqual({ ...body, map });
  });
  it.each([403, 503])('keeps binary content failure status %i without producing a blob', async status => {
    const api = new Api(vi.fn().mockResolvedValue(new Response('proxy', { status })));
    await expect(api.assetContent('c', 'a')).rejects.toMatchObject({ status });
  });
  it('encodes scene identifiers and sends session CSRF with versioned scene commands', async () => {
    const request = vi.fn().mockResolvedValueOnce(Response.json({ campaignVersion: 2, currentSceneId: 's', scenes: [] }))
      .mockResolvedValueOnce(Response.json({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ id: 'next', name: '密室', campaignVersion: 3 }))
      .mockResolvedValueOnce(Response.json({ id: 'next', name: '密室', campaignVersion: 4 }));
    const api = new Api(request);
    await api.scenes('c/id');
    await api.createScene('c/id', { requestId: 'r1', expectedVersion: 2, name: '密室' });
    await api.activateScene('c/id', 'next/id', { requestId: 'r2', expectedVersion: 3 });
    expect(request.mock.calls.map(call => call[0])).toEqual(['/api/campaigns/c%2Fid/scenes', '/api/auth/csrf', '/api/campaigns/c%2Fid/scenes', '/api/campaigns/c%2Fid/scenes/next%2Fid/activate']);
    expect(request.mock.calls[2][1]).toMatchObject({ credentials: 'include', headers: { 'X-CSRF-TOKEN': 'csrf' }, body: JSON.stringify({ requestId: 'r1', expectedVersion: 2, name: '密室' }) });
  });
  it('uses POST body secrets and CSRF for anonymous invite preview and registration', async () => {
    const request = vi.fn().mockResolvedValueOnce(Response.json({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ campaignId: 'c', campaignName: '营地', expiresAt: '2026-10-25T00:00:00Z' }))
      .mockResolvedValueOnce(Response.json({ username: 'new_user', campaignId: 'c' }, { status: 201 }));
    const api = new Api(request);
    await api.previewInvitation('secret');
    await api.registerInvitation('secret', 'new_user', ' password kept ');
    expect(request.mock.calls.map(call => call[0])).toEqual(['/api/auth/csrf', '/api/invitations/preview', '/api/invitations/register']);
    expect(request.mock.calls[1][1]).toMatchObject({ credentials: 'include', method: 'POST', headers: { 'X-CSRF-TOKEN': 'csrf' }, body: JSON.stringify({ token: 'secret' }) });
    expect(JSON.parse(request.mock.calls[2][1].body)).toEqual({ token: 'secret', username: 'new_user', password: ' password kept ' });
  });
  it('sends request identity and campaign version for DM management commands', async () => {
    const request = vi.fn().mockResolvedValueOnce(Response.json({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ id: 'c', name: '新团', role: 'DM' }, { status: 201 }))
      .mockResolvedValueOnce(Response.json({ id: 'i', token: null, campaignVersion: 4 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const api = new Api(request);
    await api.createCampaign({ requestId: 'r1', name: '新团' });
    await api.createInvitation('c', { requestId: 'r2', expectedVersion: 3 });
    await api.revokeInvitation('c', 'i', { requestId: 'r3', expectedVersion: 4 });
    await api.removeMember('c', 'user/name', { requestId: 'r4', expectedVersion: 5 });
    expect(request.mock.calls[2][1].body).toBe(JSON.stringify({ requestId: 'r2', expectedVersion: 3 }));
    expect(request.mock.calls[3][0]).toBe('/api/campaigns/c/invitations/i/revoke');
    expect(request.mock.calls[4][0]).toBe('/api/campaigns/c/members/user%2Fname/remove');
  });
  it('preserves the native browser fetch receiver when no request adapter is supplied', async () => {
    vi.spyOn(globalThis, 'fetch').mockImplementation(function (this: unknown) {
      if (this !== globalThis) throw new TypeError('Illegal invocation');
      return Promise.resolve(Response.json({ token: 'native', headerName: 'X-CSRF-TOKEN' }));
    });
    await expect(new Api().getCsrf()).resolves.toEqual({ token: 'native', headerName: 'X-CSRF-TOKEN' });
  });
  it('obtains CSRF, logs in with cookies, then rotates CSRF', async () => {
    const request = vi.fn().mockResolvedValueOnce(Response.json({ token: 'before', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ username: 'player' }))
      .mockResolvedValueOnce(Response.json({ token: 'after', headerName: 'X-CSRF-TOKEN' }))
      .mockResolvedValueOnce(Response.json({ id: 't' }));
    const api = new Api(request);
    await api.login('player', 'supplied-at-runtime');
    await api.move('c', 't', { requestId: 'request', expectedVersion: 0, x: 1, y: 2 });
    expect(request.mock.calls.map(call => call[0])).toEqual(['/api/auth/csrf', '/api/auth/login', '/api/auth/csrf', '/api/campaigns/c/tokens/t/moves']);
    expect(request.mock.calls[1][1]).toMatchObject({ credentials: 'include', method: 'POST', headers: { 'X-CSRF-TOKEN': 'before' } });
    expect(request.mock.calls[3][1].headers['X-CSRF-TOKEN']).toBe('after');
  });
  it('preserves HTTP status for a non-JSON proxy failure', async () => {
    const api = new Api(vi.fn().mockResolvedValue(new Response('Bad gateway', { status: 502 })));
    await expect(api.campaigns()).rejects.toMatchObject({ status: 502 });
    expect(new ApiError(409, '冲突')).toBeInstanceOf(Error);
  });
});
