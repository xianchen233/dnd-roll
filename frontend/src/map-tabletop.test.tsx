import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { Tabletop } from './Tabletop';
import type { Api } from './api';
const realtime = vi.hoisted(() => ({ refresh: (() => {}) as () => void }));
vi.mock('./realtime', () => ({ connectRealtime: (_api: unknown, _id: string, refresh: () => void) => { realtime.refresh = refresh; return () => {}; } }));
vi.mock('./MapCanvas', () => ({ MapCanvas: ({ mapUrl, scene }: { mapUrl?: string; scene: { tokens: { name: string }[] } }) => <div data-testid="canvas" data-url={mapUrl ?? ''}>{scene.tokens.map(t => t.name).join(',')}</div> }));
const scene = { id: 's', campaignId: 'c', name: '森林', width: 24, height: 16, gridSize: 48, version: 8, tokens: [{ id: 't', name: '公开单位', x: 2, y: 3, version: 0, canMove: true }] };
function deferred<T>() { let resolve!: (v: T) => void, reject!: (e: unknown) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function setup() {
  const create = vi.fn().mockReturnValue('blob:public'), revoke = vi.fn();
  class Url extends URL { static createObjectURL = create; static revokeObjectURL = revoke; } vi.stubGlobal('URL', Url);
  const api = { scene: vi.fn().mockImplementation(async () => ({ ...scene, tokens: [...scene.tokens] })), mapView: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 8, map: { distancePerCell: 5 } }), mapImage: vi.fn().mockResolvedValue(new Blob(['safe'])), fog: vi.fn(), assetContent: vi.fn() };
  const p = { api: api as unknown as Api, identity: { username: 'p' }, campaigns: [{ id: 'c', name: '长夜', role: 'PLAYER' as const }], campaignId: 'c', onCampaign: vi.fn(), onRefresh: vi.fn() };
  return { api, p, create, revoke };
}
afterEach(() => vi.unstubAllGlobals());
describe('map invalidation in the shared tabletop', () => {
  it.each(['manual', 'realtime/reconnect/periodic'] as const)('clears old revealed pixels before a %s snapshot read, including a failed read', async source => {
    const { api, p, revoke } = setup(); render(<Tabletop {...p} />); await waitFor(() => expect(screen.getByTestId('canvas')).toHaveAttribute('data-url', 'blob:public'));
    const read = deferred<typeof scene>(); api.scene.mockReturnValueOnce(read.promise);
    if (source === 'manual') fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' })); else act(() => realtime.refresh());
    expect(screen.getByTestId('canvas')).toHaveAttribute('data-url', ''); expect(revoke).toHaveBeenCalledWith('blob:public');
    await act(async () => { read.reject(new Error('offline')); }); expect(screen.getByTestId('canvas')).toHaveAttribute('data-url', '');
    expect(screen.getByTestId('canvas')).toHaveTextContent('公开单位'); expect(api.fog).not.toHaveBeenCalled(); expect(api.assetContent).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' })); await waitFor(() => expect(api.mapView).toHaveBeenCalledTimes(2)); await waitFor(() => expect(screen.getByTestId('canvas')).toHaveAttribute('data-url', 'blob:public'));
  });
});
