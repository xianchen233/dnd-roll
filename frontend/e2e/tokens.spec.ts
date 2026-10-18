import { randomUUID } from 'node:crypto';
import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
async function click(page: Page, name: string | RegExp) { const button = page.getByRole('button', { name, exact: typeof name === 'string' }); await expect(button).toBeEnabled(); await button.click(); }
async function login(page: Page, username: string, password: string) { await page.goto('/'); await page.getByLabel('账号', { exact: true }).fill(username); await page.getByLabel('密码', { exact: true }).fill(password); await click(page, '进入冒险'); await expect(page.locator('.topbar-right')).toContainText(username); }
async function post(request: APIRequestContext, url: string, data: unknown) { const csrf = await (await request.get('/api/auth/csrf')).json(); return request.post(url, { headers: { [csrf.headerName]: csrf.token }, data }); }
test('DM authors shared tokens; real player movement, hiding, revocation and member rejoin never replay old authority', async ({ browser, baseURL }, testInfo) => {
  const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL }), dm = await dmContext.newPage(), player = await playerContext.newPage();
  const dmUsername = required('E2E_DM_USERNAME'), playerUsername = required('E2E_PLAYER_USERNAME'), name = `共享棋子 ${randomUUID().slice(0, 8)}`, errors: string[] = [], frames: string[] = [];
  for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
  player.on('websocket', socket => { if (socket.url().endsWith('/ws')) socket.on('framereceived', frame => { if (String(frame.payload).includes('SCENE_CHANGED')) frames.push(String(frame.payload)); }); });
  try {
    await login(dm, dmUsername, required('E2E_DM_PASSWORD')); await login(player, playerUsername, required('E2E_PLAYER_PASSWORD'));
    await dm.getByLabel('战役名称').fill(`棋子验收 ${randomUUID().slice(0, 8)}`); await click(dm, '创建战役');
    await expect(dm.locator('.map-caption')).toContainText('空白战役已就绪');
    const campaignId = await dm.getByRole('combobox', { name: '选择战役' }).inputValue(), endpoint = `/api/campaigns/${campaignId}`;
    async function join() {
      const input = dm.getByLabel('一次性邀请链接'), oldLink = await input.count() ? await input.inputValue() : '';
      const created = dm.waitForResponse(r => r.request().method() === 'POST' && r.url().endsWith('/invitations'));
      await click(dm, '生成邀请链接'); expect((await created).ok()).toBeTruthy(); await expect(input).toBeVisible(); if (oldLink) await expect(input).not.toHaveValue(oldLink); const link = await input.inputValue();
      await player.goto(link); await click(player, '接受邀请');
      await expect(player.getByRole('combobox', { name: '选择战役' })).toHaveValue(campaignId); await expect(player.locator('.connection')).toHaveText('● 实时连接');
    }
    await join(); await click(dm, '刷新棋子与控制者');
    await expect(dm.getByLabel(`控制者 ${playerUsername}`, { exact: true })).toBeEnabled();
    await dm.getByLabel('棋子名称', { exact: true }).fill(name); await dm.getByLabel('棋子 X', { exact: true }).fill('2'); await dm.getByLabel('棋子 Y', { exact: true }).fill('3');
    await dm.getByLabel(`控制者 ${dmUsername}`, { exact: true }).check(); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).check();
    const createRequest = dm.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/tokens'));
    await click(dm, '创建棋子'); const createBody = (await createRequest).postDataJSON();
    await expect(dm.getByText('棋子请求已确认；当前列表已从服务器刷新。', { exact: true })).toBeVisible();
    await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled();
    const scene = await (await dmContext.request.get(`${endpoint}/scene`)).json(), token = scene.tokens.find((t: { name: string }) => t.name === name), tokenEndpoint = `${endpoint}/scenes/${scene.id}/tokens`;
    expect((await playerContext.request.get(tokenEndpoint)).status()).toBe(403); expect((await post(playerContext.request, tokenEndpoint, createBody)).status()).toBe(403);
    await click(player, new RegExp(name)); await player.getByLabel('目标 X', { exact: true }).fill('4'); await player.getByLabel('目标 Y', { exact: true }).fill('5');
    const moveRequest = player.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/moves'));
    await click(player, '确认移动'); const moveBody = (await moveRequest).postDataJSON();
    await expect(player.locator('.save-status')).toContainText('移动已保存，场景已同步'); await expect(dm.getByRole('button', { name: new RegExp(name) })).toContainText('(4, 5)');
    async function edit() { const button = dm.getByRole('button', { name: `编辑棋子 ${token.id}`, exact: true }); await expect(button).toBeEnabled(); await button.click(); }
    async function save() { await click(dm, '保存棋子配置'); await expect(dm.getByText('棋子请求已确认；当前列表已从服务器刷新。', { exact: true })).toBeVisible(); }
    await edit(); await dm.getByLabel('棋子隐藏', { exact: true }).check(); await save();
    await expect(player.getByRole('button', { name: new RegExp(name) })).toHaveCount(0); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
    const hiddenSnapshot = await (await playerContext.request.get(`${endpoint}/scene`)).json(); expect(JSON.stringify(hiddenSnapshot)).not.toContain(name); expect(JSON.stringify(hiddenSnapshot)).not.toContain(token.id); expect(JSON.stringify(hiddenSnapshot)).not.toContain('controllers'); expect(JSON.stringify(hiddenSnapshot)).not.toContain('hidden');
    expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(404);
    await edit(); await dm.getByLabel('棋子隐藏', { exact: true }).uncheck(); await save(); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled();
    await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
    await player.getByRole('button', { name: new RegExp(name) }).click(); await edit(); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).uncheck(); await save();
    await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled(); await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
    expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(403);
    await edit(); await dm.getByLabel(`控制者 ${playerUsername}`, { exact: true }).check(); await save(); await expect(player.getByRole('button', { name: new RegExp(name) })).toBeEnabled();
    await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
    await click(dm, '刷新成员与邀请'); await click(dm, `移除 ${playerUsername}`);
    await expect(dm.getByLabel(`控制者 ${playerUsername}`, { exact: true })).toHaveCount(0); expect((await playerContext.request.get(`${endpoint}/scene`)).status()).toBe(403);
    await expect(dm.getByRole('button', { name: '生成邀请链接', exact: true })).toBeEnabled(); await join();
    await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled();
    const beforeReplay = await (await dmContext.request.get(tokenEndpoint)).json(), replay = await post(dmContext.request, tokenEndpoint, createBody); expect(replay.ok()).toBeTruthy(); expect((await replay.json()).token.id).toBe(token.id);
    expect(await (await dmContext.request.get(tokenEndpoint)).json()).toEqual(beforeReplay);
    expect((await post(playerContext.request, `${endpoint}/tokens/${token.id}/moves`, moveBody)).status()).toBe(403);
    await expect(player.getByRole('button', { name: new RegExp(name) })).toBeDisabled();
    expect(frames.length).toBeGreaterThan(0); expect(frames.every(f => !f.includes(name) && !f.includes(token.id) && !f.includes('controllers') && !f.includes('hidden'))).toBe(true);
    await dm.screenshot({ path: testInfo.outputPath('dm-shared-token-editor.png'), fullPage: true }); await player.screenshot({ path: testInfo.outputPath('player-rejoined-without-control.png'), fullPage: true }); expect(errors).toEqual([]);
  } finally { await dmContext.close(); await playerContext.close(); }
});
