import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');
const [apiText, eventText, readme, accounts, scenes, assets, tokens, mapVisibility, characters] = await Promise.all([
  read('../contracts/openapi.json'),
  read('../contracts/scene-events.schema.json'),
  read('../contracts/README.md'),
  read('../contracts/accounts.md'),
  read('../contracts/scenes.md'),
  read('../contracts/assets.md'),
  read('../contracts/tokens.md'),
  read('../contracts/map-visibility.md'),
  read('../contracts/characters.md'),
]);
const api = JSON.parse(apiText);
for(const [suffix,method]of [['update-options','post'],['update-impact','post'],['updates','post'],['updates','get'],['updates/{version}/content','get']])assert.ok(api.paths['/api/campaigns/{campaignId}/module-customizations/{id}/'+suffix]?.[method],`A03b missing ${method} ${suffix}`);
assert.equal(api.paths['/api/campaigns/{campaignId}/module-customizations/{id}/update-preview']?.post?.['x-read-only'],true,'A03 read-only update preview missing');
for(const [suffix,method]of [['/module-customizations','get'],['/module-customizations','post'],['/module-customizations/{id}','get'],['/module-customizations/{id}','post'],['/module-customizations/{id}/templates','post'],['/module-customizations/{id}/campaigns','post'],['/module-instance','get'],['/module-instance','post']])assert.ok(api.paths['/api/campaigns/{campaignId}'+suffix]?.[method],`A02 lifecycle operation missing: ${method} ${suffix}`);
for(const [suffix,method] of [['','get'],['','post'],['/preview','post'],['/{id}/content','get']])assert.ok(api.paths['/api/campaigns/{campaignId}/module-packages'+suffix]?.[method],`A01 operation missing: ${method} ${suffix}`);
for(const [path,method] of [['/api/admin/backups','get'],['/api/admin/backups','post'],['/api/admin/backups/migration','post'],['/api/admin/backups/{id}/content','get']])assert.ok(api.paths[path]?.[method],`P09 operation missing: ${method} ${path}`);
for(const [suffix,method] of [['/notes','get'],['/notes','post'],['/notes/{id}','get'],['/notes/{id}','post'],['/notes/{id}/history','get'],['/notes/export','get'],['/note-attachments','post'],['/note-attachments/{id}/content','get']])assert.ok(api.paths['/api/campaigns/{campaignId}'+suffix]?.[method],`P08 operation missing: ${method} ${suffix}`);
const event = JSON.parse(eventText);
const resolve = (schema) => schema.$ref
  ? schema.$ref.slice(2).split('/').reduce((value, key) => value[key], api)
  : schema;

