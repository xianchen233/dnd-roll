// Bounded acceptance workload; measurements describe this host, not cloud sizing.
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import { access, writeFile } from 'node:fs/promises';
import { cpus, totalmem, platform, arch } from 'node:os';
import { deflateSync } from 'node:zlib';
import { Session, required, privateHeaders } from './acceptance-support.mjs';

const [file] = process.argv.slice(2); assert.ok(file, 'Pass a new external evidence JSON path');
await access(file).then(() => assert.fail('Evidence file already exists; choose a new path before seeding'), error => { if (error.code !== 'ENOENT') throw error; });
const metrics = {}, timed = async (name, operation) => { const start = performance.now(); const result = await operation(); (metrics[name] ??= []).push(performance.now() - start); return result; };
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
const c = await dm.post('/campaigns', { requestId: randomUUID(), name: `ZZZ P10 capacity ${randomUUID().slice(0, 8)}` }, 201), root = `/campaigns/${c.id}`, sessions = [dm];
const version = async () => (await dm.get(root + '/members')).campaignVersion;
for (let i = 0; i < 8; i++) {
  const invite = await dm.post(root + '/invitations', { requestId: randomUUID(), expectedVersion: await version() }, 201);
  const session = new Session(); session.csrf = await session.get('/auth/csrf');
  const username = `load_${randomBytes(6).toString('hex')}`, password = randomBytes(24).toString('base64url');
  await session.post('/invitations/register', { token: invite.token, username, password }, 201);
  await session.login(username, password); sessions.push(session);
}

function chunk(type, data) {
  const bytes = Buffer.concat([Buffer.from(type), data]); let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0); }
  const size = Buffer.alloc(4), checksum = Buffer.alloc(4); size.writeUInt32BE(data.length); checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
  return Buffer.concat([size, bytes, checksum]);
}
const width = 1152, height = 768, header = Buffer.alloc(13), pixels = Buffer.alloc(height * (width * 3 + 1));
header.writeUInt32BE(width); header.writeUInt32BE(height, 4); header[8] = 8; header[9] = 2;
let noise = 123456789;
for (let y = 0; y < height; y++) for (let x = 0; x < width * 3; x++) { noise ^= noise << 13; noise ^= noise >>> 17; noise ^= noise << 5; pixels[y * (width * 3 + 1) + 1 + x] = noise & 255; }
const png = Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]), chunk('IHDR', header), chunk('IDAT', deflateSync(pixels)), chunk('IEND', Buffer.alloc(0))]);
const multipart = new FormData(); multipart.set('command', new Blob([JSON.stringify({ requestId: randomUUID(), expectedVersion: await version() })], { type: 'application/json' }), 'command.json'); multipart.set('file', new Blob([png], { type: 'image/png' }), 'noise.png');
const asset = await timed('upload', async () => (await dm.call(root + '/assets', { method: 'POST', multipart })).json());
let scene = await dm.get(root + '/scene');
await dm.post(`${root}/scenes/${scene.id}/map`, { requestId: randomUUID(), expectedVersion: scene.version, map: { assetId: asset.asset.id, gridSize: 48, offsetX: 0, offsetY: 0, distancePerCell: 5 } });
for (let i = 0; i < 100; i++) {
  scene = await dm.get(root + '/scene');
  await dm.post(`${root}/scenes/${scene.id}/tokens`, { requestId: randomUUID(), expectedVersion: scene.version, name: `Capacity unit ${i}`, x: i % 24, y: Math.floor(i / 24), hidden: false, controllers: [] });
}
scene = await dm.get(root + '/scene');
await dm.post(`${root}/scenes/${scene.id}/fog`, { requestId: randomUUID(), expectedVersion: scene.version, revealed: Array.from({ length: 384 }, (_, i) => ({ x: i % 24, y: Math.floor(i / 24) })) });
scene = await dm.get(root + '/scene');
const mapBytes = await Promise.all(sessions.map(session => timed('mapLoad', async () => { const response = await session.call(`${root}/scene/map-image?sceneId=${scene.id}&version=${scene.version}`); privateHeaders(response); return (await response.arrayBuffer()).byteLength; })));
const sceneCampaignVersion = await version();
for (let round = 0; round < 8; round++) await Promise.all(sessions.map(session => timed('append', () => session.post(root + '/messages', { requestId: randomUUID(), expectedVersion: sceneCampaignVersion, characterId: '', kind: 'OOC', text: `Capacity round ${round} ${'长期文字记录 '.repeat(128)}`, replyTo: '', attachments: [] }))));

async function reads() {
  for (let round = 0; round < 12; round++) await Promise.all(sessions.map(session => timed('snapshotAndTimeline', async () => { assert.equal((await session.get(root + '/scene')).tokens.length, 100); assert.equal((await session.get(root + '/timeline')).entries.length, 50); })));
}
await reads();
const archive = await dm.get(root + '/timeline/export?format=json'); assert.equal(archive.entries.length, 72);
const backupStart = performance.now(), backupStatus = await dm.get('/admin/backups');
const ack = await dm.post('/admin/backups', { requestId: randomUUID(), expectedVersion: backupStatus.version });
let job;
for (let i = 0; i < 120; i++) { job = (await dm.get('/admin/backups')).jobs.find(j => j.id === ack.id); if (['COMPLETE', 'FAILED', 'INTERRUPTED'].includes(job?.state)) break; await new Promise(resolve => setTimeout(resolve, 500)); }
assert.equal(job?.state, 'COMPLETE');
metrics.backup = [performance.now() - backupStart];
const bytes = await timed('backupDownload', async () => { const response = await dm.call('/admin/backups/' + job.id + '/content'); privateHeaders(response); return (await response.arrayBuffer()).byteLength; }); assert.equal(bytes, job.bytes);
assert.equal((await dm.get(root + '/timeline/export?format=json')).entries.length, 72); assert.equal((await dm.get('/admin/backups')).paused, false);
const stats = Object.fromEntries(Object.entries(metrics).map(([name, values]) => { const sorted = [...values].sort((a, b) => a - b); return [name, { samples: values.length, p50Ms: Math.round(sorted[Math.ceil(sorted.length * .5) - 1]), p95Ms: Math.round(sorted[Math.ceil(sorted.length * .95) - 1]), maxMs: Math.round(sorted.at(-1)) }]; }));
const evidence = { at: new Date().toISOString(), host: { platform: platform(), arch: arch(), logicalCpus: cpus().length, totalMemoryBytes: totalmem() }, workload: { sessions: 9, players: 8, tokens: 100, messages: 72, imageWidth: width, imageHeight: height, sourceImageBytes: png.length, mapResponseBytes: mapBytes, totalMapBytes: mapBytes.reduce((a,b) => a+b,0), historyExportBytes: Buffer.byteLength(JSON.stringify(archive)), backupBytes: bytes, errors: 0 }, stats, limitations: ['Local HTTP workload; no WAN/TLS/physical cloud sizing or physical phone claim.', 'Nine independently authenticated HTTP sessions; browser/WebSocket correctness is checked by the separate campaign and browser suite.', 'Only successful latency samples; unexpected HTTP status fails the workload immediately.'] };
await writeFile(file, JSON.stringify(evidence, null, 2), { flag: 'wx' });
console.log('CAPACITY VERIFIED: ' + JSON.stringify(evidence));
