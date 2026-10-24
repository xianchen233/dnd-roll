import assert from 'node:assert/strict';
import { readFile, writeFile } from 'node:fs/promises';
import { Session, required, hash, privateHeaders } from './acceptance-support.mjs';

const [mode, file] = process.argv.slice(2); assert.ok(['capture', 'verify'].includes(mode) && file);
const dm = await new Session().login(required('E2E_DM_USERNAME'), required('E2E_DM_PASSWORD'));
const player = await new Session().login(required('E2E_PLAYER_USERNAME'), required('E2E_PLAYER_PASSWORD'));
async function snapshot(root) {
  const result = {};
  for (const [role, session] of [['dm', dm], ['player', player]]) {
    const data = {};
    for (const path of ['/scene', '/scene/map-view', '/characters', '/initiative', '/combat', '/content', '/actions', '/timeline/export?format=json', '/notes/export?format=json']) data[path] = await session.get(root + path);
    for (const action of data['/actions'].actions) data['/actions/' + action.id] = await session.get(root + '/actions/' + action.id);
    for (const character of data['/characters'].characters.filter(c => c.canReadFull)) {
      data['/characters/' + character.id] = await session.get(root + '/characters/' + character.id);
      if (role === 'dm' || character.canEdit) data['/characters/' + character.id + '/history'] = await session.get(root + '/characters/' + character.id + '/history');
    }
    for (const image of data['/timeline/export?format=json'].attachments) {
      const response = await session.call(root + '/attachments/' + image.id + '/content'); privateHeaders(response);
      data['image/' + image.id] = hash(Buffer.from(await response.arrayBuffer()));
    }
    const scene = data['/scene'], image = await session.call(`${root}/scene/map-image?sceneId=${scene.id}&version=${scene.version}`); privateHeaders(image);
    data['mapSha256'] = hash(Buffer.from(await image.arrayBuffer())); result[role] = data;
  }
  assert.equal(result.dm['/combat'].status, 'ACTIVE'); assert.equal(result.dm['/combat'].round, 2);
  assert.ok(result.dm['/actions'].actions.some(a => a.events.some(e => e.kind === 'VERDICT') && !a.events.some(e => e.kind === 'APPLY')), 'the continuous campaign still has a pending manual decision');
  return result;
}
if (mode === 'capture') {
  const campaign = (await dm.get('/campaigns')).find(c => c.role === 'DM' && c.name.startsWith('ZZZ P10完整团 ')); assert.ok(campaign, 'Run the continuous P10 browser scenario first');
  const root = '/campaigns/' + campaign.id, state = await snapshot(root);
  await writeFile(file, JSON.stringify({ root, state }, null, 2), { flag: 'wx' });
  console.log('CAMPAIGN CAPTURED: continuous two-player party, active round, pending result, private/public exports, histories and image hashes');
} else {
  const { root, state } = JSON.parse(await readFile(file, 'utf8')); assert.deepEqual(await snapshot(root), state);
  console.log('CAMPAIGN VERIFIED: fresh sessions retain the complete continuous party, active round, unapplied result, original dice, card histories, private filtering and map/RP image hashes');
}
