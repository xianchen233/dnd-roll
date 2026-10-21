import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { Api, ApiError } from './api';
import { CharacterWorkspace } from './CharacterWorkspace';
import { CharacterFields } from './CharacterFields';
import { emptySheet, type CharacterDetail } from './characters';
const detail: CharacterDetail = { id: 'hero', version: 4, canEdit: true, canManage: false, sheet: emptySheet('英雄'), calculations: { initiative: { baseValue: 0, value: 3, components: [{ label: 'DEX', value: 0, source: { book: 'SRD', revision: '5.2', rules: '5R', detail: 'formula' } }], overrideReason: 'DM ruling' } }, expiredConditionIds: [] };
const listing = { campaignVersion: 2, characters: [{ id: 'hero', name: '英雄', avatar: '', version: 4, canEdit: true, canReadFull: true, health: 'UNHURT', ac: 10, speed: 30, conditions: [] }] };
function setup(extra = {}) { return { characters: vi.fn().mockResolvedValue(listing), character: vi.fn().mockResolvedValue(detail), createCharacter: vi.fn().mockResolvedValue({ id: 'hero', version: 0 }), updateCharacter: vi.fn().mockResolvedValue({ id: 'hero', version: 5 }), characterHistory: vi.fn().mockResolvedValue([{ version: 0, actor: 'owner', kind: 'CREATE', reason: 'created', at: '2026-10-20T00:00:00Z', before: null, after: detail }]), ...extra }; }
function props(api: ReturnType<typeof setup>) { return { api: api as unknown as Api, campaignId: 'c', isDm: false, revision: 0, onDenied: vi.fn() }; }
function deferred<T>() { let resolve!: (v: T) => void, reject!: (e: Error) => void; const promise = new Promise<T>((r, j) => { resolve = r; reject = j; }); return { promise, resolve, reject }; }
async function click(name: string) { const b = await screen.findByRole('button', { name }); await waitFor(() => expect(b).toBeEnabled()); fireEvent.click(b); }
async function open() { await click('打开角色 英雄'); await screen.findByLabelText('角色名称'); }
describe('complete character workspace', () => {
  it('creates an independent six-group sheet with server campaign version', async () => {
    const api = setup(); render(<CharacterWorkspace {...props(api)} />); await click('新建角色');
    fireEvent.change(await screen.findByLabelText('角色名称'), { target: { value: '第二角色' } }); await click('创建角色');
    expect(api.createCharacter).toHaveBeenCalledWith('c', expect.objectContaining({ expectedVersion: 2, sheet: expect.objectContaining({ name: '第二角色', resources: [], skills: expect.any(Array), saves: expect.any(Array), equipment: [], conditions: [] }) }));
  });
  it('retains the original edit version through background refresh and shows calculation sources', async () => {
    const api = setup(), p = props(api), view = render(<CharacterWorkspace {...p} />); await open();
    fireEvent.change(screen.getByLabelText('角色名称'), { target: { value: '草稿' } }); api.character.mockResolvedValue({ ...detail, version: 9 }); view.rerender(<CharacterWorkspace {...p} revision={1} />);
    await waitFor(() => expect(api.character).toHaveBeenCalledTimes(2)); expect(screen.getByLabelText('角色名称')).toHaveValue('草稿');
    await click('计算与来源'); expect(await screen.findByText('DM ruling')).toBeInTheDocument();
    await click('保存角色卡'); expect(api.updateCharacter.mock.calls[0][2]).toMatchObject({ expectedVersion: 4, sheet: { name: '草稿' } });
  });
  it('retries the exact uncertain body and requires a fresh read before synchronization', async () => {
    const read = deferred<CharacterDetail>(), api = setup({ updateCharacter: vi.fn().mockRejectedValueOnce(new Error('lost response')).mockResolvedValue({ id: 'hero', version: 5 }) }); render(<CharacterWorkspace {...props(api)} />); await open();
    fireEvent.change(screen.getByLabelText('角色名称'), { target: { value: '草稿' } }); await click('保存角色卡'); await click('重试同一角色操作');
    expect(api.updateCharacter.mock.calls[0][2]).toEqual(api.updateCharacter.mock.calls[1][2]);
    await screen.findByText('角色请求已确认；当前数据已从服务器刷新。');
    // The manual refresh and a post-save background refresh may overlap; all
    // reads during this simulated outage fail rather than one falling back to success.
    api.character.mockReturnValue(read.promise); await click('刷新角色卡'); expect(screen.queryByText('角色请求已确认；当前数据已从服务器刷新。')).not.toBeInTheDocument();
    await act(async () => read.reject(new Error('offline'))); expect(await screen.findByText(/刷新失败/)).toBeInTheDocument();
  });
  it('clears private data and pending commands when full permission is revoked', async () => {
    const old = deferred<CharacterDetail>(), api = setup(), p = props(api), view = render(<CharacterWorkspace {...p} />); await open();
    api.character.mockReturnValueOnce(old.promise); view.rerender(<CharacterWorkspace {...p} revision={1} />);
    api.characters.mockResolvedValue({ ...listing, characters: [{ ...listing.characters[0], canReadFull: false, canEdit: false }] }); view.rerender(<CharacterWorkspace {...p} revision={2} />);
    await waitFor(() => expect(screen.queryByLabelText('角色名称')).not.toBeInTheDocument()); await act(async () => old.resolve(detail)); expect(screen.queryByLabelText('角色名称')).not.toBeInTheDocument();
  });
  it('drops late responses across campaign switches and denies membership independently', async () => {
    const old = deferred<unknown>(), api = setup(), p = props(api), view = render(<CharacterWorkspace {...p} />); await open(); api.characters.mockReturnValueOnce(old.promise); view.rerender(<CharacterWorkspace {...p} revision={1} />);
    api.characters.mockRejectedValueOnce(new ApiError(403, 'revoked')); view.rerender(<CharacterWorkspace {...p} revision={2} />); await waitFor(() => expect(p.onDenied).toHaveBeenCalled());
    await act(async () => old.resolve(listing)); expect(screen.queryByLabelText('角色名称')).not.toBeInTheDocument();
  });
  it('rejects an invalid import preview without rendering untrusted object fields', async () => {
    const api = setup(); render(<CharacterWorkspace {...props(api)} />); await click('导入角色');
    expect(() => fireEvent.change(screen.getByLabelText('角色 JSON'), { target: { value: JSON.stringify({ format: 'dnd-roll.character', formatVersion: 1, rules: '5R', sheet: { name: 'Bad', classes: { name: { bad: true } } } }) } })).not.toThrow();
    expect(screen.getByRole('alert')).toBeInTheDocument(); expect(screen.getByRole('button', { name: '确认导入角色' })).toBeDisabled();
  });
  it('refreshes the target campaign version after an import conflict before a new command', async () => {
    const importer = vi.fn().mockRejectedValueOnce(new ApiError(409, 'stale')).mockResolvedValue({ id: 'hero', version: 0 }), api = setup({ importCharacter: importer });
    render(<CharacterWorkspace {...props(api)} />); await click('导入角色');
    fireEvent.change(screen.getByLabelText('角色 JSON'), { target: { value: JSON.stringify({ format: 'dnd-roll.character', formatVersion: 1, rules: '5R', sheet: emptySheet('Import') }) } });
    api.characters.mockResolvedValue({ ...listing, campaignVersion: 9 }); await click('确认导入角色'); await screen.findByRole('alert');
    await waitFor(() => expect(api.characters).toHaveBeenCalledTimes(2)); await click('确认导入角色'); expect(importer.mock.calls[1][1].expectedVersion).toBe(9); expect(importer.mock.calls[0][1].requestId).not.toBe(importer.mock.calls[1][1].requestId);
  });
  it('selects a spellcasting source by name while retaining its stable identity', () => {
    const sheet = emptySheet('Caster'), source = sheet.combat.source, onChange = vi.fn();
    sheet.casting = [{ id: 'wizard', name: '法师施法', ability: 'INT', attackBonus: 0, dcBonus: 0, source }];
    sheet.spells = [{ id: 'spell', name: '测试法术', level: 1, prepared: true, castingId: '', description: '', source }];
    render(<CharacterFields sheet={sheet} group={3} disabled={false} isDm={false} onChange={onChange} />);
    fireEvent.change(screen.getByRole('combobox', { name: '法术 · 测试法术 · 施法来源' }), { target: { value: 'wizard' } });
    expect(onChange.mock.calls[0][0].spells[0].castingId).toBe('wizard');
  });
  it('offers named same-campaign token placements with their actual versions', async () => {
    const linker = vi.fn().mockResolvedValue({ id: 'hero', version: 5 }), api = setup({ characterAccess: vi.fn().mockResolvedValue({ version: 4, controllers: ['player'], hidden: false, shareFull: false, importReview: false }), members: vi.fn().mockResolvedValue({ campaignVersion: 2, members: [{ username: 'player', role: 'PLAYER' }] }), characterPlacements: vi.fn().mockResolvedValue([]), scenes: vi.fn().mockResolvedValue({ scenes: [{ id: 'scene', name: '营地' }] }), tokens: vi.fn().mockResolvedValue({ tokens: [{ id: 'token', name: '守卫', version: 7, x: 1, y: 2 }] }), linkCharacter: linker });
    render(<CharacterWorkspace {...props(api)} isDm={true} />); await open(); await click('DM角色授权与棋子关联');
    const select = await screen.findByRole('combobox', { name: '关联棋子' }); fireEvent.change(select, { target: { value: 'token' } }); await click('关联已有棋子'); expect(linker).toHaveBeenCalledWith('c', 'hero', expect.objectContaining({ expectedVersion: 4, tokenId: 'token', tokenVersion: 7 }));
  });
  it('discards a conflicted placement form before reading new token and character versions', async () => {
    const linker = vi.fn().mockRejectedValueOnce(new ApiError(409, 'token moved')).mockResolvedValue({ id: 'hero', version: 5 }), tokens = vi.fn().mockResolvedValueOnce({ tokens: [{ id: 'token', name: '守卫', version: 7, x: 1, y: 2 }] }).mockResolvedValue({ tokens: [{ id: 'token', name: '守卫', version: 8, x: 2, y: 2 }] }), api = setup({ characterAccess: vi.fn().mockResolvedValue({ version: 4, controllers: [], hidden: false, shareFull: false, importReview: false }), members: vi.fn().mockResolvedValue({ campaignVersion: 2, members: [] }), characterPlacements: vi.fn().mockResolvedValue([]), scenes: vi.fn().mockResolvedValue({ scenes: [{ id: 'scene', name: '营地' }] }), tokens, linkCharacter: linker });
    render(<CharacterWorkspace {...props(api)} isDm={true} />); await open(); await click('DM角色授权与棋子关联'); fireEvent.change(await screen.findByRole('combobox', { name: '关联棋子' }), { target: { value: 'token' } }); await click('关联已有棋子'); await screen.findByRole('alert');
    await waitFor(() => expect(screen.queryByRole('combobox', { name: '关联棋子' })).not.toBeInTheDocument()); await click('DM角色授权与棋子关联'); fireEvent.change(await screen.findByRole('combobox', { name: '关联棋子' }), { target: { value: 'token' } }); await click('关联已有棋子'); expect(linker.mock.calls[1][2].tokenVersion).toBe(8);
  });
  it('keeps a newly acknowledged character selected when a pre-ack list finishes in the same batch', async () => {
    const posted = deferred<unknown>(), oldList = deferred<unknown>(), api = setup({ createCharacter: vi.fn().mockReturnValue(posted.promise), character: vi.fn().mockResolvedValue({ ...detail, id: 'new', sheet: emptySheet('新角色') }) });
    api.characters.mockResolvedValue({ campaignVersion: 2, characters: [] }); const p = props(api), view = render(<CharacterWorkspace {...p} />); await click('新建角色'); fireEvent.change(screen.getByLabelText('角色名称'), { target: { value: '新角色' } });
    api.characters.mockReturnValueOnce(oldList.promise).mockResolvedValue({ ...listing, characters: [{ ...listing.characters[0], id: 'new', name: '新角色' }] }); view.rerender(<CharacterWorkspace {...p} revision={1} />); await click('创建角色');
    await act(async () => { posted.resolve({ id: 'new', version: 0 }); await Promise.resolve(); oldList.resolve({ campaignVersion: 2, characters: [] }); });
    await screen.findByRole('heading', { name: '新角色' }); expect(screen.getByLabelText('角色名称')).toHaveValue('新角色');
  });
});
