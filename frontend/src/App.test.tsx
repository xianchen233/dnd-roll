import { withSafeMapReads } from './test-map-api';
import React from 'react';
import { act, render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { App } from './App';
import { Api, ApiError } from './api';
vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div data-testid="pixi-mock">地图渲染替身</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
describe('tabletop interface', () => {
  it('opens the team note workspace for a current player',async()=>{
    const api={getCsrf:vi.fn().mockResolvedValue({}),me:vi.fn().mockResolvedValue({username:'player'}),campaigns:vi.fn().mockResolvedValue([{id:'c',name:'长夜',role:'PLAYER'}]),scene:vi.fn().mockResolvedValue({id:'s',campaignId:'c',name:'林地',width:24,height:16,gridSize:48,version:0,tokens:[]}),notes:vi.fn().mockResolvedValue({campaignVersion:0,notes:[],next:''})};
    render(<App api={withSafeMapReads(api) as unknown as Api}/>);const button=await screen.findByRole('button',{name:'打开团队笔记与 DM 资料'});await waitFor(()=>expect(button).toBeEnabled());fireEvent.click(button);await screen.findByLabelText('笔记标题');expect(api.notes).toHaveBeenCalled();
  });
  it.each(['hidden', 'revoked'] as const)('clears selection and coordinates on same-scene %s and never restores them on regrant or a late movement receipt', async reason => {
    let resolve!: (value: unknown) => void;
    const posted = new Promise(yes => { resolve = yes; });
    const token = { id: 'a', name: '游侠', x: 2, y: 3, version: 0, canMove: true }, scene = { id: 's', campaignId: 'c', name: '林地', width: 24, height: 16, gridSize: 48, version: 0, tokens: [token] };
    const api = { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'player' }), campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'PLAYER' }]), scene: vi.fn().mockResolvedValue(scene), tokens: vi.fn(), members: vi.fn(), move: vi.fn().mockReturnValue(posted) };
    render(<App api={withSafeMapReads(api) as unknown as Api} />); fireEvent.click(await screen.findByRole('button', { name: /游侠/ }));
    fireEvent.change(screen.getByLabelText('目标 X'), { target: { value: '4' } }); fireEvent.change(screen.getByLabelText('目标 Y'), { target: { value: '5' } }); fireEvent.click(screen.getByRole('button', { name: '确认移动' }));
    await waitFor(() => expect(api.move).toHaveBeenCalledTimes(1));
    api.scene.mockResolvedValue({ ...scene, version: 1, tokens: reason === 'hidden' ? [] : [{ ...token, version: 1, canMove: false }] }); fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await screen.findByRole('heading', { name: '未选择单位' }); expect(screen.getByLabelText('目标 X')).toHaveValue(null); expect(screen.getByLabelText('目标 Y')).toHaveValue(null);
    api.scene.mockResolvedValue({ ...scene, version: 2, tokens: [{ ...token, version: 2 }] }); fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await waitFor(() => expect(screen.getByRole('button', { name: /游侠/ })).toBeEnabled());
    await act(async () => { resolve({ ...token, x: 4, y: 5 }); });
    expect(screen.getByRole('heading', { name: '未选择单位' })).toBeInTheDocument(); expect(screen.getByRole('button', { name: '确认移动' })).toBeDisabled(); expect(api.scene).toHaveBeenCalledTimes(3);
    expect(api.tokens).not.toHaveBeenCalled(); expect(api.members).not.toHaveBeenCalled();
  });
  it('renders only permission-filtered server tokens and disables uncontrolled units', async () => {
    const api = { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'player' }), campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'PLAYER' }]), scene: vi.fn().mockResolvedValue({ id: 's', campaignId: 'c', name: '林地', width: 24, height: 16, gridSize: 48, version: 0, tokens: [{ id: 'a', name: '游侠', x: 2, y: 3, version: 0, canMove: true }, { id: 'b', name: '守卫', x: 8, y: 4, version: 0, canMove: false }] }) };
    render(<App api={withSafeMapReads(api) as unknown as Api} />);
    expect(await screen.findByRole('button', { name: /游侠/ })).toBeEnabled();
    expect(screen.getByRole('button', { name: /守卫/ })).toBeDisabled();
    expect(screen.queryByText('隐藏敌人')).not.toBeInTheDocument();
    expect(screen.getByText('player')).toBeInTheDocument();
    expect(screen.getByText('玩家')).toBeInTheDocument();
  });
  it('shows a login error and does not enter a fake local campaign', async () => {
    const api = { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockRejectedValue(new ApiError(401, '未登录')), login: vi.fn().mockRejectedValue(new Error('账号或密码错误')) };
    render(<App api={withSafeMapReads(api) as unknown as Api} />);
    await screen.findByLabelText('账号');
    fireEvent.change(screen.getByLabelText('账号'), { target: { value: 'player' } });
    fireEvent.change(screen.getByLabelText('密码'), { target: { value: 'runtime-password' } });
    fireEvent.click(screen.getByRole('button', { name: '进入冒险' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('账号或密码错误'));
    expect(screen.queryByTestId('pixi-mock')).not.toBeInTheDocument();
  });
});
