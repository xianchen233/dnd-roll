import { randomUUID } from 'node:crypto';
import { deflateSync } from 'node:zlib';
import { test as base, expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';

type Sessions = { dmContext: BrowserContext; playerContext: BrowserContext; dm: Page; player: Page; errors: string[] };
const test = base.extend<{ sessions: Sessions }>({
  sessions: [async ({ browser, baseURL }, use) => {
    const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL });
    const dm = await dmContext.newPage(), player = await playerContext.newPage(), errors: string[] = [];
    for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
    await player.addInitScript(() => {
      const created: string[] = [], revoked: string[] = [];
      Object.assign(window, { acceptanceUrls: { created, revoked } });
      const create = URL.createObjectURL.bind(URL), revoke = URL.revokeObjectURL.bind(URL);
      URL.createObjectURL = blob => { const url = create(blob); created.push(url); return url; };
      URL.revokeObjectURL = url => { revoked.push(url); revoke(url); };
    });
    try { await use({ dmContext, playerContext, dm, player, errors }); }
    finally { await Promise.all([dmContext.close(), playerContext.close()]); }
  }, { timeout: 20000 }],
});
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
async function read(request: APIRequestContext, path: string) { const response = await request.get(path); expect(response.ok()).toBeTruthy(); return response.json(); }
async function post(request: APIRequestContext, path: string, data: unknown) { const csrf = await read(request, '/api/auth/csrf'); return request.post(path, { headers: { [csrf.headerName]: csrf.token }, data }); }
async function command(request: APIRequestContext, path: string, data: unknown) { const response = await post(request, path, data); expect(response.ok()).toBeTruthy(); return response.json(); }
async function click(page: Page, name: string) { const button = page.getByRole('button', { name, exact: true }); await expect(button).toBeEnabled(); await button.click(); }
function image() {
  function chunk(type: string, data: Buffer) { const bytes = Buffer.concat([Buffer.from(type), data]); let crc = 0xffffffff; for (const b of bytes) { crc ^= b; for (let n = 0; n < 8; n++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); } const size = Buffer.alloc(4), checksum = Buffer.alloc(4); size.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0); return Buffer.concat([size, bytes, checksum]); }
  const header = Buffer.alloc(13); header.writeUInt32BE(96); header.writeUInt32BE(96, 4); header[8] = 8; header[9] = 2;
  const pixels = Buffer.alloc(96 * 289); for (let y = 0; y < 96; y++) for (let x = 0; x < 96; x++) { const at = y * 289 + 1 + x * 3; pixels[at] = 30; pixels[at + 1] = x < 48 ? 170 : 22; pixels[at + 2] = 44; }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}
