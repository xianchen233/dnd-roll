// Real HTTP smoke across an externally performed backend + database restart.
// Use only with dedicated acceptance databases/accounts, never production data.
import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import { createHash, randomUUID } from 'node:crypto';
import { deflateSync } from 'node:zlib';

const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath, 'Usage: node scripts/check-asset-restart.mjs seed|verify <outside-repo-evidence.json>');
const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL');
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', json, multipart, status = 200 } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ') };
    if (json !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL(`/api${path}`, base), { method, headers, ...(json !== undefined ? { body: JSON.stringify(json) } : multipart ? { body: multipart } : {}) });
    for (const cookie of response.headers.getSetCookie()) { const [name, ...value] = cookie.split(';')[0].split('='); this.cookies.set(name, value.join('=')); }
    assert.equal(response.status, status, `${method} ${path}`);
    return response;
  }
  async get(path) { return (await this.call(path)).json(); }
  async post(path, body, status = 200) { return (await this.call(path, { method: 'POST', json: body, status })).json(); }
  async login(username, password) {
    this.csrf = await this.get('/auth/csrf');
    await this.post('/auth/login', { username, password });
    this.csrf = await this.get('/auth/csrf'); return this;
  }
}

// Build a valid, deterministic PNG without depending on image packages.
function png() {
  function chunk(type, data) {
    const name = Buffer.from(type), content = Buffer.concat([name, data]); let crc = 0xffffffff;
    for (const byte of content) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
    const length = Buffer.alloc(4), checksum = Buffer.alloc(4); length.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
    return Buffer.concat([length, content, checksum]);
  }
  const header = Buffer.alloc(13); header.writeUInt32BE(64, 0); header.writeUInt32BE(64, 4); header[8] = 8; header[9] = 2;
  const pixels = Buffer.alloc(64 * (1 + 64 * 3));
  for (let y = 0; y < 64; y++) for (let x = 0; x < 64; x++) { const offset = y * 193 + 1 + x * 3; pixels[offset] = x * 4; pixels[offset + 1] = y * 4; pixels[offset + 2] = 100; }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}
const image = png();
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
async function upload(prefix, command) {
  const multipart = new FormData();
  multipart.append('command', new Blob([JSON.stringify(command)], { type: 'application/json' }));
  multipart.append('file', new Blob([image], { type: 'image/png' }), '../ignored-client-name.png');
  return (await dm.call(`${prefix}/assets`, { method: 'POST', multipart })).json();
}
async function content(prefix, id) {
  const response = await dm.call(`${prefix}/assets/${id}/content`);
  assert.match(response.headers.get('cache-control'), /no-store/);
  assert.match(response.headers.get('cache-control'), /private/);
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  assert.match(response.headers.get('content-disposition'), /^attachment/);
  assert.equal(response.headers.get('content-type'), 'image/png');
  return Buffer.from(await response.arrayBuffer());
}

if (mode === 'seed') {
  const campaign = await dm.post('/campaigns', { requestId: randomUUID(), name: `zz P02b restart ${randomUUID().slice(0, 8)}` }, 201);
  const prefix = `/campaigns/${campaign.id}`, scene = await dm.get(`${prefix}/scene`), assets = await dm.get(`${prefix}/assets`);
  const uploadCommand = { requestId: randomUUID(), expectedVersion: assets.campaignVersion };
  const uploaded = await upload(prefix, uploadCommand);
  const mapCommand = { requestId: randomUUID(), expectedVersion: scene.version, map: { assetId: uploaded.asset.id, gridSize: 32, offsetX: -3, offsetY: 7, distancePerCell: 2.5 } };
  const mapped = await dm.post(`${prefix}/scenes/${scene.id}/map`, mapCommand);
  assert.equal(hash(await content(prefix, uploaded.asset.id)), hash(image));
  await writeFile(evidencePath, JSON.stringify({ prefix, sceneId: scene.id, uploadCommand, uploaded, mapCommand, mapped, sha256: hash(image) }), { flag: 'wx' });
  console.log('PASS: private asset bytes, map reference and durable receipts saved before restart.');
} else {
  const saved = JSON.parse(await readFile(evidencePath, 'utf8'));
  assert.equal(hash(await content(saved.prefix, saved.uploaded.asset.id)), saved.sha256);
  assert.deepEqual(await dm.get(`${saved.prefix}/scenes/${saved.sceneId}/map`), saved.mapped);
  assert.deepEqual(await upload(saved.prefix, saved.uploadCommand), saved.uploaded);
  assert.deepEqual(await dm.post(`${saved.prefix}/scenes/${saved.sceneId}/map`, saved.mapCommand), saved.mapped);
  const assets = await dm.get(`${saved.prefix}/assets`);
  assert.equal(assets.campaignVersion, saved.uploaded.campaignVersion);
  assert.equal(assets.assets.filter(asset => asset.id === saved.uploaded.asset.id).length, 1);
  const detached = await dm.post(`${saved.prefix}/scenes/${saved.sceneId}/map`, { requestId: randomUUID(), expectedVersion: saved.mapped.sceneVersion, map: null });
  assert.equal(detached.map, null);
  assert.deepEqual(await dm.post(`${saved.prefix}/scenes/${saved.sceneId}/map`, saved.mapCommand), saved.mapped);
  assert.deepEqual(await dm.get(`${saved.prefix}/scenes/${saved.sceneId}/map`), detached, 'Historical receipt must not reattach');
  assert.equal(hash(await content(saved.prefix, saved.uploaded.asset.id)), saved.sha256, 'Detach does not delete committed asset');
  const player = await new Session().login(required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
  const denied = await player.call(`${saved.prefix}/assets/${saved.uploaded.asset.id}/content`, { status: 403 });
  assert.match(denied.headers.get('cache-control'), /no-store/);
  // New session authenticated as the same DM, foreign campaign path still cannot retrieve the file.
  const other = await dm.post('/campaigns', { requestId: randomUUID(), name: `zz P02b foreign ${randomUUID().slice(0, 8)}` }, 201);
  await dm.call(`/campaigns/${other.id}/assets/${saved.uploaded.asset.id}/content`, { status: 404 });
  await dm.call('/auth/logout', { method: 'POST', status: 204 });
  await dm.call(`${saved.prefix}/assets/${saved.uploaded.asset.id}/content`, { status: 401 });
  console.log('PASS: new sessions after restart preserved exact file SHA-256, map calibration, versions and original upload/map receipts; historical map replay has no effect; direct player/foreign/logout downloads denied.');
}
