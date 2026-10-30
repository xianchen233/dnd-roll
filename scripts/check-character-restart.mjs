// Run seed -> actually restart PostgreSQL and packaged application -> verify.
// Credentials come only from the operator's environment. Evidence stays outside Git.
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';
const [mode, evidencePath] = process.argv.slice(2);
assert.ok(['seed', 'verify'].includes(mode) && evidencePath, 'Usage: check-character-restart.mjs seed|verify <outside-repository.json>');
const required = name => { assert.ok(process.env[name]?.trim(), `Missing ${name}`); return process.env[name]; };
const base = required('E2E_BASE_URL');
class Session {
  cookies = new Map(); csrf;
  async call(path, { method = 'GET', body, status = 200 } = {}) {
    const headers = { Cookie: [...this.cookies].map(([key, value]) => `${key}=${value}`).join('; ') };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method === 'POST') headers[this.csrf.headerName] = this.csrf.token;
    const response = await fetch(new URL('/api' + path, base), { method, headers, cache: 'no-store', ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
    for (const cookie of response.headers.getSetCookie()) { const [key, ...value] = cookie.split(';')[0].split('='); this.cookies.set(key, value.join('=')); }
    assert.equal(response.status, status, `${method} ${path}`); return response;
  }
  async get(path) { return (await this.call(path)).json(); }
  async post(path, body, status = 200) { const r = await this.call(path, { method: 'POST', body, status }); return status === 204 ? undefined : r.json(); }
  async login(username, password) { this.csrf = await this.get('/auth/csrf'); await this.post('/auth/login', { username, password }); this.csrf = await this.get('/auth/csrf'); return this; }
}
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
const player = await new Session().login(required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
const actor = required('E2E_PLAYER_USERNAME');
async function join(campaign) { const members = await dm.get(`/campaigns/${campaign}/members`), invite = await dm.post(`/campaigns/${campaign}/invitations`, { requestId: randomUUID(), expectedVersion: members.campaignVersion }, 201); await player.post('/invitations/accept', { token: invite.token }); }
async function snapshot(campaign) {
  const path = `/campaigns/${campaign}/characters`, list = await dm.get(path), cards = [];
  for (const c of list.characters) cards.push({ detail: await dm.get(`${path}/${c.id}`), access: await dm.get(`${path}/${c.id}/access`), history: await dm.get(`${path}/${c.id}/history`), placements: await dm.get(`${path}/${c.id}/placements`) });
  return { list, cards };
}
if (mode === 'seed') {
  const campaign = await dm.post('/campaigns', { requestId: randomUUID(), name: `ZZZ Character restart ${randomUUID().slice(0, 8)}` }, 201), c = campaign.id, root = `/campaigns/${c}`, path = `${root}/characters`; await join(c);
  const document = JSON.parse(await readFile(new URL('../contracts/character.example.json', import.meta.url), 'utf8')), sheet = document.sheet, source = { book: 'Restart fixture', revision: '1', rules: 'CUSTOM', detail: 'Synthetic manual selection' };
  sheet.name = 'Restart owner'; sheet.notes = `PRIVATE_FIXTURE_${randomUUID()}`; sheet.avatar = '◇'; sheet.species.name = 'Custom species'; sheet.background.entry.name = 'Custom background'; sheet.background.increases.STR = 2; sheet.background.increases.WIS = 1;
  sheet.classes = [{ id: 'a', name: 'First class', subclass: 'Custom', level: 3, source }, { id: 'b', name: 'Second class', subclass: '', level: 2, source }]; sheet.abilities.DEX = 9; sheet.abilities.INT = 18; sheet.abilities.WIS = 14; sheet.skills.find(s => s.id === 'perception').proficiency = 'EXPERT';
  sheet.combat = { ...sheet.combat, hp: 12, maxHp: 20, tempHp: 4, speed: 35, deathSuccesses: 1, deathFailures: 2 };
  sheet.attacks = [{ id: 'blade', name: 'Blade', ability: 'DEX', proficient: true, hitBonus: 1, damageBonus: 2, dice: '1d6', damageType: 'manual', mastery: 'Explicit choice', source }]; sheet.casting = [{ id: 'cast', name: 'Casting', ability: 'INT', attackBonus: 1, dcBonus: 2, source }]; sheet.spells = [{ id: 'spell', name: 'Custom spell', level: 1, prepared: true, castingId: 'cast', description: 'Manual only', source }];
  sheet.features = [{ id: 'origin', name: 'Origin choice', kind: 'ORIGIN_FEAT', choices: 'manual', description: 'Fixture', source }]; sheet.background.originFeat = 'origin'; sheet.resources = ['HIT_DICE', 'SPELL_SLOT', 'PACT_SLOT', 'FEATURE', 'CUSTOM'].map((kind, i) => ({ id: `r${i}`, name: kind, kind, current: 2, max: 3, recovery: 'Explicit manual recovery', source })); sheet.equipment = [{ id: 'pack', name: 'Pack', quantity: 2, equipped: true, description: 'Fixture', source }]; sheet.money = { cp: 1, sp: 2, ep: 3, gp: 4, pp: 5 }; sheet.conditions = [{ id: 'due', name: 'Due reminder', icon: '!', description: 'Never auto-remove', source, remainingRounds: 1, expiresAt: '2020-01-01T00:00:00Z' }]; sheet.modifiers = [{ target: 'initiative', amount: 2, source }];
  const create = { requestId: randomUUID(), expectedVersion: (await player.get(path)).campaignVersion, sheet: structuredClone(sheet) }, a = await player.post(path, create), ap = `${path}/${a.id}`;
  const second = structuredClone(sheet); second.name = 'Independent second'; second.combat.hp = 7; const b = await player.post(path, { requestId: randomUUID(), expectedVersion: (await player.get(path)).campaignVersion, sheet: second });
  const override = structuredClone(sheet); override.overrides = [{ target: 'ac', value: 19, reason: 'Persistent DM ruling' }]; await dm.post(ap, { requestId: randomUUID(), expectedVersion: 0, sheet: override, reason: 'DM override' });
  const grant = { requestId: randomUUID(), expectedVersion: 1, controllers: [actor], hidden: false, shareFull: false, approveImport: true, reason: 'Explicit grant' }, grantAck = await dm.post(ap + '/access', grant);
  const copy = { requestId: randomUUID(), expectedVersion: 2, name: 'Independent copy' }, copyAck = await player.post(ap + '/copy', copy);
  const scene = await dm.get(root + '/scene'), other = await dm.post(root + '/scenes', { requestId: randomUUID(), expectedVersion: (await dm.get(root + '/scenes')).campaignVersion, name: 'Other placement' });
  const tokens = [];
  for (const sceneId of [scene.id, other.id]) { const t = await dm.post(`${root}/scenes/${sceneId}/tokens`, { requestId: randomUUID(), expectedVersion: 0, name: 'Representation', x: 1, y: 1, hidden: false, controllers: [actor] }); tokens.push(t.token); const d = await dm.get(ap); await dm.post(ap + '/placements', { requestId: randomUUID(), expectedVersion: d.version, tokenId: t.token.id, tokenVersion: t.token.version, reason: 'Reference same resource owner' }); }
  const d = await player.get(ap), consume = { requestId: randomUUID(), expectedVersion: d.version, resourceId: 'r1', delta: -1, reason: 'Once only' }, consumeAck = await player.post(ap + '/resources', consume);
  assert.equal((await player.get(`${path}/${b.id}`)).sheet.combat.hp, 7);
  const exported = await player.get(ap + '/export'), target = await dm.post('/campaigns', { requestId: randomUUID(), name: 'Target review' }, 201); await join(target.id);
  const tp = `/campaigns/${target.id}/characters`, importBody = { requestId: randomUUID(), expectedVersion: (await player.get(tp)).campaignVersion, document: exported }, imported = await player.post(tp + '/import', importBody);
  assert.equal((await dm.get(`${tp}/${imported.id}/access`)).importReview, true); assert.deepEqual((await player.get(`${tp}/${imported.id}`)).sheet.overrides, []);
  await dm.post(`${root}/members/${encodeURIComponent(actor)}/remove`, { requestId: randomUUID(), expectedVersion: (await dm.get(root + '/members')).campaignVersion }, 204); await join(c);
  for (const route of ['', '/history', '/export']) await player.call(ap + route, { status: 403 });
  await player.post(ap + '/resources', consume, 403); await player.post(ap + '/copy', copy, 403);
  const scenes = await dm.get(root + '/scenes'), authored = [];
  for (const s of scenes.scenes) authored.push(await dm.get(`${root}/scenes/${s.id}/tokens`));
  await writeFile(evidencePath, JSON.stringify({ campaign: c, target: target.id, a, b, create, grant, grantAck, copy, copyAck, consume, consumeAck, importBody, imported, source: await snapshot(c), targetState: await snapshot(target.id), authored }, null, 2));
  console.log('PASS: character seed; full fields/sources/overrides, independent resources, two placements, target review, revoked receipts and actual-actor history recorded.');
} else {
  const e = JSON.parse(await readFile(evidencePath, 'utf8')), root = `/campaigns/${e.campaign}`, path = root + '/characters', ap = `${path}/${e.a.id}`, tp = `/campaigns/${e.target}/characters`;
  assert.deepEqual(await snapshot(e.campaign), e.source); assert.deepEqual(await snapshot(e.target), e.targetState);
  const scenes = await dm.get(root + '/scenes'), authored = []; for (const s of scenes.scenes) authored.push(await dm.get(`${root}/scenes/${s.id}/tokens`)); assert.deepEqual(authored, e.authored);
  assert.deepEqual(await player.post(path, e.create), e.a); assert.deepEqual(await dm.post(ap + '/access', e.grant), e.grantAck); assert.deepEqual(await player.post(tp + '/import', e.importBody), e.imported);
  await player.post(ap + '/resources', e.consume, 403); await player.post(ap + '/copy', e.copy, 403); for (const suffix of ['', '/history', '/export']) await player.call(ap + suffix, { status: 403 });
  assert.deepEqual(await snapshot(e.campaign), e.source); assert.deepEqual(await snapshot(e.target), e.targetState); assert.equal((await dm.get(ap)).expiredConditionIds.length, 1); assert.equal((await dm.get(ap)).sheet.conditions.length, 1);
  const response = await dm.call(ap + '/export'); assert.match(response.headers.get('cache-control'), /no-store/);
  await dm.post('/auth/logout', undefined, 204); await dm.call(ap + '/export', { status: 401 });
  console.log('PASS: character restart; every sheet/derived source/override/history/access/placement/version unchanged; fresh sessions, independent resources, pending target review and historical receipts cannot restore grants or consume twice.');
}
