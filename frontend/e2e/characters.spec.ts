import { randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { test as base, expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';
import { emptySheet, source, type Sheet } from '../src/characters';
type Sessions = { dm: Page; player: Page; dmContext: BrowserContext; playerContext: BrowserContext; errors: string[]; frames: string[]; sockets: { connected: number; closed: number } };
const test = base.extend<{ sessions: Sessions }>({ sessions: [async ({ browser, baseURL }, use) => {
  const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL }), dm = await dmContext.newPage(), player = await playerContext.newPage(), errors: string[] = [], frames: string[] = [], sockets = { connected: 0, closed: 0 };
  for (const page of [dm, player]) { page.on('pageerror', e => errors.push(e.message)); page.on('websocket', s => { s.on('framereceived', f => { const text = String(f.payload); frames.push(text); if (page === player && text.startsWith('CONNECTED')) sockets.connected++; }); if (page === player) s.on('close', () => sockets.closed++); }); }
  await player.addInitScript(() => { const sockets: WebSocket[] = []; Object.assign(window, { characterSockets: sockets }); window.WebSocket = new Proxy(window.WebSocket, { construct(target, args) { const socket = Reflect.construct(target, args) as WebSocket; sockets.push(socket); return socket; } }); });
  try { await use({ dm, player, dmContext, playerContext, errors, frames, sockets }); } finally { await Promise.all([dmContext.close(), playerContext.close()]); }
}, { timeout: 20000 }] });
function required(name: string) { const v = process.env[name]; if (!v?.trim()) throw Error(`Missing ${name}`); return v; }
async function read(r: APIRequestContext, path: string) { const response = await r.get(path); expect(response.ok()).toBeTruthy(); return response.json(); }
async function post(r: APIRequestContext, path: string, data: unknown, status = 200) { const csrf = await read(r, '/api/auth/csrf'); const response = await r.post(path, { headers: { [csrf.headerName]: csrf.token }, data }); expect(response.status()).toBe(status); return status === 204 ? undefined : response.json(); }
async function click(page: Page, name: string) { const b = page.getByRole('button', { name, exact: true }); await expect(b).toBeEnabled(); await b.click(); }
async function prepare(s: Sessions, card?: Sheet) {
  await post(s.dmContext.request, '/api/auth/login', { username: required('E2E_DM_USERNAME'), password: required('E2E_DM_PASSWORD') }); await post(s.playerContext.request, '/api/auth/login', { username: required('E2E_PLAYER_USERNAME'), password: required('E2E_PLAYER_PASSWORD') });
  const c = await post(s.dmContext.request, '/api/campaigns', { requestId: randomUUID(), name: `P03 ${randomUUID().slice(0, 8)}` }, 201), root = `/api/campaigns/${c.id}`, chars = `${root}/characters`;
  const invitation = await post(s.dmContext.request, `${root}/invitations`, { requestId: randomUUID(), expectedVersion: 0 }, 201); await post(s.playerContext.request, '/api/invitations/accept', { token: invitation.token });
  const listing = await read(s.playerContext.request, chars); let id: string | undefined;
  if (card) id = (await post(s.playerContext.request, chars, { requestId: randomUUID(), expectedVersion: listing.campaignVersion, sheet: card })).id;
  return { root, chars, campaignId: c.id as string, id: id!, playerUsername: required('E2E_PLAYER_USERNAME') };
}
async function enter(page: Page, campaignId: string, name?: string) { await page.goto('/'); await expect(page.getByRole('combobox', { name: '选择战役' })).not.toHaveValue(''); await page.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId); await expect(page.locator('.connection')).toHaveText('● 实时连接'); await click(page, '打开角色卡工作区'); if (name) { await click(page, `打开角色 ${name}`); await expect(page.getByLabel('角色名称', { exact: true })).toHaveValue(name); } }
function richSheet(): Sheet { const s = emptySheet(`英雄 ${randomUUID().slice(0, 6)}`); s.notes = `PRIVATE_STORY_${randomUUID()}`; s.combat = { ...s.combat, hp: 12, maxHp: 15, tempHp: 3 }; s.abilities.DEX = 15; s.abilities.WIS = 14; s.resources = [{ id: 'slots', name: '法术位', kind: 'SPELL_SLOT', current: 2, max: 2, recovery: '长休手动恢复', source: source() }]; s.conditions = [{ id: 'effect', name: '手动计时状态', description: '到期只提醒', icon: '◇', source: source(), remainingRounds: 1, expiresAt: null }]; return s; }
function safeFrames(frames: string[], ...secrets: string[]) { const messages = frames.filter(f => f.startsWith('MESSAGE')); expect(messages.length).toBeGreaterThan(0); for (const frame of messages) { const payload = JSON.parse(frame.slice(frame.indexOf('\n\n') + 2).replace(/\0+$/, '')); expect(Object.keys(payload).sort()).toEqual(['campaignId', 'type']); for (const secret of secrets) expect(frame).not.toContain(secret); } }

