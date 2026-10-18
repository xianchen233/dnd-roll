import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');
const [apiText, eventText, readme, accounts, scenes] = await Promise.all([
  read('../contracts/openapi.json'),
  read('../contracts/scene-events.schema.json'),
  read('../contracts/README.md'),
  read('../contracts/accounts.md'),
  read('../contracts/scenes.md'),
]);
const api = JSON.parse(apiText);
const event = JSON.parse(eventText);
const resolve = (schema) => schema.$ref
  ? schema.$ref.slice(2).split('/').reduce((value, key) => value[key], api)
  : schema;

// Deliberately a small checker for our contract vocabulary, not a general OpenAPI validator.
function validate(schema, value) {
  schema = resolve(schema);
  assert.ok(schema, 'Referenced schema exists');
  if (schema.type === 'null') { assert.equal(value, null); return; }
  if (Array.isArray(schema.type)) {
    const types = schema.type;
    assert.ok(types.includes(value === null ? 'null' : typeof value), 'Value has an allowed type');
    if (value === null) return;
    schema = { ...schema, type: types.find((type) => type !== 'null') };
  }
  if (schema.const !== undefined) assert.equal(value, schema.const);
  if (schema.enum) assert.ok(schema.enum.includes(value));
  if (schema.type === 'object') {
    assert.ok(value !== null && typeof value === 'object' && !Array.isArray(value));
    for (const key of schema.required ?? []) assert.ok(Object.hasOwn(value, key), `Missing ${key}`);
    if (schema.additionalProperties === false) {
      for (const key of Object.keys(value)) assert.ok(Object.hasOwn(schema.properties, key), `Unexpected ${key}`);
    }
    for (const [key, property] of Object.entries(schema.properties)) {
      if (Object.hasOwn(value, key)) validate(property, value[key]);
    }
  } else if (schema.type === 'array') {
    assert.ok(Array.isArray(value));
    value.forEach((item) => validate(schema.items, item));
  } else if (schema.type === 'integer') {
    assert.ok(Number.isSafeInteger(value));
    if (schema.minimum !== undefined) assert.ok(value >= schema.minimum);
  } else if (schema.type === 'string') {
    assert.equal(typeof value, 'string');
    if (schema['x-trimBeforeValidation']) value = value.trim();
    if (schema.minLength !== undefined) assert.ok([...value].length >= schema.minLength);
    if (schema.maxLength !== undefined) assert.ok([...value].length <= schema.maxLength);
    if (schema.pattern !== undefined) assert.match(value, new RegExp(schema.pattern));
    if (schema['x-maxUtf8Bytes'] !== undefined) assert.ok(Buffer.byteLength(value, 'utf8') <= schema['x-maxUtf8Bytes']);
    if (schema.format === 'date-time') {
      assert.match(value, /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?Z$/);
      assert.ok(Number.isFinite(Date.parse(value)), 'UTC instant is valid');
    }
    if (schema.format === 'uuid') assert.match(value, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
  } else if (schema.type === 'boolean') assert.equal(typeof value, 'boolean');
}

function checkRefs(value) {
  if (value === null || typeof value !== 'object') return;
  if (value.$ref) {
    assert.ok(value.$ref.startsWith('#/'), 'Only local references are used');
    assert.ok(resolve(value), `Unresolved reference ${value.$ref}`);
  }
  Object.values(value).forEach(checkRefs);
}

assert.equal(api.openapi, '3.1.0');
assert.equal(api.jsonSchemaDialect, 'https://json-schema.org/draft/2020-12/schema');
assert.equal(event.$schema, api.jsonSchemaDialect);
checkRefs(api);
const documentedOperations = [...`${readme}\n${accounts}\n${scenes}`.matchAll(/(?:`|### )(GET|POST) (\/api[^` \r\n]*)/g)]
  .map(([, method, path]) => `${method} ${path}`);
const actualOperations = Object.entries(api.paths).flatMap(([path, item]) =>
  Object.keys(item).filter((method) => ['get', 'post'].includes(method))
    .map((method) => `${method.toUpperCase()} ${path}`));
