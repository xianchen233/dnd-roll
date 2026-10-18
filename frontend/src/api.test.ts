import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';

describe('session HTTP client', () => {
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
