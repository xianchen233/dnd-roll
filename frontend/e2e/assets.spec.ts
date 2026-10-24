import { createHash, randomUUID } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { deflateSync } from 'node:zlib';
import { test as base, expect, type BrowserContext, type Page } from '@playwright/test';
type Sessions = { dmContext: BrowserContext; playerContext: BrowserContext; campaignId: string; originalId: string };
const test = base.extend<{ sessions: Sessions }>({
  sessions: [async ({ browser, baseURL }, use) => {
    const sessions = { dmContext: await browser.newContext({ baseURL }), playerContext: await browser.newContext({ baseURL }), campaignId: '', originalId: '' };
    try { await use(sessions); }
    finally {
      try {
        if (sessions.campaignId && sessions.originalId) {
          const endpoint = `/api/campaigns/${encodeURIComponent(sessions.campaignId)}`, response = await sessions.dmContext.request.get(`${endpoint}/scenes`, { timeout: 5000 });
          if (response.ok()) {
            const list = await response.json();
            if (list.currentSceneId !== sessions.originalId) {
              const csrf = await (await sessions.dmContext.request.get('/api/auth/csrf', { timeout: 5000 })).json();
              expect((await sessions.dmContext.request.post(`${endpoint}/scenes/${sessions.originalId}/activate`, { timeout: 5000, headers: { [csrf.headerName]: csrf.token }, data: { requestId: randomUUID(), expectedVersion: list.campaignVersion } })).ok()).toBeTruthy();
            }
          }
        }
      } finally { await sessions.dmContext.close(); await sessions.playerContext.close(); }
    }
  }, { timeout: 20000 }],
});
function required(name: string) { const value = process.env[name]; if (!value?.trim()) throw new Error(`Missing required E2E environment variable: ${name}`); return value; }
async function login(page: Page, username: string, password: string) {
  await page.goto('/'); await page.getByLabel('账号', { exact: true }).fill(username); await page.getByLabel('密码', { exact: true }).fill(password);
  await page.getByRole('button', { name: '进入冒险', exact: true }).click(); await expect(page.locator('.topbar-right')).toContainText(username);
}
// A real, lossless PNG generated at runtime. No binary fixture or private map is
// checked into source, and downloads are compared byte-for-byte by SHA-256.
function png() {
  function chunk(type: string, data: Buffer) {
    const bytes = Buffer.concat([Buffer.from(type), data]); let crc = 0xffffffff;
    for (const byte of bytes) { crc ^= byte; for (let n = 0; n < 8; n++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
    const size = Buffer.alloc(4), checksum = Buffer.alloc(4); size.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
    return Buffer.concat([size, bytes, checksum]);
  }
  const header = Buffer.alloc(13); header.writeUInt32BE(96); header.writeUInt32BE(64, 4); header[8] = 8; header[9] = 2;
  const pixels = Buffer.alloc(64 * (96 * 3 + 1));
  for (let y = 0; y < 64; y++) for (let x = 0; x < 96; x++) { const at = y * 289 + 1 + x * 3; pixels[at] = x % 16 ? 58 : 184; pixels[at + 1] = y % 16 ? 94 : 153; pixels[at + 2] = 103; }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}
test('DM uploads, downloads and calibrates a durable private map; players cannot fetch original or metadata', async ({ sessions }, testInfo) => {
  const { dmContext, playerContext } = sessions;
  const dm = await dmContext.newPage(), player = await playerContext.newPage();
  dm.setDefaultTimeout(10000); player.setDefaultTimeout(10000);
  const errors: string[] = [], playerPaths: string[] = [];
  for (const page of [dm, player]) page.on('pageerror', e => errors.push(e.message));
  player.on('request', r => playerPaths.push(new URL(r.url()).pathname));
  await login(dm, required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD')); await login(player, required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
  await expect(player.getByRole('combobox', { name: '选择战役' })).not.toHaveValue('');
  const campaignId = await player.getByRole('combobox', { name: '选择战役' }).inputValue(); sessions.campaignId = campaignId; expect(campaignId).not.toBe('');
  await dm.getByRole('combobox', { name: '选择战役' }).selectOption(campaignId);
  const endpoint = `/api/campaigns/${encodeURIComponent(campaignId)}`;
  const original = await (await playerContext.request.get(`${endpoint}/scene`)).json(); sessions.originalId = original.id;
  const name = `私密地图备团 ${randomUUID().slice(0, 8)}`;
  await dm.getByLabel('场景名称', { exact: true }).fill(name); await dm.getByRole('button', { name: '创建空场景', exact: true }).click();
  await expect(dm.getByRole('button', { name: `切换到 ${name}`, exact: true })).toBeEnabled(); await dm.getByRole('button', { name: `切换到 ${name}`, exact: true }).click();
  await expect(dm.getByRole('heading', { name, exact: true })).toBeVisible(); await expect(player.getByRole('heading', { name, exact: true })).toBeVisible({ timeout: 8000 });
  const prepared = await (await dmContext.request.get(`${endpoint}/scene`)).json();
  const bytes = png(), hash = (data: Buffer) => createHash('sha256').update(data).digest('hex');
  await dm.getByLabel('上传地图文件', { exact: true }).setInputFiles({ name: 'private-preparation.png', mimeType: 'image/png', buffer: bytes });
  const upload = dm.waitForResponse(r => r.url().endsWith(`${endpoint}/assets`) && r.request().method() === 'POST');
  await dm.getByRole('button', { name: '上传素材', exact: true }).click(); const response = await upload; expect(response.status()).toBe(200);
  const { asset } = await response.json(); expect(asset).toMatchObject({ width: 96, height: 64, byteSize: bytes.length, mediaType: 'image/png' });
  await expect(dm.getByText('上传请求已确认；素材列表已刷新。', { exact: true })).toBeVisible();
  await dm.getByRole('button', { name: `预览 ${asset.id}`, exact: true }).click(); await expect(dm.getByAltText('DM 私密地图预览')).toBeVisible();
  await expect.poll(() => dm.getByAltText('DM 私密地图预览').evaluate((image: HTMLImageElement) => image.naturalWidth)).toBe(96);
  expect(await dm.getByAltText('DM 私密地图预览').getAttribute('src')).toMatch(/^blob:/);
  const downloadEvent = dm.waitForEvent('download'); await dm.getByRole('button', { name: `下载 ${asset.id}`, exact: true }).click(); const download = await downloadEvent;
  expect(hash(await readFile((await download.path())!))).toBe(hash(bytes));
  const downloaded = await dmContext.request.get(`${endpoint}/assets/${asset.id}/content`); expect(downloaded.status()).toBe(200); expect(hash(await downloaded.body())).toBe(hash(bytes));
  expect(downloaded.headers()['cache-control']).toContain('no-store'); expect(downloaded.headers()['x-content-type-options']).toBe('nosniff'); expect(downloaded.headers()['content-disposition']).toContain('attachment');
  // The select has an explicit accessible name, independent of option text.
  await dm.getByRole('combobox', { name: '当前场景地图素材', exact: true }).selectOption(asset.id); await dm.getByLabel('图片每格像素', { exact: true }).fill('64');
  await dm.getByLabel('图片偏移 X', { exact: true }).fill('-12'); await dm.getByLabel('图片偏移 Y', { exact: true }).fill('7'); await dm.getByLabel('每格距离', { exact: true }).fill('5');
  await dm.getByRole('button', { name: '保存地图配置', exact: true }).click(); await expect(dm.getByText('地图请求已确认；当前配置已从服务器刷新。', { exact: true })).toBeVisible();
  const saved = await (await dmContext.request.get(`${endpoint}/scenes/${prepared.id}/map`)).json(); expect(saved.map).toEqual({ assetId: asset.id, gridSize: 64, offsetX: -12, offsetY: 7, distancePerCell: 5 });
  expect(saved.sceneVersion).toBe(prepared.version + 1);
  await expect(player.getByRole('heading', { name: '地图备团', exact: true })).toHaveCount(0);
  // Production bundles are public /assets/*.js; only campaign API paths carry
  // private originals or authoring metadata. Report actual offending paths.
  expect(playerPaths.filter(p => /^\/api\/campaigns\/[^/]+\/(?:assets(?:\/|$)|scenes\/[^/]+\/map$)/.test(p))).toEqual([]);
  const playerSnapshot = await (await playerContext.request.get(`${endpoint}/scene`)).json(); expect(Object.keys(playerSnapshot).sort()).toEqual(Object.keys(original).sort());
  expect(playerSnapshot).toMatchObject({ id: prepared.id, width: 24, height: 16, gridSize: 48, tokens: [] }); expect(JSON.stringify(playerSnapshot)).not.toContain(asset.id);
  for (const path of [`${endpoint}/assets`, `${endpoint}/assets/${asset.id}/content`, `${endpoint}/assets/${randomUUID()}/content`, `${endpoint}/scenes/${prepared.id}/map`]) {
    const denial = await playerContext.request.get(path); expect(denial.status()).toBe(403); expect(await denial.text()).not.toContain(asset.id);
  }
  const direct = await playerContext.newPage(); expect((await direct.goto(`${endpoint}/assets/${asset.id}/content`))?.status()).toBe(403); await direct.close();
  await dm.getByRole('button', { name: `切换到 ${original.name}`, exact: true }).click(); await expect(dm.getByRole('heading', { name: original.name, exact: true })).toBeVisible();
  await expect(dm.getByAltText('DM 私密地图预览')).toHaveCount(0);
  await dm.getByRole('button', { name: `切换到 ${name}`, exact: true }).click(); await expect(dm.getByRole('heading', { name, exact: true })).toBeVisible();
  await expect(dm.getByRole('combobox', { name: '当前场景地图素材', exact: true })).toHaveValue(asset.id); await expect(dm.getByLabel('图片每格像素', { exact: true })).toHaveValue('64');
  expect(await (await dmContext.request.get(`${endpoint}/scenes/${prepared.id}/map`)).json()).toEqual(saved);
  await dm.screenshot({ path: testInfo.outputPath('dm-private-map-preparation.png'), fullPage: true }); await player.screenshot({ path: testInfo.outputPath('player-no-private-map.png'), fullPage: true }); expect(errors).toEqual([]);
});
