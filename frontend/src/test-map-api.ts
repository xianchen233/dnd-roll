import { vi } from 'vitest';
// Older interface tests keep their scene/move assertions. Supply ordinary safe
// read endpoints for the new independent map lifecycle, without extra scene calls.
export function withSafeMapReads<T extends object>(api: T) {
  async function current() { return await (api as { scene?: { mock: { results: { value: unknown }[] } } }).scene?.mock.results.at(-1)?.value as { id: string; version: number } | undefined; }
  return Object.assign(api, {
    mapView: vi.fn().mockImplementation(async () => { const s = await current(); return { sceneId: s?.id ?? 's', sceneVersion: s?.version ?? 0, map: null }; }),
    mapImage: vi.fn(),
    fog: vi.fn().mockImplementation(async (_campaignId: string, sceneId: string) => ({ sceneId, sceneVersion: (await current())?.version ?? 0, revealed: [] })),
  });
}
