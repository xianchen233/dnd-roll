// Dedicated acceptance database/accounts only; restart real services between modes.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
import { Session, required, hash, privateHeaders, sourcePng, decode } from './acceptance-support.mjs';

const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath,
  'Usage: node scripts/check-p02-restart.mjs seed|verify <outside-repo-new-evidence.json>');
const dmName = required('E2E_DM_USERNAME'), playerName = required('E2E_PLAYER_USERNAME');
const dm = await new Session().login(dmName, required('E2E_DM_PASSWORD'));
const player = await new Session().login(playerName, required('E2E_PLAYER_PASSWORD'));
const command = fields => ({ requestId: randomUUID(), ...fields });
async function activate(prefix, sceneId) {
  const list = await dm.get(`${prefix}/scenes`);
  const body = command({ expectedVersion: list.campaignVersion });
  const result = await dm.post(`${prefix}/scenes/${sceneId}/activate`, body);
  return { body, result };
}
async function image(session, prefix, view, status = 200) {
  const response = await session.call(`${prefix}/scene/map-image?sceneId=${view.sceneId}&version=${view.sceneVersion}`, { status });
  privateHeaders(response);
  if (status !== 200) return;
  assert.match(response.headers.get('content-type'), /^image\/png/);
  const bytes = Buffer.from(await response.arrayBuffer()), png = decode(bytes);
  assert.equal(png.width, 1152); assert.equal(png.height, 768);
  return { sha256: hash(bytes), png };
}
function pixels(png, cell) {
  const publicX = cell * 48 + 20, otherX = cell === 0 ? 68 : 20;
  assert.deepEqual(png.pixel(publicX, 20), cell === 0 ? [20,180,60,255] : [240,30,210,255]);
  assert.deepEqual(png.pixel(otherX, 20), [0,0,0,255]);
}
async function capture(prefix, id, cell) {
  const root = `${prefix}/scenes/${id}`;
  const snapshot = await player.get(`${prefix}/scene`);
  assert.equal(snapshot.id, id);
  assert.deepEqual(Object.keys(snapshot).sort(), ['campaignId','gridSize','height','id','name','tokens','version','width']);
  for (const token of snapshot.tokens) assert.deepEqual(Object.keys(token).sort(), ['canMove','id','name','version','x','y']);
  const view = await player.get(`${prefix}/scene/map-view`);
  assert.deepEqual(Object.keys(view.map), ['distancePerCell']);
  const rendered = await image(player, prefix, view); pixels(rendered.png, cell);
  return { id, cell, snapshot, view, imageSha256: rendered.sha256,
    fog: await dm.get(`${root}/fog`), map: await dm.get(`${root}/map`), tokens: await dm.get(`${root}/tokens`) };
}
async function verifyScene(prefix, saved) {
  const root = `${prefix}/scenes/${saved.id}`;
  assert.deepEqual(await player.get(`${prefix}/scene`), saved.snapshot);
  assert.deepEqual(await player.get(`${prefix}/scene/map-view`), saved.view);
  assert.deepEqual(await dm.get(`${root}/fog`), saved.fog);
  assert.deepEqual(await dm.get(`${root}/map`), saved.map);
  assert.deepEqual(await dm.get(`${root}/tokens`), saved.tokens);
  const rendered = await image(player, prefix, saved.view);
  assert.equal(rendered.sha256, saved.imageSha256); pixels(rendered.png, saved.cell);
}

