import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';
import { TokenManagement } from './TokenManagement';
const token = { id: 't', name: '侦察兵', x: 2, y: 3, version: 4, hidden: true, controllers: ['p1'] };
const listed = { sceneId: 's', sceneVersion: 8, tokens: [token] };
const membership = { campaignVersion: 2, members: [{ username: 'dm', role: 'DM' }, { username: 'p1', role: 'PLAYER' }, { username: 'p2', role: 'PLAYER' }] };
function setup(extra: object = {}) { return { tokens: vi.fn().mockResolvedValue(listed), members: vi.fn().mockResolvedValue(membership), createToken: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, token }), updateToken: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, token }), ...extra }; }
function deferred<T>() { let resolve!: (value: T) => void, reject!: (error: Error) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function props(api: ReturnType<typeof setup>) { return { api: api as unknown as Api, campaignId: 'c', sceneId: 's', width: 24, height: 16, revision: 8, membersRevision: 0, onChanged: vi.fn(), onDenied: vi.fn() }; }
async function click(name: string) { const button = await screen.findByRole('button', { name }); await waitFor(() => expect(button).toBeEnabled()); fireEvent.click(button); }
async function name(value: string) { fireEvent.change(await screen.findByLabelText('棋子名称', { exact: true }), { target: { value } }); }
describe('DM token authoring', () => {
  it.each(['own-first', 'newest-first'] as const)('synchronizes from the newest post-ack read while ignoring historical receipts (%s)', async order => {
    const posted = deferred<unknown>(), own = deferred<unknown>(), newest = deferred<unknown>(), api = setup({ updateToken: vi.fn().mockReturnValue(posted.promise) }), p = props(api);
    const view = render(<TokenManagement {...p} />); await click('编辑棋子 t'); await name('历史名'); await click('保存棋子配置');
    api.tokens.mockReturnValueOnce(own.promise).mockReturnValueOnce(newest.promise);
    await act(async () => { posted.resolve({ token: { ...token, name: '历史名' } }); });
    view.rerender(<TokenManagement {...p} revision={10} />);
    if (order === 'own-first') await act(async () => { own.resolve({ ...listed, tokens: [{ ...token, name: '中间状态' }] }); });
    expect(screen.queryByText('棋子请求已确认；当前列表已从服务器刷新。')).not.toBeInTheDocument();
    await act(async () => { newest.resolve({ ...listed, sceneVersion: 10, tokens: [{ ...token, name: '当前名称', version: 6, controllers: [] }] }); });
    await screen.findByText('棋子请求已确认；当前列表已从服务器刷新。'); expect(screen.getByLabelText('棋子名称')).toHaveValue('当前名称'); expect(screen.getByLabelText('控制者 p1')).not.toBeChecked();
    if (order === 'newest-first') await act(async () => { own.reject(new Error('older offline')); });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument(); expect(api.updateToken).toHaveBeenCalledTimes(1);
  });
  it('keeps acknowledgment without claiming sync after the newest read fails, and recovers without another POST', async () => {
    const api = setup(), p = props(api); render(<TokenManagement {...p} />); await name('确认');
    api.createToken.mockImplementation(async () => { api.tokens.mockRejectedValue(new Error('offline read')); return { token }; });
    await click('创建棋子'); await screen.findByText('请求已确认；刷新失败，棋子列表尚未同步。请刷新棋子与控制者。');
    expect(screen.queryByRole('button', { name: '重试同一棋子操作' })).not.toBeInTheDocument();
    api.tokens.mockResolvedValue(listed); await click('刷新棋子与控制者'); await screen.findByText('棋子请求已确认；当前列表已从服务器刷新。'); expect(api.createToken).toHaveBeenCalledTimes(1);
  });
  it.each([401, 403])('a late member denial %i clears private state and isolates an unresolved authoring POST', async status => {
    const posted = deferred<unknown>(), denied = deferred<unknown>(), api = setup({ createToken: vi.fn().mockReturnValue(posted.promise) }), p = props(api);
    const view = render(<TokenManagement {...p} />); await name('秘密'); await click('创建棋子');
    api.tokens.mockRejectedValueOnce(new Error('peer offline')); api.members.mockReturnValueOnce(denied.promise); view.rerender(<TokenManagement {...p} revision={9} />);
    await act(async () => { denied.reject(new ApiError(status, 'denied')); }); await act(async () => { posted.resolve({ token }); });
    expect(p.onDenied).toHaveBeenCalledTimes(1); expect(p.onChanged).not.toHaveBeenCalled(); expect(screen.queryByText('侦察兵')).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: '重试同一棋子操作' })).not.toBeInTheDocument(); expect(screen.getByLabelText('棋子名称')).toHaveValue('');
  });
  it('keeps an unsaved draft on background refresh with its original version and clears removed controllers', async () => {
    const api = setup(), p = props(api), view = render(<TokenManagement {...p} />); await click('编辑棋子 t'); await name('未保存名称');
    api.tokens.mockResolvedValue({ ...listed, sceneVersion: 10, tokens: [{ ...token, version: 6, name: '另一位 DM', controllers: [] }] });
    api.members.mockResolvedValue({ ...membership, members: membership.members.filter(m => m.username !== 'p1') });
    view.rerender(<TokenManagement {...p} revision={10} membersRevision={1} />);
    await waitFor(() => expect(screen.queryByLabelText('控制者 p1')).not.toBeInTheDocument());
    expect(screen.getByLabelText('棋子名称')).toHaveValue('未保存名称');
    await click('保存棋子配置'); await waitFor(() => expect(api.updateToken).toHaveBeenCalledTimes(1));
    expect(api.updateToken.mock.calls[0][3]).toMatchObject({ expectedVersion: 4, name: '未保存名称', controllers: [] });
  });
  it('waits for both current tokens and members then creates with scene version and shared controllers', async () => {
    const memberRead = deferred<unknown>(), api = setup({ members: vi.fn().mockReturnValue(memberRead.promise) }); render(<TokenManagement {...props(api)} />);
    await name('新同伴'); expect(screen.getByRole('button', { name: '创建棋子' })).toBeDisabled();
    await act(async () => { memberRead.resolve(membership); });
    fireEvent.click(screen.getByLabelText('控制者 p1')); fireEvent.click(screen.getByLabelText('控制者 p2'));
    await click('创建棋子'); await screen.findByText('棋子请求已确认；当前列表已从服务器刷新。');
    expect(api.createToken).toHaveBeenCalledWith('c', 's', { requestId: expect.any(String), expectedVersion: 8, name: '新同伴', x: 0, y: 0, hidden: false, controllers: ['p1', 'p2'] });
  });
  it('edits the complete configuration with token version and rereads after historical receipts', async () => {
    const api = setup(), p = props(api); render(<TokenManagement {...p} />); await click('编辑棋子 t');
    expect(screen.getByLabelText('棋子隐藏')).toBeChecked(); expect(screen.getByLabelText('控制者 p1')).toBeChecked();
    await name('新名'); fireEvent.change(screen.getByLabelText('棋子 X'), { target: { value: '5' } }); fireEvent.click(screen.getByLabelText('棋子隐藏')); fireEvent.click(screen.getByLabelText('控制者 p1')); fireEvent.click(screen.getByLabelText('控制者 p2'));
    await click('保存棋子配置'); await screen.findByText('棋子请求已确认；当前列表已从服务器刷新。');
    expect(api.updateToken).toHaveBeenCalledWith('c', 's', 't', { requestId: expect.any(String), expectedVersion: 4, name: '新名', x: 5, y: 3, hidden: false, controllers: ['p2'] });
    expect(screen.getByLabelText('棋子名称')).toHaveValue('侦察兵'); expect(p.onChanged).toHaveBeenCalledTimes(1);
  });
  it.each([new Error('offline'), new ApiError(503, 'unknown')])('freezes edits and retries the exact command after %s', async error => {
    const api = setup({ createToken: vi.fn().mockRejectedValueOnce(error).mockResolvedValueOnce({ token }) }); render(<TokenManagement {...props(api)} />);
    await name('重试'); await click('创建棋子'); await screen.findByRole('button', { name: '重试同一棋子操作' });
    expect(screen.getByLabelText('棋子名称')).toBeDisabled(); await click('刷新棋子与控制者');
    await click('重试同一棋子操作'); await screen.findByText('棋子请求已确认；当前列表已从服务器刷新。');
    expect(api.createToken.mock.calls[1][2]).toBe(api.createToken.mock.calls[0][2]);
  });
  it('discards conflict commands and reloads authoritative versions', async () => {
    const api = setup({ createToken: vi.fn().mockRejectedValueOnce(new ApiError(409, 'conflict')).mockResolvedValueOnce({ token }) }); render(<TokenManagement {...props(api)} />);
    await name('并发'); await click('创建棋子'); await screen.findByText('操作冲突，已刷新棋子与控制者。请检查后重新操作。');
    expect(screen.queryByRole('button', { name: '重试同一棋子操作' })).not.toBeInTheDocument();
    await name('再试'); await click('创建棋子'); await waitFor(() => expect(api.createToken).toHaveBeenCalledTimes(2));
    expect(api.createToken.mock.calls[1][2].requestId).not.toBe(api.createToken.mock.calls[0][2].requestId);
  });
  it.each(['tokens', 'members'] as const)('observes any %s authorization denial after a superseding success', async method => {
    const api = setup(), p = props(api); const view = render(<TokenManagement {...p} />); await screen.findByText('侦察兵');
    const old = deferred<unknown>(); api[method].mockReturnValueOnce(old.promise);
    await click('刷新棋子与控制者'); view.rerender(<TokenManagement {...p} revision={9} />); await waitFor(() => expect(api.tokens).toHaveBeenCalledTimes(3));
    await act(async () => { old.reject(new ApiError(403, 'removed')); });
    expect(p.onDenied).toHaveBeenCalledTimes(1); expect(screen.queryByText('侦察兵')).not.toBeInTheDocument();
  });
  it('ignores a late POST and private reads after scene scope changes', async () => {
    const post = deferred<unknown>(), api = setup({ createToken: vi.fn().mockReturnValue(post.promise) }), p = props(api);
    const view = render(<TokenManagement {...p} />); await name('旧命令'); await click('创建棋子');
    api.tokens.mockResolvedValue({ sceneId: 'next', sceneVersion: 0, tokens: [] }); view.rerender(<TokenManagement {...p} sceneId="next" />);
    await act(async () => { post.resolve({ token }); });
    expect(p.onChanged).not.toHaveBeenCalled(); expect(screen.queryByText('侦察兵')).not.toBeInTheDocument(); expect(screen.getByLabelText('棋子名称')).toHaveValue('');
  });
});
