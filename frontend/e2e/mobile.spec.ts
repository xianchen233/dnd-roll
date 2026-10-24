import { randomUUID } from 'node:crypto';
import { test, expect } from '@playwright/test';
import { click, createCharacter, enter, join, login, noOverflow, post, read } from './p10-support';

for (const width of [390, 360]) test(`P10 ${width}px touch player switches cards, retries a resource once, rolls and posts RP without overflow`, async ({ browser, baseURL }, info) => {
  const dm = await browser.newContext({ baseURL }), phone = await browser.newContext({ baseURL, viewport: { width, height: 844 }, isMobile: true, hasTouch: true, deviceScaleFactor: 1 });
  try {
    await login(dm.request, 'DM'); await login(phone.request, 'PLAYER');
    const campaign = await post(dm.request, '/api/campaigns', { requestId: randomUUID(), name: `ZZZ P10手机 ${randomUUID().slice(0, 8)}` }, 201), root = `/api/campaigns/${campaign.id}`;
    await join(dm.request, phone.request, root);
    const id = await createCharacter(phone.request, root, '手机游侠');
    const page = await phone.newPage(), errors: string[] = []; page.on('pageerror', e => errors.push(e.message));
    await enter(page, campaign.id); await noOverflow(page);
    const nav = page.getByRole('tablist', { name: '手机工作区' });
    await nav.getByRole('tab', { name: '角色卡', exact: true }).tap();
    await click(page, '打开角色 手机游侠'); await expect(page.getByLabel('角色名称', { exact: true })).toHaveValue('手机游侠');
    await noOverflow(page);
    for (const group of ['属性与检定', '生命与战斗', '攻击与施法', '能力、资源与状态', '装备与物品', '基本资料']) {
      await page.getByRole('tablist', { name: '角色卡分组' }).getByRole('tab', { name: group, exact: true }).tap(); await noOverflow(page);
    }
    await page.getByLabel('角色名称', { exact: true }).fill('手机未提交草稿');
    await nav.getByRole('tab', { name: '掷骰', exact: true }).tap(); await noOverflow(page);
    await nav.getByRole('tab', { name: '角色卡', exact: true }).tap();
    await expect(page.getByLabel('角色名称', { exact: true })).toHaveValue('手机未提交草稿');
    await page.getByLabel('角色名称', { exact: true }).fill('手机游侠'); await click(page, '保存角色卡');
    await expect(page.getByRole('button', { name: '消耗 专注点', exact: true })).toBeEnabled();
    const requests: unknown[] = [];
    await page.route(`**${root}/characters/${id}/resources`, async route => {
      requests.push(route.request().postDataJSON()); const response = await route.fetch(); expect(response.status()).toBe(200);
      if (requests.length === 1) await route.abort('failed'); else await route.fulfill({ response });
    });
    await page.getByRole('button', { name: '消耗 专注点', exact: true }).tap();
    await expect(page.getByRole('button', { name: '重试同一角色操作', exact: true })).toBeEnabled();
    await nav.getByRole('tab', { name: '掷骰', exact: true }).tap(); await nav.getByRole('tab', { name: '角色卡', exact: true }).tap();
    await page.getByRole('button', { name: '重试同一角色操作', exact: true }).tap();
    await expect(page.getByText('专注点 1/2', { exact: true })).toBeVisible(); expect(requests).toHaveLength(2); expect(requests[0]).toEqual(requests[1]);
    await nav.getByRole('tab', { name: '掷骰', exact: true }).tap(); await expect(page.locator('.character-workspace')).not.toBeVisible();
    await page.getByLabel('掷骰身份', { exact: true }).selectOption(id); await click(page, '掷骰');
    await expect.poll(async () => (await read(dm.request, `${root}/rolls`)).rolls.length).toBe(1);
    await noOverflow(page); await nav.getByRole('tab', { name: 'RP 与日志', exact: true }).tap();
    await page.getByLabel('发言身份', { exact: true }).selectOption(id);
    await page.getByLabel('消息正文', { exact: true }).fill('我们从手机继续冒险。');
    await page.getByRole('button', { name: '发送消息', exact: true }).tap();
    await expect(page.getByText('我们从手机继续冒险。', { exact: true })).toBeVisible(); await noOverflow(page);
    expect((await read(phone.request, `${root}/characters/${id}`)).sheet.resources[0].current).toBe(1);
    expect((await read(phone.request, `${root}/timeline?kind=RP`)).entries).toHaveLength(1);
    expect(await page.evaluate(() => navigator.maxTouchPoints)).toBeGreaterThan(0);
    await page.screenshot({ path: info.outputPath(`phone-${width}-rp.png`), fullPage: true }); expect(errors).toEqual([]);
  } finally { await phone.close(); await dm.close(); }
});
