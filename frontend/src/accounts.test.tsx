import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { Api, ApiError } from './api';
vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div>地图替身</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
const campaign = { id: 'c', name: '新冒险', role: 'DM' };
const scene = { id: 's', campaignId: 'c', name: '空白场景', width: 24, height: 16, gridSize: 48, version: 0, tokens: [] };
function setup<T extends object>(extra: T) { return { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'dm' }), campaigns: vi.fn().mockResolvedValue([campaign]), scene: vi.fn().mockResolvedValue(scene), members: vi.fn().mockResolvedValue({ campaignVersion: 2, members: [{ username: 'dm', role: 'DM' }, { username: 'guest', role: 'PLAYER' }] }), invitations: vi.fn().mockResolvedValue({ campaignVersion: 2, invitations: [{ id: 'i', status: 'ACTIVE', expiresAt: '2026-10-25T00:00:00Z', createdBy: 'dm', usedBy: null }] }), ...extra }; }
afterEach(() => window.history.replaceState(null, '', '/'));
describe('accounts and invitations', () => {
  it('recovers a lost registration response through login and same-actor accept even when preview is unavailable', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), previewInvitation: vi.fn().mockRejectedValue(new ApiError(409, '邀请已使用')), login: vi.fn().mockResolvedValue({ username: 'registered_user' }), acceptInvitation: vi.fn().mockResolvedValue({ campaignId: 'c' }) });
    render(<App api={api as unknown as Api} />);
    await screen.findByText(/无法读取邀请/);
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'registered_user' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'safe-password-123' } });
    fireEvent.click(screen.getByRole('button', { name: '登录并接受邀请' }));
    await screen.findByText('空白场景');
    expect(api.acceptInvitation).toHaveBeenCalledWith('secret');
    expect(location.hash).toBe('');
  });
  it('keeps recovery acceptance visible after failed preview and displays authoritative membership denial', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ previewInvitation: vi.fn().mockRejectedValue(new ApiError(409, '邀请已使用')), acceptInvitation: vi.fn().mockRejectedValue(new ApiError(409, '邀请不可用，成员已移除')) });
    render(<App api={api as unknown as Api} />);
    await screen.findByText(/无法读取邀请/);
    fireEvent.click(screen.getByRole('button', { name: '接受邀请' }));
    await screen.findByText('邀请不可用，成员已移除');
    expect(location.hash).toBe('#invite=secret');
    expect(api.campaigns).not.toHaveBeenCalled();
  });
  it('counts Unicode password characters consistently before attempting registration', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), previewInvitation: vi.fn().mockResolvedValue({ campaignId: 'c', campaignName: '新冒险', expiresAt: '2026-10-25T00:00:00Z' }), registerInvitation: vi.fn().mockResolvedValue({ username: 'new_user', campaignId: 'c' }), login: vi.fn().mockRejectedValue(new Error('unexpected login')) });
    render(<App api={api as unknown as Api} />);
    fireEvent.change(await screen.findByLabelText('新账号'), { target: { value: 'new_user' } });
    fireEvent.change(screen.getByLabelText('新密码'), { target: { value: '😀'.repeat(11) } });
    fireEvent.click(screen.getByRole('button', { name: '注册并加入战役' }));
    await screen.findByText('密码至少 12 个字符，且不超过 72 个 UTF-8 字节。');
    expect(api.registerInvitation).not.toHaveBeenCalled();
  });
  it('revokes invitations with the loaded campaign version and refreshes both lists', async () => {
    const api = setup({ revokeInvitation: vi.fn().mockResolvedValue(undefined) });
    render(<App api={api as unknown as Api} />);
    fireEvent.click(await screen.findByRole('button', { name: '撤销邀请 i' }));
    await waitFor(() => expect(api.members).toHaveBeenCalledTimes(2));
    expect(api.revokeInvitation).toHaveBeenCalledWith('c', 'i', { requestId: expect.any(String), expectedVersion: 2 });
    expect(api.invitations).toHaveBeenCalledTimes(2);
  });
  it('keeps the same request on an uncertain invite creation and recovers a one-time replay', async () => {
    const api = setup({ createInvitation: vi.fn().mockRejectedValueOnce(new Error('连接中断')).mockResolvedValue({ id: 'i', token: null, campaignVersion: 3 }) });
    render(<App api={api as unknown as Api} />);
    fireEvent.click(await screen.findByRole('button', { name: '生成邀请链接' }));
    fireEvent.click(await screen.findByRole('button', { name: '重试同一管理操作' }));
    await screen.findByText(/邀请链接仅首次返回/);
    expect(api.createInvitation.mock.calls[0]).toEqual(api.createInvitation.mock.calls[1]);
  });
  it('recovers an account whose registration succeeded but automatic login failed', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), previewInvitation: vi.fn().mockResolvedValue({ campaignId: 'c', campaignName: '新冒险', expiresAt: '2026-10-25T00:00:00Z' }), registerInvitation: vi.fn().mockResolvedValue({ username: 'new_user', campaignId: 'c' }), login: vi.fn().mockRejectedValueOnce(new Error('登录连接中断')).mockResolvedValue({ username: 'new_user' }), acceptInvitation: vi.fn().mockResolvedValue({ campaignId: 'c' }) });
    render(<App api={api as unknown as Api} />);
    fireEvent.change(await screen.findByLabelText('新账号'), { target: { value: 'new_user' } });
    fireEvent.change(screen.getByLabelText('新密码'), { target: { value: 'safe-password-123' } });
    fireEvent.click(screen.getByRole('button', { name: '注册并加入战役' }));
    await screen.findByText(/账号已创建；如无法继续/);
    expect(location.hash).toBe('#invite=secret');
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'new_user' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'safe-password-123' } });
    fireEvent.click(screen.getByRole('button', { name: '登录并接受邀请' }));
    await screen.findByText('空白场景');
    expect(api.registerInvitation).toHaveBeenCalledTimes(1);
    expect(api.acceptInvitation).toHaveBeenCalledWith('secret');
  });
  it('shows an acceptance failure to an already authenticated user and retains the fragment', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ previewInvitation: vi.fn().mockResolvedValue({ campaignId: 'c', campaignName: '新冒险', expiresAt: '2026-10-25T00:00:00Z' }), acceptInvitation: vi.fn().mockRejectedValue(new ApiError(409, '邀请已使用')) });
    render(<App api={api as unknown as Api} />);
    fireEvent.click(await screen.findByRole('button', { name: '接受邀请' }));
    await screen.findByText('邀请已使用');
    expect(location.hash).toBe('#invite=secret');
    expect(api.campaigns).not.toHaveBeenCalled();
  });
  it('lets an authenticated account with no campaigns create and open an empty campaign', async () => {
    const api = setup({ campaigns: vi.fn().mockResolvedValueOnce([]).mockResolvedValue([campaign]), createCampaign: vi.fn().mockResolvedValue(campaign) });
    render(<App api={api as unknown as Api} />);
    fireEvent.change(await screen.findByLabelText('战役名称'), { target: { value: '新冒险' } });
    fireEvent.click(screen.getByRole('button', { name: '创建战役' }));
    await screen.findByText('空白场景');
    expect(api.createCampaign).toHaveBeenCalledWith({ name: '新冒险', requestId: expect.any(String) });
  });
  it('hides DM management from players while keeping campaign creation available', async () => {
    const api = setup({ campaigns: vi.fn().mockResolvedValue([{ ...campaign, role: 'PLAYER' }]) });
    render(<App api={api as unknown as Api} />);
    await screen.findByText('空白场景');
    expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '创建战役' })).toBeVisible();
    expect(api.invitations).not.toHaveBeenCalled();
  });
  it('creates a one-time fragment link and explains a token-null replay; conflicts refresh lists', async () => {
    const api = setup({ createInvitation: vi.fn().mockResolvedValueOnce({ id: 'i', token: 'one-time-secret', campaignVersion: 3 }).mockResolvedValueOnce({ id: 'i', token: null, campaignVersion: 3 }), removeMember: vi.fn().mockRejectedValue(new ApiError(409, '版本冲突')) });
    render(<App api={api as unknown as Api} />);
    fireEvent.click(await screen.findByRole('button', { name: '生成邀请链接' }));
    const link = await screen.findByLabelText('一次性邀请链接');
    expect(link).toHaveValue(`${location.origin}/#invite=one-time-secret`);
    expect(api.createInvitation).toHaveBeenCalledWith('c', { requestId: expect.any(String), expectedVersion: 2 });
    fireEvent.click(screen.getByRole('button', { name: '生成邀请链接' }));
    await screen.findByText(/邀请链接仅首次返回/);
    expect(screen.queryByLabelText('一次性邀请链接')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '移除 guest' }));
    await screen.findByText(/已刷新成员与邀请/);
    expect(api.members.mock.calls.length).toBeGreaterThan(2);
  });
  it('previews an anonymous fragment then registers, explicitly logs in, clears fragment and opens joined campaign', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), previewInvitation: vi.fn().mockResolvedValue({ campaignId: 'c', campaignName: '新冒险', expiresAt: '2026-10-25T00:00:00Z' }), registerInvitation: vi.fn().mockResolvedValue({ username: 'new_user', campaignId: 'c' }), login: vi.fn().mockResolvedValue({ username: 'new_user' }) });
    render(<App api={api as unknown as Api} />);
    await screen.findByText('受邀加入「新冒险」');
    fireEvent.change(screen.getByLabelText('新账号'), { target: { value: 'new_user' } });
    fireEvent.change(screen.getByLabelText('新密码'), { target: { value: 'safe-password-123' } });
    fireEvent.click(screen.getByRole('button', { name: '注册并加入战役' }));
    await screen.findByText('空白场景');
    expect(api.registerInvitation).toHaveBeenCalledWith('secret', 'new_user', 'safe-password-123');
    expect(api.login).toHaveBeenCalledWith('new_user', 'safe-password-123');
    expect(location.hash).toBe('');
  });
  it('logs an existing account in before accepting and selects the invited campaign', async () => {
    window.history.replaceState(null, '', '/#invite=secret');
    const api = setup({ me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), previewInvitation: vi.fn().mockResolvedValue({ campaignId: 'c', campaignName: '新冒险', expiresAt: '2026-10-25T00:00:00Z' }), acceptInvitation: vi.fn().mockResolvedValue({ campaignId: 'c' }), login: vi.fn().mockResolvedValue({ username: 'existing' }) });
    render(<App api={api as unknown as Api} />);
    fireEvent.change(await screen.findByLabelText('账号'), { target: { value: 'existing' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'existing-password' } });
    fireEvent.click(screen.getByRole('button', { name: '登录并接受邀请' }));
    await screen.findByText('空白场景');
    expect(api.acceptInvitation).toHaveBeenCalledWith('secret');
    expect(location.hash).toBe('');
  });
});
