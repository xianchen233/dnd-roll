import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(path, import.meta.url), 'utf8');
const [apiText, eventText, readme] = await Promise.all([
  read('../contracts/openapi.json'),
  read('../contracts/scene-events.schema.json'),
  read('../contracts/README.md'),
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
    if (schema.minLength !== undefined) assert.ok(value.length >= schema.minLength);
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
const documentedOperations = [...readme.matchAll(/`(GET|POST) (\/api[^` ]*)`/g)]
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
assert.deepEqual(schemas.ApiError.properties.code.enum, ['UNAUTHENTICATED', 'FORBIDDEN', 'NOT_FOUND', 'VERSION_CONFLICT', 'IDEMPOTENCY_CONFLICT', 'INVALID_INPUT']);
assert.deepEqual(Object.keys(event.properties).sort(), ['campaignId', 'type']);
assert.deepEqual(event.required, ['type', 'campaignId']);
assert.equal(event.additionalProperties, false);
assert.equal(event['x-stomp'].endpoint, '/ws');
assert.equal(event['x-stomp'].subscription, '/user/queue/scene-changes');
assert.equal(event['x-stomp'].connectCsrfHeader, 'X-CSRF-TOKEN');
assert.equal(event['x-stomp'].clientSendAllowed, false);
assert.equal(event['x-stomp'].arbitrarySubscriptionsAllowed, false);

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
console.log(`Contract checks passed: ${actualOperations.length} HTTP operations; CSRF/session, movement/idempotency, snapshot and token-free STOMP event constraints.`);