// Deliberately a small checker for our contract vocabulary, not a general OpenAPI validator.
function validate(schema, value) {
  schema = resolve(schema);
  assert.ok(schema, 'Referenced schema exists');
  if (schema.anyOf) {
    assert.ok(schema.anyOf.some((option) => {
      try { validate(option, value); return true; } catch { return false; }
    }), 'Value matches one documented option');
    return;
  }
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
    if (schema.maxProperties !== undefined) assert.ok(Object.keys(value).length <= schema.maxProperties);
    for (const key of schema.required ?? []) assert.ok(Object.hasOwn(value, key), `Missing ${key}`);
    if (schema.additionalProperties === false) {
      for (const key of Object.keys(value)) assert.ok(Object.hasOwn(schema.properties, key), `Unexpected ${key}`);
    }
    for (const [key, property] of Object.entries(schema.properties ?? {})) {
      if (Object.hasOwn(value, key)) validate(property, value[key]);
    }
    for (const [key, property] of Object.entries(value)) {
      if (schema.propertyNames?.enum) assert.ok(schema.propertyNames.enum.includes(key));
      if (schema.additionalProperties && typeof schema.additionalProperties === 'object' && !Object.hasOwn(schema.properties ?? {}, key)) validate(schema.additionalProperties, property);
    }
  } else if (schema.type === 'array') {
    assert.ok(Array.isArray(value));
    if (schema.minItems !== undefined) assert.ok(value.length >= schema.minItems);
    if (schema.maxItems !== undefined) assert.ok(value.length <= schema.maxItems);
    if (schema.uniqueItems) assert.equal(new Set(value.map((item) => JSON.stringify(item))).size, value.length);
    value.forEach((item) => validate(schema.items, item));
  } else if (schema.type === 'integer') {
    assert.ok(Number.isSafeInteger(value));
    if (schema.minimum !== undefined) assert.ok(value >= schema.minimum);
    if (schema.maximum !== undefined) assert.ok(value <= schema.maximum);
  } else if (schema.type === 'number') {
    assert.ok(typeof value === 'number' && Number.isFinite(value));
    if (schema.minimum !== undefined) assert.ok(value >= schema.minimum);
    if (schema.maximum !== undefined) assert.ok(value <= schema.maximum);
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
for (const [path, method] of [
  ['/api/campaigns/{campaignId}/content','get'],
  ['/api/campaigns/{campaignId}/content/custom','post'],
  ['/api/campaigns/{campaignId}/content/settings','post'],
  ['/api/campaigns/{campaignId}/characters/{id}/content-preview','post'],
  ['/api/campaigns/{campaignId}/characters/{id}/content','post'],
]) assert.ok(api.paths[path]?.[method], `P04 operation missing: ${method} ${path}`);
const documentedOperations = [...`${readme}\n${accounts}\n${scenes}\n${assets}\n${tokens}\n${mapVisibility}\n${characters}`.matchAll(/(?:`|### )(GET|POST) (\/api[^` \r\n]*)/g)]
  .map(([, method, path]) => `${method} ${path.split('?')[0]}`);
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
const liveReview={expectedVersion:0,customizationVersion:2};
validate(schemas.ModuleLiveReview,liveReview);
for(const invalid of [{expectedVersion:0},{...liveReview,customizationVersion:-1},{...liveReview,expectedVersion:0.5},{...liveReview,confirmStructural:true}])assert.throws(()=>validate(schemas.ModuleLiveReview,invalid));
validate(schemas.ModuleLiveApply,{...liveReview,requestId:'00000000-0000-4000-8000-000000000001',impactHash:'a'.repeat(64)});
// A blocked review retains every required item, even beyond the command limit.
const migrationConfirmations=Array.from({length:501},(_,n)=>`APPLY /nodes/item-${n}/text`);
const migrationApply={...liveReview,requestId:'00000000-0000-4000-8000-000000000001',impactHash:'a'.repeat(64),confirmations:migrationConfirmations.slice(0,500)};
validate(schemas.ModuleMigrationApply,migrationApply);
assert.throws(()=>validate(schemas.ModuleMigrationApply,{...migrationApply,confirmations:migrationConfirmations}));
assert.throws(()=>validate(schemas.ModuleMigrationApply,{...migrationApply,confirmations:['duplicate','duplicate']}));
validate(schemas.ModuleMigrationImpact,{version:0,customizationVersion:0,impactHash:'a'.repeat(64),confirmations:migrationConfirmations,blockers:['/confirmation-limit'],changes:[],runtime:{}});
assert.match(api.paths['/api/campaigns/{campaignId}/module-instance/updates'].post['x-idempotency'].authorization,/source and target before receipt replay/);
const updateSelection={path:'/nodes/00000000-0000-4000-8000-000000000001/text',choice:'INCOMING'};
validate(schemas.ModuleUpdateSelection,updateSelection);
for(const invalid of [{...updateSelection,choice:'AUTO'},{...updateSelection,actor:'forged'},{path:updateSelection.path}])assert.throws(()=>validate(schemas.ModuleUpdateSelection,invalid));
const updateImpact={expectedVersion:0,packageId:'00000000-0000-4000-8000-000000000001',selections:[updateSelection]};validate(schemas.ModuleUpdateImpactRequest,updateImpact);
for(const invalid of [{...updateImpact,expectedVersion:0.5},{...updateImpact,selections:null},{...updateImpact,impactHash:'forged'}])assert.throws(()=>validate(schemas.ModuleUpdateImpactRequest,invalid));
validate(schemas.ModuleUpdateApply,{...updateImpact,requestId:updateImpact.packageId,sha256:'a'.repeat(64),impactHash:'b'.repeat(64)});
const updatePreviewBody={expectedVersion:0,packageId:'10000000-0000-4000-8000-000000000001'};
validate(schemas.ModuleUpdatePreviewRequest,updatePreviewBody);
for(const field of Object.keys(updatePreviewBody)){const broken={...updatePreviewBody};delete broken[field];assert.throws(()=>validate(schemas.ModuleUpdatePreviewRequest,broken));}
for(const value of [-1,0.5,'0',null])assert.throws(()=>validate(schemas.ModuleUpdatePreviewRequest,{...updatePreviewBody,expectedVersion:value}));
assert.throws(()=>validate(schemas.ModuleUpdatePreviewRequest,{...updatePreviewBody,requestId:updatePreviewBody.packageId}));
assert.equal(api.paths['/api/campaigns/{campaignId}/module-customizations/{id}/update-preview'].post['x-idempotency'],undefined);
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
const assetList = '/api/campaigns/{campaignId}/assets';
const assetContent = `${assetList}/{assetId}/content`;
const sceneMap = '/api/campaigns/{campaignId}/scenes/{sceneId}/map';
for (const [path, method] of [[assetList, 'get'], [assetList, 'post'], [assetContent, 'get'], [sceneMap, 'get'], [sceneMap, 'post']]) {
  const operation = api.paths[path][method];
  assert.equal(operation['x-required-role'], 'DM');
  assert.match(operation.description, /current DM.*PLAYER.*403.*foreign.*404/i);
  for (const status of ['401', '403', '404']) assert.ok(operation.responses[status]);
}
const upload = api.paths[assetList].post;
const mapCommand = api.paths[sceneMap].post;
const multipart = upload.requestBody.content['multipart/form-data'];
assert.equal(upload.requestBody.required, true);
assert.deepEqual(Object.keys(upload.requestBody.content), ['multipart/form-data']);
assert.deepEqual(resolve(multipart.schema).required, ['command', 'file']);
assert.deepEqual(Object.keys(resolve(multipart.schema).properties), ['command', 'file']);
assert.equal(resolve(multipart.schema).additionalProperties, false);
assert.deepEqual(resolve(resolve(multipart.schema).properties.command).required, ['requestId', 'expectedVersion']);
assert.equal(resolve(resolve(multipart.schema).properties.command).additionalProperties, false);
assert.equal(multipart.encoding.command.contentType, 'application/json');
assert.equal(multipart.encoding.file.contentType, 'image/png, image/jpeg');
assert.deepEqual(upload['x-image-validation'], {
  maxBytes: 10485760, minDimension: 1, maxDimension: 8192, maxPixels: 32000000,
  allowedMediaTypes: ['image/png', 'image/jpeg'], decodedFormatMustMatch: true,
  rejectMalformedOrTruncated: true, ignoreOriginalFilename: true,
});
assert.match(resolve(upload.responses['400']).description, /INVALID_INPUT/);
assert.ok(upload.responses['413']);
for (const [operation, version, fields] of [
  [upload, 'campaign management version shared with P01/P02a', ['commandKind', 'campaignId', 'expectedVersion', 'validatedFileSha256', 'mediaType']],
  [mapCommand, 'target scene version shared with movement', ['commandKind', 'campaignId', 'sceneId', 'expectedVersion', 'map']],
]) {
  assert.equal(operation['x-version'], version);
  assert.equal(operation['x-idempotency'].namespace, 'asset-commands');
  assert.deepEqual(operation['x-idempotency'].key, ['sessionActor', 'requestId']);
  assert.deepEqual(operation['x-idempotency'].payload, fields);
  assert.match(operation['x-idempotency'].authorization, /current DM.*before.*replay/i);
  assert.match(operation['x-idempotency'].samePayload, /original.*before.*stale.*no.*event/i);
  assert.match(operation['x-idempotency'].differentPayload, /409 IDEMPOTENCY_CONFLICT/);
  assert.match(resolve(operation.responses['409']).description, /VERSION_CONFLICT.*IDEMPOTENCY_CONFLICT/);
}
assert.deepEqual(requestSchema(sceneMap).required, ['requestId', 'expectedVersion', 'map']);
assert.deepEqual(responseSchema(assetList, 'get', '200').required, ['campaignVersion', 'assets']);
assert.deepEqual(responseSchema(assetList, 'post', '200').required, ['asset', 'campaignVersion']);
assert.deepEqual(responseSchema(sceneMap, 'get', '200').required, ['sceneId', 'sceneVersion', 'map']);
assert.deepEqual(responseSchema(sceneMap, 'post', '200'), responseSchema(sceneMap, 'get', '200'));
const download = api.paths[assetContent].get;
assert.deepEqual(Object.keys(download.responses['200'].content), ['image/png', 'image/jpeg']);
assert.equal(download.responses['200'].headers['Content-Disposition'].schema.const, 'attachment');
assert.equal(download.responses['200'].headers['X-Content-Type-Options'].schema.const, 'nosniff');
for (const response of Object.values(download.responses)) {
  assert.equal(resolve(response).headers['Cache-Control'].schema.const, 'no-store, private');
  assert.equal(resolve(response).headers.ETag, undefined);
}
assert.equal(download.responses['304'], undefined);
assert.match(resolve(download.responses['503']).description, /ASSET_UNAVAILABLE.*never.*path/i);
assert.ok(schemas.ApiError.properties.code.enum.includes('ASSET_UNAVAILABLE'));
const assetPolicy = api['x-asset-policy'];
assert.match(assetPolicy.storage, /server.generated.*opaque.*atomic.*before.*metadata/i);
assert.match(assetPolicy.atomicity, /rollback.*metadata.*versions.*receipts.*clean.*new.*files/i);
assert.match(assetPolicy.atomicity, /uncertain commit.*after the transaction.*preserve committed.*database is unavailable.*absence is confirmed.*original failure.*durable receipt/i);
assert.match(assetPolicy.visibility, /snapshot.*unchanged.*no.*asset.*map.*player/i);
assert.match(assetPolicy.download, /every.*read.*logout.*revocation.*no.*public.*signed/i);
assert.match(assetPolicy.map, /same.campaign.*inactive.*logical.*unchanged/i);
assert.match(assetPolicy.historicalReplay, /never.*reattach.*reread/i);
assert.match(assetPolicy.locking, /actor.*campaign/i);
assert.match(assetPolicy.migration, /V4.*V1.*V3.*same.campaign/i);
const assetView = { id, mediaType: 'image/png', byteSize: 67, width: 1, height: 1 };
const map = { assetId: id, gridSize: 48, offsetX: 0, offsetY: 0, distancePerCell: 5 };
const mapView = { sceneId: id, sceneVersion: 0, map };
const setMap = { requestId: id, expectedVersion: 0, map };
for (const [schema, value] of [
  [schemas.AssetView, assetView], [schemas.AssetList, { campaignVersion: 0, assets: [assetView] }],
  [schemas.AssetUploadResult, { asset: assetView, campaignVersion: 1 }],
  [schemas.SceneMap, mapView], [schemas.SceneMap, { ...mapView, map: null }],
  [schemas.SceneMapRequest, setMap], [schemas.SceneMapRequest, { ...setMap, map: null }],
  [schemas.MapConfiguration, { ...map, gridSize: 8, offsetX: -8192, offsetY: 8192, distancePerCell: 0.01 }],
  [schemas.MapConfiguration, { ...map, gridSize: 512, distancePerCell: 10000 }],
]) validate(schema, value);
for (const invalid of [
  {}, { ...map, assetId: 'bad' }, { ...map, gridSize: 7 }, { ...map, gridSize: 513 }, { ...map, gridSize: 8.5 },
  { ...map, offsetX: -8193 }, { ...map, offsetY: 8193 }, { ...map, offsetX: 0.1 },
  { ...map, distancePerCell: 0 }, { ...map, distancePerCell: 10001 }, { ...map, distancePerCell: NaN },
  { ...map, distancePerCell: Infinity }, { ...map, distancePerCell: '5' }, { ...map, hidden: true },
]) assert.throws(() => validate(schemas.MapConfiguration, invalid));
for (const key of Object.keys(map)) {
  const partial = { ...map }; delete partial[key];
  assert.throws(() => validate(schemas.MapConfiguration, partial));
}
for (const invalid of [
  { requestId: id, expectedVersion: 0 }, { ...setMap, map: false }, { ...setMap, map: [] },
  { ...setMap, expectedVersion: -1 }, { ...setMap, requestId: 'bad' }, { ...setMap, actor: 'dm' },
]) assert.throws(() => validate(schemas.SceneMapRequest, invalid));
for (const invalid of [
  { ...assetView, mediaType: 'image/svg+xml' }, { ...assetView, byteSize: 0 }, { ...assetView, byteSize: 10485761 },
  { ...assetView, width: 0 }, { ...assetView, height: 8193 }, { ...assetView, width: 1.5 },
]) assert.throws(() => validate(schemas.AssetView, invalid));
for (const secret of ['assetId', 'map', 'storageKey', 'filename', 'originalFilename', 'path', 'sha256', 'imageWidth', 'imageHeight']) {
  assert.throws(() => validate(schemas.SceneSnapshot, { ...snapshot, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.TokenView, { ...token, [secret]: 'secret' }));
  assert.throws(() => validate(event, { type: 'SCENE_CHANGED', campaignId: id, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.ApiError, { code: 'ASSET_UNAVAILABLE', message: 'Unavailable', [secret]: 'secret' }));
}
for (const secret of ['storageKey', 'filename', 'originalFilename', 'path', 'sha256', 'campaignId']) {
  assert.throws(() => validate(schemas.AssetView, { ...assetView, [secret]: 'secret' }));
}
const tokenList = '/api/campaigns/{campaignId}/scenes/{sceneId}/tokens';
const configureToken = `${tokenList}/{tokenId}`;
for (const [path, method] of [[tokenList, 'get'], [tokenList, 'post'], [configureToken, 'post']]) {
  const operation = api.paths[path][method];
  assert.equal(operation['x-required-role'], 'DM');
  assert.match(operation.description, /current DM.*PLAYER.*403.*foreign.*404/i);
  assert.match(operation.description, /inactive.*without.*activat/i);
  for (const status of ['400', '401', '403', '404']) assert.ok(operation.responses[status]);
}
assert.deepEqual(responseSchema(tokenList, 'get', '200').required, ['sceneId', 'sceneVersion', 'tokens']);
for (const [path, version, payload] of [
  [tokenList, 'target scene version shared with maps/movement', ['commandKind', 'campaignId', 'sceneId', 'expectedVersion', 'strippedName', 'x', 'y', 'hidden', 'canonicalControllers']],
  [configureToken, 'target token version shared with movement', ['commandKind', 'campaignId', 'sceneId', 'tokenId', 'expectedVersion', 'strippedName', 'x', 'y', 'hidden', 'canonicalControllers']],
]) {
  const operation = api.paths[path].post;
  const command = requestSchema(path);
  assert.equal(operation['x-version'], version);
  assert.deepEqual(command.required, ['requestId', 'expectedVersion', 'name', 'x', 'y', 'hidden', 'controllers']);
  assert.equal(command.additionalProperties, false);
  assert.deepEqual(operation['x-grid-bounds'], { x: '0 <= x < scene.width', y: '0 <= y < scene.height' });
  assert.equal(operation['x-idempotency'].namespace, 'token-commands');
  assert.deepEqual(operation['x-idempotency'].key, ['sessionActor', 'requestId']);
  assert.deepEqual(operation['x-idempotency'].payload, payload);
  assert.match(operation['x-idempotency'].authorization, /current DM.*target ownership.*before.*replay/i);
  assert.match(operation['x-idempotency'].samePayload, /original.*before.*stale.*controller.membership.*no.*event/i);
  assert.match(operation['x-idempotency'].differentPayload, /409 IDEMPOTENCY_CONFLICT/);
  assert.match(resolve(operation.responses['409']).description, /VERSION_CONFLICT.*IDEMPOTENCY_CONFLICT/);
  assert.deepEqual(responseSchema(path, 'post', '200').required, ['sceneId', 'sceneVersion', 'token']);
}
assert.deepEqual(responseSchema(tokenList, 'post', '200'), responseSchema(configureToken, 'post', '200'));
assert.equal(schemas.TokenCommandRequest.properties.name['x-trimBeforeValidation'], true);
assert.deepEqual(schemas.TokenAuthoringView.required, ['id', 'name', 'x', 'y', 'version', 'hidden', 'controllers']);
assert.equal(schemas.TokenAuthoringView.additionalProperties, false);
const controllersSchema = resolve(schemas.TokenCommandRequest.properties.controllers);
assert.equal(controllersSchema.minItems, 0);
assert.equal(controllersSchema.maxItems, 100);
assert.equal(controllersSchema.uniqueItems, true);
assert.equal(resolve(controllersSchema.items).minLength, 1);
assert.equal(resolve(controllersSchema.items).maxLength, 80);
assert.match(controllersSchema.description, /exact.*current.*member.*sorted/i);
const tokenPolicy = api['x-token-policy'];
assert.match(tokenPolicy.versions, /create.*scene.*0.*update.*token.*scene.*unchanged/i);
assert.match(tokenPolicy.locking, /actor.*campaign.*same.request.*one effect.*same version.*one winner/i);
assert.match(tokenPolicy.atomicity, /token.*controllers.*scene version.*receipt.*atomically/i);
assert.match(tokenPolicy.receiptNamespace, /token.commands.*create.*update.*separate.*account.*scene.*asset.*movement/i);
assert.match(tokenPolicy.historicalReplay, /never.*recreate.*restore.*regrant.*reread/i);
assert.match(tokenPolicy.membership, /current.*member.*generic 400.*before.*expected version/i);
assert.match(tokenPolicy.revocation, /campaign.wide.*token.*scene versions.*rejoin.*never.*restore/i);
assert.match(tokenPolicy.movement, /current.*membership.*current.scene.*visibility.*control.*before.*receipt.*403.*404/i);
assert.match(tokenPolicy.visibility, /hidden.*omitted.*player.*unchanged.*hidden.*controller.*error.*event/i);
assert.match(tokenPolicy.realtime, /after.commit.*SCENE_CHANGED.*type.*campaignId/i);
assert.match(tokenPolicy.migration, /V5.*V1.*V4.*checksums.*same.campaign.*backfill/i);
const tokenCommand = { requestId: id, expectedVersion: 0, name: 'Shared token', x: 2, y: 3, hidden: false, controllers: ['dm', 'player'] };
const authoredToken = { id, name: 'Shared token', x: 2, y: 3, version: 0, hidden: false, controllers: ['dm', 'player'] };
const tokenResult = { sceneId: id, sceneVersion: 1, token: authoredToken };
const tokenAuthoringList = { sceneId: id, sceneVersion: 1, tokens: [authoredToken] };
for (const [schema, value] of [
  [schemas.TokenCommandRequest, tokenCommand],
  [schemas.TokenCommandRequest, { ...tokenCommand, name: ` ${'😀'.repeat(100)} `, controllers: [] }],
  [schemas.TokenCommandRequest, { ...tokenCommand, controllers: ['a', '界'.repeat(80)] }],
  [schemas.TokenCommandRequest, { ...tokenCommand, controllers: Array.from({ length: 100 }, (_, n) => `member${n}`) }],
  [schemas.TokenAuthoringView, authoredToken], [schemas.TokenCommandResult, tokenResult],
  [schemas.TokenAuthoringList, tokenAuthoringList],
]) validate(schema, value);
for (const key of Object.keys(tokenCommand)) {
  const partial = { ...tokenCommand }; delete partial[key];
  assert.throws(() => validate(schemas.TokenCommandRequest, partial));
  assert.throws(() => validate(schemas.TokenCommandRequest, { ...tokenCommand, [key]: null }));
}
for (const invalid of [
  { ...tokenCommand, name: '' }, { ...tokenCommand, name: ' \t\n ' }, { ...tokenCommand, name: '😀'.repeat(101) },
  { ...tokenCommand, expectedVersion: -1 }, { ...tokenCommand, expectedVersion: 0.5 },
  { ...tokenCommand, x: -1 }, { ...tokenCommand, x: 1.5 }, { ...tokenCommand, y: -1 },
  { ...tokenCommand, y: NaN }, { ...tokenCommand, x: Infinity }, { ...tokenCommand, x: '1' },
  { ...tokenCommand, requestId: 'bad' }, { ...tokenCommand, hidden: 'false' },
  { ...tokenCommand, controllers: 'player' }, { ...tokenCommand, controllers: [null] },
  { ...tokenCommand, controllers: [''] }, { ...tokenCommand, controllers: ['x'.repeat(81)] },
  { ...tokenCommand, controllers: ['player', 'player'] },
  { ...tokenCommand, controllers: Array.from({ length: 101 }, (_, n) => `member${n}`) },
  { ...tokenCommand, actor: 'dm' }, { ...tokenCommand, tokenId: id }, { ...tokenCommand, canMove: true },
]) assert.throws(() => validate(schemas.TokenCommandRequest, invalid));
for (const schema of [schemas.TokenAuthoringView, schemas.TokenCommandResult, schemas.TokenAuthoringList]) {
  const value = schema === schemas.TokenAuthoringView ? authoredToken : schema === schemas.TokenCommandResult ? tokenResult : tokenAuthoringList;
  for (const key of schema.required) {
    const partial = { ...value }; delete partial[key];
    assert.throws(() => validate(schema, partial));
    assert.throws(() => validate(schema, { ...value, [key]: null }));
  }
  assert.throws(() => validate(schema, { ...value, unexpected: 'secret' }));
}
for (const invalid of [{ ...authoredToken, version: -1 }, { ...authoredToken, id: 'bad' }, { ...authoredToken, controllers: ['dm', 'dm'] }]) {
  assert.throws(() => validate(schemas.TokenAuthoringView, invalid));
}
for (const secret of ['hidden', 'controllers', 'hiddenTokenIds', 'hiddenCount', 'tokenVersion', 'sceneVersion']) {
  assert.throws(() => validate(schemas.SceneSnapshot, { ...snapshot, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.TokenView, { ...token, [secret]: 'secret' }));
  assert.throws(() => validate(event, { type: 'SCENE_CHANGED', campaignId: id, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.ApiError, { code: 'NOT_FOUND', message: 'Unavailable', [secret]: 'secret' }));
}
const currentMapView = '/api/campaigns/{campaignId}/scene/map-view';
const currentMapImage = '/api/campaigns/{campaignId}/scene/map-image';
const fogPath = '/api/campaigns/{campaignId}/scenes/{sceneId}/fog';
const mapOperations = [[currentMapView, 'get'], [currentMapImage, 'get'], [fogPath, 'get'], [fogPath, 'post']];
for (const [path, method] of mapOperations) {
  const operation = api.paths[path][method];
  for (const status of ['400', '401', '403']) assert.ok(operation.responses[status]);
  for (const response of Object.values(operation.responses)) {
    const headers = resolve(response).headers;
    assert.equal(headers['Cache-Control'].required, true);
    assert.equal(headers['Cache-Control'].schema.const, 'no-store, private');
    assert.equal(headers['X-Content-Type-Options'].required, true);
    assert.equal(headers['X-Content-Type-Options'].schema.const, 'nosniff');
    for (const name of ['ETag', 'Last-Modified', 'Location', 'Content-Range', 'Accept-Ranges']) assert.equal(headers[name], undefined);
  }
  for (const status of ['206', '301', '302', '303', '304', '307', '308']) assert.equal(operation.responses[status], undefined);
}
const sharedMap = responseSchema(currentMapView, 'get', '200');
assert.deepEqual(sharedMap, schemas.CurrentMapView);
assert.deepEqual(sharedMap.required, ['sceneId', 'sceneVersion', 'map']);
assert.deepEqual(Object.keys(sharedMap.properties), sharedMap.required);
assert.equal(sharedMap.additionalProperties, false);
assert.deepEqual(sharedMap.properties.map.anyOf, [{ $ref: '#/components/schemas/SharedMapConfiguration' }, { type: 'null' }]);
assert.deepEqual(schemas.SharedMapConfiguration.required, ['distancePerCell']);
assert.deepEqual(Object.keys(schemas.SharedMapConfiguration.properties), ['distancePerCell']);
assert.equal(schemas.SharedMapConfiguration.additionalProperties, false);
assert.match(api.paths[currentMapView].get.description, /current members.*403.*current scene/i);
const safeImage = api.paths[currentMapImage].get;
const imageQueries = safeImage.parameters.map(resolve);
assert.deepEqual(imageQueries.map(({ name, in: location, required }) => ({ name, in: location, required })), [
  { name: 'sceneId', in: 'query', required: true }, { name: 'version', in: 'query', required: true },
]);
assert.equal(resolve(imageQueries[0].schema).format, 'uuid');
assert.deepEqual(resolve(imageQueries[1].schema), schemas.Version);
for (const invalid of [null, -1, 0.5, '0', NaN, Infinity]) assert.throws(() => validate(imageQueries[1].schema, invalid));
for (const invalid of [null, '', 'bad', 0]) assert.throws(() => validate(imageQueries[0].schema, invalid));
assert.deepEqual(Object.keys(safeImage.responses['200'].content), ['image/png']);
assert.deepEqual(safeImage.responses['200'].content['image/png'].schema, { type: 'string', format: 'binary' });
for (const status of ['404', '409', '503']) assert.ok(safeImage.responses[status]);
assert.match(resolve(safeImage.responses['409']).description, /VERSION_CONFLICT/);
assert.match(resolve(safeImage.responses['503']).description, /ASSET_UNAVAILABLE.*never.*path/i);
assert.match(safeImage.description, /membership first.*current scene.*404.*exact.*version.*409.*attached map.*404/i);
for (const method of ['get', 'post']) {
  const operation = api.paths[fogPath][method];
  assert.equal(operation['x-required-role'], 'DM');
  assert.match(operation.description, /current DM.*before.*target.*replay.*PLAYER.*403.*foreign.*404/i);
  assert.match(operation.description, /inactive.*without.*activat/i);
  assert.ok(operation.responses['404']);
  assert.deepEqual(responseSchema(fogPath, method, '200'), schemas.SceneFog);
}
const fogCommandOperation = api.paths[fogPath].post;
assert.deepEqual(requestSchema(fogPath), schemas.FogCommandRequest);
assert.equal(fogCommandOperation['x-version'], 'target scene version shared with moves/maps/token authoring');
assert.deepEqual(fogCommandOperation['x-grid-bounds'], { x: '0 <= x < scene.width', y: '0 <= y < scene.height', maxItems: 'scene.width * scene.height' });
assert.equal(fogCommandOperation['x-idempotency'].namespace, 'fog-commands');
assert.deepEqual(fogCommandOperation['x-idempotency'].key, ['sessionActor', 'requestId']);
assert.deepEqual(fogCommandOperation['x-idempotency'].payload, ['campaignId', 'sceneId', 'expectedVersion', 'canonicalRevealed']);
assert.match(fogCommandOperation['x-idempotency'].authorization, /current DM.*target ownership.*before.*replay/i);
assert.match(fogCommandOperation['x-idempotency'].samePayload, /original.*before.*stale.*no.*event/i);
assert.match(fogCommandOperation['x-idempotency'].differentPayload, /409 IDEMPOTENCY_CONFLICT/);
assert.match(resolve(fogCommandOperation.responses['409']).description, /VERSION_CONFLICT.*IDEMPOTENCY_CONFLICT/);
for (const [schema, required] of [[schemas.FogCell, ['x', 'y']], [schemas.SceneFog, ['sceneId', 'sceneVersion', 'revealed']],
  [schemas.FogCommandRequest, ['requestId', 'expectedVersion', 'revealed']]]) {
  assert.deepEqual(schema.required, required);
  assert.deepEqual(Object.keys(schema.properties), required);
  assert.equal(schema.additionalProperties, false);
}
for (const schema of [schemas.SceneFog, schemas.FogCommandRequest]) {
  const revealedSchema = resolve(schema.properties.revealed);
  assert.equal(revealedSchema.uniqueItems, true);
  assert.equal(revealedSchema.minItems, 0);
  assert.equal(revealedSchema['x-maxItemsFromScene'], 'scene.width * scene.height');
  assert.deepEqual(resolve(revealedSchema.items), schemas.FogCell);
}
const fogPolicy = api['x-map-visibility-policy'];
for (const [key, pattern] of [
  ['pixels', /width.*gridSize.*height.*gridSize.*nearest.neighbor.*opaque black.*alpha.*fresh PNG.*metadata/i],
  ['sampling', /offsetX.*floor.*map.gridSize.*scene.gridSize.*offsetY.*out.of.source.*black/i],
  ['visibility', /default.*covered.*revealed.*never.*original bytes.*DM.*full.*unit.*independent/i],
  ['authorization', /every.*membership.*current scene.*version.*campaign lock.*no historical.*role/i],
  ['cache', /no.store.*nosniff.*no ETag.*Last.Modified.*304.*Range.*206.*redirect.*no.*derivative cache.*disk/i],
  ['originals', /DM.only.*guessed.*foreign.*no.store.*never.*static/i],
  ['reCover', /cannot.*retract.*previously.*saved.*authorized before.*finish/i],
  ['versions', /map.*assignment.*calibration.*detach.*reset.*covered.*once.*token.*unchanged/i],
  ['fog', /distinct.*in.bounds.*y then x.*without.*map/i],
  ['locking', /actor.*campaign.*one effect.*one winner/i],
  ['atomicity', /state.*version.*receipt.*atomically.*historical.*never.*restore/i],
  ['realtime', /after.commit.*SCENE_CHANGED.*type.*campaignId.*no.*pixels.*fog.*URLs/i],
  ['migration', /V6.*V1.*V5.*checksums.*rows/i],
  ['clientLifecycle', /no.store.*remove.*before.*revoke.*textures.*401.*403.*late.*post.ack/i],
]) assert.match(fogPolicy[key], pattern);
const safeMapView = { sceneId: id, sceneVersion: 0, map: { distancePerCell: 5 } };
const fogState = { sceneId: id, sceneVersion: 1, revealed: [{ x: 1, y: 2 }] };
const fogCommand = { requestId: id, expectedVersion: 0, revealed: [{ x: 1, y: 2 }] };
for (const [schema, value] of [[schemas.CurrentMapView, safeMapView], [schemas.CurrentMapView, { ...safeMapView, map: null }],
  [schemas.SceneFog, fogState], [schemas.SceneFog, { ...fogState, revealed: [] }], [schemas.FogCommandRequest, fogCommand],
  [schemas.FogCommandRequest, { ...fogCommand, revealed: [] }]]) validate(schema, value);
for (const [schema, value] of [[schemas.CurrentMapView, safeMapView], [schemas.SharedMapConfiguration, safeMapView.map],
  [schemas.FogCell, fogCommand.revealed[0]], [schemas.SceneFog, fogState], [schemas.FogCommandRequest, fogCommand]]) {
  for (const key of schema.required) {
    const partial = { ...value }; delete partial[key];
    assert.throws(() => validate(schema, partial));
    if (key !== 'map') assert.throws(() => validate(schema, { ...value, [key]: null }));
  }
  assert.throws(() => validate(schema, { ...value, unexpected: 'secret' }));
}
for (const cell of [null, {}, { x: 0 }, { x: -1, y: 0 }, { x: 0.5, y: 0 }, { x: 0, y: '0' }, { x: 0, y: 0, hidden: true }]) {
  assert.throws(() => validate(schemas.FogCommandRequest, { ...fogCommand, revealed: [cell] }));
}
for (const invalid of [{ ...fogCommand, revealed: null }, { ...fogCommand, revealed: [fogCommand.revealed[0], fogCommand.revealed[0]] },
  { ...fogCommand, expectedVersion: -1 },
  { ...fogCommand, expectedVersion: 1.5 }, { ...fogCommand, requestId: 'bad' }, { ...fogCommand, actor: 'dm' }]) {
  assert.throws(() => validate(schemas.FogCommandRequest, invalid));
}
for (const distancePerCell of [0, 10001, NaN, Infinity, '5']) {
  assert.throws(() => validate(schemas.SharedMapConfiguration, { distancePerCell }));
}
for (const secret of ['assetId', 'gridSize', 'offsetX', 'offsetY', 'width', 'height', 'storageKey', 'originalUrl', 'revealed', 'pixels', 'imageUrl', 'fog']) {
  assert.throws(() => validate(schemas.CurrentMapView, { ...safeMapView, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.CurrentMapView, { ...safeMapView, map: { ...safeMapView.map, [secret]: 'secret' } }));
  assert.throws(() => validate(event, { type: 'SCENE_CHANGED', campaignId: id, [secret]: 'secret' }));
  assert.throws(() => validate(schemas.ApiError, { code: 'NOT_FOUND', message: 'Unavailable', [secret]: 'secret' }));
}
const characterExample = JSON.parse(await read('../contracts/character.example.json'));
validate(schemas.CharacterDocument, characterExample);
assert.deepEqual(schemas.CharacterDocument.required, ['format', 'formatVersion', 'rules', 'sheet']);
assert.deepEqual(schemas.CharacterSheet.required, ['name', 'avatar', 'species', 'background', 'classes', 'abilities', 'skills', 'saves', 'combat', 'attacks', 'casting', 'spells', 'features', 'resources', 'equipment', 'money', 'conditions', 'modifiers', 'overrides', 'notes']);
for (const [name, schema] of Object.entries(schemas).filter(([name]) => name.startsWith('Character') && name !== 'CharacterCalculation')) {
  if (schema.properties) { assert.equal(schema.additionalProperties, false, name); assert.deepEqual(schema.required, Object.keys(schema.properties), name); }
}
for (const field of schemas.CharacterSheet.required) {
  const broken = structuredClone(characterExample); delete broken.sheet[field];
  assert.throws(() => validate(schemas.CharacterDocument, broken));
}
for (const field of ['campaignId', 'controllers', 'hidden', 'actor', 'history', 'placements']) {
  assert.throws(() => validate(schemas.CharacterDocument, { ...characterExample, [field]: 'forged' }));
  assert.throws(() => validate(schemas.CharacterSheet, { ...characterExample.sheet, [field]: 'forged' }));
}
for (const field of ['hp', 'maxHp', 'resources', 'notes', 'controllers', 'sheet', 'history', 'source', 'hidden']) assert.ok(!Object.hasOwn(schemas.CharacterSummary.properties, field));
for (const path of Object.keys(api.paths).filter(p => p.includes('/characters'))) {
  for (const [method, op] of Object.entries(api.paths[path]).filter(([method]) => ['get', 'post'].includes(method))) {
    assert.ok(op['x-character-permission']);
    if (method === 'post' && !path.endsWith('/content-preview')) { assert.equal(op['x-idempotency'].namespace, 'character-commands'); assert.match(op['x-idempotency'].authorization, /before receipt replay/); assert.match(op['x-idempotency'].samePayload, /before stale.*no mutation or event/); }
  }
}
assert.deepEqual(schemas.CharacterSource.properties.rules.enum, ['5R', '2014', 'CUSTOM']);
assert.match(api['x-character-policy'].expiry, /only reminds DM.*confirmation/);
assert.match(api['x-character-policy'].transfer, /target DM review/);
assert.match(api['x-character-policy'].revocation, /actual actor.*never restore/);
assert.match(api['x-character-policy'].authorization, /creation\/copy\/import.*current visibility.*result identity/);
for (const value of [0, 31, 1.5, '12', null]) assert.throws(() => validate(schemas.CharacterSheet, { ...characterExample.sheet, abilities: { ...characterExample.sheet.abilities, STR: value } }));
for (const value of [null, -1, 4, 0.5]) assert.throws(() => validate(schemas.CharacterCombat, { ...characterExample.sheet.combat, deathFailures: value }));
assert.throws(() => validate(schemas.CharacterDocument, { ...characterExample, formatVersion: 2 }));
assert.equal(api.paths['/api/campaigns/{campaignId}/characters/{id}/content-preview'].post['x-read-only'], true);
const seed = JSON.parse(await read('../backend/src/main/resources/content/srd-selection.json'));
assert.equal(new Set(seed.map(e => e.id)).size, seed.length);
for (const e of seed) {
  validate(schemas.ContentData, e.data);
  assert.equal(e.builtin, true); assert.equal(e.license, 'CC-BY-4.0');
  assert.match(e.attribution, /Wizards of the Coast LLC/); assert.match(e.attribution, /creativecommons.org/);
  assert.ok(e.changes.length); assert.equal(e.data.source.rules, '5R');
}
const sample={requestId:id,expectedVersion:0,entry:seed[0].data};
validate(schemas.CustomContentRequest,sample);
for (const field of Object.keys(sample.entry)) { const broken=structuredClone(sample); delete broken.entry[field]; assert.throws(()=>validate(schemas.CustomContentRequest,broken)); }
assert.throws(()=>validate(schemas.ContentFillRequest,{requestId:id,expectedVersion:0,policyVersion:0,entryId:id,ability:'STR',castingId:'',reason:'chosen',sheet:characterExample.sheet}));
const diceCommand={requestId:id,expectedVersion:0,characterId:'',key:'EXPRESSION',expression:'2d6+3',visibility:'PUBLIC',advantage:[],disadvantage:[]};
validate(schemas.DiceCommand,diceCommand);
for(const field of Object.keys(diceCommand)){const broken={...diceCommand};delete broken[field];assert.throws(()=>validate(schemas.DiceCommand,broken));assert.throws(()=>validate(schemas.DiceCommand,{...diceCommand,[field]:null}));}
for(const field of ['actor','total','result','dice','kept'])assert.throws(()=>validate(schemas.DiceCommand,{...diceCommand,[field]:20}));
assert.throws(()=>validate(schemas.DiceCommand,{...diceCommand,expectedVersion:0.5}));
assert.throws(()=>validate(schemas.DiceCommand,{...diceCommand,visibility:'PLAYER'}));
assert.throws(()=>validate(schemas.DiceCommand,{...diceCommand,advantage:['']}));
const publicInitiative={status:'CONFIRMED',entries:[{characterId:id,name:'Visible hero',total:15}]};validate(schemas.InitiativePublicState,publicInitiative);
for(const secret of ['version','group','surprised','result','calculation','hidden','position','count']){assert.throws(()=>validate(schemas.InitiativePublicState,{...publicInitiative,[secret]:'secret'}));assert.throws(()=>validate(schemas.InitiativePublicEntry,{...publicInitiative.entries[0],[secret]:'secret'}));}
assert.equal(api.paths['/api/campaigns/{campaignId}/rolls'].post['x-idempotency'].namespace,api.paths['/api/campaigns/{campaignId}/roll-commands'].post['x-idempotency'].namespace);
assert.match(api['x-dice-policy'].events,/DM-only.*DMs only/);assert.match(api['x-dice-policy'].visibility,/filter before pagination/);
const combatDeclaration={requestId:id,expectedVersion:0,characterId:id,targetId:id,attackId:'sword',visibility:'PUBLIC'};
const combatCommand={requestId:id,expectedVersion:0,kind:'APPLY',rollId:'',verdict:'',changes:[{characterId:id,expectedVersion:0,hp:10,tempHp:0,resources:[]}],correctionOf:'',reason:'DM confirmed final values'};
for(const [schema,sample] of [[schemas.CombatDeclare,combatDeclaration],[schemas.CombatCommand,combatCommand]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const forged of ['actor','total','sourceSheet','automatic'])assert.throws(()=>validate(schema,{...sample,[forged]:'forged'}));
}
const publicTurn={status:'ACTIVE',order:[{characterId:id,name:'Hero'}],currentId:id,endVersion:123};validate(schemas.TurnStatePublic,publicTurn);
for(const secret of ['version','round','grants','reason','hidden','count','position'])assert.throws(()=>validate(schemas.TurnStatePublic,{...publicTurn,[secret]:'secret'}));
for(const secret of ['reason','changes','privateOnly','sheet','hp','resources'])assert.ok(!Object.hasOwn(schemas.CombatEventPublic.properties,secret));
for(const path of ['/actions','/actions/{id}','/actions/{id}/events','/combat','/combat/history'])for(const method of ['get','post']){const op=api.paths['/api/campaigns/{campaignId}'+path]?.[method];if(op&&method==='post'){assert.equal(op['x-idempotency'].namespace,'combat-commands');assert.match(op['x-idempotency'].authorization,/before receipt replay/);}}
assert.match(api['x-combat-policy'].movement,/check before receipt replay/);assert.match(api['x-combat-policy'].correction,/current-version.*preserve later play/);
const timelineSend={requestId:id,expectedVersion:0,characterId:id,kind:'RP',text:'Plain <script>text</script>',replyTo:'',attachments:[]};
const timelineEdit={requestId:id,expectedVersion:0,text:'Corrected',attachments:[id],reason:'Correct spelling'};
for(const [schema,sample] of [[schemas.TimelineSend,timelineSend],[schemas.TimelineEdit,timelineEdit],[schemas.TimelineUpload,{requestId:id,expectedVersion:0}],[schemas.TimelineSessionCommand,{requestId:id,expectedVersion:0,kind:'START',name:'Night 1'}]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const forged of ['actor','sourceSheet','result','storageKey','nameSnapshot'])assert.throws(()=>validate(schema,{...sample,[forged]:'forged'}));
}
assert.throws(()=>validate(schemas.TimelineSend,{...timelineSend,attachments:[id,id]}));
assert.throws(()=>validate(schemas.TimelineSend,{...timelineSend,text:'x'.repeat(8001)}));
assert.throws(()=>validate(schemas.TimelineSend,{...timelineSend,expectedVersion:0.5}));
assert.throws(()=>validate(schemas.TimelineSend,{...timelineSend,replyTo:'foreign-string'}));
for(const secret of ['ordinal','total','count','storageKey','dmReason','replyTo'])assert.ok(!Object.hasOwn(schemas.TimelineMessage.properties,secret));
assert.ok(!Object.hasOwn(schemas.TimelineRevision.properties,'replyTo'));
assert.match(api['x-timeline-policy'].authorization,/before pagination.*search.*references.*export.*downloads/);
assert.match(api['x-timeline-policy'].retention,/No expiry.*backup rotation/);
assert.match(api['x-timeline-policy'].atomicity,/atomically.*uncertain.*fresh campaign lock/);
for(const path of ['/messages','/messages/{id}','/attachments','/timeline/sessions'])assert.match(api.paths['/api/campaigns/{campaignId}'+path].post['x-idempotency'].authorization,/before receipt replay/);
const noteCreate={requestId:id,expectedVersion:0,visibility:'TEAM',title:'Team plan',text:'Plain <script>text</script> %_',attachments:[]};
const noteEdit={requestId:id,expectedVersion:0,title:'Corrected',text:'Updated',attachments:[id],reason:'Spelling'};
for(const [schema,sample]of [[schemas.NoteCreate,noteCreate],[schemas.NoteEdit,noteEdit],[schemas.NoteUpload,{requestId:id,expectedVersion:0,visibility:'DM'}]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const forged of ['actor','storageKey','sourceSheet','hidden','canEdit'])assert.throws(()=>validate(schema,{...sample,[forged]:'forged'}));
 assert.throws(()=>validate(schema,{...sample,expectedVersion:0.5}));
}
for(const invalid of [{...noteCreate,visibility:'PLAYER'},{...noteCreate,attachments:[id,id]},{...noteCreate,text:'x'.repeat(16001)},{...noteCreate,title:''}])assert.throws(()=>validate(schemas.NoteCreate,invalid));
assert.throws(()=>validate(schemas.NoteEdit,{...noteEdit,visibility:'TEAM'}));
assert.match(api['x-notes-policy'].visibility,/before receipt replay.*filter before search\/pagination/);
for(const suffix of ['/notes','/notes/{id}','/note-attachments'])assert.match(api.paths['/api/campaigns/{campaignId}'+suffix].post['x-idempotency'].authorization,/before receipt replay/);
validate(schemas.BackupCommand,{requestId:id,expectedVersion:0});
for(const invalid of [{requestId:id},{expectedVersion:0},{requestId:id,expectedVersion:-1},{requestId:id,expectedVersion:0.5},{requestId:id,expectedVersion:0,actor:'forged'},{requestId:id,expectedVersion:0,storageRoot:'/private'}])assert.throws(()=>validate(schemas.BackupCommand,invalid));
for(const path of ['/api/admin/backups','/api/admin/backups/migration'])assert.match(api.paths[path].post['x-idempotency'].authorization,/operator before receipt replay/);
assert.match(api['x-backups-policy'].retention,/no deletion of game histories or live files/);
assert.match(api['x-backups-policy'].migration,/durable source write fence.*isolated empty-target restore/);
const moduleCommand={requestId:id,expectedVersion:0,createCopy:false,confirmLegacy:false};
validate(schemas.ModuleCommand,moduleCommand);
for(const field of Object.keys(moduleCommand)){const missing={...moduleCommand};delete missing[field];assert.throws(()=>validate(schemas.ModuleCommand,missing));assert.throws(()=>validate(schemas.ModuleCommand,{...moduleCommand,[field]:null}));}
for(const invalid of [{...moduleCommand,actor:'forged'},{...moduleCommand,expectedVersion:0.5},{...moduleCommand,createCopy:'false'}])assert.throws(()=>validate(schemas.ModuleCommand,invalid));
const moduleManifest={format:'dnd-roll.module',formatVersion:1,moduleId:id,moduleVersion:'1.2.3',name:'Synthetic',rules:'5R',rights:'Original',dependencies:[],content:{size:10,sha256:'a'.repeat(64)},assets:[]};
validate(schemas.ModuleManifest,moduleManifest);
validate(schemas.ModuleManifest,{...moduleManifest,formatVersion:2});
for(const invalid of [{...moduleManifest,moduleVersion:'01.2.3'},{...moduleManifest,formatVersion:3},{...moduleManifest,script:'run()'},{...moduleManifest,rules:'2024'}])assert.throws(()=>validate(schemas.ModuleManifest,invalid));
const moduleNode={id,kind:'SCENE',name:'Literal text',text:'<script>literal</script>',visibility:'DM',nodeIds:[],assetIds:[],dependencyIds:[]};validate(schemas.ModuleDocument,{nodes:[moduleNode]});
for(const invalid of [{...moduleNode,script:'x'},{...moduleNode,assetIds:[id,id]},{...moduleNode,text:'x'.repeat(10001)}])assert.throws(()=>validate(schemas.ModuleNode,invalid));
const moduleRoot=api.paths['/api/campaigns/{campaignId}/module-packages'];assert.match(moduleRoot.post['x-idempotency'].authorization,/current DM before receipt replay/);
for(const [suffix,fields]of [['',['command','file']],['/preview',['file']]]){const post=api.paths['/api/campaigns/{campaignId}/module-packages'+suffix].post;assert.deepEqual(post.requestBody.content['multipart/form-data'].schema.required.sort(),fields);assert.equal(post.requestBody.content['multipart/form-data'].schema.additionalProperties,false);}
assert.match(api['x-modules-policy'].validation,/no extraction or package code execution/);assert.match(api['x-modules-policy'].atomicity,/BYTEA.*one transaction.*P09/);
const moduleDefinition={nodes:[moduleNode],play:{scenes:[{nodeId:id,width:20,height:15,gridSize:48,map:null}],units:[],placements:[],admissionMode:'REVIEW',approvedDependencyIds:[]}};
validate(schemas.ModuleDefinition,moduleDefinition);
for(const key of Object.keys(moduleDefinition.play)){const play={...moduleDefinition.play};delete play[key];assert.throws(()=>validate(schemas.ModuleDefinition,{...moduleDefinition,play}));}
for(const name of ['ModuleCopyCommand','ModuleEditCommand','ModuleTemplateCommand','ModuleLaunchCommand','ModuleProgressCommand']){assert.equal(schemas[name].additionalProperties,false);assert.ok(schemas[name].required.includes('requestId'));assert.ok(schemas[name].required.includes('expectedVersion'));}
for(const suffix of ['/module-customizations','/module-customizations/{id}','/module-customizations/{id}/templates','/module-customizations/{id}/campaigns','/module-instance'])assert.match(api.paths['/api/campaigns/{campaignId}'+suffix].post['x-idempotency'].authorization,/current DM before receipt replay/);
console.log(`Contract checks passed: ${actualOperations.length} HTTP operations; A02 independent customizations, typed v2 templates and campaign instances. Previous operations retained; focused checker, not a general OpenAPI validator.`);
