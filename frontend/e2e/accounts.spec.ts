import { randomBytes } from 'node:crypto';
import { test, expect, type Page } from '@playwright/test';
function required(name: string) {
  const value = process.env[name];
  if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`);
  return value;
}
async function login(page: Page, username: string, password: string) {
  await page.goto('/');
  await page.getByLabel('账号', { exact: true }).fill(username);
  await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: '进入冒险', exact: true }).click();
  await expect(page.locator('.topbar-right')).toContainText(username);
}
test('DM creates an empty campaign, a fragment invitation registers a fixed account, and member removal clears access', async ({ browser, baseURL }) => {
  const dmUsername = required('E2E_DM_USERNAME'), dmPassword = required('E2E_DM_PASSWORD');
  const playerUsername = required('E2E_PLAYER_USERNAME'), playerPassword = required('E2E_PLAYER_PASSWORD');
  const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL }), guestContext = await browser.newContext({ baseURL });
  const dm = await dmContext.newPage(), player = await playerContext.newPage(), guest = await guestContext.newPage();
  const suffix = `${Date.now().toString(36)}_${randomBytes(3).toString('hex')}`;
  const username = `invite_${suffix}`, password = randomBytes(24).toString('base64url'), name = `邀请验收 ${suffix}`;
  const pageErrors: string[] = []; let guestSocketClosed = false;
  for (const page of [dm, player, guest]) page.on('pageerror', error => pageErrors.push(error.message));
  guest.on('websocket', socket => { if (socket.url().endsWith('/ws')) socket.on('close', () => { guestSocketClosed = true; }); });
  try {
    await login(dm, dmUsername, dmPassword);
    await login(player, playerUsername, playerPassword);
    // Every fixed account can create campaigns, including an ordinary PLAYER.
    await expect(player.getByRole('button', { name: '创建战役', exact: true })).toBeVisible();
    await expect(player.getByRole('heading', { name: '成员与邀请' })).toHaveCount(0);
    await dm.getByLabel('战役名称').fill(name);
    await dm.getByRole('button', { name: '创建战役', exact: true }).click();
    await expect(dm.getByRole('combobox', { name: '选择战役' })).toContainText(name);
    await expect(dm.locator('.map-caption')).toContainText('空白战役已就绪');
    const campaignId = await dm.getByRole('combobox', { name: '选择战役' }).inputValue();
    const createdScene = await dmContext.request.get(`/api/campaigns/${encodeURIComponent(campaignId)}/scene`);
    expect(createdScene.ok()).toBeTruthy(); expect((await createdScene.json()).tokens).toEqual([]);
    await dm.getByRole('button', { name: '生成邀请链接', exact: true }).click();
    const link = await dm.getByLabel('一次性邀请链接').inputValue();
    const url = new URL(link);
    expect(url.search).toBe(''); expect(url.hash).toMatch(/^#invite=[A-Za-z0-9_-]+$/);
    await guest.goto(link);
    await expect(guest.getByRole('heading', { name: `受邀加入「${name}」` })).toBeVisible();
    await guest.getByLabel('新账号').fill(username);
    await guest.getByLabel('新密码').fill(password);
    await guest.getByRole('button', { name: '注册并加入战役', exact: true }).click();
    await expect(guest.locator('.topbar-right')).toContainText(username);
    await expect(guest.getByRole('combobox', { name: '选择战役' })).toHaveValue(campaignId);
    await expect(guest.locator('.map-surface canvas')).toBeVisible();
    await expect(guest.locator('.connection')).toHaveText('● 实时连接');
    expect(new URL(guest.url()).hash).toBe('');
    await expect(guest.getByRole('heading', { name: '成员与邀请' })).toHaveCount(0);
    await dm.getByRole('button', { name: '刷新成员与邀请', exact: true }).click();
    await expect(dm.getByRole('button', { name: `移除 ${username}`, exact: true })).toBeVisible();
    await dm.getByRole('button', { name: `移除 ${username}`, exact: true }).click();
    await expect(dm.getByRole('button', { name: `移除 ${username}`, exact: true })).toHaveCount(0);
    // Real server socket closure plus reconnect/periodic HTTP clears the previously rendered map.
    await expect.poll(() => guestSocketClosed).toBe(true);
    await expect(guest.locator('.map-surface canvas')).toHaveCount(0, { timeout: 20000 });
    await expect(guest.locator('.save-status')).toContainText('已失去战役访问权限');
    expect((await guestContext.request.get(`/api/campaigns/${encodeURIComponent(campaignId)}/scene`)).status()).toBe(403);
    expect((await guestContext.request.get('/api/auth/me')).status()).toBe(200);
    await guest.getByRole('button', { name: '刷新战役列表', exact: true }).click();
    await expect(guest.getByRole('combobox', { name: '选择战役' })).toHaveValue('');
    // The newly registered fixed account still logs in after removal; campaign access stays removed.
    await guest.getByRole('button', { name: '退出', exact: true }).click();
    await login(guest, username, password);
    await expect(guest.getByRole('combobox', { name: '选择战役' })).toHaveValue('');
    expect(pageErrors).toEqual([]);
  } finally { await dmContext.close(); await playerContext.close(); await guestContext.close(); }
});