if (mode === 'seed') {
  const campaign = await dm.post('/campaigns', command({ name: `zz P02e restart ${randomUUID().slice(0,8)}` }), 201);
  const prefix = `/campaigns/${campaign.id}`, sceneA = await dm.get(`${prefix}/scene`);
  const members = await dm.get(`${prefix}/members`);
  const invitation = await dm.post(`${prefix}/invitations`, command({ expectedVersion: members.campaignVersion }), 201);
  await player.post('/invitations/accept', { token: invitation.token });
  const assets = await dm.get(`${prefix}/assets`), multipart = new FormData(), source = sourcePng();
  multipart.append('command', new Blob([JSON.stringify(command({ expectedVersion: assets.campaignVersion }))], { type: 'application/json' }));
  multipart.append('file', new Blob([source], { type: 'image/png' }), 'generated.png');
  const upload = await (await dm.call(`${prefix}/assets`, { method: 'POST', multipart })).json();
  const scenes = await dm.get(`${prefix}/scenes`);
  const sceneB = await dm.post(`${prefix}/scenes`, command({ expectedVersion: scenes.campaignVersion, name: 'P02e B' }));
  assert.equal((await player.get(`${prefix}/scene`)).id, sceneA.id, 'Preparation never switches current scene');
  const prepared = [], receipts = [];
  for (const [id, cell] of [[sceneA.id, 0], [sceneB.id, 1]]) {
    const root = `${prefix}/scenes/${id}`, config = await dm.get(`${root}/map`);
    const mapBody = command({ expectedVersion: config.sceneVersion,
      map: { assetId: upload.asset.id, gridSize: 8, offsetX: 0, offsetY: 0, distancePerCell: cell ? 10 : 5 } });
    const mapped = await dm.post(`${root}/map`, mapBody);
    const createBody = command({ expectedVersion: mapped.sceneVersion, name: `P02e shared ${cell}`, x: 2, y: 3,
      hidden: false, controllers: [dmName, playerName].sort() });
    const created = await dm.post(`${root}/tokens`, createBody);
    const hiddenBody = command({ expectedVersion: created.sceneVersion, name: `P02e secret ${cell}`, x: cell, y: 0,
      hidden: true, controllers: [playerName] });
    const hidden = await dm.post(`${root}/tokens`, hiddenBody);
    const fogBody = command({ expectedVersion: hidden.sceneVersion, revealed: [{ x: cell, y: 0 }] });
    const fogResult = await dm.post(`${root}/fog`, fogBody);
    receipts.push({ path: `${root}/map`, body: mapBody, result: mapped },
      { path: `${root}/tokens`, body: createBody, result: created },
      { path: `${root}/tokens`, body: hiddenBody, result: hidden },
      { path: `${root}/fog`, body: fogBody, result: fogResult });
    prepared.push({ id, cell, created, hidden, fogBody });
  }
  const moved = prepared[0], moveBody = command({ expectedVersion: moved.created.token.version, x: 4, y: 5 });
  const movePath = `${prefix}/tokens/${moved.created.token.id}/moves`, moveResult = await player.post(movePath, moveBody);
  // A's fog survives a move; B's calibration clears only B's fog and does not alter units.
  const a = await capture(prefix, sceneA.id, 0), firstActivation = await activate(prefix, sceneB.id);
  const before = await dm.get(`${prefix}/scenes/${sceneB.id}/tokens`), bMap = await dm.get(`${prefix}/scenes/${sceneB.id}/map`);
  const calibrateBody = command({ expectedVersion: bMap.sceneVersion, map: { ...bMap.map, distancePerCell: 12 } });
  const calibrated = await dm.post(`${prefix}/scenes/${sceneB.id}/map`, calibrateBody);
  assert.deepEqual((await dm.get(`${prefix}/scenes/${sceneB.id}/fog`)).revealed, []);
  assert.deepEqual((await dm.get(`${prefix}/scenes/${sceneB.id}/tokens`)).tokens, before.tokens);
  const coveredView = await player.get(`${prefix}/scene/map-view`), black = await image(player, prefix, coveredView);
  assert.deepEqual(black.png.pixel(68,20), [0,0,0,255]);
  const finalFogBody = command({ expectedVersion: calibrated.sceneVersion, revealed: [{x:1,y:0}] });
  const finalFog = await dm.post(`${prefix}/scenes/${sceneB.id}/fog`, finalFogBody);
  receipts.push({ path: `${prefix}/scenes/${sceneB.id}/map`, body: calibrateBody, result: calibrated },
    { path: `${prefix}/scenes/${sceneB.id}/fog`, body: finalFogBody, result: finalFog });
  const b = await capture(prefix, sceneB.id, 1);
  await image(player, prefix, a.view, 404);
  const returnActivation = await activate(prefix, sceneA.id); await verifyScene(prefix, a);
  await activate(prefix, sceneB.id); await verifyScene(prefix, b);
  assert.deepEqual(await dm.post(`${prefix}/scenes/${sceneA.id}/activate`, returnActivation.body), returnActivation.result);
  assert.equal((await player.get(`${prefix}/scene`)).id, sceneB.id, 'Historical activation cannot switch current scene');
  const original = await dm.call(`${prefix}/assets/${upload.asset.id}/content`); privateHeaders(original);
  assert.equal(hash(Buffer.from(await original.arrayBuffer())), hash(source));
  assert.ok(!JSON.stringify(a.snapshot).includes(prepared[0].hidden.token.id));
  assert.ok(!JSON.stringify(b.snapshot).includes(prepared[1].hidden.token.id));
  assert.ok(a.snapshot.tokens[0].canMove, 'Public controlled token stays visible on covered cell');
  const listing = await dm.get(`${prefix}/scenes`);
  await writeFile(evidencePath, JSON.stringify({ prefix, a, b, receipts, listing, returnActivation,
    firstActivation, movePath, moveBody, moveResult, sourceSha256: hash(source), assetId: upload.asset.id }), { flag: 'wx' });
  console.log('PASS: P02e two-scene fog/calibration/hidden/shared/moved state, decoded safe pixels, source SHA and historical activation seeded.');
} else {
  const saved = JSON.parse(await readFile(evidencePath, 'utf8')), { prefix, a, b } = saved;
  assert.deepEqual(await dm.get(`${prefix}/scenes`), saved.listing);
  await verifyScene(prefix, b); await image(player, prefix, a.view, 404);
  const original = await dm.call(`${prefix}/assets/${saved.assetId}/content`); privateHeaders(original);
  assert.equal(hash(Buffer.from(await original.arrayBuffer())), saved.sourceSha256);
  for (const receipt of saved.receipts) assert.deepEqual(await dm.post(receipt.path, receipt.body), receipt.result);
  await verifyScene(prefix, b);
  assert.deepEqual(await dm.post(`${prefix}/scenes/${a.id}/activate`, saved.returnActivation.body), saved.returnActivation.result);
  await verifyScene(prefix, b);
  await activate(prefix, a.id); await verifyScene(prefix, a);
  assert.deepEqual(await player.post(saved.movePath, saved.moveBody), saved.moveResult);
  await verifyScene(prefix, a);
  await image(player, prefix, b.view, 404);
  await activate(prefix, b.id); await verifyScene(prefix, b);
  // Removal advances versions and clears controls in BOTH scenes; rejoining never restores them.
  const members = await dm.get(`${prefix}/members`);
  await dm.post(`${prefix}/members/${encodeURIComponent(playerName)}/remove`, command({ expectedVersion: members.campaignVersion }), 204);
  await player.call(`${prefix}/scene`, { status: 403 });
  await image(player, prefix, b.view, 403);
  privateHeaders(await player.call(`${prefix}/assets/${saved.assetId}/content`, { status: 403 }));
  const currentMembers = await dm.get(`${prefix}/members`);
  const invitation = await dm.post(`${prefix}/invitations`, command({ expectedVersion: currentMembers.campaignVersion }), 201);
  await player.post('/invitations/accept', { token: invitation.token });
  const afterRemoval = [];
  for (const state of [a,b]) {
    const root = `${prefix}/scenes/${state.id}`;
    const tokens = await dm.get(`${root}/tokens`);
    assert.deepEqual(tokens, { ...state.tokens, sceneVersion: state.tokens.sceneVersion + 1,
      tokens: state.tokens.tokens.map(token => ({ ...token, version: token.version + 1,
        controllers: token.controllers.filter(name => name !== playerName) })) });
    const fog = await dm.get(`${root}/fog`), map = await dm.get(`${root}/map`);
    assert.deepEqual(fog, { ...state.fog, sceneVersion: state.fog.sceneVersion + 1 });
    assert.deepEqual(map, { ...state.map, sceneVersion: state.map.sceneVersion + 1 });
    afterRemoval.push({ root, tokens, fog, map });
  }
  // Authoring receipt replays cannot reverse calibration or removed controls.
  for (const receipt of saved.receipts) assert.deepEqual(await dm.post(receipt.path, receipt.body), receipt.result);
  for (const state of afterRemoval) {
    assert.deepEqual(await dm.get(`${state.root}/tokens`), state.tokens);
    assert.deepEqual(await dm.get(`${state.root}/fog`), state.fog);
    assert.deepEqual(await dm.get(`${state.root}/map`), state.map);
  }
  const currentB = await player.get(`${prefix}/scene/map-view`), renderedB = await image(player, prefix, currentB);
  assert.equal(renderedB.sha256, b.imageSha256); pixels(renderedB.png, 1);
  await activate(prefix, a.id); await player.post(saved.movePath, saved.moveBody, 403);
  await image(player, prefix, a.view, 409);
  const current = await player.get(`${prefix}/scene/map-view`), rendered = await image(player, prefix, current); pixels(rendered.png, 0);
  assert.equal(rendered.sha256, a.imageSha256);
  await player.call('/auth/logout', { method: 'POST', status: 204 });
  await image(player, prefix, current, 401);
  console.log('PASS: P02e real restart preserves BOTH scenes, safe PNG/source SHA, versions and all receipts; cross-scene revocation/rejoin and stale/logout image boundaries hold.');
}
