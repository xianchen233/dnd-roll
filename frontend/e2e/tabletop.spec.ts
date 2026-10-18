import { test, expect, type Page } from '@playwright/test';

function required(name: string): string {
  const value = process.env[name];
  if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}. Provision real test accounts before running this test.`);
  return value;
}

async function login(page: Page, username: string, password: string) {
  await page.goto('/');
  await page.getByLabel('账号', { exact: true }).fill(username);
  await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: '进入冒险', exact: true }).click();
  await expect(page.locator('.topbar-right')).toContainText(username);
  await expect(page.getByRole('combobox', { name: '选择战役' })).toBeVisible();
}

function observeErrors(page: Page, errors: string[]) {
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => {
    if (message.type() !== 'error') return;
    // The initial session probe intentionally returns 401 for a fresh browser context.
    if (message.location().url.endsWith('/api/auth/me') && message.text().includes('401')) return;
    errors.push(`${message.text()} (${message.location().url})`);
  });
}

test('two real sessions enforce visibility, persist a move and receive realtime invalidation', async ({ browser, baseURL }, testInfo) => {
  const dmUsername = required('E2E_DM_USERNAME'), dmPassword = required('E2E_DM_PASSWORD');
  const playerUsername = required('E2E_PLAYER_USERNAME'), playerPassword = required('E2E_PLAYER_PASSWORD');
  expect(dmUsername).not.toBe(playerUsername);
  const dmContext = await browser.newContext({ baseURL, viewport: { width: 1440, height: 1000 } });
  const playerContext = await browser.newContext({ baseURL, viewport: { width: 1440, height: 1000 } });
  const dm = await dmContext.newPage(), player = await playerContext.newPage();
  const dmErrors: string[] = [], playerErrors: string[] = [], dmInvalidations: string[] = [];
  observeErrors(dm, dmErrors); observeErrors(player, playerErrors);
  dm.on('websocket', socket => {
    if (!socket.url().endsWith('/ws')) return;
    socket.on('framereceived', frame => {
      const payload = String(frame.payload);
      if (payload.includes('SCENE_CHANGED')) dmInvalidations.push(payload);
    });
  });
  try {
    await test.step('log in through real CSRF and cookie-backed sessions', async () => {
      await login(dm, dmUsername, dmPassword);
      await login(player, playerUsername, playerPassword);
    });
    const campaignId = await player.getByRole('combobox', { name: '选择战役' }).inputValue();
    expect(campaignId).not.toBe('');
    await dm.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
    const playerHero = player.getByRole('button', { name: /Adventurer/ });
    const dmHero = dm.getByRole('button', { name: /Adventurer/ });
    await test.step('permission-filtered snapshots and real Pixi canvases', async () => {
      await expect(dm.getByRole('button', { name: /Hidden enemy/ })).toBeVisible();
      await expect(player.getByRole('button', { name: /Hidden enemy/ })).toHaveCount(0);
      await expect(playerHero).toBeEnabled();
      const snapshotResponse = await playerContext.request.get(`/api/campaigns/${encodeURIComponent(campaignId)}/scene`);
      expect(snapshotResponse.ok()).toBeTruthy();
      const snapshot = await snapshotResponse.json();
      expect(snapshot.tokens.map((token: { name: string }) => token.name)).toEqual(['Adventurer']);
      await expect(dm.locator('.map-surface canvas')).toBeVisible();
      await expect(player.locator('.map-surface canvas')).toBeVisible();
      for (const page of [dm, player]) {
        const canvas = await page.locator('.map-surface canvas').boundingBox();
        const surface = await page.locator('.map-surface').boundingBox();
        expect(canvas).not.toBeNull();
        expect(surface).not.toBeNull();
        expect(canvas!.width).toBeLessThanOrEqual(surface!.width);
        expect(canvas!.width / canvas!.height).toBeCloseTo(snapshot.width / snapshot.height, 1);
      }
      await expect(dm.locator('.connection')).toHaveText('● 实时连接');
      await expect(player.locator('.connection')).toHaveText('● 实时连接');
    });
    const initial = (await playerHero.innerText()).match(/\((\d+), (\d+)\)/);
    expect(initial).not.toBeNull();
    let x = (Number(initial![1]) + 1) % 24, y = Number(initial![2]);
    await test.step('move with integer coordinates and synchronize the other session', async () => {
      await playerHero.click();
      await player.getByLabel('目标 X', { exact: true }).fill(String(x));
      await player.getByLabel('目标 Y', { exact: true }).fill(String(y));
      dmInvalidations.length = 0;
      await player.getByRole('button', { name: '确认移动', exact: true }).click();
      await expect(player.locator('.save-status')).toContainText('移动已保存，场景已同步');
      await expect(playerHero).toContainText(`(${x}, ${y})`);
      // A real received STOMP frame plus the other page's changed position verifies realtime.
      await expect.poll(() => dmInvalidations.length, { timeout: 8000 }).toBeGreaterThan(0);
      await expect(dmHero).toContainText(`(${x}, ${y})`, { timeout: 8000 });
      expect(dmInvalidations.every(frame => !frame.includes('Hidden enemy'))).toBeTruthy();
    });
    await test.step('clicking the far edge of the scaled canvas uses logical grid coordinates', async () => {
      x = 22; y = 14;
      const canvas = player.locator('.map-surface canvas');
      const bounds = await canvas.boundingBox();
      expect(bounds).not.toBeNull();
      await canvas.click({ position: { x: bounds!.width * (x + 0.5) / 24, y: bounds!.height * (y + 0.5) / 16 } });
      await expect(playerHero).toContainText(`(${x}, ${y})`);
      await expect(dmHero).toContainText(`(${x}, ${y})`);
    });
    await test.step('reload restores the server-confirmed coordinates', async () => {
      await player.reload();
      await expect(playerHero).toContainText(`(${x}, ${y})`);
      await expect(player.locator('.map-surface canvas')).toBeVisible();
      await expect(player.getByRole('button', { name: /Hidden enemy/ })).toHaveCount(0);
      await dm.screenshot({ path: testInfo.outputPath('dm-tabletop.png'), fullPage: true });
      await player.screenshot({ path: testInfo.outputPath('player-tabletop.png'), fullPage: true });
    });
    await test.step('logout invalidates both sessions', async () => {
      // Stop collecting before the explicitly expected post-logout 401 assertions.
      await player.getByRole('button', { name: '退出', exact: true }).click();
      await dm.getByRole('button', { name: '退出', exact: true }).click();
      await expect(player.getByRole('button', { name: '进入冒险', exact: true })).toBeVisible();
      await expect(dm.getByRole('button', { name: '进入冒险', exact: true })).toBeVisible();
      expect((await playerContext.request.get('/api/auth/me')).status()).toBe(401);
      expect((await dmContext.request.get('/api/auth/me')).status()).toBe(401);
    });
    expect(dmErrors, 'DM browser JavaScript/console errors').toEqual([]);
    expect(playerErrors, 'Player browser JavaScript/console errors').toEqual([]);
  } finally {
    await dmContext.close();
    await playerContext.close();
  }
});
