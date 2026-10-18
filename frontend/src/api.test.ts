import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';

describe('session HTTP client', () => {
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
