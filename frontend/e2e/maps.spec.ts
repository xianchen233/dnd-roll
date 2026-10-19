import { randomUUID } from 'node:crypto';
import { deflateSync } from 'node:zlib';
import { test as base, expect, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test';
type Sessions = { dmContext: BrowserContext; playerContext: BrowserContext; dm: Page; player: Page; errors: string[]; frames: string[]; paths: string[] };
const test = base.extend<{ sessions: Sessions }>({
  sessions: [async ({ browser, baseURL }, use) => {
    const dmContext = await browser.newContext({ baseURL }), playerContext = await browser.newContext({ baseURL }), dm = await dmContext.newPage(), player = await playerContext.newPage();
    const errors: string[] = [], frames: string[] = [], paths: string[] = [];
    for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
    player.on('request', r => paths.push(new URL(r.url()).pathname));
    player.on('websocket', socket => { if (socket.url().endsWith('/ws')) socket.on('framereceived', frame => { if (String(frame.payload).includes('SCENE_CHANGED')) frames.push(String(frame.payload)); }); });
    await player.addInitScript(() => {
      const created: string[] = [], revoked: string[] = []; Object.assign(window, { mapUrls: { created, revoked } });
      const create = URL.createObjectURL.bind(URL), revoke = URL.revokeObjectURL.bind(URL);
      URL.createObjectURL = blob => { const url = create(blob); created.push(url); return url; }; URL.revokeObjectURL = url => { revoked.push(url); revoke(url); };
    });
    try { await use({ dmContext, playerContext, dm, player, errors, frames, paths }); }
    finally { await Promise.all([dmContext.close(), playerContext.close()]); }
  }, { timeout: 20000 }],
});
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
async function csrf(request: APIRequestContext) { return (await request.get('/api/auth/csrf')).json(); }
async function post(request: APIRequestContext, path: string, data: unknown) { const c = await csrf(request); return request.post(path, { headers: { [c.headerName]: c.token }, data }); }
async function click(page: Page, name: string) { const button = page.getByRole('button', { name, exact: true }); await expect(button).toBeEnabled(); await button.click(); }
function sourcePng() {
  function chunk(type: string, data: Buffer) { const bytes = Buffer.concat([Buffer.from(type), data]); let crc = 0xffffffff; for (const b of bytes) { crc ^= b; for (let n = 0; n < 8; n++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); } const size = Buffer.alloc(4), checksum = Buffer.alloc(4); size.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0); return Buffer.concat([size, bytes, checksum]); }
  const header = Buffer.alloc(13); header.writeUInt32BE(96); header.writeUInt32BE(96, 4); header[8] = 8; header[9] = 2;
  const pixels = Buffer.alloc(96 * 289); for (let y = 0; y < 96; y++) for (let x = 0; x < 96; x++) { const at = y * 289 + 1 + x * 3; pixels[at] = x < 48 && y < 48 ? 30 : 221; pixels[at + 1] = x < 48 && y < 48 ? 170 : 22; pixels[at + 2] = 44; }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('tEXt', Buffer.from('private\0SECRET_MAP_METADATA')), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}
