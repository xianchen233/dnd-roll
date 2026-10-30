// Dedicated acceptance accounts/database only. Restart the real services between modes.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';

const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath, 'Usage: node scripts/check-token-restart.mjs seed|verify <outside-repo-evidence.json>');
const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL');
class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', json, status = 200 } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ') };
    if (json !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL(`/api${path}`, base), { method, headers, ...(json === undefined ? {} : { body: JSON.stringify(json) }) });
    for (const cookie of response.headers.getSetCookie()) { const [name, ...value] = cookie.split(';')[0].split('='); this.cookies.set(name, value.join('=')); }
    assert.equal(response.status, status, `${method} ${path}`);
    return response;
  }
  async get(path) { return (await this.call(path)).json(); }
  async post(path, json, status = 200) { const response = await this.call(path, { method: 'POST', json, status }); return response.status === 204 ? undefined : response.json(); }
  async login(username, password) {
    this.csrf = await this.get('/auth/csrf'); await this.post('/auth/login', { username, password });
    this.csrf = await this.get('/auth/csrf'); return this;
  }
}
const dmName = required('E2E_DM_USERNAME'), playerName = required('E2E_PLAYER_USERNAME');
const dm = await new Session().login(dmName, required('E2E_DM_PASSWORD'));
const player = await new Session().login(playerName, required('E2E_PLAYER_PASSWORD'));
function publicShape(scene) {
  for (const token of scene.tokens) assert.deepEqual(Object.keys(token).sort(), ['canMove', 'id', 'name', 'version', 'x', 'y']);
}
async function join(prefix) {
  const members = await dm.get(`${prefix}/members`);
  const invite = await dm.post(`${prefix}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }, 201);
  await player.post('/invitations/accept', { token: invite.token });
}
if (mode === 'seed') {
  const campaign = await dm.post('/campaigns', { requestId: randomUUID(), name: `zz P02c restart ${randomUUID().slice(0, 8)}` }, 201);
  const prefix = `/campaigns/${campaign.id}`, scene = await dm.get(`${prefix}/scene`), path = `${prefix}/scenes/${scene.id}/tokens`;
  await join(prefix);
  const createCommand = { requestId: randomUUID(), expectedVersion: scene.version, name: 'Shared restart token', x: 2, y: 3, hidden: false, controllers: [dmName, playerName].sort() };
  const created = await dm.post(path, createCommand);
  const moveCommand = { requestId: randomUUID(), expectedVersion: 0, x: 4, y: 5 };
  const moved = await player.post(`${prefix}/tokens/${created.token.id}/moves`, moveCommand);
  const revokeCommand = { requestId: randomUUID(), expectedVersion: moved.version, name: 'Revoked restart token', x: 4, y: 5, hidden: false, controllers: [dmName] };
  const revoked = await dm.post(`${path}/${created.token.id}`, revokeCommand);
  await player.post(`${prefix}/tokens/${created.token.id}/moves`, moveCommand, 403);
  let list = await dm.get(path);
  const hiddenCommand = { requestId: randomUUID(), expectedVersion: list.sceneVersion, name: 'Secret restart unit', x: 7, y: 8, hidden: true, controllers: [playerName] };
  const hidden = await dm.post(path, hiddenCommand);
  const hiddenMove = { requestId: randomUUID(), expectedVersion: 0, x: 8, y: 8 };
  const hiddenError = await player.post(`${prefix}/tokens/${hidden.token.id}/moves`, hiddenMove, 404);
  assert.ok(!JSON.stringify(hiddenError).includes(hiddenCommand.name));
  list = await dm.get(path);
  const memberCommand = { requestId: randomUUID(), expectedVersion: list.sceneVersion, name: 'Membership restart token', x: 10, y: 10, hidden: false, controllers: [playerName] };
  const memberToken = await dm.post(path, memberCommand);
  const memberMove = { requestId: randomUUID(), expectedVersion: 0, x: 11, y: 10 };
  const memberMoved = await player.post(`${prefix}/tokens/${memberToken.token.id}/moves`, memberMove);
  const staleEdit = { ...memberCommand, requestId: randomUUID(), expectedVersion: memberMoved.version };
  const members = await dm.get(`${prefix}/members`);
  const removeCommand = { requestId: randomUUID(), expectedVersion: members.campaignVersion };
  await dm.post(`${prefix}/members/${encodeURIComponent(playerName)}/remove`, removeCommand, 204);
  await player.call(`${prefix}/scene`, { status: 403 });
  await join(prefix);
  await player.post(`${prefix}/tokens/${memberToken.token.id}/moves`, memberMove, 403);
  await dm.post(`${path}/${memberToken.token.id}`, staleEdit, 409);
  assert.deepEqual(await dm.post(path, memberCommand), memberToken, 'Old authoring receipt remains historical after rejoin');
  list = await dm.get(path);
  assert.deepEqual(list.tokens.find(token => token.id === memberToken.token.id).controllers, []);
  assert.deepEqual(list.tokens.find(token => token.id === hidden.token.id).controllers, []);
  const publicScene = await player.get(`${prefix}/scene`); publicShape(publicScene);
  assert.ok(!JSON.stringify(publicScene).includes(hidden.token.id));
  assert.ok(publicScene.tokens.every(token => token.canMove === false));
  await writeFile(evidencePath, JSON.stringify({ prefix, path, createCommand, created, revokeCommand, revoked, hiddenCommand, hidden, memberCommand, memberToken, moveCommand, memberMove, staleEdit, removeCommand, list, publicScene }), { flag: 'wx' });
  console.log('PASS: token creation/configuration, shared movement, hide/revoke and membership removal/rejoin saved; old receipts cannot restore control.');
} else {
  const saved = JSON.parse(await readFile(evidencePath, 'utf8'));
  assert.deepEqual(await dm.get(saved.path), saved.list);
  assert.deepEqual(await player.get(`${saved.prefix}/scene`), saved.publicScene);
  for (const [command, result, suffix] of [
    [saved.createCommand, saved.created, ''], [saved.revokeCommand, saved.revoked, `/${saved.created.token.id}`],
    [saved.hiddenCommand, saved.hidden, ''], [saved.memberCommand, saved.memberToken, ''],
  ]) assert.deepEqual(await dm.post(`${saved.path}${suffix}`, command), result);
  await dm.post(`${saved.path}/${saved.memberToken.token.id}`, saved.staleEdit, 409);
  await dm.post(`${saved.prefix}/members/${encodeURIComponent(playerName)}/remove`, saved.removeCommand, 204);
  await player.post(`${saved.prefix}/tokens/${saved.created.token.id}/moves`, saved.moveCommand, 403);
  await player.post(`${saved.prefix}/tokens/${saved.memberToken.token.id}/moves`, saved.memberMove, 403);
  await player.call(saved.path, { status: 403 });
  await player.post(saved.path, saved.createCommand, 403);
  assert.deepEqual(await dm.get(saved.path), saved.list, 'Historical authoring/remove receipts must not change versions or restore controls');
  const visible = await player.get(`${saved.prefix}/scene`); publicShape(visible); assert.deepEqual(visible, saved.publicScene);
  await dm.call('/auth/logout', { method: 'POST', status: 204 });
  await dm.call(saved.path, { status: 401 });
  console.log('PASS: new sessions after actual restart preserve token names/positions/visibility/controllers, scene/token versions and original receipts; revoked/rejoined players cannot replay moves; historical authoring/remove replay has no effect.');
}