assert.deepEqual(actualOperations.sort(), [...new Set(documentedOperations)].sort());
assert.deepEqual(api.security, [{ sessionCookie: [] }]);
assert.equal(api.components.securitySchemes.sessionCookie.in, 'cookie');
for (const [path, item] of Object.entries(api.paths)) {
  if (item.post) {
    const csrf = item.post.parameters.map(resolve).find((parameter) => parameter.in === 'header');
    assert.equal(csrf.name, 'X-CSRF-TOKEN', `${path} requires CSRF`);
    assert.equal(csrf.required, true);
    assert.ok(item.post.responses['403']);
  }
  for (const name of path.matchAll(/\{([^}]+)\}/g)) {
    assert.ok(item.parameters.map(resolve).some((parameter) => parameter.name === name[1]
      && parameter.in === 'path' && parameter.required === true));
  }
}
assert.deepEqual(api.paths['/api/auth/csrf'].get.security, []);
assert.deepEqual(api.paths['/api/auth/login'].post.security, []);
assert.equal(api.paths['/api/auth/logout'].post.responses['204'].content, undefined);
const schemas = api.components.schemas;
const move = api.paths['/api/campaigns/{campaignId}/tokens/{tokenId}/moves'].post;
assert.deepEqual(schemas.MoveRequest.required, ['requestId', 'expectedVersion', 'x', 'y']);
assert.equal(schemas.MoveRequest.additionalProperties, false);
assert.deepEqual(move['x-idempotency'].key, ['campaignId', 'sessionActor', 'requestId']);
assert.deepEqual(move['x-idempotency'].payload, ['tokenId', 'expectedVersion', 'x', 'y']);
assert.match(move['x-idempotency'].samePayload, /original response.*stale/);
assert.match(move['x-idempotency'].differentPayload, /409 IDEMPOTENCY_CONFLICT/);
assert.match(move['x-idempotency'].versionConflict, /409 VERSION_CONFLICT/);
assert.deepEqual(move['x-grid-bounds'], { x: '0 <= x < scene.width', y: '0 <= y < scene.height' });
assert.deepEqual(schemas.SceneSnapshot.required, ['id', 'campaignId', 'name', 'width', 'height', 'gridSize', 'version', 'tokens']);
assert.deepEqual(schemas.TokenView.required, ['id', 'name', 'x', 'y', 'version', 'canMove']);
assert.equal(schemas.TokenView.additionalProperties, false);
assert.deepEqual(schemas.ApiError.required, ['code', 'message']);
for (const code of ['UNAUTHENTICATED', 'FORBIDDEN', 'NOT_FOUND', 'VERSION_CONFLICT', 'IDEMPOTENCY_CONFLICT', 'INVALID_INPUT',
  'INVITE_NOT_FOUND', 'INVITE_EXPIRED', 'INVITE_UNAVAILABLE', 'USERNAME_TAKEN', 'DM_REMOVAL_DENIED']) {
  assert.ok(schemas.ApiError.properties.code.enum.includes(code), `Error code ${code} is documented`);
}
assert.deepEqual(Object.keys(event.properties).sort(), ['campaignId', 'type']);
assert.deepEqual(event.required, ['type', 'campaignId']);
assert.equal(event.additionalProperties, false);
assert.equal(event['x-stomp'].endpoint, '/ws');
assert.equal(event['x-stomp'].subscription, '/user/queue/scene-changes');
assert.equal(event['x-stomp'].connectCsrfHeader, 'X-CSRF-TOKEN');
assert.equal(event['x-stomp'].clientSendAllowed, false);
assert.equal(event['x-stomp'].arbitrarySubscriptionsAllowed, false);

