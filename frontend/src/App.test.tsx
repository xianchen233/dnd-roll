import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { App } from './App';
import { Api, ApiError } from './api';
vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div data-testid="pixi-mock">地图渲染替身</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
describe('tabletop interface', () => {
  it('renders only permission-filtered server tokens and disables uncontrolled units', async () => {
    const api = { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'player' }), campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'PLAYER' }]), scene: vi.fn().mockResolvedValue({ id: 's', campaignId: 'c', name: '林地', width: 24, height: 16, gridSize: 48, version: 0, tokens: [{ id: 'a', name: '游侠', x: 2, y: 3, version: 0, canMove: true }, { id: 'b', name: '守卫', x: 8, y: 4, version: 0, canMove: false }] }) };
    render(<App api={api as unknown as Api} />);
    expect(await screen.findByRole('button', { name: /游侠/ })).toBeEnabled();
    expect(screen.getByRole('button', { name: /守卫/ })).toBeDisabled();
    expect(screen.queryByText('隐藏敌人')).not.toBeInTheDocument();
    expect(screen.getByText('player')).toBeInTheDocument();
    expect(screen.getByText('玩家')).toBeInTheDocument();
  });
  it('shows a login error and does not enter a fake local campaign', async () => {
    const api = { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), login: vi.fn().mockRejectedValue(new Error('账号或密码错误')) };
    render(<App api={api as unknown as Api} />);
    await screen.findByLabelText('账号');
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'player' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'runtime-password' } });
    fireEvent.click(screen.getByRole('button', { name: '进入冒险' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('账号或密码错误'));
    expect(screen.queryByTestId('pixi-mock')).not.toBeInTheDocument();
  });
});
