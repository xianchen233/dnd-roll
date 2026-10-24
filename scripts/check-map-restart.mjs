// Dedicated acceptance accounts/database. Externally restart real services between modes.
import assert from 'node:assert/strict';
import { randomUUID, createHash } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { deflateSync, inflateSync } from 'node:zlib';

const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath, 'Usage: node scripts/check-map-restart.mjs seed|verify <outside-repo-evidence.json>');
const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL'), hash = bytes => createHash('sha256').update(bytes).digest('hex');
class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', json, multipart, status = 200, headers: extra = {} } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; '), ...extra };
    if (json !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL(`/api${path}`, base), { method, headers, cache: 'no-store', ...(json !== undefined ? { body: JSON.stringify(json) } : multipart ? { body: multipart } : {}) });
    for (const cookie of response.headers.getSetCookie()) { const [name, ...value] = cookie.split(';')[0].split('='); this.cookies.set(name, value.join('=')); }
    assert.equal(response.status, status, `${method} ${path}`);
    return response;
  }
  async get(path) { return (await this.call(path)).json(); }
  async post(path, json, status = 200) { const r = await this.call(path, { method: 'POST', json, status }); return status === 204 ? undefined : r.json(); }
  async login(username, password) {
    this.csrf = await this.get('/auth/csrf'); await this.post('/auth/login', { username, password }); this.csrf = await this.get('/auth/csrf'); return this;
  }
}
function privateHeaders(response) {
  assert.match(response.headers.get('cache-control'), /no-store/); assert.match(response.headers.get('cache-control'), /private/);
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  assert.equal(response.headers.get('etag'), null); assert.equal(response.headers.get('last-modified'), null);
}
function chunk(type, data) {
  const bytes = Buffer.concat([Buffer.from(type), data]); let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let n = 0; n < 8; n++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
  const size = Buffer.alloc(4), checksum = Buffer.alloc(4); size.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
  return Buffer.concat([size, bytes, checksum]);
}
function sourcePng() {
  const header = Buffer.alloc(13); header.writeUInt32BE(16); header.writeUInt32BE(8, 4); header[8] = 8; header[9] = 2;
  const pixels = Buffer.alloc(8 * 49);
  for (let y = 0; y < 8; y++) for (let x = 0; x < 16; x++) { const at = y * 49 + 1 + x * 3; pixels[at] = x < 8 ? 20 : 240; pixels[at + 1] = x < 8 ? 180 : 30; pixels[at + 2] = x < 8 ? 60 : 210; }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
}
// Decode actual server PNG rows, including all PNG prediction filters. Fixtures
// use known public/secret colors so byte hashes alone cannot hide pixel leaks.
function decode(bytes) {
  assert.deepEqual([...bytes.subarray(0, 8)], [137,80,78,71,13,10,26,10]);
  let width, height, channels; const data = [];
  for (let at = 8; at < bytes.length;) {
    const size = bytes.readUInt32BE(at), name = bytes.toString('ascii', at + 4, at + 8), part = bytes.subarray(at + 8, at + 8 + size); at += size + 12;
    assert.ok(['IHDR', 'IDAT', 'IEND'].includes(name), `Derived PNG contains no source metadata: ${name}`);
    if (name === 'IHDR') { width = part.readUInt32BE(0); height = part.readUInt32BE(4); assert.equal(part[8], 8); assert.ok([2,6].includes(part[9])); channels = part[9] === 2 ? 3 : 4; assert.equal(part[12], 0); }
    if (name === 'IDAT') data.push(part);
  }
  const raw = inflateSync(Buffer.concat(data)), stride = width * channels, rows = Buffer.alloc(height * stride);
  assert.equal(raw.length, height * (stride + 1));
  const paeth = (a,b,c) => { const p = a+b-c, pa = Math.abs(p-a), pb = Math.abs(p-b), pc = Math.abs(p-c); return pa <= pb && pa <= pc ? a : pb <= pc ? b : c; };
  for (let y = 0; y < height; y++) {
    const filter = raw[y * (stride + 1)]; assert.ok(filter <= 4);
    for (let x = 0; x < stride; x++) {
      const a = x < channels ? 0 : rows[y * stride + x - channels], b = y ? rows[(y-1) * stride + x] : 0, c = y && x >= channels ? rows[(y-1) * stride + x - channels] : 0;
      rows[y * stride + x] = (raw[y * (stride+1) + 1 + x] + [0,a,b,Math.floor((a+b)/2),paeth(a,b,c)][filter]) & 255;
    }
  }
  return { width, height, pixel(x,y) { const at = (y * width + x) * channels; return [...rows.subarray(at, at+3), channels === 4 ? rows[at+3] : 255]; } };
}
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
const player = await new Session().login(required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
async function image(session, prefix, view, options = {}) {
  const response = await session.call(`${prefix}/scene/map-image?sceneId=${view.sceneId}&version=${view.sceneVersion}`, options); privateHeaders(response);
  if (response.status !== 200) return;
  assert.match(response.headers.get('content-type'), /^image\/png/);
  const bytes = Buffer.from(await response.arrayBuffer()), png = decode(bytes); assert.equal(png.width, 1152); assert.equal(png.height, 768);
  return { bytes, png };
}
if (mode === 'seed') {
  const campaign = await dm.post('/campaigns', { requestId: randomUUID(), name: `zz P02d restart ${randomUUID().slice(0,8)}` }, 201);
  const prefix = `/campaigns/${campaign.id}`, scene = await dm.get(`${prefix}/scene`), members = await dm.get(`${prefix}/members`);
  const invitation = await dm.post(`${prefix}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }, 201);
  await player.post('/invitations/accept', { token: invitation.token });
  const listing = await dm.get(`${prefix}/assets`), multipart = new FormData();
  multipart.append('command', new Blob([JSON.stringify({ requestId: randomUUID(), expectedVersion: listing.campaignVersion })], { type: 'application/json' }));
  multipart.append('file', new Blob([sourcePng()], { type: 'image/png' }), 'generated.png');
  const uploaded = await (await dm.call(`${prefix}/assets`, { method: 'POST', multipart })).json();
  await dm.post(`${prefix}/scenes/${scene.id}/map`, { requestId: randomUUID(), expectedVersion: scene.version, map: { assetId: uploaded.asset.id, gridSize: 8, offsetX: 0, offsetY: 0, distancePerCell: 5 } });
  const path = `${prefix}/scenes/${scene.id}/fog`, covered = await dm.get(path), oldView = await player.get(`${prefix}/scene/map-view`);
  assert.deepEqual(oldView, { sceneId: scene.id, sceneVersion: covered.sceneVersion, map: { distancePerCell: 5 } });
  const black = await image(player, prefix, oldView); assert.deepEqual(black.png.pixel(20,20), [0,0,0,255]); assert.deepEqual(black.png.pixel(68,20), [0,0,0,255]);
  const reveal = { requestId: randomUUID(), expectedVersion: covered.sceneVersion, revealed: [{ x:0,y:0 }] }, revealed = await dm.post(path, reveal);
  const publicView = await player.get(`${prefix}/scene/map-view`), published = await image(player, prefix, publicView);
  assert.deepEqual(published.png.pixel(20,20), [20,180,60,255]); assert.deepEqual(published.png.pixel(68,20), [0,0,0,255]);
  const full = await image(dm, prefix, publicView); assert.deepEqual(full.png.pixel(68,20), [240,30,210,255]);
  await image(player, prefix, oldView, { status:409 });
  const cover = { requestId:randomUUID(), expectedVersion:revealed.sceneVersion, revealed:[] }, finalFog = await dm.post(path, cover);
  const view = await player.get(`${prefix}/scene/map-view`), final = await image(player, prefix, view);
  assert.deepEqual(final.png.pixel(20,20), [0,0,0,255]);
  await image(player, prefix, publicView, { status:409, headers:{'If-None-Match':'*','If-Modified-Since':'Wed, 21 Oct 2099 07:28:00 GMT','Range':'bytes=0-100'} });
  assert.deepEqual(await dm.post(path, reveal), revealed); assert.deepEqual(await dm.get(path), finalFog);
  const original = await player.call(`${prefix}/assets/${uploaded.asset.id}/content`, {status:403}); privateHeaders(original);
  await writeFile(evidencePath, JSON.stringify({prefix,path,sceneId:scene.id,assetId:uploaded.asset.id,oldView,publicView,view,reveal,revealed,cover,finalFog,sha256:hash(final.bytes)}), {flag:'wx'});
  console.log('PASS: actual PNG public/secret pixels, full-cover default, stale/conditional URLs, original denial and historical fog receipt checked before restart.');
} else {
  const saved = JSON.parse(await readFile(evidencePath,'utf8'));
  assert.deepEqual(await dm.get(saved.path), saved.finalFog); assert.deepEqual(await player.get(`${saved.prefix}/scene/map-view`), saved.view);
  const final = await image(player, saved.prefix, saved.view); assert.equal(hash(final.bytes), saved.sha256); assert.deepEqual(final.png.pixel(20,20), [0,0,0,255]); assert.deepEqual(final.png.pixel(68,20), [0,0,0,255]);
  assert.deepEqual(await dm.post(saved.path,saved.reveal), saved.revealed); assert.deepEqual(await dm.post(saved.path,saved.cover), saved.finalFog); assert.deepEqual(await dm.get(saved.path),saved.finalFog);
  for (const view of [saved.oldView,saved.publicView]) await image(player,saved.prefix,view,{status:409});
  await dm.post(saved.path,{...saved.cover,requestId:randomUUID(),expectedVersion:saved.revealed.sceneVersion},409);
  const members = await dm.get(`${saved.prefix}/members`);
  await dm.post(`${saved.prefix}/members/${encodeURIComponent(required('E2E_PLAYER_USERNAME'))}/remove`,{requestId:randomUUID(),expectedVersion:members.campaignVersion},204);
  await image(player,saved.prefix,saved.view,{status:403}); privateHeaders(await player.call(`${saved.prefix}/assets/${saved.assetId}/content`,{status:403}));
  await dm.call('/auth/logout',{method:'POST',status:204}); await image(dm,saved.prefix,saved.view,{status:401});
  console.log('PASS: real restart preserves fog/version/receipts and masked PNG hash; stale URLs, historical reveal, revocation and logout cannot retrieve secret pixels.');
}