const requestSchema = (path) => resolve(api.paths[path].post.requestBody.content['application/json'].schema);
const responseSchema = (path, method, status) => resolve(api.paths[path][method].responses[status].content['application/json'].schema);
const createCampaign = '/api/campaigns';
const invitations = '/api/campaigns/{campaignId}/invitations';
const members = '/api/campaigns/{campaignId}/members';
const revoke = `${invitations}/{invitationId}/revoke`;
const remove = `${members}/{username}/remove`;
assert.deepEqual(requestSchema(createCampaign).required, ['requestId', 'name']);
for (const path of [invitations, revoke, remove]) {
  assert.deepEqual(requestSchema(path).required, ['requestId', 'expectedVersion']);
  assert.equal(requestSchema(path).additionalProperties, false);
  assert.ok(api.paths[path].post.responses['409']);
  assert.deepEqual(api.paths[path].post['x-idempotency'].key, ['sessionActor', 'requestId']);
  assert.ok(api.paths[path].post['x-idempotency'].payload.includes('campaignId'));
  assert.match(api.paths[path].post['x-idempotency'].authorization, /before.*replay/i);
}
for (const path of ['/api/invitations/preview', '/api/invitations/register']) {
  assert.deepEqual(api.paths[path].post.security, []);
}
for (const path of ['/api/invitations/preview', '/api/invitations/accept', '/api/invitations/register']) {
  for (const status of ['400', '404', '409', '410']) assert.ok(api.paths[path].post.responses[status]);
}
assert.deepEqual(requestSchema('/api/invitations/register').required, ['token', 'username', 'password']);
assert.deepEqual(requestSchema('/api/invitations/accept').required, ['token']);
assert.deepEqual(requestSchema('/api/invitations/preview').required, ['token']);
assert.deepEqual(responseSchema(members, 'get', '200').required, ['campaignVersion', 'members']);
assert.deepEqual(responseSchema(invitations, 'get', '200').required, ['campaignVersion', 'invitations']);
assert.deepEqual(schemas.InvitationCreated.properties.token.type, ['string', 'null']);
assert.equal(schemas.InvitationCreated.properties.token.writeOnly, undefined);
assert.match(api['x-invitation-policy'].shareUrl, /\/#invite=TOKEN$/);
assert.equal(api['x-invitation-policy'].backendTokenTransport, 'POST JSON body only; never URL paths or query strings');
assert.equal(api['x-invitation-policy'].tokenLifetimeSeconds, 7 * 24 * 60 * 60);
assert.match(api['x-invitation-policy'].persistence, /SHA-256.*receipts.*logs/);
assert.match(api['x-invitation-policy'].fragmentLifecycle, /clear.*success.*never log/i);
assert.equal(api.paths[revoke].post.responses['204'].content, undefined);
assert.equal(api.paths[remove].post.responses['204'].content, undefined);

const sceneList = '/api/campaigns/{campaignId}/scenes';
const activateScene = `${sceneList}/{sceneId}/activate`;
for (const [path, method] of [[sceneList, 'get'], [sceneList, 'post'], [activateScene, 'post']]) {
  const operation = api.paths[path][method];
  assert.equal(operation['x-required-role'], 'DM');
  assert.match(operation.description, /inactive.*metadata.*never.*player/i);
  for (const status of ['401', '403']) assert.ok(operation.responses[status]);
}
assert.deepEqual(requestSchema(sceneList).required, ['requestId', 'expectedVersion', 'name']);
assert.deepEqual(requestSchema(activateScene).required, ['requestId', 'expectedVersion']);
assert.deepEqual(responseSchema(sceneList, 'get', '200').required, ['campaignVersion', 'currentSceneId', 'scenes']);
for (const path of [sceneList, activateScene]) {
  const operation = api.paths[path].post;
  assert.equal(requestSchema(path).additionalProperties, false);
  assert.deepEqual(responseSchema(path, 'post', '200').required, ['id', 'name', 'campaignVersion']);
  for (const status of ['400', '409']) assert.ok(operation.responses[status]);
  assert.match(resolve(operation.responses['409']).description, /VERSION_CONFLICT.*IDEMPOTENCY_CONFLICT/);
  assert.equal(operation['x-idempotency'].namespace, 'scene-commands');
  assert.deepEqual(operation['x-idempotency'].key, ['sessionActor', 'requestId']);
  assert.ok(operation['x-idempotency'].payload.includes('commandKind'));
  assert.ok(operation['x-idempotency'].payload.includes('campaignId'));
  assert.ok(operation['x-idempotency'].payload.includes('expectedVersion'));
  assert.match(operation['x-idempotency'].authorization, /current.*DM.*before.*replay/i);
  assert.match(operation['x-idempotency'].samePayload, /original.*before.*stale.*no.*event/i);
  assert.match(operation['x-idempotency'].differentPayload, /409 IDEMPOTENCY_CONFLICT/);
}
assert.ok(api.paths[sceneList].post['x-idempotency'].payload.includes('trimmedName'));
assert.ok(api.paths[activateScene].post['x-idempotency'].payload.includes('sceneId'));
assert.ok(api.paths[activateScene].post.responses['404']);
assert.match(api.paths[activateScene].post.description, /already current.*advance/i);
const scenePolicy = api['x-scene-policy'];
assert.match(scenePolicy.managementVersion, /shared.*P01.*independent.*scene.*token/i);
assert.match(scenePolicy.receiptNamespace, /create.*activate.*separate.*account.*movement/i);
assert.match(scenePolicy.atomicity, /command.*version.*receipt.*atomically/i);
assert.match(scenePolicy.visibility, /inactive.*IDs.*names.*never.*player/i);
assert.match(scenePolicy.historicalReplay, /historical.*refresh.*snapshot.*list/i);
assert.match(scenePolicy.movement, /current scene.*including.*replay.*404/i);
assert.match(scenePolicy.realtime, /after.commit.*SCENE_CHANGED.*type.*campaignId/i);
assert.match(api.paths['/api/campaigns/{campaignId}/scene'].get.description, /DM.selected current scene/i);
assert.match(move.description, /current scene.*including.*replay/i);
assert.ok(!api.paths['/api/campaigns/{campaignId}/scenes/{sceneId}'], 'No inactive scene-by-ID read route');

const id = '12345678-1234-1234-1234-123456789abc';
const token = { id, name: 'Adventurer', x: 2, y: 3, version: 0, canMove: true };
const snapshot = { id, campaignId: id, name: 'First scene', width: 24, height: 16, gridSize: 48, version: 0, tokens: [token] };
const movement = { requestId: id, expectedVersion: 0, x: 4, y: 5 };
validate(schemas.SceneSnapshot, snapshot);
validate(schemas.MoveRequest, movement);
validate(schemas.CsrfToken, { token: 'session-csrf-token', headerName: 'X-CSRF-TOKEN' });
validate(event, { type: 'SCENE_CHANGED', campaignId: id });
for (const invalid of [
  { ...movement, x: -1 }, { ...movement, y: 1.5 }, { ...movement, expectedVersion: -1 },
  { ...movement, requestId: 'not-a-uuid' }, { x: 1, y: 2, expectedVersion: 0 },
  { ...movement, actor: 'dm' },
]) assert.throws(() => validate(schemas.MoveRequest, invalid));
assert.throws(() => validate(schemas.SceneSnapshot, { ...snapshot, width: 0 }));
assert.throws(() => validate(schemas.TokenView, { ...token, hidden: true }));
for (const extra of [{ tokenId: id }, { tokens: [token] }, { name: 'hidden enemy' }, { x: 2 }, { count: 1 }]) {
  assert.throws(() => validate(event, { type: 'SCENE_CHANGED', campaignId: id, ...extra }));
}
const inviteToken = 'A'.repeat(43);
const expiresAt = '2026-10-25T09:30:00Z';
const invitation = { id, createdBy: 'dm', expiresAt, status: 'ACTIVE', usedBy: null };
const invitationCreated = { id, token: inviteToken, expiresAt, campaignVersion: 1 };
const registration = { token: inviteToken, username: 'new_player', password: 'strong password' };
const membersView = { campaignVersion: 1, members: [{ username: 'dm', role: 'DM' }] };
const invitationsView = { campaignVersion: 1, invitations: [invitation] };
for (const [schema, value] of [
  [schemas.CampaignCreateRequest, { requestId: id, name: 'New campaign' }],
  [schemas.ManagementRequest, { requestId: id, expectedVersion: 0 }],
  [schemas.InvitationCreated, invitationCreated],
  [schemas.InvitationCreated, { ...invitationCreated, token: null }],
  [schemas.InvitationList, invitationsView], [schemas.MemberList, membersView],
  [schemas.InvitationPreview, { campaignId: id, campaignName: 'New campaign', expiresAt }],
  [schemas.InvitationTokenRequest, { token: inviteToken }], [schemas.InvitationRegisterRequest, registration],
  [schemas.InvitationAccepted, { campaignId: id }],
  [schemas.InvitationRegistered, { username: 'new_player', campaignId: id }],
]) validate(schema, value);
for (const invalid of [false, true, 0, {}, '', 'x'.repeat(42), 'x'.repeat(43), 'x'.repeat(44), '+'.repeat(43)]) {
  assert.throws(() => validate(schemas.InvitationCreated, { ...invitationCreated, token: invalid }));
  assert.throws(() => validate(schemas.InvitationTokenRequest, { token: invalid }));
}
for (const username of ['ab', 'New_player', '1player', ' has_space', 'x'.repeat(33)]) {
  assert.throws(() => validate(schemas.InvitationRegisterRequest, { ...registration, username }));
}
for (const password of ['short', '界'.repeat(25), '😀'.repeat(6)]) {
  assert.throws(() => validate(schemas.InvitationRegisterRequest, { ...registration, password }));
}
validate(schemas.InvitationRegisterRequest, { ...registration, password: ' 界'.repeat(18) });
validate(schemas.InvitationRegisterRequest, { ...registration, password: '😀'.repeat(12) });
for (const invalid of [
  { name: 'Name' }, { requestId: id, name: '' }, { requestId: id, name: 'x'.repeat(101) },
  { requestId: id, name: 'Name', role: 'DM' },
]) assert.throws(() => validate(schemas.CampaignCreateRequest, invalid));
for (const invalid of [{ requestId: id }, { expectedVersion: 0 }, { requestId: id, expectedVersion: -1 }, { requestId: 'bad', expectedVersion: 0 }]) {
  assert.throws(() => validate(schemas.ManagementRequest, invalid));
}
for (const date of ['2026-10-25T17:30:00+08:00', 'not-a-date', '2026-99-25T09:30:00Z']) {
  assert.throws(() => validate(schemas.InvitationCreated, { ...invitationCreated, expiresAt: date }));
}
for (const secret of ['token', 'tokenHash', 'password', 'passwordHash', 'accountId']) {
  assert.throws(() => validate(schemas.InvitationList, { ...invitationsView, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.InvitationList, { ...invitationsView, invitations: [{ ...invitation, [secret]: 'secret' }] }));
  assert.throws(() => validate(schemas.MemberList, { ...membersView, members: [{ ...membersView.members[0], [secret]: 'secret' }] }));
  assert.throws(() => validate(schemas.ApiError, { code: 'INVITE_NOT_FOUND', message: 'Unavailable', [secret]: 'secret' }));
}
assert.throws(() => validate(schemas.MemberList, membersView.members));
assert.throws(() => validate(schemas.InvitationList, invitationsView.invitations));
assert.throws(() => validate(schemas.InvitationList, { ...invitationsView, invitations: [{ ...invitation, status: 'UNKNOWN' }] }));
assert.throws(() => validate(schemas.InvitationRegisterRequest, { ...registration, role: 'DM' }));
const sceneCreate = { requestId: id, expectedVersion: 0, name: 'Next scene' };
const sceneActivate = { requestId: id, expectedVersion: 0 };
const sceneResult = { id, name: 'Next scene', campaignVersion: 1 };
const sceneListView = { campaignVersion: 1, currentSceneId: id, scenes: [{ id, name: 'Next scene' }] };
for (const [schema, value] of [
  [schemas.SceneCreateRequest, sceneCreate], [schemas.SceneActivateRequest, sceneActivate],
  [schemas.SceneCommandResult, sceneResult], [schemas.SceneList, sceneListView],
  [schemas.SceneCreateRequest, { ...sceneCreate, name: ` ${'😀'.repeat(100)} ` }],
]) validate(schema, value);
for (const invalid of [
  { ...sceneCreate, name: '' }, { ...sceneCreate, name: ' \t\n ' },
  { ...sceneCreate, name: '😀'.repeat(101) }, { requestId: id, name: 'Name' },
  { ...sceneCreate, expectedVersion: -1 }, { ...sceneCreate, expectedVersion: 0.5 },
  { ...sceneCreate, requestId: 'bad' }, { ...sceneCreate, actor: 'dm' },
  { ...sceneCreate, sceneId: id },
]) assert.throws(() => validate(schemas.SceneCreateRequest, invalid));
for (const invalid of [
  { requestId: id }, { expectedVersion: 0 }, { ...sceneActivate, name: 'Name' },
  { ...sceneActivate, expectedVersion: -1 }, { ...sceneActivate, expectedVersion: 0.5 },
  { ...sceneActivate, requestId: 'bad' }, { ...sceneActivate, actor: 'dm' },
]) assert.throws(() => validate(schemas.SceneActivateRequest, invalid));
for (const secret of ['inactiveSceneIds', 'inactiveSceneNames', 'scenes', 'currentSceneId', 'campaignVersion']) {
  assert.throws(() => validate(schemas.SceneSnapshot, { ...snapshot, [secret]: [] }));
  assert.throws(() => validate(event, { type: 'SCENE_CHANGED', campaignId: id, [secret]: [] }));
}
for (const secret of ['tokens', 'hidden', 'controllers', 'assetId', 'tokenHash']) {
  assert.throws(() => validate(schemas.SceneCommandResult, { ...sceneResult, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.SceneList, { ...sceneListView, scenes: [{ ...sceneListView.scenes[0], [secret]: 'secret' }] }));
}
for (const invalid of [
  { ...sceneResult, campaignVersion: -1 }, { id, name: 'Name' }, { ...sceneResult, id: 'bad' },
]) assert.throws(() => validate(schemas.SceneCommandResult, invalid));
for (const invalid of [
  { ...sceneListView, campaignVersion: -1 }, { campaignVersion: 1, scenes: [] },
  { ...sceneListView, currentSceneId: 'bad' }, { ...sceneListView, scenes: [{ id }] },
]) assert.throws(() => validate(schemas.SceneList, invalid));
console.log(`Contract checks passed: ${actualOperations.length} HTTP operations; CSRF/session, P01 credentials/version/secret-free views/one-time invitation token/fragment links, P02a DM-only scenes/shared management version/scene receipt namespace, movement/idempotency, snapshot and token-free STOMP event constraints. This is a focused contract checker, not a general OpenAPI validator.`);
