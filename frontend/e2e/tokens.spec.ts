import { randomUUID } from 'node:crypto';
import { test as base, expect, type Page, type BrowserContext, type APIRequestContext } from '@playwright/test';
type Sessions = { dmContext: BrowserContext; playerContext: BrowserContext; dm: Page; player: Page; errors: string[]; frames: string[]; dmFrames: string[] };
const test = base.extend<{ sessions: Sessions }>({
  sessions: [async ({ browser, baseURL }, use) => {
    const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL });
    const dm = await dmContext.newPage(), player = await playerContext.newPage(), errors: string[] = [], frames: string[] = [], dmFrames: string[] = [];
    for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
    player.on('websocket', socket => { if (socket.url().endsWith('/ws')) socket.on('framereceived', frame => { if (String(frame.payload).includes('SCENE_CHANGED')) frames.push(String(frame.payload)); }); });
    dm.on('websocket', socket => { if (socket.url().endsWith('/ws')) socket.on('framereceived', frame => { if (String(frame.payload).includes('SCENE_CHANGED')) dmFrames.push(String(frame.payload)); }); });
    try { await use({ dmContext, playerContext, dm, player, errors, frames, dmFrames }); }
    finally { await Promise.all([dmContext.close(), playerContext.close()]); }
  }, { timeout: 20000 }],
});
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
function usernames() { return { dmUsername: required('E2E_DM_USERNAME'), playerUsername: required('E2E_PLAYER_USERNAME') }; }
async function click(page: Page, name: string | RegExp) { const button = page.getByRole('button', { name, exact: typeof name === 'string' }); await expect(button).toBeEnabled(); await button.click(); }
async function login(page: Page, username: string, password: string) { await page.goto('/'); await page.getByLabel('账号', { exact: true }).fill(username); await page.getByLabel('密码', { exact: true }).fill(password); await click(page, '进入冒险'); await expect(page.locator('.topbar-right')).toContainText(username); }
async function post(request: APIRequestContext, url: string, data: unknown) { const csrf = await (await request.get('/api/auth/csrf')).json(); return request.post(url, { headers: { [csrf.headerName]: csrf.token }, data }); }
async function joinThroughUi(dm: Page, player: Page, campaignId: string) {
  const input = dm.getByLabel('一次性邀请链接'), oldLink = await input.count() ? await input.inputValue() : '';
  const created = dm.waitForResponse(r => r.request().method() === 'POST' && r.url().endsWith('/invitations'));
  await click(dm, '生成邀请链接'); expect((await created).ok()).toBeTruthy(); await expect(input).toBeVisible();
  if (oldLink) await expect(input).not.toHaveValue(oldLink);
  await player.goto(await input.inputValue()); await click(player, '接受邀请');
  await expect(player.getByRole('combobox', { name: '选择战役' })).toHaveValue(campaignId); await expect(player.locator('.connection')).toHaveText('● 实时连接');
}
async function edit(dm: Page, tokenId: string) { await click(dm, `编辑棋子 ${tokenId}`); }
async function save(dm: Page) { await click(dm, '保存棋子配置'); await expect(dm.getByText('棋子请求已确认；当前列表已从服务器刷新。', { exact: true })).toBeVisible(); }
function assertPrivateHints(frames: string[], name: string, tokenId: string) { expect(frames.length).toBeGreaterThan(0); expect(frames.every(f => !f.includes(name) && !f.includes(tokenId) && !f.includes('controllers') && !f.includes('hidden'))).toBe(true); }

