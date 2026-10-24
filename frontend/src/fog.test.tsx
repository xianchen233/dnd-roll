import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';
import { FogManagement } from './FogManagement';
import type { Cell } from './types';
const initial = { sceneId: 's', sceneVersion: 8, revealed: [] as Cell[] };
function deferred<T>() { let resolve!: (v: T) => void, reject!: (e: unknown) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function setup(extra: object = {}) { return { fog: vi.fn().mockResolvedValue(initial), saveFog: vi.fn().mockResolvedValue({ ...initial, sceneVersion: 9 }), ...extra }; }
function props(api: ReturnType<typeof setup>) {
  return { api: api as unknown as Api, campaignId: 'c', sceneId: 's', width: 24, height: 16, revision: 8, onChanged: vi.fn(), onDenied: vi.fn(), children: ({ revealed, locked, onRectangle }: { revealed: Cell[]; locked: boolean; onRectangle(a: Cell, b: Cell, reveal: boolean): void }) => <><output data-testid="draft">{JSON.stringify(revealed)}</output><button disabled={locked} onClick={() => onRectangle({ x: 2, y: 2 }, { x: 1, y: 1 }, true)}>模拟矩形手势</button></> };
}
async function click(name: string) { const b = await screen.findByRole('button', { name }); await waitFor(() => expect(b).toBeEnabled()); fireEvent.click(b); }
describe('DM manual fog authoring', () => {
  it('keeps a loaded fog draft editable during background revalidation and saves its original version', async () => {
    const api = setup(), p = props(api), view = render(<FogManagement {...p} />); await waitFor(() => expect(screen.getByRole('button', { name: '模拟矩形手势' })).toBeEnabled());
    const revalidating = deferred<typeof initial>(); api.fog.mockReturnValueOnce(revalidating.promise); view.rerender(<FogManagement {...p} revision={9} />);
    expect(screen.getByRole('button', { name: '保存地图遮盖' })).toBeDisabled(); act(() => screen.getByRole('button', { name: '模拟矩形手势' }).click());
    expect(screen.getByTestId('draft')).toHaveTextContent('[{"x":1,"y":1},{"x":2,"y":1},{"x":1,"y":2},{"x":2,"y":2}]');
    await act(async () => { revalidating.resolve({ ...initial, sceneVersion: 9 }); }); await click('保存地图遮盖'); expect(api.saveFog.mock.calls[0][2].expectedVersion).toBe(8);
  });
  it('locks loaded local gestures after a stale authorization denial even when no command is pending', async () => {
    const api = setup(), p = props(api), view = render(<FogManagement {...p} />); await click('模拟矩形手势');
    const old = deferred<typeof initial>(); api.fog.mockReturnValueOnce(old.promise); view.rerender(<FogManagement {...p} revision={9} />); await click('刷新遮盖');
    await act(async () => { old.reject(new ApiError(403, 'denied')); }); expect(p.onDenied).toHaveBeenCalledOnce();
    expect(screen.getByRole('button', { name: '模拟矩形手势' })).toBeDisabled(); expect(screen.getByTestId('draft')).toHaveTextContent('[]');
  });
  it('keeps gestures local until explicit save, canonicalizes reverse rectangles, and does not send token fields', async () => {
    const api = setup(); render(<FogManagement {...props(api)} />); await click('模拟矩形手势'); expect(api.saveFog).not.toHaveBeenCalled();
    expect(screen.getByTestId('draft')).toHaveTextContent('[{"x":1,"y":1},{"x":2,"y":1},{"x":1,"y":2},{"x":2,"y":2}]');
    await click('保存地图遮盖'); expect(api.saveFog).toHaveBeenCalledWith('c', 's', { requestId: expect.any(String), expectedVersion: 8, revealed: [{ x: 1, y: 1 }, { x: 2, y: 1 }, { x: 1, y: 2 }, { x: 2, y: 2 }] });
    await screen.findByText('遮盖请求已确认；当前揭露状态已从服务器刷新。'); expect(screen.getByTestId('draft')).toHaveTextContent('[]');
  });
  it('coordinate fallback reveals and re-covers the same set and rejects empty/outside coordinates', async () => {
    const api = setup(); render(<FogManagement {...props(api)} />); await click('加入揭露矩形'); expect(screen.getByTestId('draft')).toHaveTextContent('[{"x":0,"y":0}]');
    await click('加入遮盖矩形'); expect(screen.getByTestId('draft')).toHaveTextContent('[]');
    fireEvent.change(screen.getByLabelText('起点 X'), { target: { value: '-1' } }); await click('加入揭露矩形'); await screen.findByText('请填写网格内的整数坐标。'); expect(api.saveFog).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText('起点 X'), { target: { value: '' } }); await click('加入揭露矩形'); expect(screen.getByTestId('draft')).toHaveTextContent('[]');
  });
  it('retains original draft scene version across a concurrent move/map/fog invalidation', async () => {
    const api = setup(), p = props(api), view = render(<FogManagement {...p} />); await click('模拟矩形手势'); api.fog.mockResolvedValue({ ...initial, sceneVersion: 9 });
    view.rerender(<FogManagement {...p} revision={9} />); await screen.findByText('场景已变化；遮盖草稿保留原版本，保存时将检查冲突。'); await click('保存地图遮盖');
    expect(api.saveFog.mock.calls[0][2].expectedVersion).toBe(8);
  });
  it.each([new Error('offline'), new ApiError(503, 'unavailable')])('locks conflicting edits after uncertainty and retries the exact body/requestId (%s)', async error => {
    const api = setup({ saveFog: vi.fn().mockRejectedValueOnce(error).mockResolvedValueOnce(initial) }); render(<FogManagement {...props(api)} />); await click('模拟矩形手势'); await click('保存地图遮盖');
    const retry = await screen.findByRole('button', { name: '重试同一遮盖操作' }); expect(screen.getByRole('button', { name: '保存地图遮盖' })).toBeDisabled(); expect(screen.getByLabelText('起点 X')).toBeDisabled(); expect(screen.getByRole('button', { name: '模拟矩形手势' })).toBeDisabled();
    fireEvent.click(retry); await screen.findByText('遮盖请求已确认；当前揭露状态已从服务器刷新。'); expect(api.saveFog.mock.calls[1][2]).toBe(api.saveFog.mock.calls[0][2]); expect(api.saveFog).toHaveBeenCalledTimes(2);
  });
  it('rejects stale drafts, refreshes latest state and requires a new explicit command', async () => {
    const api = setup({ saveFog: vi.fn().mockRejectedValueOnce(new ApiError(409, 'stale')).mockResolvedValueOnce(initial) }); render(<FogManagement {...props(api)} />); await click('模拟矩形手势');
    api.fog.mockResolvedValue({ ...initial, sceneVersion: 10, revealed: [{ x: 7, y: 7 }] }); await click('保存地图遮盖'); await screen.findByText('遮盖操作冲突，已请求最新状态；请检查后重新编辑。');
    expect(screen.getByTestId('draft')).toHaveTextContent('[{"x":7,"y":7}]'); expect(screen.queryByRole('button', { name: '重试同一遮盖操作' })).not.toBeInTheDocument(); await click('保存地图遮盖'); expect(api.saveFog.mock.calls[1][2].expectedVersion).toBe(10);
  });
  it.each(['older-first', 'latest-first'] as const)('only the newest post-ack read confirms synchronization (%s)', async order => {
    const post = deferred<unknown>(), own = deferred<typeof initial>(), latest = deferred<typeof initial>(), api = setup({ saveFog: vi.fn().mockReturnValue(post.promise) }), p = props(api), view = render(<FogManagement {...p} />);
    await click('模拟矩形手势'); await click('保存地图遮盖'); api.fog.mockReturnValueOnce(own.promise).mockReturnValueOnce(latest.promise);
    await act(async () => { post.resolve({ ...initial, sceneVersion: 9, revealed: [{ x: 1, y: 1 }] }); }); view.rerender(<FogManagement {...p} revision={9} />);
    if (order === 'older-first') { await act(async () => { own.resolve({ ...initial, sceneVersion: 9 }); }); expect(screen.queryByText('遮盖请求已确认；当前揭露状态已从服务器刷新。')).not.toBeInTheDocument(); }
    await act(async () => { latest.resolve({ ...initial, sceneVersion: 10, revealed: [{ x: 4, y: 4 }] }); }); await screen.findByText('遮盖请求已确认；当前揭露状态已从服务器刷新。');
    if (order === 'latest-first') await act(async () => { own.resolve({ ...initial, sceneVersion: 9 }); }); expect(screen.getByTestId('draft')).toHaveTextContent('[{"x":4,"y":4}]'); expect(api.saveFog).toHaveBeenCalledOnce();
  });
  it('keeps a failed latest post-ack read unsynchronized even after an older successful read, then recovers without another POST', async () => {
    const post = deferred<unknown>(), own = deferred<typeof initial>(), latest = deferred<typeof initial>(), api = setup({ saveFog: vi.fn().mockReturnValue(post.promise) }), p = props(api), view = render(<FogManagement {...p} />);
    await click('保存地图遮盖'); api.fog.mockReturnValueOnce(own.promise).mockReturnValueOnce(latest.promise); await act(async () => { post.resolve(initial); }); view.rerender(<FogManagement {...p} revision={9} />);
    await act(async () => { latest.reject(new ApiError(503, 'unavailable')); }); await screen.findByText('请求已确认；刷新失败，遮盖尚未同步。请刷新遮盖。');
    await act(async () => { own.resolve(initial); }); expect(screen.queryByText('遮盖请求已确认；当前揭露状态已从服务器刷新。')).not.toBeInTheDocument(); await click('刷新遮盖'); await screen.findByText('遮盖请求已确认；当前揭露状态已从服务器刷新。'); expect(api.saveFog).toHaveBeenCalledOnce();
  });
  it('a pre-ack read cannot confirm a saved command', async () => {
    const post = deferred<unknown>(), before = deferred<typeof initial>(), after = deferred<typeof initial>(), api = setup({ saveFog: vi.fn().mockReturnValue(post.promise) }), p = props(api), view = render(<FogManagement {...p} />);
    await click('保存地图遮盖'); api.fog.mockReturnValueOnce(before.promise).mockReturnValueOnce(after.promise); view.rerender(<FogManagement {...p} revision={9} />);
    await act(async () => { post.resolve(initial); before.resolve(initial); }); expect(screen.queryByText('遮盖请求已确认；当前揭露状态已从服务器刷新。')).not.toBeInTheDocument();
    await act(async () => { after.resolve(initial); }); await screen.findByText('遮盖请求已确认；当前揭露状态已从服务器刷新。');
  });
  it.each([401, 403])('a concurrent old read %s clears drafts and prevents a late POST from restoring private state', async status => {
    const old = deferred<typeof initial>(), post = deferred<unknown>(), api = setup({ saveFog: vi.fn().mockReturnValue(post.promise) }), p = props(api), view = render(<FogManagement {...p} />);
    await click('模拟矩形手势'); api.fog.mockReturnValueOnce(old.promise); view.rerender(<FogManagement {...p} revision={9} />); api.fog.mockResolvedValue({ ...initial, sceneVersion: 9 }); await click('刷新遮盖'); await click('保存地图遮盖');
    await act(async () => { old.reject(new ApiError(status, 'denied')); }); expect(p.onDenied).toHaveBeenCalledOnce(); expect(screen.getByTestId('draft')).toHaveTextContent('[]'); expect(screen.getByRole('button', { name: '模拟矩形手势' })).toBeDisabled();
    await act(async () => { post.resolve(initial); }); expect(screen.queryByText('遮盖请求已确认；当前揭露状态已从服务器刷新。')).not.toBeInTheDocument(); expect(screen.getByRole('button', { name: '保存地图遮盖' })).toBeDisabled();
  });
  it('disposes commands and late reads on scene/campaign switch', async () => {
    const post = deferred<unknown>(), api = setup({ saveFog: vi.fn().mockReturnValue(post.promise) }), p = props(api), view = render(<FogManagement {...p} />);
    await click('模拟矩形手势'); await click('保存地图遮盖'); api.fog.mockResolvedValue({ ...initial, sceneId: 'other', sceneVersion: 1 }); view.rerender(<FogManagement {...p} sceneId="other" revision={1} />);
    await waitFor(() => expect(screen.getByRole('button', { name: '保存地图遮盖' })).toBeEnabled()); await act(async () => { post.resolve(initial); }); expect(p.onChanged).not.toHaveBeenCalled(); expect(screen.getByTestId('draft')).toHaveTextContent('[]'); expect(api.fog).toHaveBeenCalledTimes(2);
  });
});
