import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Tabletop } from './Tabletop';
import type { Api } from './api';
import { ApiError } from './api';
import { withSafeMapReads } from './test-map-api';

vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div>地图</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
vi.mock('./CharacterWorkspace', () => ({ CharacterWorkspace: () => <label>未提交角色草稿<input /></label> }));
vi.mock('./DiceWorkspace', () => ({ DiceWorkspace: () => <p>掷骰面板</p> }));

describe('phone workspace navigation', () => {
  it('clears phone drafts on campaign switch and disables all tabs after access denial', async () => {
    const api = withSafeMapReads({ scene: vi.fn().mockImplementation(async (campaignId: string) => ({ id: 's', campaignId, name: '林地', width: 24, height: 16, gridSize: 48, version: 0, tokens: [] })) });
    const props = { api: api as unknown as Api, identity: { username: 'player' }, campaigns: [{ id: 'c', name: '长夜', role: 'PLAYER' as const }, { id: 'd', name: '黎明', role: 'PLAYER' as const }], onCampaign: vi.fn(), onRefresh: vi.fn() };
    const view = render(<Tabletop {...props} campaignId="c" />);
    await screen.findByRole('heading', { name: '林地' });
    fireEvent.click(screen.getByRole('tab', { name: '角色卡' }));
    fireEvent.change(await screen.findByLabelText('未提交角色草稿'), { target: { value: '旧团秘密草稿' } });
    view.rerender(<Tabletop {...props} campaignId="d" />);
    await waitFor(() => expect(screen.queryByLabelText('未提交角色草稿')).not.toBeInTheDocument());
    expect(screen.getByRole('tab', { name: '地图' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.click(screen.getByRole('tab', { name: '角色卡' }));
    expect(await screen.findByLabelText('未提交角色草稿')).toHaveValue('');
    api.scene.mockRejectedValue(new ApiError(403, '已撤销成员'));
    fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await waitFor(() => expect(screen.queryByLabelText('未提交角色草稿')).not.toBeInTheDocument());
    for (const tab of screen.getAllByRole('tab')) expect(tab).toBeDisabled();
  });
  it('selects a single phone workspace and retains a mounted draft across tabs', async () => {
    const api = withSafeMapReads({ scene: vi.fn().mockResolvedValue({ id: 's', campaignId: 'c', name: '林地', width: 24, height: 16, gridSize: 48, version: 0, tokens: [] }) }) as unknown as Api;
    render(<Tabletop api={api} identity={{ username: 'player' }} campaigns={[{ id: 'c', name: '长夜', role: 'PLAYER' }]} campaignId="c" onCampaign={vi.fn()} onRefresh={vi.fn()} />);
    await screen.findByRole('heading', { name: '林地' });
    const nav = screen.getByRole('tablist', { name: '手机工作区' });
    fireEvent.click(within(nav).getByRole('tab', { name: '角色卡' }));
    expect(within(nav).getByRole('tab', { name: '角色卡' })).toHaveAttribute('aria-selected', 'true');
    fireEvent.change(await screen.findByLabelText('未提交角色草稿'), { target: { value: '保留草稿' } });
    fireEvent.click(within(nav).getByRole('tab', { name: '掷骰' }));
    expect(await screen.findByText('掷骰面板')).toBeInTheDocument();
    expect(screen.getByLabelText('未提交角色草稿')).toHaveValue('保留草稿');
    expect(screen.getByLabelText('未提交角色草稿').closest('[data-mobile-panel]')).toHaveAttribute('data-mobile-visible', 'false');
    fireEvent.click(within(nav).getByRole('tab', { name: '角色卡' }));
    expect(screen.getByLabelText('未提交角色草稿')).toHaveValue('保留草稿');
    fireEvent.keyDown(within(nav).getByRole('tab', { name: '角色卡' }), { key: 'ArrowRight' });
    await waitFor(() => expect(within(nav).getByRole('tab', { name: '掷骰' })).toHaveFocus());
    expect(within(nav).getByRole('tab', { name: '掷骰' })).toHaveAttribute('aria-selected', 'true');
  });
});
