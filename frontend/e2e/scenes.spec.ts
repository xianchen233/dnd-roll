import { randomUUID } from 'node:crypto';
import { test, expect, type Page } from '@playwright/test';
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
async function login(page: Page, username: string, password: string) {
  await page.goto('/'); await page.getByLabel('账号', { exact: true }).fill(username); await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: '进入冒险', exact: true }).click(); await expect(page.locator('.topbar-right')).toContainText(username);
}
test('DM creates a preparation scene, switches both real sessions, then returns to the preserved moved scene', async ({ browser, baseURL }, testInfo) => {
  const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL });
  const dm = await dmContext.newPage(), player = await playerContext.newPage();
  const errors: string[] = [], frames: string[] = [], playerPaths: string[] = [];
  for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
  player.on('request', r => playerPaths.push(new URL(r.url()).pathname));
  player.on('websocket', s => { if (s.url().endsWith('/ws')) s.on('framereceived', frame => { const text = String(frame.payload); if (text.includes('SCENE_CHANGED')) frames.push(text); }); });
  let campaignId = '', originalId = '';
  try {
    await login(dm, required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
    await login(player, required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
    await expect(player.getByRole('combobox', { name: '选择战役' })).not.toHaveValue('');
    // Use the opt-in demo fixture explicitly; A02 can create earlier-sorted campaigns.
    await player.getByRole('combobox', { name: '选择战役' }).selectOption('84b37902-114a-4a26-9f66-4041022a2024');
    campaignId = await player.getByRole('combobox', { name: '选择战役' }).inputValue();
    await expect(dm.getByRole('combobox', { name: '选择战役' }).locator(`option[value="${campaignId}"]`)).toHaveCount(1);
    await dm.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
    const endpoint = `/api/campaigns/${encodeURIComponent(campaignId)}`;
    const original = await (await playerContext.request.get(`${endpoint}/scene`)).json(); originalId = original.id;
    await expect(player.getByRole('button', { name: /Adventurer/ })).toBeVisible();
    await expect(dm.locator('.connection')).toHaveText('● 实时连接'); await expect(player.locator('.connection')).toHaveText('● 实时连接');
    const hero = original.tokens.find((t: { name: string }) => t.name === 'Adventurer');
    const x = (hero.x + 1) % original.width, y = hero.y;
    await player.getByRole('button', { name: /Adventurer/ }).click();
    await player.getByLabel('目标 X', { exact: true }).fill(String(x)); await player.getByLabel('目标 Y', { exact: true }).fill(String(y));
    await player.getByRole('button', { name: '确认移动', exact: true }).click();
    await expect(player.locator('.save-status')).toContainText('移动已保存，场景已同步');
    await expect(dm.getByRole('button', { name: /Adventurer/ })).toContainText(`(${x}, ${y})`);
    const moved = await (await playerContext.request.get(`${endpoint}/scene`)).json();
    const name = `备团场景 ${randomUUID().slice(0, 8)}`;
    await dm.getByLabel('场景名称', { exact: true }).fill(name); await dm.getByRole('button', { name: '创建空场景', exact: true }).click();
    await expect(dm.getByRole('button', { name: `切换到 ${name}`, exact: true })).toBeEnabled();
    await expect(dm.getByRole('heading', { name: original.name, exact: true })).toBeVisible();
    await expect(player.getByRole('heading', { name: original.name, exact: true })).toBeVisible();
    await expect(player.getByRole('heading', { name: '场景管理', exact: true })).toHaveCount(0);
    expect(playerPaths.some(p => p.endsWith('/scenes'))).toBe(false);
    expect(JSON.stringify(await (await playerContext.request.get(`${endpoint}/scene`)).json())).not.toContain(name);
    expect((await playerContext.request.get(`${endpoint}/scenes`)).status()).toBe(403);
    const sceneList = await (await dmContext.request.get(`${endpoint}/scenes`)).json();
    const prepared = sceneList.scenes.find((s: { name: string }) => s.name === name);
    const csrf = await (await playerContext.request.get('/api/auth/csrf')).json();
    expect((await playerContext.request.post(`${endpoint}/scenes/${prepared.id}/activate`, { headers: { [csrf.headerName]: csrf.token }, data: { requestId: randomUUID(), expectedVersion: sceneList.campaignVersion } })).status()).toBe(403);
    frames.length = 0;
    await dm.getByRole('button', { name: `切换到 ${name}`, exact: true }).click();
    await expect(dm.getByRole('heading', { name, exact: true })).toBeVisible();
    await expect(player.getByRole('heading', { name, exact: true })).toBeVisible({ timeout: 8000 });
    await expect.poll(() => frames.length, { timeout: 8000 }).toBeGreaterThan(0);
    expect(frames.every(f => !f.includes(name) && !f.includes('Hidden enemy'))).toBe(true);
    await expect(player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
    await expect(player.getByRole('button', { name: /Adventurer/ })).toHaveCount(0);
    const empty = await (await playerContext.request.get(`${endpoint}/scene`)).json();
    expect(empty).toMatchObject({ id: prepared.id, width: 24, height: 16, gridSize: 48, version: 0, tokens: [] });
    await dm.getByRole('button', { name: `切换到 ${original.name}`, exact: true }).click();
    await expect(player.getByRole('heading', { name: original.name, exact: true })).toBeVisible({ timeout: 8000 });
    await expect(player.getByRole('button', { name: /Adventurer/ })).toContainText(`(${x}, ${y})`);
    expect(await (await playerContext.request.get(`${endpoint}/scene`)).json()).toEqual(moved);
    await expect(player.getByRole('button', { name: /Hidden enemy/ })).toHaveCount(0);
    await expect(dm.getByRole('button', { name: /Hidden enemy/ })).toBeVisible();
    await player.screenshot({ path: testInfo.outputPath('player-returned-scene.png'), fullPage: true });
    await dm.screenshot({ path: testInfo.outputPath('dm-scene-management.png'), fullPage: true });
    expect(errors).toEqual([]);
  } finally {
    // A failed assertion must not leave the shared demo campaign on an empty scene
    // and break the existing movement flow on the next real browser run.
    try {
      if (campaignId && originalId) {
        const endpoint = `/api/campaigns/${encodeURIComponent(campaignId)}`;
        const listResponse = await dmContext.request.get(`${endpoint}/scenes`);
        if (listResponse.ok()) {
          const data = await listResponse.json();
          if (data.currentSceneId !== originalId) {
            const csrf = await (await dmContext.request.get('/api/auth/csrf')).json();
            expect((await dmContext.request.post(`${endpoint}/scenes/${originalId}/activate`, { headers: { [csrf.headerName]: csrf.token }, data: { requestId: randomUUID(), expectedVersion: data.campaignVersion } })).ok()).toBeTruthy();
          }
        }
      }
    } finally { await dmContext.close(); await playerContext.close(); }
  }
});