// Later cases prepare independent campaigns through real authenticated HTTP.
// Sessions come only from CSRF + login; no cookies or payloads are mocked.
async function prepare(sessions: Sessions) {
  const { dmContext, playerContext, dm, player } = sessions, { dmUsername, playerUsername } = usernames();
  expect((await post(dmContext.request, '/api/auth/login', { username: dmUsername, password: required('E2E_DM_PASSWORD') })).ok()).toBeTruthy();
  expect((await post(playerContext.request, '/api/auth/login', { username: playerUsername, password: required('E2E_PLAYER_PASSWORD') })).ok()).toBeTruthy();
  const created = await post(dmContext.request, '/api/campaigns', { requestId: randomUUID(), name: `棋子独立验收 ${randomUUID().slice(0, 8)}` }); expect(created.ok()).toBeTruthy();
  const campaignId = (await created.json()).id as string, endpoint = `/api/campaigns/${campaignId}`;
  const members = await (await dmContext.request.get(`${endpoint}/members`)).json();
  const invitation = await post(dmContext.request, `${endpoint}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }); expect(invitation.ok()).toBeTruthy();
  expect((await post(playerContext.request, '/api/invitations/accept', { token: (await invitation.json()).token })).ok()).toBeTruthy();
  const scene = await (await dmContext.request.get(`${endpoint}/scene`)).json(), name = `共享棋子 ${randomUUID().slice(0, 8)}`, tokenEndpoint = `${endpoint}/scenes/${scene.id}/tokens`;
  const createBody = { requestId: randomUUID(), expectedVersion: scene.version, name, x: 2, y: 3, hidden: false, controllers: [dmUsername, playerUsername].sort() };
  const authored = await post(dmContext.request, tokenEndpoint, createBody); expect(authored.ok()).toBeTruthy(); const token = (await authored.json()).token;
  const moveBody = { requestId: randomUUID(), expectedVersion: token.version, x: 4, y: 5 };
  const moved = await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody); expect(moved.ok()).toBeTruthy();
  await dm.goto('/'); await player.goto('/');
  await expect(dm.locator('.topbar-right')).toContainText(dmUsername); await expect(player.locator('.topbar-right')).toContainText(playerUsername);
  await dm.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId); await player.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
  await expect(dm.locator('.connection')).toHaveText('● 实时连接'); await expect(player.locator('.connection')).toHaveText('● 实时连接');
  await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled();
  return { campaignId, endpoint, name, token, tokenEndpoint, createBody, moveBody, playerUsername };
}

test('DM creates a token with shared controllers through the UI and the real player moves it', async ({ sessions }, testInfo) => {
  const { dmContext, playerContext, dm, player, errors, frames } = sessions, { dmUsername, playerUsername } = usernames(), name = `共享棋子 ${randomUUID().slice(0, 8)}`;
  await login(dm, dmUsername, required('E2E_DM_PASSWORD')); await login(player, playerUsername, required('E2E_PLAYER_PASSWORD'));
  await dm.getByLabel('战役名称').fill(`棋子验收 ${randomUUID().slice(0, 8)}`); await click(dm, '创建战役'); await expect(dm.locator('.map-caption')).toContainText('空白战役已就绪');
  const campaignId = await dm.getByRole('combobox', { name: '选择战役' }).inputValue(), endpoint = `/api/campaigns/${campaignId}`;
  await joinThroughUi(dm, player, campaignId); await click(dm, '刷新棋子与控制者'); await expect(dm.getByLabel(`控制者 ${playerUsername}`, { exact: true })).toBeEnabled();
  await dm.getByLabel('棋子名称', { exact: true }).fill(name); await dm.getByLabel('棋子 X', { exact: true }).fill('2'); await dm.getByLabel('棋子 Y', { exact: true }).fill('3');
  await dm.getByLabel(`控制者 ${dmUsername}`, { exact: true }).check(); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).check();
  const createRequest = dm.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/tokens'));
  await click(dm, '创建棋子'); const createBody = (await createRequest).postDataJSON();
  await expect(dm.getByText('棋子请求已确认；当前列表已从服务器刷新。', { exact: true })).toBeVisible(); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled();
  const scene = await (await dmContext.request.get(`${endpoint}/scene`)).json(), token = scene.tokens.find((t: { name: string }) => t.name === name), tokenEndpoint = `${endpoint}/scenes/${scene.id}/tokens`;
  expect((await playerContext.request.get(tokenEndpoint)).status()).toBe(403); expect((await post(playerContext.request, tokenEndpoint, createBody)).status()).toBe(403);
  const authored = await (await dmContext.request.get(tokenEndpoint)).json(); expect(authored.tokens).toEqual([{ id: token.id, name, x: 2, y: 3, version: 0, hidden: false, controllers: [dmUsername, playerUsername].sort() }]);
  await click(player, new RegExp(name)); await player.getByLabel('目标 X', { exact: true }).fill('4'); await player.getByLabel('目标 Y', { exact: true }).fill('5');
  const moveRequest = player.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/moves'));
  await click(player, '确认移动'); const moveBody = (await moveRequest).postDataJSON();
  await expect(player.locator('.save-status')).toContainText('移动已保存，场景已同步'); await expect(dm.getByRole('button', { name: new RegExp(name) })).toContainText('(4, 5)');
  const beforeReplay = await (await dmContext.request.get(tokenEndpoint)).json(); expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).ok()).toBeTruthy(); expect(await (await dmContext.request.get(tokenEndpoint)).json()).toEqual(beforeReplay);
  assertPrivateHints(frames, name, token.id); await dm.screenshot({ path: testInfo.outputPath('dm-shared-token-created.png'), fullPage: true }); expect(errors).toEqual([]);
});

test('DM hides and reveals tokens then revokes and regrants shared control without restoring old selections or receipts', async ({ sessions }, testInfo) => {
  const { dmContext, playerContext, dm, player, errors, frames } = sessions, { endpoint, name, token, moveBody, playerUsername } = await prepare(sessions);
  await click(player, new RegExp(name)); await edit(dm, token.id); await dm.getByLabel('棋子隐藏', { exact: true }).check(); await save(dm);
  await expect(player.getByRole('button', { name: new RegExp(name) })).toHaveCount(0); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
  const hiddenSnapshot = await (await playerContext.request.get(`${endpoint}/scene`)).json(); expect(JSON.stringify(hiddenSnapshot)).not.toContain(name); expect(JSON.stringify(hiddenSnapshot)).not.toContain(token.id); expect(JSON.stringify(hiddenSnapshot)).not.toContain('controllers'); expect(JSON.stringify(hiddenSnapshot)).not.toContain('hidden');
  expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(404);
  await edit(dm, token.id); await dm.getByLabel('棋子隐藏', { exact: true }).uncheck(); await save(dm); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled(); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
  await click(player, new RegExp(name)); await edit(dm, token.id); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).uncheck(); await save(dm);
  await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled(); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
  expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(403);
  await edit(dm, token.id); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).check(); await save(dm); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled(); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
  const scene = await (await dmContext.request.get(`${endpoint}/scene`)).json(); expect(scene.tokens.find((t: { id: string }) => t.id === token.id)).toMatchObject({ x: 4, y: 5 });
  assertPrivateHints(frames, name, token.id); await dm.screenshot({ path: testInfo.outputPath('dm-shared-token-editor.png'), fullPage: true }); expect(errors).toEqual([]);
});

test('UI member removal and invitation rejoin leave old token and movement receipts unable to restore control', async ({ sessions }, testInfo) => {
  const { dmContext, playerContext, dm, player, errors, dmFrames } = sessions, { campaignId, endpoint, name, token, tokenEndpoint, createBody, moveBody, playerUsername } = await prepare(sessions);
  await click(dm, '刷新成员与邀请'); await click(dm, `移除 ${playerUsername}`);
  await expect(dm.getByLabel(`控制者 ${playerUsername}`, { exact: true })).toHaveCount(0); expect((await playerContext.request.get(`${endpoint}/scene`)).status()).toBe(403);
  expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(403);
  await joinThroughUi(dm, player, campaignId); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled();
  const beforeReplay = await (await dmContext.request.get(tokenEndpoint)).json(), replay = await post(dmContext.request, tokenEndpoint, createBody); expect(replay.ok()).toBeTruthy(); expect((await replay.json()).token.id).toBe(token.id);
  expect(await (await dmContext.request.get(tokenEndpoint)).json()).toEqual(beforeReplay); expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(403); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled();
  // Removal excludes and closes the player's socket; acceptance precedes its
  // replacement connection, and historical replays intentionally emit nothing.
  // The continuing DM receives the real membership invalidation throughout.
  assertPrivateHints(dmFrames, name, token.id); await player.screenshot({ path: testInfo.outputPath('player-rejoined-without-control.png'), fullPage: true }); expect(errors).toEqual([]);
});