test('one player creates two independent full sheets through the UI', async ({ sessions: s }, info) => {
  const p = await prepare(s), a = `角色A ${randomUUID().slice(0, 4)}`, b = `角色B ${randomUUID().slice(0, 4)}`; await enter(s.player, p.campaignId);
  for (const name of [a, b]) { await click(s.player, '新建角色'); await s.player.getByLabel('角色名称', { exact: true }).fill(name); await click(s.player, '创建角色'); await expect(s.player.getByRole('heading', { name, exact: true })).toBeVisible(); }
  await click(s.player, `打开角色 ${a}`); await click(s.player, '消耗 生命'); await expect(s.player.getByText('角色请求已确认；当前数据已从服务器刷新。', { exact: true })).toBeVisible();
  const list = await read(s.playerContext.request, p.chars); expect(list.characters).toHaveLength(2); const first = list.characters.find((c: { name: string }) => c.name === a), second = list.characters.find((c: { name: string }) => c.name === b);
  expect((await read(s.playerContext.request, `${p.chars}/${first.id}`)).sheet.combat.hp).toBe(0); expect((await read(s.playerContext.request, `${p.chars}/${second.id}`)).sheet.combat.hp).toBe(1);
  await s.player.getByRole('region', { name: '角色卡工作区' }).scrollIntoViewIfNeeded(); await s.player.screenshot({ path: info.outputPath('player-independent-characters.png') }); expect(s.errors).toEqual([]);
});
test('six field groups preserve edits, calculated sources and immutable actor history', async ({ sessions: s }, info) => {
  const sheet = richSheet(), p = await prepare(s, sheet); await enter(s.player, p.campaignId, sheet.name);
  await s.player.getByRole('tab', { name: '属性与检定', exact: true }).click(); await s.player.getByLabel('六项属性 · DEX', { exact: true }).fill('9'); await click(s.player, '保存角色卡'); await expect(s.player.getByText('角色请求已确认；当前数据已从服务器刷新。', { exact: true })).toBeVisible();
  await click(s.player, '计算与来源'); await expect(s.player.getByText('initiative = -1', { exact: true })).toBeVisible(); await expect(s.player.locator('.calculations')).toContainText('SRD 5.2');
  for (const name of ['基本资料', '属性与检定', '生命与战斗', '攻击与施法', '能力、资源与状态', '装备与物品']) await expect(s.player.getByRole('tab', { name, exact: true })).toBeVisible();
  await click(s.player, '查看编辑历史'); await expect(s.player.locator('.character-history')).toContainText(p.playerUsername); const history = await read(s.playerContext.request, `${p.chars}/${p.id}/history`); expect(history).toHaveLength(2); expect(history[0].after.sheet.abilities.DEX).toBe(15); expect(history[1].after.sheet.abilities.DEX).toBe(9); expect(history[1].after.sheet.notes).toBe(sheet.notes);
  await s.player.screenshot({ path: info.outputPath('player-source-history.png') }); safeFrames(s.frames, sheet.notes, p.id); expect(s.errors).toEqual([]);
});
test('DM overrides and explicit condition ticks only remind until confirmed removal', async ({ sessions: s }, info) => {
  const sheet = richSheet(), p = await prepare(s, sheet); await enter(s.dm, p.campaignId, sheet.name); await s.dm.getByRole('tab', { name: '能力、资源与状态', exact: true }).click(); await click(s.dm, '添加DM覆盖');
  await s.dm.getByLabel('DM覆盖 · 1 · 覆盖值', { exact: true }).fill('9'); await s.dm.getByLabel('DM覆盖 · 1 · 覆盖原因', { exact: true }).fill('本团明确裁定'); await click(s.dm, '保存角色卡'); await expect(s.dm.getByText('角色请求已确认；当前数据已从服务器刷新。', { exact: true })).toBeVisible();
  await click(s.dm, 'DM推进状态一回合'); await expect(s.dm.locator('.expiry-reminder')).toContainText('手动计时状态'); expect((await read(s.playerContext.request, `${p.chars}/${p.id}`)).sheet.conditions).toHaveLength(1); expect((await read(s.playerContext.request, `${p.chars}/${p.id}`)).calculations.initiative.value).toBe(9);
  await s.dm.screenshot({ path: info.outputPath('dm-expiry-reminder.png') }); await click(s.dm, '确认移除到期状态 effect'); await expect.poll(async () => (await read(s.playerContext.request, `${p.chars}/${p.id}`)).sheet.conditions.length).toBe(0); expect(s.errors).toEqual([]);
});
test('a committed lost resource response retries exactly and waits for a fresh authoritative read', async ({ sessions: s }) => {
  const sheet = richSheet(), p = await prepare(s, sheet), path = `${p.chars}/${p.id}`; await enter(s.player, p.campaignId, sheet.name);
  const bodies: unknown[] = []; let blockRead = false;
  await s.player.route(`**${path}`, async route => { if (route.request().method() === 'GET' && blockRead) await route.fulfill({ status: 503, contentType: 'application/json', body: '{"message":"injected read failure"}' }); else await route.continue(); });
  await s.player.route(`**${path}/resources`, async route => { bodies.push(route.request().postDataJSON()); const response = await route.fetch(); expect(response.status()).toBe(200); if (bodies.length === 1) await route.abort('failed'); else { blockRead = true; await route.fulfill({ response }); } });
  await click(s.player, '消耗 法术位'); await expect(s.player.getByRole('button', { name: '重试同一角色操作', exact: true })).toBeVisible();
  const latest = await read(s.dmContext.request, path); await post(s.dmContext.request, `${path}/resources`, { requestId: randomUUID(), expectedVersion: latest.version, resourceId: 'slots', delta: -1, reason: 'later independent command' });
  await click(s.player, '重试同一角色操作'); await expect(s.player.getByRole('alert')).toContainText('刷新失败'); expect(bodies).toHaveLength(2); expect(bodies[0]).toEqual(bodies[1]); await expect(s.player.getByText('角色请求已确认；当前数据已从服务器刷新。', { exact: true })).toHaveCount(0);
  blockRead = false; await click(s.player, '刷新角色卡'); await expect(s.player.getByText('角色请求已确认；当前数据已从服务器刷新。', { exact: true })).toBeVisible(); expect((await read(s.dmContext.request, path)).sheet.resources[0].current).toBe(0); expect(await read(s.dmContext.request, `${path}/history`)).toHaveLength(3); expect(s.errors).toEqual([]);
});
test('real offline socket reconnect clears a hidden or revoked full sheet', async ({ sessions: s }, info) => {
  const sheet = richSheet(), p = await prepare(s, sheet), path = `${p.chars}/${p.id}`; await enter(s.player, p.campaignId, sheet.name); const connected = s.sockets.connected;
  await s.playerContext.setOffline(true); await s.player.evaluate(() => { for (const socket of (window as unknown as { characterSockets: WebSocket[] }).characterSockets) socket.close(); }); await expect.poll(() => s.sockets.closed).toBeGreaterThan(0);
  const access = await read(s.dmContext.request, `${path}/access`); await post(s.dmContext.request, `${path}/access`, { requestId: randomUUID(), expectedVersion: access.version, controllers: [], hidden: true, shareFull: false, approveImport: true, reason: 'offline revoke and hide' });
  await s.playerContext.setOffline(false); await expect.poll(() => s.sockets.connected).toBeGreaterThan(connected); await expect(s.player.getByLabel('角色名称', { exact: true })).toHaveCount(0); await expect(s.player.getByRole('button', { name: `打开角色 ${sheet.name}`, exact: true })).toHaveCount(0);
  expect((await s.playerContext.request.get(`${path}/export`)).status()).toBe(404); expect((await s.playerContext.request.get(`${path}/history`)).status()).toBe(404); await expect(s.player.getByRole('img', { name: '场景网格地图' })).toBeVisible();
  const hidden = await read(s.dmContext.request, path); await post(s.dmContext.request, `${path}/resources`, { requestId: randomUUID(), expectedVersion: hidden.version, resourceId: 'hp', delta: -1, reason: 'hidden update after reconnect' }); await expect.poll(() => s.frames.filter(f => f.startsWith('MESSAGE')).length).toBeGreaterThan(0);
  await s.player.screenshot({ path: info.outputPath('player-revoked-character.png') }); safeFrames(s.frames, sheet.notes, p.id); expect(s.errors).toEqual([]);
});
test('structured download, copy and target-campaign import create new identities and review', async ({ sessions: s }) => {
  const sheet = richSheet(), p = await prepare(s, sheet); await enter(s.player, p.campaignId, sheet.name);
  const downloading = s.player.waitForEvent('download'); await click(s.player, '导出角色 JSON'); const download = await downloading, file = await download.path(); expect(file).not.toBeNull(); const document = JSON.parse(await readFile(file!, 'utf8')); expect(document.sheet).toEqual(sheet); expect(Object.keys(document).sort()).toEqual(['format', 'formatVersion', 'rules', 'sheet']);
  await s.player.getByLabel('副本名称').fill('独立副本'); await click(s.player, '复制为独立角色'); await expect(s.player.getByRole('heading', { name: '独立副本', exact: true })).toBeVisible(); const list = await read(s.playerContext.request, p.chars); expect(list.characters).toHaveLength(2); expect(list.characters.find((c: { name: string }) => c.name === '独立副本').id).not.toBe(p.id);
  const target = await post(s.dmContext.request, '/api/campaigns', { requestId: randomUUID(), name: '跨战役导入目标' }, 201), targetRoot = `/api/campaigns/${target.id}`, targetChars = `${targetRoot}/characters`; const invite = await post(s.dmContext.request, `${targetRoot}/invitations`, { requestId: randomUUID(), expectedVersion: 0 }, 201); await post(s.playerContext.request, '/api/invitations/accept', { token: invite.token }); await enter(s.player, target.id);
  await click(s.player, '导入角色'); await s.player.getByLabel('角色 JSON', { exact: true }).fill(JSON.stringify(document)); await expect(s.player.getByText(/预览：/)).toContainText(sheet.name); await click(s.player, '确认导入角色'); await expect(s.player.getByRole('heading', { name: sheet.name, exact: true })).toBeVisible();
  const all = await read(s.dmContext.request, targetChars); expect(all.characters).toHaveLength(1); expect((await read(s.playerContext.request, p.chars)).characters).toHaveLength(2); const imported = all.characters.find((c: { id: string; name: string }) => c.id !== p.id && c.name === sheet.name); const access = await read(s.dmContext.request, `${targetChars}/${imported.id}/access`); expect(access.importReview).toBe(true); expect(access.controllers).toEqual([p.playerUsername]); expect(await read(s.playerContext.request, `${targetChars}/${imported.id}/history`)).toHaveLength(1); expect(s.errors).toEqual([]);
});