async function prepare(s: Sessions, secondScene = false) {
  const dmUsername = required('E2E_DM_USERNAME'), playerUsername = required('E2E_PLAYER_USERNAME');
  await command(s.dmContext.request, '/api/auth/login', { username: dmUsername, password: required('E2E_DM_PASSWORD') });
  await command(s.playerContext.request, '/api/auth/login', { username: playerUsername, password: required('E2E_PLAYER_PASSWORD') });
  const campaign = await command(s.dmContext.request, '/api/campaigns', { requestId: randomUUID(), name: `P02整体验收 ${randomUUID().slice(0, 8)}` }), endpoint = `/api/campaigns/${campaign.id}`;
  const members = await read(s.dmContext.request, `${endpoint}/members`);
  const invitation = await command(s.dmContext.request, `${endpoint}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion });
  await command(s.playerContext.request, '/api/invitations/accept', { token: invitation.token });
  const assets = await read(s.dmContext.request, `${endpoint}/assets`), csrf = await read(s.dmContext.request, '/api/auth/csrf');
  const upload = await s.dmContext.request.post(`${endpoint}/assets`, { headers: { [csrf.headerName]: csrf.token }, multipart: { command: { name: 'command.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify({ requestId: randomUUID(), expectedVersion: assets.campaignVersion })) }, file: { name: 'generated.png', mimeType: 'image/png', buffer: image() } } });
  expect(upload.ok()).toBeTruthy(); const assetId = (await upload.json()).asset.id as string;
  const first = await read(s.dmContext.request, `${endpoint}/scene`);
  const map = { assetId, gridSize: 48, offsetX: 0, offsetY: 0, distancePerCell: 5 };
  await command(s.dmContext.request, `${endpoint}/scenes/${first.id}/map`, { requestId: randomUUID(), expectedVersion: first.version, map });
  let current = await read(s.dmContext.request, `${endpoint}/scene`);
  const publicName = `共享向导 ${randomUUID().slice(0, 8)}`, hiddenName = `SECRET_P02_${randomUUID()}`, tokenPath = `${endpoint}/scenes/${first.id}/tokens`;
  const token = (await command(s.dmContext.request, tokenPath, { requestId: randomUUID(), expectedVersion: current.version, name: publicName, x: 0, y: 0, hidden: false, controllers: [dmUsername, playerUsername].sort() })).token;
  current = await read(s.dmContext.request, `${endpoint}/scene`);
  await command(s.dmContext.request, tokenPath, { requestId: randomUUID(), expectedVersion: current.version, name: hiddenName, x: 0, y: 0, hidden: true, controllers: [dmUsername] });
  const fogPath = `${endpoint}/scenes/${first.id}/fog`, fog = await read(s.dmContext.request, fogPath);
  await command(s.dmContext.request, fogPath, { requestId: randomUUID(), expectedVersion: fog.sceneVersion, revealed: [{ x: 0, y: 0 }] });
  let second: { id: string; name: string } | undefined;
  if (secondScene) {
    const list = await read(s.dmContext.request, `${endpoint}/scenes`);
    second = await command(s.dmContext.request, `${endpoint}/scenes`, { requestId: randomUUID(), expectedVersion: list.campaignVersion, name: `独立洞穴 ${randomUUID().slice(0, 8)}` });
    const configured = await command(s.dmContext.request, `${endpoint}/scenes/${second!.id}/map`, { requestId: randomUUID(), expectedVersion: 0, map: { ...map, distancePerCell: 10 } });
    await command(s.dmContext.request, `${endpoint}/scenes/${second!.id}/fog`, { requestId: randomUUID(), expectedVersion: configured.sceneVersion, revealed: [{ x: 1, y: 0 }] });
    expect((await read(s.dmContext.request, `${endpoint}/scene`)).id).toBe(first.id);
  }
  for (const [page, username] of [[s.dm, dmUsername], [s.player, playerUsername]] as const) {
    await page.goto('/'); await expect(page.locator('.topbar-right')).toContainText(username);
    await page.getByRole('combobox', { name: '选择战役' }).selectOption(campaign.id);
    await expect(page.locator('.connection')).toHaveText('● 实时连接');
    await expect(page.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  }
  return { endpoint, campaignId: campaign.id as string, sceneId: first.id as string, firstName: first.name as string, second, fogPath, tokenPath, map, token, publicName, hiddenName, playerUsername, dmUsername };
}
async function pixels(s: Sessions, endpoint: string, sceneId: string, version: number) {
  const response = await s.playerContext.request.get(`${endpoint}/scene/map-image?sceneId=${sceneId}&version=${version}`);
  expect(response.status()).toBe(200); expect(response.headers()['cache-control']).toContain('no-store');
  return s.player.evaluate(async data => { const bitmap = await createImageBitmap(new Blob([Uint8Array.from(atob(data), c => c.charCodeAt(0))], { type: 'image/png' })), canvas = document.createElement('canvas'); canvas.width = bitmap.width; canvas.height = bitmap.height; const ctx = canvas.getContext('2d')!; ctx.drawImage(bitmap, 0, 0); bitmap.close(); return [10, 60].map(x => [...ctx.getImageData(x, 10, 1, 1).data]); }, (await response.body()).toString('base64'));
}
async function offline(context: BrowserContext, page: Page) { await context.setOffline(true); await expect.poll(() => page.evaluate(() => navigator.onLine)).toBe(false); }
async function online(context: BrowserContext, page: Page) { await context.setOffline(false); await expect.poll(() => page.evaluate(() => navigator.onLine)).toBe(true); }
async function imageReady(page: Page) { await expect(page.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready'); }

test('P02e dual clients retain independent scene fog and calibration, then recalibrate to full cover without changing unit visibility', async ({ sessions: s }, info) => {
  const p = await prepare(s, true), first = await read(s.dmContext.request, `${p.endpoint}/scene`), firstFog = await read(s.dmContext.request, p.fogPath);
  expect(await pixels(s, p.endpoint, p.sceneId, first.version)).toEqual([[30, 170, 44, 255], [0, 0, 0, 255]]);
  await click(s.dm, `切换到 ${p.second!.name}`); await expect(s.player.getByRole('heading', { name: p.second!.name, exact: true })).toBeVisible(); await imageReady(s.player);
  const secondFog = await read(s.dmContext.request, `${p.endpoint}/scenes/${p.second!.id}/fog`);
  expect(await pixels(s, p.endpoint, p.second!.id, secondFog.sceneVersion)).toEqual([[0, 0, 0, 255], [30, 22, 44, 255]]);
  expect((await s.playerContext.request.get(`${p.endpoint}/scene/map-image?sceneId=${p.sceneId}&version=${first.version}`)).status()).toBe(404);
  expect((await s.playerContext.request.get(`${p.endpoint}/scenes`)).status()).toBe(403);
  await click(s.dm, `切换到 ${p.firstName}`); await expect(s.player.getByRole('heading', { name: p.firstName, exact: true })).toBeVisible(); await imageReady(s.player);
  expect(await read(s.dmContext.request, p.fogPath)).toEqual(firstFog); expect(await read(s.dmContext.request, `${p.endpoint}/scene`)).toEqual(first);
  expect(await read(s.dmContext.request, `${p.endpoint}/scenes/${p.second!.id}/fog`)).toEqual(secondFog);
  await s.dm.getByLabel('图片偏移 X', { exact: true }).fill('1'); await click(s.dm, '保存地图配置');
  await expect(s.dm.getByText('地图请求已确认；当前配置已从服务器刷新。', { exact: true })).toBeVisible();
  const covered = await read(s.dmContext.request, p.fogPath); expect(covered.revealed).toEqual([]);
  expect(await pixels(s, p.endpoint, p.sceneId, covered.sceneVersion)).toEqual([[0, 0, 0, 255], [0, 0, 0, 255]]);
  await expect(s.player.getByRole('button', { name: new RegExp(p.publicName) })).toBeEnabled(); await expect(s.player.getByText(p.hiddenName, { exact: true })).toHaveCount(0);
  expect(JSON.stringify(await read(s.playerContext.request, `${p.endpoint}/scene`))).not.toContain(p.hiddenName);
  await s.player.screenshot({ path: info.outputPath('player-recalibrated-covered-scene.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('P02e actual player offline reconnect replaces missed scene and fog changes and clears obsolete move retry and image', async ({ sessions: s }, info) => {
  const p = await prepare(s, true); await s.player.getByRole('button', { name: new RegExp(p.publicName) }).click();
  const old = await s.player.evaluate(() => (window as unknown as { acceptanceUrls: { created: string[] } }).acceptanceUrls.created.at(-1)!);
  await offline(s.playerContext, s.player);
  await s.player.getByLabel('目标 X', { exact: true }).fill('3'); await s.player.getByLabel('目标 Y', { exact: true }).fill('3'); await click(s.player, '确认移动');
  await expect(s.player.getByRole('button', { name: '重试同一操作', exact: true })).toBeEnabled();
  await click(s.dm, `切换到 ${p.second!.name}`);
  const fogPath = `${p.endpoint}/scenes/${p.second!.id}/fog`, fog = await read(s.dmContext.request, fogPath);
  await command(s.dmContext.request, fogPath, { requestId: randomUUID(), expectedVersion: fog.sceneVersion, revealed: [] });
  await online(s.playerContext, s.player);
  await expect(s.player.getByRole('heading', { name: p.second!.name, exact: true })).toBeVisible(); await imageReady(s.player);
  await expect(s.player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible(); await expect(s.player.getByRole('button', { name: '重试同一操作', exact: true })).toHaveCount(0);
  expect(await s.player.evaluate(url => (window as unknown as { acceptanceUrls: { revoked: string[] } }).acceptanceUrls.revoked.includes(url), old)).toBe(true);
  const recovered = await read(s.playerContext.request, `${p.endpoint}/scene`); expect(recovered.id).toBe(p.second!.id);
  expect(await pixels(s, p.endpoint, recovered.id, recovered.version)).toEqual([[0, 0, 0, 255], [0, 0, 0, 255]]);
  await s.player.screenshot({ path: info.outputPath('player-offline-reconnected-current-scene.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('P02e actual offline permission changes revoke old movement receipts, then clear the map and deny known image URLs after membership removal', async ({ sessions: s }, info) => {
  const p = await prepare(s), moveBody = { requestId: randomUUID(), expectedVersion: p.token.version, x: 2, y: 3 };
  await command(s.playerContext.request, `${p.endpoint}/tokens/${p.token.id}/moves`, moveBody);
  await expect(s.player.getByRole('button', { name: new RegExp(p.publicName) })).toContainText('(2, 3)');
  await s.player.getByRole('button', { name: new RegExp(p.publicName) }).click(); await offline(s.playerContext, s.player);
  const tokens = await read(s.dmContext.request, p.tokenPath), current = tokens.tokens.find((t: { id: string }) => t.id === p.token.id);
  await command(s.dmContext.request, `${p.tokenPath}/${p.token.id}`, { requestId: randomUUID(), expectedVersion: current.version, name: current.name, x: current.x, y: current.y, hidden: false, controllers: [p.dmUsername] });
  await online(s.playerContext, s.player);
  await expect(s.player.getByRole('button', { name: new RegExp(p.publicName) })).toBeDisabled(); await expect(s.player.getByRole('heading', { name: '未选择单位', exact: true })).toBeVisible();
  expect((await post(s.playerContext.request, `${p.endpoint}/tokens/${p.token.id}/moves`, moveBody)).status()).toBe(403);
  const known = await read(s.playerContext.request, `${p.endpoint}/scene`), imageUrl = `${p.endpoint}/scene/map-image?sceneId=${p.sceneId}&version=${known.version}`;
  await offline(s.playerContext, s.player); await click(s.dm, '刷新成员与邀请'); await click(s.dm, `移除 ${p.playerUsername}`); await online(s.playerContext, s.player);
  await expect(s.player.locator('.save-status')).toContainText('地图已清空'); await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveCount(0);
  for (const path of [`${p.endpoint}/scene`, imageUrl, `${p.endpoint}/assets/${p.map.assetId}/content`]) expect((await s.playerContext.request.get(path)).status()).toBe(403);
  await s.player.screenshot({ path: info.outputPath('player-revoked-after-offline.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('P02e committed fog response loss retains exact retry and historical receipt cannot restore later covered pixels', async ({ sessions: s }, info) => {
  const p = await prepare(s), before = await read(s.dmContext.request, p.fogPath), sent: unknown[] = [];
  await s.dm.getByLabel('起点 X', { exact: true }).fill('1'); await s.dm.getByLabel('终点 X', { exact: true }).fill('1'); await click(s.dm, '加入揭露矩形');
  let committed: unknown;
  // Fault injection loses only the response: route.fetch executes the actual
  // server transaction first. It is separate from the real offline cases above.
  await s.dm.route(`**${p.fogPath}`, async route => {
    if (route.request().method() !== 'POST') return route.continue();
    sent.push(route.request().postDataJSON()); const response = await route.fetch(); expect(response.ok()).toBeTruthy(); committed = await response.json(); await route.abort('failed');
  });
  await click(s.dm, '保存地图遮盖'); await expect(s.dm.getByRole('button', { name: '重试同一遮盖操作', exact: true })).toBeEnabled();
  expect(committed).toEqual(await read(s.dmContext.request, p.fogPath)); const saved = await read(s.dmContext.request, p.fogPath); expect(saved.sceneVersion).toBe(before.sceneVersion + 1);
  // A later command legitimately supersedes the uncertain receipt before retry.
  const covered = await command(s.dmContext.request, p.fogPath, { requestId: randomUUID(), expectedVersion: saved.sceneVersion, revealed: [] });
  await s.dm.unroute(`**${p.fogPath}`); s.dm.on('request', request => { if (request.method() === 'POST' && new URL(request.url()).pathname === p.fogPath) sent.push(request.postDataJSON()); });
  let blockConfirmedReads = false;
  await s.dm.route(`**${p.fogPath}`, async route => {
    if (route.request().method() === 'GET') return blockConfirmedReads ? route.abort('failed') : route.continue();
    const response = await route.fetch(); blockConfirmedReads = true; await route.fulfill({ response });
  });
  await click(s.dm, '重试同一遮盖操作');
  await expect(s.dm.getByText('请求已确认；刷新失败，遮盖尚未同步。请刷新遮盖。', { exact: true })).toBeVisible();
  await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toHaveCount(0);
  await s.dm.unroute(`**${p.fogPath}`); await click(s.dm, '刷新遮盖');
  await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toBeVisible();
  expect(sent).toHaveLength(2); expect(sent[1]).toEqual(sent[0]); expect(await read(s.dmContext.request, p.fogPath)).toEqual(covered);
  await expect(s.dm.getByText('已揭露 0 格', { exact: true })).toBeVisible(); expect(await pixels(s, p.endpoint, p.sceneId, covered.sceneVersion)).toEqual([[0, 0, 0, 255], [0, 0, 0, 255]]);
  await s.dm.screenshot({ path: info.outputPath('dm-historical-fog-retry-current-cover.png'), fullPage: true }); expect(s.errors).toEqual([]);
});
