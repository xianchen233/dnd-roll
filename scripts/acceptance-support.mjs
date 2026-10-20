// HTTP and generated PNG helpers for the integrated P02 restart acceptance.
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';

import { deflateSync, inflateSync } from 'node:zlib';



const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL'), hash = bytes => createHash('sha256').update(bytes).digest('hex');
class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', json, multipart, status = 200, headers: extra = {} } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; '), ...extra };
    if (json !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL(`/api${path}`, base), { method, headers, signal: AbortSignal.timeout(15000), cache: 'no-store', ...(json !== undefined ? { body: JSON.stringify(json) } : multipart ? { body: multipart } : {}) });
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

export { Session, required, hash, privateHeaders, sourcePng, decode };
