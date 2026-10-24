import { expect, it, vi } from 'vitest';
import { Api } from './api';

it('keeps complete manual definition, review and exact execution bodies on encoded DM routes', async () => {
  const request = vi.fn().mockResolvedValueOnce(Response.json([]))
    .mockResolvedValueOnce(Response.json({ token: 'csrf', headerName: 'X-CSRF-TOKEN' }))
    .mockImplementation(() => Promise.resolve(Response.json({ id: 'op', version: 1 })));
  const api = new Api(request), save = { requestId: 'r', expectedVersion: 0, name: '揭露守卫', steps: [{ kind: 'REVEAL_TOKEN' as const, sceneId: 's', tokenId: 't', expectedVersion: 4 }] };
  await api.moduleOperations('c/x'); await api.saveModuleOperation('c/x', 'o/x', save);
  const review = { expectedVersion: 1, campaignVersion: 7 };
  await api.moduleOperationImpact('c/x', 'o/x', review);
  const execute = { ...review, requestId: 'exact', impactHash: 'a'.repeat(64) };
  await api.executeModuleOperation('c/x', 'o/x', execute); await api.moduleOperationExecutions('c/x', 'o/x');
  expect(request.mock.calls.map(c => c[0])).toEqual(['/api/campaigns/c%2Fx/module-operations', '/api/auth/csrf', '/api/campaigns/c%2Fx/module-operations/o%2Fx', '/api/campaigns/c%2Fx/module-operations/o%2Fx/impact', '/api/campaigns/c%2Fx/module-operations/o%2Fx/executions', '/api/campaigns/c%2Fx/module-operations/o%2Fx/executions']);
  for (const [index, body] of [[2, save], [3, review], [4, execute]] as const) expect(request.mock.calls[index][1]).toMatchObject({ method: 'POST', credentials: 'include', headers: { 'X-CSRF-TOKEN': 'csrf' }, body: JSON.stringify(body) });
});