async function prepare(s: Sessions) {
  const dmUsername = required('E2E_DM_USERNAME'), playerUsername = required('E2E_PLAYER_USERNAME');
  expect((await post(s.dmContext.request, '/api/auth/login', { username: dmUsername, password: required('E2E_DM_PASSWORD') })).ok()).toBeTruthy();
  expect((await post(s.playerContext.request, '/api/auth/login', { username: playerUsername, password: required('E2E_PLAYER_PASSWORD') })).ok()).toBeTruthy();
  const created = await post(s.dmContext.request, '/api/campaigns', { requestId: randomUUID(), name: `地图交互验收 ${randomUUID().slice(0, 8)}` }); expect(created.ok()).toBeTruthy();
  const campaignId = (await created.json()).id as string, endpoint = `/api/campaigns/${campaignId}`, members = await (await s.dmContext.request.get(`${endpoint}/members`)).json();
  const invitation = await post(s.dmContext.request, `${endpoint}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }); expect(invitation.ok()).toBeTruthy();
  expect((await post(s.playerContext.request, '/api/invitations/accept', { token: (await invitation.json()).token })).ok()).toBeTruthy();
  const listing = await (await s.dmContext.request.get(`${endpoint}/assets`)).json(), c = await csrf(s.dmContext.request), bytes = sourcePng();
  const uploaded = await s.dmContext.request.post(`${endpoint}/assets`, { headers: { [c.headerName]: c.token }, multipart: { command: { name: 'command.json', mimeType: 'application/json', buffer: Buffer.from(JSON.stringify({ requestId: randomUUID(), expectedVersion: listing.campaignVersion })) }, file: { name: 'generated.png', mimeType: 'image/png', buffer: bytes } } });
  expect(uploaded.ok()).toBeTruthy(); const assetId = (await uploaded.json()).asset.id as string;
  const scene = await (await s.dmContext.request.get(`${endpoint}/scene`)).json();
  expect((await post(s.dmContext.request, `${endpoint}/scenes/${scene.id}/map`, { requestId: randomUUID(), expectedVersion: scene.version, map: { assetId, gridSize: 48, offsetX: 0, offsetY: 0, distancePerCell: 5 } })).ok()).toBeTruthy();
  const tokenEndpoint = `${endpoint}/scenes/${scene.id}/tokens`;
  let current = await (await s.dmContext.request.get(`${endpoint}/scene`)).json();
  const publicName = `公开向导 ${randomUUID().slice(0, 8)}`, hiddenName = `SECRET_TOKEN_${randomUUID()}`;
  const publicResponse = await post(s.dmContext.request, tokenEndpoint, { requestId: randomUUID(), expectedVersion: current.version, name: publicName, x: 2, y: 3, hidden: false, controllers: [dmUsername, playerUsername].sort() }); expect(publicResponse.ok()).toBeTruthy(); const token = (await publicResponse.json()).token;
  current = await (await s.dmContext.request.get(`${endpoint}/scene`)).json();
  expect((await post(s.dmContext.request, tokenEndpoint, { requestId: randomUUID(), expectedVersion: current.version, name: hiddenName, x: 0, y: 0, hidden: true, controllers: [dmUsername] })).ok()).toBeTruthy();
  await s.dm.goto('/'); await s.player.goto('/'); await expect(s.dm.locator('.topbar-right')).toContainText(dmUsername); await expect(s.player.locator('.topbar-right')).toContainText(playerUsername);
  await s.dm.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId); await s.player.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
  await expect(s.dm.locator('.connection')).toHaveText('● 实时连接'); await expect(s.player.locator('.connection')).toHaveText('● 实时连接');
  await expect(s.dm.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-interaction-ready', 'ready'); await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-interaction-ready', 'ready');
  await expect(s.dm.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready'); await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toBeEnabled(); await expect(s.player.getByText(hiddenName, { exact: true })).toHaveCount(0);
  return { campaignId, endpoint, sceneId: scene.id as string, assetId, bytes, token, publicName, hiddenName, fogPath: `${endpoint}/scenes/${scene.id}/fog` };
}
async function position(page: Page, x: number, y: number) { const canvas = page.locator('.map-surface canvas'); await canvas.scrollIntoViewIfNeeded(); const b = await canvas.boundingBox(); if (!b) throw new Error('Canvas has no bounds'); return { x: b.x + (x + 0.5) * b.width / 24, y: b.y + (y + 0.5) * b.height / 16 }; }
async function cell(page: Page, x: number, y: number) { const p = await position(page, x, y); await page.mouse.click(p.x, p.y); }
async function drag(page: Page, ax: number, ay: number, bx: number, by: number, revalidate = false) {
  const a = await position(page, ax, ay), b = await position(page, bx, by); await page.mouse.move(a.x, a.y); await page.mouse.down();
  if (revalidate) {
    const read = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/scene') && r.request().method() === 'GET');
    // A real authoritative read is deliberately interleaved while the native
    // pointer remains held, reproducing the periodic-refresh gesture boundary.
    await page.getByRole('button', { name: '↻ 刷新场景', exact: true }).evaluate(button => (button as HTMLButtonElement).click()); await read;
    await expect(page.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  }
  await page.mouse.move(b.x, b.y, { steps: 5 }); await page.mouse.up();
}
async function safeImage(request: APIRequestContext, page: Page, endpoint: string, sceneId: string, version: number, headers: Record<string, string> = {}) {
  const response = await request.get(`${endpoint}/scene/map-image?sceneId=${sceneId}&version=${version}`, { headers }); expect(response.status()).toBe(200);
  expect(response.headers()['cache-control']).toContain('no-store'); expect(response.headers()['cache-control']).toContain('private'); expect(response.headers()['x-content-type-options']).toBe('nosniff'); expect(response.headers()['content-type']).toContain('image/png'); expect(response.headers()['etag']).toBeUndefined(); expect(response.headers()['last-modified']).toBeUndefined();
  const bytes = await response.body(); expect(bytes.includes(Buffer.from('SECRET_MAP_METADATA'))).toBe(false);
  return page.evaluate(async data => { const bytes = Uint8Array.from(atob(data), c => c.charCodeAt(0)), bitmap = await createImageBitmap(new Blob([bytes], { type: 'image/png' })); const canvas = document.createElement('canvas'); canvas.width = bitmap.width; canvas.height = bitmap.height; const ctx = canvas.getContext('2d')!; ctx.drawImage(bitmap, 0, 0); bitmap.close(); return { width: canvas.width, height: canvas.height, public: [...ctx.getImageData(10, 10, 1, 1).data], secret: [...ctx.getImageData(60, 10, 1, 1).data] }; }, bytes.toString('base64'));
}
function assertHints(s: Sessions, assetId: string, hiddenName: string) { expect(s.frames.length).toBeGreaterThan(0); for (const frame of s.frames) { expect(frame).not.toContain(assetId); expect(frame).not.toContain(hiddenName); expect(frame).not.toContain('revealed'); expect(frame).not.toContain('map-image'); } expect(s.paths.filter(p => /^\/api\/campaigns\/[^/]+\/(?:assets(?:\/|$)|scenes\/[^/]+\/(?:map|fog)$)/.test(p))).toEqual([]); }

test('real player Pixi selects, snaps a drag through a same-scene refresh and click-moves without exposing hidden units', async ({ sessions: s }, info) => {
  const { endpoint, publicName, hiddenName, assetId } = await prepare(s);
  await cell(s.player, 2, 3); await expect(s.player.getByRole('heading', { name: publicName, exact: true })).toBeVisible(); await drag(s.player, 2, 3, 4, 5, true);
  await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toContainText('(4, 5)'); await expect(s.dm.getByRole('button', { name: new RegExp(publicName) })).toContainText('(4, 5)');
  await cell(s.player, 6, 7); await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toContainText('(6, 7)');
  expect(JSON.stringify(await (await s.playerContext.request.get(`${endpoint}/scene`)).json())).not.toContain(hiddenName); assertHints(s, assetId, hiddenName);
  await s.player.screenshot({ path: info.outputPath('player-pixi-movement.png'), fullPage: true }); await s.dm.screenshot({ path: info.outputPath('dm-pixi-tabletop.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('real player Pixi keeps geometric measurement through refresh and resets partial pairs', async ({ sessions: s }, info) => {
  const { endpoint, hiddenName } = await prepare(s);
  await click(s.player, '两点测距'); await cell(s.player, 1, 1);
  const read = s.player.waitForResponse(r => new URL(r.url()).pathname === `${endpoint}/scene` && r.request().method() === 'GET');
  await s.player.getByRole('button', { name: '↻ 刷新场景', exact: true }).evaluate(button => (button as HTMLButtonElement).click()); await read;
  await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  await cell(s.player, 4, 5); await expect(s.player.getByLabel('测距结果')).toHaveText('25.00 距离单位');
  await cell(s.player, 1, 1); await click(s.player, '重置视图'); await cell(s.player, 4, 5); await expect(s.player.getByLabel('测距结果')).toContainText('起点 (4, 5)');
  expect(JSON.stringify(await (await s.playerContext.request.get(`${endpoint}/scene`)).json())).not.toContain(hiddenName);
  await s.player.screenshot({ path: info.outputPath('player-pixi-measurement.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('real player Pixi pans the map, zooms at the cursor and resets the viewport', async ({ sessions: s }, info) => {
  const { endpoint, hiddenName } = await prepare(s);
  await click(s.player, '平移地图'); const before = await s.player.locator('.map-surface canvas').screenshot(); await drag(s.player, 0, 0, 1, 1); const after = await s.player.locator('.map-surface canvas').screenshot(); expect(after.equals(before)).toBe(false);
  const center = await position(s.player, 8, 6); await s.player.mouse.move(center.x, center.y); await s.player.mouse.wheel(0, -100); await expect(s.player.getByLabel('地图缩放')).toHaveText('120%'); await click(s.player, '重置视图'); await expect(s.player.getByLabel('地图缩放')).toHaveText('100%');
  expect(JSON.stringify(await (await s.playerContext.request.get(`${endpoint}/scene`)).json())).not.toContain(hiddenName);
  await s.player.screenshot({ path: info.outputPath('player-pixi-interaction.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('DM Pixi rectangle reveal survives a same-scene refresh and saves only explicitly public PNG pixels', async ({ sessions: s }, info) => {
  const { endpoint, sceneId, assetId, hiddenName, publicName, fogPath } = await prepare(s);
  const before = await (await s.playerContext.request.get(`${endpoint}/scene`)).json(); expect((await safeImage(s.playerContext.request, s.player, endpoint, sceneId, before.version)).public).toEqual([0, 0, 0, 255]);
  await click(s.dm, '矩形揭露'); await drag(s.dm, 0, 0, 0, 0, true); await expect(s.dm.getByText('已揭露 1 格', { exact: true })).toBeVisible(); expect((await (await s.dmContext.request.get(fogPath)).json()).revealed).toEqual([]);
  await click(s.dm, '保存地图遮盖'); await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toBeVisible();
  const revealed = await (await s.dmContext.request.get(fogPath)).json(); expect(revealed.revealed).toEqual([{ x: 0, y: 0 }]);
  const pixels = await safeImage(s.playerContext.request, s.player, endpoint, sceneId, revealed.sceneVersion); expect(pixels).toEqual({ width: 1152, height: 768, public: [30, 170, 44, 255], secret: [0, 0, 0, 255] });
  await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toBeEnabled(); await expect(s.player.getByText(hiddenName, { exact: true })).toHaveCount(0); assertHints(s, assetId, hiddenName);
  await s.dm.screenshot({ path: info.outputPath('dm-manual-reveal.png'), fullPage: true }); await s.player.screenshot({ path: info.outputPath('player-revealed-safe-map.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('DM Pixi rectangle cover invalidates old image URLs and preserves original-image and cache protections', async ({ sessions: s }, info) => {
  const { endpoint, sceneId, assetId, hiddenName, publicName, fogPath } = await prepare(s);
  const before = await (await s.dmContext.request.get(fogPath)).json();
  // This case has its own real campaign and HTTP reveal precondition; UI reveal
  // is accepted independently above, keeping each business flow within 60s.
  const prepared = await post(s.dmContext.request, fogPath, { requestId: randomUUID(), expectedVersion: before.sceneVersion, revealed: [{ x: 0, y: 0 }] }); expect(prepared.ok()).toBeTruthy(); const revealed = await prepared.json();
  await expect(s.dm.getByText('已揭露 1 格', { exact: true })).toBeVisible();
  expect((await safeImage(s.playerContext.request, s.player, endpoint, sceneId, revealed.sceneVersion)).public).toEqual([30, 170, 44, 255]);
  for (const id of [assetId, randomUUID()]) { const denied = await s.playerContext.request.get(`${endpoint}/assets/${id}/content`); expect(denied.status()).toBe(403); expect(denied.headers()['cache-control']).toContain('no-store'); }
  expect((await s.playerContext.request.get(`${endpoint}/scene/map-image?sceneId=${sceneId}&version=${before.sceneVersion}`)).status()).toBe(409);
  await click(s.dm, '矩形遮盖'); await drag(s.dm, 0, 0, 0, 0, true); await click(s.dm, '保存地图遮盖'); await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toBeVisible();
  const covered = await (await s.dmContext.request.get(fogPath)).json(); expect(covered.revealed).toEqual([]); expect((await safeImage(s.playerContext.request, s.player, endpoint, sceneId, covered.sceneVersion, { Range: 'bytes=0-50', 'If-None-Match': '*', 'If-Modified-Since': 'Wed, 21 Oct 2099 07:28:00 GMT' })).public).toEqual([0, 0, 0, 255]);
  expect((await s.playerContext.request.get(`${endpoint}/scene/map-image?sceneId=${sceneId}&version=${revealed.sceneVersion}`)).status()).toBe(409); await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toBeEnabled(); await expect(s.player.getByText(hiddenName, { exact: true })).toHaveCount(0); assertHints(s, assetId, hiddenName);
  await s.dm.screenshot({ path: info.outputPath('dm-manual-fog.png'), fullPage: true }); await s.player.screenshot({ path: info.outputPath('player-covered-safe-map.png'), fullPage: true }); expect(s.errors).toEqual([]);
});

test('coordinate fog fallback invalidates displayed public pixels before a failed refresh, revokes the blob and recovers with covered PNG', async ({ sessions: s }, info) => {
  const { endpoint, sceneId, fogPath, publicName } = await prepare(s); await click(s.dm, '加入揭露矩形'); await click(s.dm, '保存地图遮盖');
  await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toBeVisible();
  const revealed = await (await s.dmContext.request.get(fogPath)).json(); await expect.poll(async () => (await (await s.playerContext.request.get(`${endpoint}/scene`)).json()).version).toBe(revealed.sceneVersion);
  await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  const old = await s.player.evaluate(() => (window as unknown as { mapUrls: { created: string[] } }).mapUrls.created.at(-1)!);
  // Only transport delivery is interrupted; the server executes the real cover.
  const scenePattern = `**${endpoint}/scene`; await s.player.route(scenePattern, route => route.abort('failed'));
  await click(s.dm, '加入遮盖矩形'); await click(s.dm, '保存地图遮盖'); await expect(s.dm.getByText('遮盖请求已确认；当前揭露状态已从服务器刷新。', { exact: true })).toBeVisible();
  await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'empty'); await expect(s.player.locator('.save-status')).toContainText('场景刷新失败');
  expect(await s.player.evaluate(url => (window as unknown as { mapUrls: { revoked: string[] } }).mapUrls.revoked.includes(url), old)).toBe(true);
  expect(await s.player.evaluate(async url => { try { await fetch(url); return false; } catch { return true; } }, old)).toBe(true);
  await expect(s.player.getByRole('button', { name: new RegExp(publicName) })).toBeEnabled();
  await s.player.unroute(scenePattern); await click(s.player, '↻ 刷新场景'); await expect(s.player.getByRole('img', { name: '场景网格地图' })).toHaveAttribute('data-map-image', 'ready');
  const covered = await (await s.dmContext.request.get(fogPath)).json(); expect((await safeImage(s.playerContext.request, s.player, endpoint, sceneId, covered.sceneVersion)).public).toEqual([0, 0, 0, 255]);
  await s.player.screenshot({ path: info.outputPath('player-recovered-covered-map.png'), fullPage: true }); expect(s.errors).toEqual([]);
});
