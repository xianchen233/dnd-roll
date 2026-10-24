import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';
import { SceneManagement } from './SceneManagement';
const list = (campaignVersion = 2) => ({ campaignVersion, currentSceneId: 's', scenes: [{ id: 's', name: '营地' }, { id: 'next', name: '密室' }] });
function deferred<T>() { let resolve!: (value: T) => void, reject!: (error: Error) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function setup() {
  const api = { scenes: vi.fn().mockResolvedValue(list()), createScene: vi.fn().mockResolvedValue({ id: 'new', name: '地牢', campaignVersion: 9 }), activateScene: vi.fn().mockResolvedValue({ id: 'next', name: '密室', campaignVersion: 9 }) };
  const p = { api: api as unknown as Api, campaignId: 'c', revision: 0, onChanged: vi.fn().mockResolvedValue(undefined), onDenied: vi.fn() }; return { api, p };
}
async function click(name: string) { const button = await screen.findByRole('button', { name }); await waitFor(() => expect(button).toBeEnabled()); fireEvent.click(button); }
describe('scene command preflight concurrency', () => {
  it.each([
    { kind: 'activate' as const, order: 'preflight-first' }, { kind: 'activate' as const, order: 'background-first' },
    { kind: 'create' as const, order: 'preflight-first' }, { kind: 'create' as const, order: 'background-first' },
  ])('does not silently cancel $kind when a revision read supersedes its own preflight ($order)', async ({ kind, order }) => {
    const { api, p } = setup(), view = render(<SceneManagement {...p} />); await waitFor(() => expect(screen.getByRole('button', { name: '创建空场景' })).toBeEnabled());
    const preflight = deferred<ReturnType<typeof list>>(), background = deferred<ReturnType<typeof list>>(); api.scenes.mockReturnValueOnce(preflight.promise).mockReturnValueOnce(background.promise).mockResolvedValue(list(9));
    if (kind === 'create') fireEvent.change(screen.getByLabelText('场景名称'), { target: { value: '地牢' } });
    await click(kind === 'activate' ? '切换到 密室' : '创建空场景'); await waitFor(() => expect(api.scenes).toHaveBeenCalledTimes(2));
    view.rerender(<SceneManagement {...p} revision={1} />); await waitFor(() => expect(api.scenes).toHaveBeenCalledTimes(3));
    if (order === 'background-first') await act(async () => { background.resolve(list(9)); });
    await act(async () => { preflight.resolve(list(8)); });
    if (kind === 'activate') expect(api.activateScene).toHaveBeenCalledWith('c', 'next', { requestId: expect.any(String), expectedVersion: 8 });
    else expect(api.createScene).toHaveBeenCalledWith('c', { requestId: expect.any(String), expectedVersion: 8, name: '地牢' });
    if (order === 'preflight-first') await act(async () => { background.resolve(list(9)); });
    expect(api.activateScene.mock.calls.length + api.createScene.mock.calls.length).toBe(1);
  });
  it.each([401, 403])('an overlapping background %i denial still wins over a later successful command preflight', async status => {
    const { api, p } = setup(), view = render(<SceneManagement {...p} />); await waitFor(() => expect(screen.getByRole('button', { name: '创建空场景' })).toBeEnabled());
    const preflight = deferred<ReturnType<typeof list>>(), background = deferred<ReturnType<typeof list>>(); api.scenes.mockReturnValueOnce(preflight.promise).mockReturnValueOnce(background.promise);
    await click('切换到 密室'); view.rerender(<SceneManagement {...p} revision={1} />); await act(async () => { background.reject(new ApiError(status, 'denied')); });
    await act(async () => { preflight.resolve(list(8)); }); expect(p.onDenied).toHaveBeenCalledOnce(); expect(api.activateScene).not.toHaveBeenCalled(); expect(api.createScene).not.toHaveBeenCalled();
  });
  it('submits its own preflight version and reports an explicit 409 instead of adopting a newer background version', async () => {
    const { api, p } = setup(), view = render(<SceneManagement {...p} />); await waitFor(() => expect(screen.getByRole('button', { name: '创建空场景' })).toBeEnabled());
    const preflight = deferred<ReturnType<typeof list>>(), background = deferred<ReturnType<typeof list>>(); api.scenes.mockReturnValueOnce(preflight.promise).mockReturnValueOnce(background.promise).mockResolvedValue(list(9)); api.activateScene.mockRejectedValueOnce(new ApiError(409, 'stale'));
    await click('切换到 密室'); view.rerender(<SceneManagement {...p} revision={1} />); await act(async () => { background.resolve(list(9)); preflight.resolve(list(8)); });
    await screen.findByText('操作冲突，已刷新场景列表。请检查当前状态后重新操作。');
    expect(api.activateScene).toHaveBeenCalledOnce(); expect(api.activateScene.mock.calls[0][2].expectedVersion).toBe(8); expect(screen.queryByRole('button', { name: '重试同一场景操作' })).not.toBeInTheDocument();
  });
});
