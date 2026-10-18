import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { Api, ApiError } from './api';
vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div>地图替身</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
const oldScene = { id: 's', campaignId: 'c', name: '营地', width: 24, height: 16, gridSize: 48, version: 3, tokens: [{ id: 'hero', name: '游侠', x: 2, y: 3, version: 3, canMove: true }] };
const list = (campaignVersion = 2, currentSceneId = 's') => ({ campaignVersion, currentSceneId, scenes: [{ id: 's', name: '营地' }, { id: 'next', name: '密室' }] });
function setup(extra: object = {}) { return { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'dm' }), campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'DM' }]), scene: vi.fn().mockResolvedValue(oldScene), scenes: vi.fn().mockResolvedValue(list()), assets: vi.fn().mockResolvedValue({ campaignVersion: 2, assets: [] }), sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map: null }), members: vi.fn().mockResolvedValue({ campaignVersion: 2, members: [] }), invitations: vi.fn().mockResolvedValue({ campaignVersion: 2, invitations: [] }), createScene: vi.fn().mockResolvedValue({ id: 'new', name: '地牢', campaignVersion: 3 }), activateScene: vi.fn().mockResolvedValue({ id: 'next', name: '密室', campaignVersion: 3 }), ...extra }; }
function mount(api: ReturnType<typeof setup>) { render(<App api={api as unknown as Api} />); }
async function create() { fireEvent.change(await screen.findByLabelText('场景名称'), { target: { value: ' 地牢 ' } }); const button = screen.getByRole('button', { name: '创建空场景' }); await waitFor(() => expect(button).toBeEnabled()); fireEvent.click(button); }
function deferred<T>() { let resolve!: (value: T) => void, reject!: (error: Error) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
describe('DM scene management', () => {
  it.each([
    { first: 'members' as const, status: 401, failure: new ApiError(503, 'upstream503') },
    { first: 'members' as const, status: 403, failure: new Error('offline') },
    { first: 'invitations' as const, status: 401, failure: new Error('offline') },
    { first: 'invitations' as const, status: 403, failure: new ApiError(503, 'upstream503') },
  ])('handles later management denial $status after $first fails first and isolates pending successes', async ({ first, status, failure }) => {
    const api = setup(); mount(api);
    await screen.findByRole('button', { name: '切换到 密室' });
    await waitFor(() => expect(screen.getByRole('button', { name: '生成邀请链接' })).toBeEnabled());
    const failed = deferred<unknown>(), denied = deferred<unknown>(), staleMap = deferred<unknown>(), stalePreparation = deferred<unknown>();
    api[first].mockReturnValueOnce(failed.promise);
    api[first === 'members' ? 'invitations' : 'members'].mockReturnValueOnce(denied.promise);
    api.scene.mockReturnValueOnce(staleMap.promise); api.scenes.mockReturnValueOnce(stalePreparation.promise);
    fireEvent.click(screen.getByRole('button', { name: '刷新成员与邀请' }));
    fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    fireEvent.click(screen.getByRole('button', { name: '刷新场景列表' }));
    await act(async () => { failed.reject(failure); });
    expect(screen.getByText('地图替身')).toBeInTheDocument();
    await act(async () => { denied.reject(new ApiError(status, 'removed')); });
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: '场景管理' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
    await act(async () => { staleMap.resolve(oldScene); stalePreparation.resolve(list(99)); });
    expect(screen.queryByText('地图替身')).not.toBeInTheDocument();
    expect(screen.queryByText('当前：营地')).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
  });
  it.each(['members', 'invitations'] as const)('clears immediately on %s denial while the other management request remains pending', async deniedMethod => {
    const api = setup(); mount(api);
    await screen.findByRole('button', { name: '切换到 密室' });
    await waitFor(() => expect(screen.getByRole('button', { name: '生成邀请链接' })).toBeEnabled());
    const denied = deferred<unknown>(), pending = deferred<unknown>();
    api[deniedMethod].mockReturnValueOnce(denied.promise);
    api[deniedMethod === 'members' ? 'invitations' : 'members'].mockReturnValueOnce(pending.promise);
    fireEvent.click(screen.getByRole('button', { name: '刷新成员与邀请' }));
    await act(async () => { denied.reject(new ApiError(403, 'removed')); });
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: '场景管理' })).not.toBeInTheDocument();
    await act(async () => { pending.resolve(deniedMethod === 'members' ? { campaignVersion: 99, invitations: [] } : { campaignVersion: 99, members: [] }); });
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
    expect(screen.queryByText('地图替身')).not.toBeInTheDocument();
  });
  it('keeps an acknowledged switch distinct from a failed snapshot read and never repeats the POST', async () => {
    const api = setup(); mount(api);
    await screen.findByRole('button', { name: '切换到 密室' });
    api.scene.mockRejectedValue(new ApiError(503, 'offline'));
    fireEvent.click(screen.getByRole('button', { name: '切换到 密室' }));
    await screen.findByText('切换请求已确认；全团当前场景以最新服务器快照为准。');
    expect(screen.getByText(/场景刷新失败；保留上次确认状态/)).toBeInTheDocument();
    expect(api.activateScene).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: '重试同一场景操作' })).not.toBeInTheDocument();
  });
  it.each([401, 403])('clears map, preparation and account management on scene command %i', async status => {
    const api = setup({ activateScene: vi.fn().mockRejectedValue(new ApiError(status, 'removed')) }); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: '切换到 密室' }));
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: '场景管理' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
  });
  it('creates a trimmed empty scene using the latest shared campaign version without activating it', async () => {
    const api = setup(); mount(api);
    await screen.findByRole('button', { name: '切换到 密室' });
    api.scenes.mockResolvedValue(list(8));
    await create();
    await waitFor(() => expect(api.createScene).toHaveBeenCalledWith('c', { requestId: expect.any(String), expectedVersion: 8, name: '地牢' }));
    await screen.findByText('空场景已创建；全团当前场景保持不变。');
    expect(api.activateScene).not.toHaveBeenCalled();
    expect(screen.getByRole('heading', { name: '营地' })).toBeInTheDocument();
    expect(api.members.mock.calls.length).toBeGreaterThan(1);
  });
  it('rereads authoritative snapshot and list after activation instead of treating a historical receipt as current', async () => {
    const api = setup({ activateScene: vi.fn().mockResolvedValue({ id: 'next', name: '密室', campaignVersion: 3 }) }); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: '切换到 密室' }));
    await screen.findByText('切换请求已确认；全团当前场景以最新服务器快照为准。');
    expect(api.activateScene).toHaveBeenCalledWith('c', 'next', { requestId: expect.any(String), expectedVersion: 2 });
    expect(api.scene.mock.calls.length).toBeGreaterThan(1);
    expect(screen.getByRole('heading', { name: '营地' })).toBeInTheDocument();
    expect(screen.getByText('当前：营地')).toBeInTheDocument();
  });
  it.each([new Error('offline'), new ApiError(503, 'unavailable')])('retains the exact body for explicit retry after ambiguous failure %s', async failure => {
    const api = setup({ createScene: vi.fn().mockRejectedValueOnce(failure).mockResolvedValueOnce({ id: 'new', name: '地牢', campaignVersion: 3 }) }); mount(api);
    await create(); fireEvent.click(await screen.findByRole('button', { name: '重试同一场景操作' }));
    await screen.findByText('空场景已创建；全团当前场景保持不变。');
    expect(api.createScene.mock.calls[0]).toEqual(api.createScene.mock.calls[1]);
  });
  it('refreshes a 409 without blindly retrying; a new action uses a new request and version', async () => {
    const api = setup({ createScene: vi.fn().mockRejectedValueOnce(new ApiError(409, 'version')).mockResolvedValueOnce({ id: 'new', name: '地牢', campaignVersion: 10 }) }); mount(api);
    await create();
    await screen.findByText(/操作冲突，已刷新场景列表/);
    expect(api.createScene).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole('button', { name: '重试同一场景操作' })).not.toBeInTheDocument();
    api.scenes.mockResolvedValue(list(9));
    fireEvent.click(screen.getByRole('button', { name: '创建空场景' }));
    await waitFor(() => expect(api.createScene).toHaveBeenCalledTimes(2));
    expect(api.createScene.mock.calls[1][1]).toMatchObject({ expectedVersion: 9 });
    expect(api.createScene.mock.calls[0][1].requestId).not.toBe(api.createScene.mock.calls[1][1].requestId);
  });
  it.each([401, 403])('clears map and all management data on %i and ignores delayed command success', async status => {
    let resolve!: (value: unknown) => void;
    const api = setup({ createScene: vi.fn().mockImplementation(() => new Promise(r => { resolve = r; })) }); mount(api);
    await create(); await waitFor(() => expect(api.createScene).toHaveBeenCalledTimes(1));
    api.scene.mockRejectedValue(new ApiError(status, 'removed'));
    fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    await act(async () => { resolve({ id: 'new', name: 'private', campaignVersion: 3 }); });
    expect(screen.queryByRole('heading', { name: '场景管理' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
    expect(screen.queryByText('空场景已创建；全团当前场景保持不变。')).not.toBeInTheDocument();
  });
  it('clears selection when another scene arrives even if token ids coincide', async () => {
    const api = setup(); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: /游侠/ }));
    expect(screen.getByRole('heading', { name: '游侠' })).toBeInTheDocument();
    api.scene.mockResolvedValue({ ...oldScene, id: 'next', name: '密室', version: 0 });
    fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await screen.findByRole('heading', { name: '密室' });
    expect(screen.getByRole('heading', { name: '未选择单位' })).toBeInTheDocument();
  });
  it('never requests preparation metadata or renders management for a player', async () => {
    const api = setup({ campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'PLAYER' }]) }); mount(api);
    await screen.findByRole('heading', { name: '营地' });
    expect(api.scenes).not.toHaveBeenCalled();
    expect(screen.queryByRole('heading', { name: '场景管理' })).not.toBeInTheDocument();
  });
});
