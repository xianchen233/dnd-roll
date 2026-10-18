import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { Api, ApiError } from './api';
import { AssetManagement } from './AssetManagement';
vi.mock('./MapCanvas', () => ({ MapCanvas: () => <div>地图替身</div> }));
vi.mock('./realtime', () => ({ connectRealtime: () => () => {} }));
const asset = { id: 'a', mediaType: 'image/png', width: 2, height: 2, byteSize: 70 };
const map = { assetId: 'a', gridSize: 64, offsetX: -2, offsetY: 3, distancePerCell: 5 };
const scene = { id: 's', campaignId: 'c', name: '营地', width: 24, height: 16, gridSize: 48, version: 3, tokens: [] };
function setup(extra: object = {}) { return { getCsrf: vi.fn().mockResolvedValue({}), me: vi.fn().mockResolvedValue({ username: 'dm' }), campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'DM' }]), scene: vi.fn().mockResolvedValue(scene), scenes: vi.fn().mockResolvedValue({ campaignVersion: 2, currentSceneId: 's', scenes: [{ id: 's', name: '营地' }] }), members: vi.fn().mockResolvedValue({ campaignVersion: 2, members: [] }), invitations: vi.fn().mockResolvedValue({ campaignVersion: 2, invitations: [] }), assets: vi.fn().mockResolvedValue({ campaignVersion: 2, assets: [asset] }), sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map: null }), assetContent: vi.fn().mockResolvedValue(new Blob(['png'], { type: 'image/png' })), uploadAsset: vi.fn().mockResolvedValue({ asset, campaignVersion: 3 }), saveSceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 4, map }), ...extra }; }
function deferred<T>() { let resolve!: (value: T) => void, reject!: (error: Error) => void; const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; }); return { promise, resolve, reject }; }
function mount(api: ReturnType<typeof setup>) { render(<App api={api as unknown as Api} />); }
async function upload() { const file = new File(['png'], 'private-name.png', { type: 'image/png' }); fireEvent.change(await screen.findByLabelText('上传地图文件'), { target: { files: [file] } }); fireEvent.click(screen.getByRole('button', { name: '上传素材' })); return file; }
beforeEach(() => { Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: vi.fn().mockReturnValue('blob:private') }); Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: vi.fn() }); });
describe('private DM asset preparation', () => {
  it.each(['command-first', 'revision-first'] as const)('confirms from the latest post-acknowledgment read when refreshes overlap: %s', async order => {
    const posted = deferred<unknown>(), commandRead = deferred<unknown>(), revisionRead = deferred<unknown>();
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map }), saveSceneMap: vi.fn().mockReturnValue(posted.promise) });
    const props = { api: api as unknown as Api, campaignId: 'c', sceneId: 's', revision: 3, onChanged: vi.fn(), onDenied: vi.fn() };
    const view = render(<AssetManagement {...props} />);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64));
    fireEvent.change(screen.getByLabelText('图片每格像素'), { target: { value: '80' } }); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await waitFor(() => expect(api.saveSceneMap).toHaveBeenCalledTimes(1));
    api.sceneMap.mockReturnValueOnce(commandRead.promise).mockReturnValueOnce(revisionRead.promise);
    await act(async () => { posted.resolve({ sceneId: 's', sceneVersion: 4, map: { ...map, gridSize: 80 } }); });
    await waitFor(() => expect(api.sceneMap).toHaveBeenCalledTimes(3));
    view.rerender(<AssetManagement {...props} revision={4} />);
    await waitFor(() => expect(api.sceneMap).toHaveBeenCalledTimes(4));
    expect(screen.queryByText('地图请求已确认；当前配置已从服务器刷新。')).not.toBeInTheDocument();
    if (order === 'command-first') {
      await act(async () => { commandRead.resolve({ sceneId: 's', sceneVersion: 4, map: { ...map, gridSize: 80 } }); });
      expect(screen.queryByText('地图请求已确认；当前配置已从服务器刷新。')).not.toBeInTheDocument();
    }
    // A later edit is authoritative even when the receipt/earlier refresh differs.
    await act(async () => { revisionRead.resolve({ sceneId: 's', sceneVersion: 5, map: { ...map, gridSize: 96 } }); });
    await screen.findByText('地图请求已确认；当前配置已从服务器刷新。');
    expect(screen.getByLabelText('图片每格像素')).toHaveValue(96);
    if (order === 'revision-first') await act(async () => { commandRead.resolve({ sceneId: 's', sceneVersion: 4, map: { ...map, gridSize: 80 } }); });
    expect(screen.getByLabelText('图片每格像素')).toHaveValue(96); expect(api.saveSceneMap).toHaveBeenCalledTimes(1);
  });
  it.each(['success', 'failure'] as const)('keeps a newer failed refresh unsynchronized after older %s and recovers without another POST', async olderOutcome => {
    const posted = deferred<unknown>(), commandRead = deferred<unknown>(), revisionRead = deferred<unknown>();
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map }), saveSceneMap: vi.fn().mockReturnValue(posted.promise) });
    const props = { api: api as unknown as Api, campaignId: 'c', sceneId: 's', revision: 3, onChanged: vi.fn(), onDenied: vi.fn() };
    const view = render(<AssetManagement {...props} />);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64)); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await waitFor(() => expect(api.saveSceneMap).toHaveBeenCalledTimes(1)); api.sceneMap.mockReturnValueOnce(commandRead.promise).mockReturnValueOnce(revisionRead.promise);
    await act(async () => { posted.resolve({ sceneId: 's', sceneVersion: 4, map }); }); view.rerender(<AssetManagement {...props} revision={4} />);
    await act(async () => { revisionRead.reject(new ApiError(503, 'newest unavailable')); });
    await screen.findByText('请求已确认；刷新失败，当前配置尚未同步。请刷新素材与地图。');
    await act(async () => { if (olderOutcome === 'success') commandRead.resolve({ sceneId: 's', sceneVersion: 4, map }); else commandRead.reject(new Error('older offline')); });
    expect(screen.queryByText('地图请求已确认；当前配置已从服务器刷新。')).not.toBeInTheDocument();
    expect(screen.getByText('请求已确认；刷新失败，当前配置尚未同步。请刷新素材与地图。')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '重试同一素材操作' })).not.toBeInTheDocument();
    api.sceneMap.mockResolvedValue({ sceneId: 's', sceneVersion: 5, map: { ...map, gridSize: 96 } });
    fireEvent.click(screen.getByRole('button', { name: '刷新素材与地图' })); await screen.findByText('地图请求已确认；当前配置已从服务器刷新。');
    expect(screen.queryByText('请求已确认；刷新失败，当前配置尚未同步。请刷新素材与地图。')).not.toBeInTheDocument();
    expect(screen.getByLabelText('图片每格像素')).toHaveValue(96); expect(api.saveSceneMap).toHaveBeenCalledTimes(1);
  });
  it('ignores an older failed read after a newer authoritative read confirms the map', async () => {
    const posted = deferred<unknown>(), commandRead = deferred<unknown>(), revisionRead = deferred<unknown>();
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map }), saveSceneMap: vi.fn().mockReturnValue(posted.promise) });
    const props = { api: api as unknown as Api, campaignId: 'c', sceneId: 's', revision: 3, onChanged: vi.fn(), onDenied: vi.fn() };
    const view = render(<AssetManagement {...props} />);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64)); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await waitFor(() => expect(api.saveSceneMap).toHaveBeenCalledTimes(1)); api.sceneMap.mockReturnValueOnce(commandRead.promise).mockReturnValueOnce(revisionRead.promise);
    await act(async () => { posted.resolve({ sceneId: 's', sceneVersion: 4, map }); }); view.rerender(<AssetManagement {...props} revision={4} />);
    await act(async () => { revisionRead.resolve({ sceneId: 's', sceneVersion: 5, map: { ...map, gridSize: 96 } }); });
    await screen.findByText('地图请求已确认；当前配置已从服务器刷新。'); await act(async () => { commandRead.reject(new ApiError(503, 'older unavailable')); });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument(); expect(screen.getByLabelText('图片每格像素')).toHaveValue(96);
    expect(screen.getByText('地图请求已确认；当前配置已从服务器刷新。')).toBeInTheDocument(); expect(api.saveSceneMap).toHaveBeenCalledTimes(1);
  });
  it('does not let a pre-acknowledgment read confirm a map even if it resolves after the POST', async () => {
    const posted = deferred<unknown>(), beforeAck = deferred<unknown>(), afterAck = deferred<unknown>();
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 3, map }), saveSceneMap: vi.fn().mockReturnValue(posted.promise) });
    const props = { api: api as unknown as Api, campaignId: 'c', sceneId: 's', revision: 3, onChanged: vi.fn(), onDenied: vi.fn() };
    const view = render(<AssetManagement {...props} />);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64)); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await waitFor(() => expect(api.saveSceneMap).toHaveBeenCalledTimes(1)); api.sceneMap.mockReturnValueOnce(beforeAck.promise).mockReturnValueOnce(afterAck.promise);
    view.rerender(<AssetManagement {...props} revision={4} />);
    await act(async () => { posted.resolve({ sceneId: 's', sceneVersion: 4, map }); beforeAck.resolve({ sceneId: 's', sceneVersion: 3, map }); });
    expect(screen.queryByText('地图请求已确认；当前配置已从服务器刷新。')).not.toBeInTheDocument();
    await act(async () => { afterAck.resolve({ sceneId: 's', sceneVersion: 4, map: null }); }); await screen.findByText('地图请求已确认；当前配置已从服务器刷新。');
    expect(screen.getByLabelText('当前场景地图素材')).toHaveValue(''); expect(api.saveSceneMap).toHaveBeenCalledTimes(1);
  });
  it('gives the asset chooser an exact accessible name independent of its option text', async () => {
    const api = setup(); mount(api); await screen.findByRole('button', { name: '预览 a' });
    expect(screen.getByRole('combobox', { name: '当前场景地图素材' })).toHaveAccessibleName('当前场景地图素材');
  });
  it.each([new Error('offline'), new ApiError(503, 'storage unavailable')])('retries the same file and versioned upload after uncertainty %s', async error => {
    const api = setup({ uploadAsset: vi.fn().mockRejectedValueOnce(error).mockResolvedValueOnce({ asset, campaignVersion: 3 }) }); mount(api);
    const file = await upload(); fireEvent.click(await screen.findByRole('button', { name: '重试同一素材操作' }));
    await screen.findByText('上传请求已确认；素材列表已刷新。');
    expect(api.uploadAsset.mock.calls[0]).toEqual(api.uploadAsset.mock.calls[1]);
    expect(api.uploadAsset.mock.calls[1][1]).toBe(api.uploadAsset.mock.calls[0][1]); expect(api.uploadAsset.mock.calls[1][2]).toBe(file);
    expect(api.uploadAsset.mock.calls[0]).toEqual(['c', { requestId: expect.any(String), expectedVersion: 2 }, file]);
  });
  it('refreshes shared campaign management versions after an asset upload', async () => {
    const api = setup(); mount(api); await screen.findByLabelText('上传地图文件');
    await waitFor(() => expect(api.members).toHaveBeenCalledTimes(1)); await upload(); await screen.findByText('上传请求已确认；素材列表已刷新。');
    await waitFor(() => expect(api.members.mock.calls.length).toBeGreaterThan(1));
    expect(api.scenes.mock.calls.length).toBeGreaterThan(1);
  });
  it('reads authoritative map after a historical receipt, preserving logical grid', async () => {
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, map }) }); mount(api);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64));
    fireEvent.change(screen.getByLabelText('图片每格像素'), { target: { value: '80' } });
    fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await screen.findByText('地图请求已确认；当前配置已从服务器刷新。');
    expect(api.saveSceneMap).toHaveBeenCalledWith('c', 's', { requestId: expect.any(String), expectedVersion: 9, map: { ...map, gridSize: 80 } });
    expect(screen.getByLabelText('图片每格像素')).toHaveValue(64);
    expect(screen.getByText('24 × 16 方格')).toBeInTheDocument();
  });
  it('detaches through the current scene version', async () => {
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, map }) }); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: '移除地图配置' }));
    await waitFor(() => expect(api.saveSceneMap).toHaveBeenCalledWith('c', 's', { requestId: expect.any(String), expectedVersion: 9, map: null }));
  });
  it('refreshes conflict state and discards deterministic invalid requests', async () => {
    const api = setup({ uploadAsset: vi.fn().mockRejectedValueOnce(new ApiError(409, 'version')).mockRejectedValueOnce(new ApiError(400, 'invalid')) }); mount(api);
    await upload(); await screen.findByText('操作冲突，已刷新素材与地图配置。请检查后重新操作。');
    expect(screen.queryByRole('button', { name: '重试同一素材操作' })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '上传素材' })); await screen.findByText('invalid');
    expect(screen.queryByRole('button', { name: '重试同一素材操作' })).not.toBeInTheDocument();
    expect(api.uploadAsset.mock.calls[0][1].requestId).not.toBe(api.uploadAsset.mock.calls[1][1].requestId);
  });
  it('does not claim synchronization or repeat an acknowledged upload when refresh fails', async () => {
    const api = setup(); mount(api); await screen.findByLabelText('上传地图文件');
    api.uploadAsset.mockImplementation(async () => { api.assets.mockRejectedValue(new ApiError(503, 'refresh offline')); return { asset, campaignVersion: 3 }; });
    await upload(); await screen.findByText(/请求已确认；刷新失败，当前配置尚未同步/);
    expect(screen.queryByRole('button', { name: '重试同一素材操作' })).not.toBeInTheDocument(); expect(api.uploadAsset).toHaveBeenCalledTimes(1);
  });
  it.each([{ method: 'assets' as const, status: 401 }, { method: 'assets' as const, status: 403 }, { method: 'sceneMap' as const, status: 401 }, { method: 'sceneMap' as const, status: 403 }])('observes $method denial $status independently after the peer fails and isolates late content', async ({ method, status }) => {
    const api = setup(); mount(api); fireEvent.click(await screen.findByRole('button', { name: '预览 a' })); await screen.findByAltText('DM 私密地图预览');
    const denied = deferred<unknown>(), failed = deferred<unknown>(), content = deferred<Blob>();
    api[method].mockReturnValueOnce(denied.promise); api[method === 'assets' ? 'sceneMap' : 'assets'].mockReturnValueOnce(failed.promise);
    api.assetContent.mockReturnValueOnce(content.promise); fireEvent.click(screen.getByRole('button', { name: '预览 a' }));
    fireEvent.click(screen.getByRole('button', { name: '刷新素材与地图' })); await act(async () => { failed.reject(new Error('offline')); });
    await act(async () => { denied.reject(new ApiError(status, 'removed')); });
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    expect(screen.queryByAltText('DM 私密地图预览')).not.toBeInTheDocument(); expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:private');
    await act(async () => { content.resolve(new Blob(['secret'])); }); expect(screen.queryByAltText('DM 私密地图预览')).not.toBeInTheDocument();
  });
  it.each(['assets', 'sceneMap'] as const)('clears immediately when %s denies while its peer remains pending', async method => {
    const api = setup(); mount(api); fireEvent.click(await screen.findByRole('button', { name: '预览 a' })); await screen.findByAltText('DM 私密地图预览');
    const denied = deferred<unknown>(), peer = deferred<unknown>(); api[method].mockReturnValueOnce(denied.promise); api[method === 'assets' ? 'sceneMap' : 'assets'].mockReturnValueOnce(peer.promise);
    fireEvent.click(screen.getByRole('button', { name: '刷新素材与地图' })); await act(async () => { denied.reject(new ApiError(403, 'removed')); });
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    await act(async () => { peer.resolve(method === 'assets' ? { sceneId: 's', sceneVersion: 99, map } : { campaignVersion: 99, assets: [asset] }); });
    expect(screen.queryByRole('heading', { name: '地图备团' })).not.toBeInTheDocument();
  });
  it('revokes replaced/closed previews and temporary downloads; denied content clears all private state', async () => {
    const api = setup(); mount(api); fireEvent.click(await screen.findByRole('button', { name: '预览 a' })); await screen.findByAltText('DM 私密地图预览');
    fireEvent.click(screen.getByRole('button', { name: '预览 a' })); await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalledTimes(1));
    fireEvent.click(screen.getByRole('button', { name: '关闭预览' })); expect(screen.queryByAltText('DM 私密地图预览')).not.toBeInTheDocument(); expect(URL.revokeObjectURL).toHaveBeenCalledTimes(2);
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    fireEvent.click(screen.getByRole('button', { name: '下载 a' })); await waitFor(() => expect(URL.revokeObjectURL).toHaveBeenCalledTimes(3));
    expect(api.assetContent).toHaveBeenCalledTimes(3);
    api.assetContent.mockRejectedValue(new ApiError(403, 'removed')); fireEvent.click(screen.getByRole('button', { name: '预览 a' }));
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
  });
  it('keeps the same map body after 5xx even if background reads change', async () => {
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, map }), saveSceneMap: vi.fn().mockRejectedValueOnce(new ApiError(503, 'unknown')).mockResolvedValueOnce({ sceneId: 's', sceneVersion: 10, map }) }); mount(api);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64));
    fireEvent.change(screen.getByLabelText('图片每格像素'), { target: { value: '80' } }); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await screen.findByRole('button', { name: '重试同一素材操作' }); api.sceneMap.mockResolvedValue({ sceneId: 's', sceneVersion: 99, map });
    fireEvent.click(screen.getByRole('button', { name: '刷新素材与地图' })); await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64));
    fireEvent.click(screen.getByRole('button', { name: '重试同一素材操作' })); await screen.findByText('地图请求已确认；当前配置已从服务器刷新。');
    expect(api.saveSceneMap.mock.calls[0]).toEqual(api.saveSceneMap.mock.calls[1]); expect(api.saveSceneMap.mock.calls[1][2]).toMatchObject({ expectedVersion: 9, map: { ...map, gridSize: 80 } });
  });
  it('revokes preview and ignores pending upload after scene change', async () => {
    const pending = deferred<unknown>(), api = setup({ uploadAsset: vi.fn().mockReturnValue(pending.promise) }); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: '预览 a' })); await screen.findByAltText('DM 私密地图预览');
    await upload(); await waitFor(() => expect(api.uploadAsset).toHaveBeenCalledTimes(1));
    api.scene.mockResolvedValue({ ...scene, id: 'next', name: '密室' }); fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' }));
    await screen.findByRole('heading', { name: '密室' }); expect(screen.queryByAltText('DM 私密地图预览')).not.toBeInTheDocument();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:private');
    await act(async () => { pending.resolve({ asset, campaignVersion: 3 }); }); expect(screen.queryByText('上传请求已确认；素材列表已刷新。')).not.toBeInTheDocument();
  });
  it('clears private preview and pending upload on campaign change without allowing late success to restore them', async () => {
    const pending = deferred<unknown>(), api = setup({ campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'DM' }, { id: 'other', name: '另一团', role: 'PLAYER' }]), uploadAsset: vi.fn().mockReturnValue(pending.promise) }); mount(api);
    fireEvent.click(await screen.findByRole('button', { name: '预览 a' })); await screen.findByAltText('DM 私密地图预览'); await upload(); await waitFor(() => expect(api.uploadAsset).toHaveBeenCalledTimes(1));
    api.scene.mockResolvedValue({ ...scene, id: 'other-scene', campaignId: 'other', name: '另一地图' }); fireEvent.change(screen.getByLabelText('选择战役'), { target: { value: 'other' } });
    await screen.findByRole('heading', { name: '另一地图' }); expect(screen.queryByAltText('DM 私密地图预览')).not.toBeInTheDocument(); expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:private');
    await act(async () => { pending.resolve({ asset, campaignVersion: 99 }); }); expect(screen.queryByRole('heading', { name: '地图备团' })).not.toBeInTheDocument(); expect(screen.queryByRole('button', { name: '重试同一素材操作' })).not.toBeInTheDocument();
  });
  it.each([401, 403])('clears all campaign state on map command %i', async status => {
    const api = setup({ sceneMap: vi.fn().mockResolvedValue({ sceneId: 's', sceneVersion: 9, map }), saveSceneMap: vi.fn().mockRejectedValue(new ApiError(status, 'removed')) }); mount(api);
    await waitFor(() => expect(screen.getByLabelText('图片每格像素')).toHaveValue(64)); fireEvent.click(screen.getByRole('button', { name: '保存地图配置' }));
    await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument()); expect(screen.queryByRole('heading', { name: '地图备团' })).not.toBeInTheDocument(); expect(screen.queryByRole('heading', { name: '成员与邀请' })).not.toBeInTheDocument();
  });
  it('does not restore an acknowledged upload after independent scene authorization loss', async () => {
    const pending = deferred<unknown>(), api = setup({ uploadAsset: vi.fn().mockReturnValue(pending.promise) }); mount(api); await upload(); await waitFor(() => expect(api.uploadAsset).toHaveBeenCalledTimes(1));
    api.scene.mockRejectedValue(new ApiError(403, 'removed')); fireEvent.click(screen.getByRole('button', { name: '↻ 刷新场景' })); await waitFor(() => expect(screen.queryByText('地图替身')).not.toBeInTheDocument());
    await act(async () => { pending.resolve({ asset, campaignVersion: 99 }); }); expect(screen.queryByRole('heading', { name: '地图备团' })).not.toBeInTheDocument(); expect(screen.queryByText('上传请求已确认；素材列表已刷新。')).not.toBeInTheDocument();
  });
  it('never fetches authoring metadata or bytes for players', async () => {
    const api = setup({ campaigns: vi.fn().mockResolvedValue([{ id: 'c', name: '长夜', role: 'PLAYER' }]) }); mount(api); await screen.findByRole('heading', { name: '营地' });
    expect(api.assets).not.toHaveBeenCalled(); expect(api.sceneMap).not.toHaveBeenCalled(); expect(api.assetContent).not.toHaveBeenCalled();
    expect(screen.queryByRole('heading', { name: '地图备团' })).not.toBeInTheDocument();
  });
});
