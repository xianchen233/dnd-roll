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
for(const suffix of ['knockout-first-aid-rolls','knockout-first-aid-effect','knockout-first-aid-completions'])assert.ok(api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/'+suffix]?.post,`A15 ${suffix} route missing`);
const aidRoute=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/knockout-first-aid-effect'].post;
assert.equal(aidRoute['x-read-only'],true);assert.ok(aidRoute.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));
assert.equal(aidRoute.requestBody.content['application/json'].schema.$ref,'#/components/schemas/FirstAidReview');
for(const code of ['200','400','401','403','404','409'])assert.ok(aidRoute.responses[code]);
for(const kind of ['ROLL_KNOCKOUT_FIRST_AID','END_KNOCKOUT_FIRST_AID'])assert.ok(api.components.schemas.AttackPreviewEvent.properties.kind.enum.includes(kind));
for(const suffix of ['knockout-rest-effect','knockout-rest-completions'])assert.ok(api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/'+suffix]?.post,`A14 ${suffix} route missing`);
const restRoute=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/knockout-rest-effect'].post;
assert.equal(restRoute['x-read-only'],true);assert.ok(restRoute.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));
assert.equal(restRoute.requestBody.content['application/json'].schema.$ref,'#/components/schemas/KnockoutRestReview');
for(const code of ['200','400','401','403','404','409'])assert.ok(restRoute.responses[code]);
const restComplete=api.components.schemas.KnockoutRestComplete;
assert.equal(restComplete.additionalProperties,false);assert.deepEqual([...restComplete.required].sort(),['requestId','expectedVersion','previewHash','targetVersion','minutes','interrupted','reviewed','planHash','reason'].sort());
assert.ok(api.components.schemas.AttackPreviewEvent.properties.kind.enum.includes('END_KNOCKOUT_REST'));
for(const suffix of ['knockout-healing-effect','knockout-healings'])assert.ok(api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/'+suffix]?.post,`A13 ${suffix} route missing`);
const healingRoute=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/knockout-healing-effect'].post;
assert.equal(healingRoute['x-read-only'],true);
assert.ok(healingRoute.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));
assert.equal(healingRoute.requestBody.content['application/json'].schema.$ref,'#/components/schemas/KnockoutHealingReview');
for(const code of ['200','400','401','403','404','409'])assert.ok(healingRoute.responses[code]);
const healingApply=api.components.schemas.KnockoutHealingApply;
assert.equal(healingApply.additionalProperties,false);
assert.deepEqual([...healingApply.required].sort(),['requestId','expectedVersion','previewHash','targetVersion','amount','reviewed','planHash','reason'].sort());
assert.equal(healingApply.properties.amount.minimum,1);assert.equal(healingApply.properties.amount.maximum,100000);
assert.ok(api.components.schemas.AttackPreviewEvent.properties.kind.enum.includes('HEAL_KNOCKOUT'));
for(const suffix of ['ammunition-effect','ammunition-confirmations'])assert.ok(api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/'+suffix]?.post,`A09 ${suffix} route missing`);
assert.ok(api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/damage-effect']?.get,'A08 damage effect route missing');
const rangeRoute=api.paths['/api/campaigns/{campaignId}/range-hints/preview']?.post;
assert.ok(rangeRoute,'A07 range hint route missing');
assert.equal(rangeRoute['x-read-only'],true);
assert.ok(rangeRoute.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));
assert.equal(rangeRoute.requestBody.content['application/json'].schema.$ref,'#/components/schemas/RangeHintRequest');
for(const code of ['200','400','401','403','404','409'])assert.ok(rangeRoute.responses[code],`A07 missing ${code}`);
for(const [suffix,method]of [['','get'],['/{nodeId}/preview','post'],['/{nodeId}/bind','post'],['/{nodeId}/history','get']])assert.ok(api.paths['/api/campaigns/{campaignId}/module-operation-bindings'+suffix]?.[method],`A03h missing ${method} ${suffix}`);
for(const [suffix,method]of [['','get'],['/preview','post'],['/paths','post'],['/adjustments','post']])assert.ok(api.paths['/api/campaigns/{campaignId}/tokens/{tokenId}/movement'+suffix]?.[method],`A06 missing ${method} ${suffix}`);
for(const [suffix,method]of [['/attack-previews','get'],['/attack-previews','post'],['/attack-previews/{id}','get'],['/attack-previews/{id}/events','post'],['/attack-results','get']])assert.ok(api.paths['/api/campaigns/{campaignId}'+suffix]?.[method],`A05 missing ${method} ${suffix}`);
for(const [suffix,method] of [['','get'],['/{id}','post'],['/{id}/impact','post'],['/{id}/executions','post'],['/{id}/executions','get']])assert.ok(api.paths['/api/campaigns/{campaignId}/module-operations'+suffix]?.[method],`A03d missing ${method} ${suffix}`);
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

const healingExample={profile:'5R-SRD-5.2.1-KNOCKOUT-HEALING-V1',status:'CANDIDATE',reasons:[],hp:5,tempHp:0,healed:4,conditionId:'a11-knockout:origin',targetVersion:2,hash:'a'.repeat(64)};
const aidExample={profile:'5R-SRD-5.2.1-KNOCKOUT-FIRST-AID-V1',status:'CANDIDATE',reasons:[],dc:10,total:10,hp:1,tempHp:0,conditionId:'a11-knockout:origin',targetVersion:2,hash:'a'.repeat(64)};
validate(api.components.schemas.FirstAidPlan,aidExample);validate(api.components.schemas.FirstAidPlan,{...aidExample,status:'MANUAL_REQUIRED',reasons:['FIRST_AID_CHECK_FAILED'],total:9,hp:null,tempHp:null,conditionId:null});
const aidAttempt={requestId:'00000000-0000-0000-0000-000000000015',expectedVersion:2,previewHash:'a'.repeat(64),targetVersion:2,helperId:'00000000-0000-0000-0000-000000000016',helperVersion:3,reviewed:true,reason:'Action reviewed'};
validate(api.components.schemas.FirstAidAttempt,aidAttempt);
const aidReview={expectedVersion:3,previewHash:aidAttempt.previewHash,targetVersion:2,helperId:aidAttempt.helperId,helperVersion:3,rollId:'00000000-0000-0000-0000-000000000017',reviewed:true};
validate(api.components.schemas.FirstAidReview,aidReview);const aidComplete={...aidReview,requestId:aidAttempt.requestId,planHash:'b'.repeat(64),reason:'Result reviewed'};validate(api.components.schemas.FirstAidComplete,aidComplete);
for(const patch of [{total:20},{dc:5},{hp:1},{conditionId:'client'},{helperVersion:-1},{reviewed:null}])assert.throws(()=>validate(api.components.schemas.FirstAidComplete,{...aidComplete,...patch}));
const restExample={profile:'5R-SRD-5.2.1-KNOCKOUT-REST-V1',status:'CANDIDATE',reasons:[],hp:1,tempHp:0,conditionId:'a11-knockout:origin',targetVersion:2,hash:'a'.repeat(64)};
validate(api.components.schemas.KnockoutRestPlan,restExample);validate(api.components.schemas.KnockoutRestPlan,{...restExample,status:'MANUAL_REQUIRED',reasons:['UNINTERRUPTED_REST_REQUIRED'],hp:null,tempHp:null,conditionId:null});
const restRequest={requestId:'00000000-0000-0000-0000-000000000014',expectedVersion:2,previewHash:'a'.repeat(64),targetVersion:2,minutes:60,interrupted:false,reviewed:true,planHash:'b'.repeat(64),reason:'Reviewed'};
validate(restComplete,restRequest);
for(const patch of [{minutes:-1},{minutes:60.5},{minutes:100001},{hp:1},{conditionId:'client'},{interrupted:null},{reviewed:null}])assert.throws(()=>validate(restComplete,{...restRequest,...patch}));
validate(api.components.schemas.KnockoutHealingPlan,healingExample);
validate(api.components.schemas.KnockoutHealingPlan,{...healingExample,status:'MANUAL_REQUIRED',reasons:['NO_ACTUAL_HEALING'],hp:null,tempHp:null,healed:null,conditionId:null});
const healingRequest={requestId:'00000000-0000-0000-0000-000000000013',expectedVersion:2,previewHash:'a'.repeat(64),targetVersion:2,amount:4,reviewed:true,planHash:'b'.repeat(64),reason:'Reviewed'};
validate(healingApply,healingRequest);
for(const patch of [{amount:0},{amount:1.5},{amount:100001},{hp:5},{conditionId:'client'},{reviewed:null}])assert.throws(()=>validate(healingApply,{...healingRequest,...patch}));

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
const rangeSchemas=api.components.schemas;
const rangeRequest={sceneId:'00000000-0000-0000-0000-000000000001',sceneVersion:0,sourceId:'00000000-0000-0000-0000-000000000002',sourceVersion:0,targetId:'00000000-0000-0000-0000-000000000003',targetVersion:0,profile:'GRID_5R_5FT_RANGE_V1',reviewed:true,mode:'RANGED',normalFeet:10,longFeet:15};
validate(rangeSchemas.RangeHintRequest,rangeRequest);
for(const field of Object.keys(rangeRequest)){const bad={...rangeRequest};delete bad[field];assert.throws(()=>validate(rangeSchemas.RangeHintRequest,bad));}
for(const extra of [{x:1},{actor:'forged'},{hidden:true},{normalFeet:2.5},{longFeet:100001},{sceneVersion:9007199254740992},{profile:'2014'},{mode:'SPELL_AREA'},{reviewed:'true'}])assert.throws(()=>validate(rangeSchemas.RangeHintRequest,{...rangeRequest,...extra}));
validate(rangeSchemas.RangeHintResult,{status:'CANDIDATE',distanceFeet:15,band:'LONG_RANGE',reason:''});
validate(rangeSchemas.RangeHintResult,{status:'MANUAL_REQUIRED',distanceFeet:null,band:null,reason:'GRID_REQUIRES_MANUAL'});
assert.throws(()=>validate(rangeSchemas.RangeHintResult,{status:'MANUAL_REQUIRED',distanceFeet:15,band:'LONG_RANGE',reason:'GRID_REQUIRES_MANUAL'}));
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
const ammunitionSample={expectedVersion:0,previewHash:'a'.repeat(64),resourceId:'arrows',reviewed:true};
const ammunitionConfirm={...ammunitionSample,requestId:'00000000-0000-4000-8000-000000000001',reason:'Explicit arrow mapping and exceptions reviewed'};
for(const [schema,sample]of [[schemas.AmmunitionReview,ammunitionSample],[schemas.AmmunitionConfirm,ammunitionConfirm]]){
 validate(schema,sample);for(const key of Object.keys(sample)){const bad={...sample};delete bad[key];assert.throws(()=>validate(schema,bad));assert.throws(()=>validate(schema,{...sample,[key]:null}));}
 for(const bad of [{actor:'dm'},{changes:[]},{amount:1},{hp:1},{resourceId:''},{reviewed:'true'},{previewHash:'bad'},{expectedVersion:-1}])assert.throws(()=>validate(schema,{...sample,...bad}));
}
const ammunitionPlan={profile:'5R-SRD-5.2.1-SHORTBOW-AMMO-V1',status:'CANDIDATE',reasons:[],resourceId:'arrows',before:1,after:0,hp:20,tempHp:1};validate(schemas.AmmunitionEffect,ammunitionPlan);
validate(schemas.AmmunitionEffect,{...ammunitionPlan,status:'MANUAL_REQUIRED',reasons:['INSUFFICIENT_AMMUNITION'],before:null,after:null,hp:null,tempHp:null});
for(const bad of [{...ammunitionPlan,profile:'2014'},{...ammunitionPlan,after:-1},{...ammunitionPlan,status:'MANUAL_REQUIRED',reasons:['INSUFFICIENT_AMMUNITION']},{...ammunitionPlan,amount:1}])assert.throws(()=>validate(schemas.AmmunitionEffect,bad));
assert.ok(schemas.AttackPreviewEvent.properties.kind.enum.includes('CONFIRM_AMMUNITION'));
assert.ok(!schemas.AttackPreviewCommand.properties.kind.enum.includes('CONFIRM_AMMUNITION'));
for(const suffix of ['ammunition-effect','ammunition-confirmations']){const route=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/'+suffix].post;assert.equal(route['x-read-only'],suffix==='ammunition-effect');for(const response of Object.values(route.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');}
const damageRoute=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/damage-effect'].get;
assert.equal(damageRoute['x-read-only'],true);assert.equal(damageRoute['x-required-role'],'DM');
for(const response of Object.values(damageRoute.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');
assert.ok(schemas.AttackPreviewCommand.properties.kind.enum.includes('CONFIRM_DAMAGE'));
assert.ok(schemas.AttackPreviewEvent.properties.kind.enum.includes('CONFIRM_DAMAGE'));
const damagePlan={profile:'5R-SRD-5.2.1-DAMAGE-HP-V1',status:'CANDIDATE',reasons:[],hp:18,tempHp:0,absorbed:2,hpLoss:2};
validate(schemas.DamageEffect,damagePlan);
validate(schemas.DamageEffect,{...damagePlan,status:'MANUAL_REQUIRED',reasons:['ZERO_HP_REQUIRES_MANUAL'],hp:null,tempHp:null,absorbed:null,hpLoss:null});
for(const bad of [{...damagePlan,profile:'2014'},{...damagePlan,hp:-1},{...damagePlan,damage:4},{...damagePlan,status:'MANUAL_REQUIRED',reasons:['ZERO_HP_REQUIRES_MANUAL']},{...damagePlan,status:'CANDIDATE',reasons:['CONDITIONS_REQUIRE_MANUAL']}])assert.throws(()=>validate(schemas.DamageEffect,bad));
const bindingReviewSample={expectedVersion:1,operationVersion:0,instanceVersion:2,campaignVersion:3},bindingBindSample={...bindingReviewSample,requestId:'00000000-0000-4000-8000-000000000001',impactHash:'a'.repeat(64),reason:'Explicit current targets'};
for(const [schema,sample]of [[schemas.ModuleBindingReview,bindingReviewSample],[schemas.ModuleBindingBind,bindingBindSample]]){validate(schema,sample);for(const field of Object.keys(sample)){const broken={...sample};delete broken[field];assert.throws(()=>validate(schema,broken));assert.throws(()=>validate(schema,{...sample,[field]:null}));}for(const extra of ['steps','actor','automatic','sourceVersion'])assert.throws(()=>validate(schema,{...sample,[extra]:'forged'}));}
for(const bad of [{...bindingReviewSample,expectedVersion:0},{...bindingReviewSample,operationVersion:-1},{...bindingReviewSample,instanceVersion:0.5}])assert.throws(()=>validate(schemas.ModuleBindingReview,bad));
for(const bad of [{...bindingBindSample,reason:''},{...bindingBindSample,reason:'x'.repeat(1001)},{...bindingBindSample,impactHash:'bad'}])assert.throws(()=>validate(schemas.ModuleBindingBind,bad));
const selectedReviewSample={expectedVersion:0,customizationVersion:1,campaignVersion:2,selections:[{nodeId:'10000000-0000-4000-8000-000000000001',choice:'KEEP',bindingVersion:2,operationVersion:3}]};
for(const choice of ['KEEP','INCOMING','RETIRE'])validate(schemas.ModuleOperationMigrationReview,{...selectedReviewSample,selections:[{...selectedReviewSample.selections[0],choice}]});
for(const field of Object.keys(selectedReviewSample)){const broken={...selectedReviewSample};delete broken[field];assert.throws(()=>validate(schemas.ModuleOperationMigrationReview,broken));}
for(const bad of [{...selectedReviewSample,actor:'forged'},{...selectedReviewSample,selections:[{...selectedReviewSample.selections[0],choice:'DELETE'}]},{...selectedReviewSample,selections:[{...selectedReviewSample.selections[0],operationVersion:-1}]},{...selectedReviewSample,selections:Array(201).fill(selectedReviewSample.selections[0])}])assert.throws(()=>validate(schemas.ModuleOperationMigrationReview,bad));
validate(schemas.ModuleOperationMigrationApply,{...selectedReviewSample,requestId:'10000000-0000-4000-8000-000000000002',impactHash:'a'.repeat(64),confirmations:['OPERATION stable KEEP']});
for(const suffix of ['operation-options','operation-migration-impact','operation-migrations']){const p=api.paths['/api/campaigns/{campaignId}/module-instance/'+suffix].post;assert.equal(p['x-read-only'],suffix!=='operation-migrations');assert.ok(p.parameters.some(x=>x.$ref==='#/components/parameters/CsrfHeader'));assert.equal(p.responses['403'].headers['Cache-Control'].schema.const,'no-store, private');}
assert.equal(api.paths['/api/campaigns/{campaignId}/module-operation-bindings/{nodeId}/preview'].post['x-read-only'],true);
assert.match(api.paths['/api/campaigns/{campaignId}/module-operation-bindings/{nodeId}/bind'].post['x-idempotency'].authorization,/current target.*before receipt replay/);
const movementSample={requestId:'00000000-0000-4000-8000-000000000001',expectedVersion:0,sceneVersion:1,characterVersion:2,budgetVersion:'00000000-0000-4000-8000-000000000002',turnKey:42,reviewed:true,path:[{x:2,y:2,mode:'WALK',difficult:false}],override:false,cost:0,reason:''};
validate(schemas.MovementPath,movementSample);
for(const key of Object.keys(movementSample)){const invalid={...movementSample};delete invalid[key];assert.throws(()=>validate(schemas.MovementPath,invalid));assert.throws(()=>validate(schemas.MovementPath,{...movementSample,[key]:null}));}
for(const invalid of [{...movementSample,actor:'forged'},{...movementSample,path:[]},{...movementSample,path:Array(201).fill(movementSample.path[0])},{...movementSample,path:[{...movementSample.path[0],mode:'FLY'}]},{...movementSample,expectedVersion:-1},{...movementSample,turnKey:0.5},{...movementSample,reason:'x'.repeat(1001)}])assert.throws(()=>validate(schemas.MovementPath,invalid));
assert.equal(api.paths['/api/campaigns/{campaignId}/tokens/{tokenId}/movement/preview'].post['x-read-only'],true);
for(const suffix of ['/paths','/adjustments'])assert.match(api.paths['/api/campaigns/{campaignId}/tokens/{tokenId}/movement'+suffix].post['x-idempotency'].authorization,/before receipt replay/);
const attackPreviewCreate={requestId:'00000000-0000-4000-8000-000000000001',characterId:'00000000-0000-4000-8000-000000000001',sourceVersion:0,targetId:'00000000-0000-4000-8000-000000000002',targetVersion:0,attackId:'content:trusted',reviewed:true,exceptions:[],advantage:[],disadvantage:[],immunities:[],resistances:[],vulnerabilities:[]};
const attackPreviewCommand={requestId:attackPreviewCreate.requestId,expectedVersion:0,kind:'CONFIRM',previewHash:'a'.repeat(64),changes:[],correctionOf:'',reason:'DM reviewed',summary:''};
for(const [schema,sample]of [[schemas.AttackPreviewCreate,attackPreviewCreate],[schemas.AttackPreviewCommand,attackPreviewCommand]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const field of ['actor','dice','armorClass','sheet','damage','total'])assert.throws(()=>validate(schema,{...sample,[field]:20}));
}
for(const invalid of [{...attackPreviewCreate,sourceVersion:0.5},{...attackPreviewCreate,targetVersion:-1},{...attackPreviewCreate,advantage:Array(11).fill({reason:'x',source:{book:'SRD 5.2.1',revision:'5.2.1',rules:'5R',detail:''}})},{...attackPreviewCommand,previewHash:'forged'},{...attackPreviewCommand,kind:'AUTOMATIC'}])assert.throws(()=>validate(invalid.kind?schemas.AttackPreviewCommand:schemas.AttackPreviewCreate,invalid));
assert.deepEqual(Object.keys(schemas.AttackPublicResult.properties).sort(),['actor','at','id','name','summary','targetName']);
validate(schemas.AttackFactor,{reason:'Reviewed factor',source:{book:'Unreviewed source',revision:'1',rules:'UNKNOWN',detail:'x'.repeat(1000)}});
assert.throws(()=>validate(schemas.AttackFactor,{reason:'Reviewed factor',source:{book:'Unreviewed source',revision:'1',rules:'UNKNOWN',detail:'x'.repeat(1001)}}));
validate(schemas.AttackPreviewEvidence.properties.manualReasons,['UNSUPPORTED_EXCEPTION: '+'x'.repeat(1000)]);
for(const suffix of ['/attack-previews','/attack-previews/{id}/events'])assert.match(api.paths['/api/campaigns/{campaignId}'+suffix].post['x-idempotency'].authorization,/current campaign DM.*before receipt replay/);
const opId='00000000-0000-4000-8000-000000000001';
const opStep={kind:'REVEAL_TOKEN',sceneId:opId,tokenId:opId,expectedVersion:0};
const opSave={requestId:opId,expectedVersion:0,name:'中文组合',steps:[opStep]};
validate(schemas.ModuleOperationSave,opSave);
const materialStep={kind:'PUBLISH_MATERIAL',noteId:opId,expectedVersion:0};
validate(schemas.ModuleOperationSave,{...opSave,steps:[materialStep]});
for(const field of Object.keys(materialStep)){const missing={...materialStep};delete missing[field];assert.throws(()=>validate(schemas.ModuleOperationStep,missing));assert.throws(()=>validate(schemas.ModuleOperationStep,{...materialStep,[field]:null}));}
for(const extra of [{attachments:[]},{visibility:'TEAM'},{actor:'forged'}])assert.throws(()=>validate(schemas.ModuleOperationStep,{...materialStep,...extra}));
for(const invalid of [{...opSave,steps:[]},{...opSave,steps:Array(21).fill(opStep)},{...opSave,actor:'forged'},{...opSave,steps:[{...opStep,expectedVersion:0.5}]},{...opSave,steps:[{...opStep,hidden:false}]},{...opSave,steps:[{kind:'SCRIPT',code:'anything'}]}])assert.throws(()=>validate(schemas.ModuleOperationSave,invalid));
for(const field of Object.keys(opSave)){const missing={...opSave};delete missing[field];assert.throws(()=>validate(schemas.ModuleOperationSave,missing));assert.throws(()=>validate(schemas.ModuleOperationSave,{...opSave,[field]:null}));}
for(const step of [{kind:'REVEAL_CHARACTER',characterId:opId,expectedVersion:0},{kind:'PUBLISH_TEXT',noteId:opId,expectedVersion:0},{kind:'START_ENCOUNTER',expectedVersion:0,initiativeVersion:2,reason:'DM starts'}])validate(schemas.ModuleOperationStep,step);
validate(schemas.ModuleOperationExecute,{requestId:opId,expectedVersion:1,campaignVersion:0,impactHash:'a'.repeat(64)});
assert.throws(()=>validate(schemas.ModuleOperationExecute,{requestId:opId,expectedVersion:1,campaignVersion:0,impactHash:'forged'}));
for(const suffix of ['/{id}','/{id}/executions'])assert.match(api.paths['/api/campaigns/{campaignId}/module-operations'+suffix].post['x-idempotency'].authorization,/target campaign DM.*before receipt replay/);
assert.equal(api.paths['/api/campaigns/{campaignId}/module-operations/{id}/impact'].post['x-read-only'],true);
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
    if (method === 'post' && !path.endsWith('/content-preview') && !path.endsWith('/stabilization/preview') && !path.endsWith('/stabilization/healing-preview') && !path.endsWith('/stabilization/damage-preview') && !path.endsWith('/restabilization/preview') && !path.endsWith('/restabilization/healing-preview') && !path.endsWith('/restabilization/damage-preview') && !path.endsWith('/instability/healing-preview')) { assert.equal(op['x-idempotency'].namespace, path.includes('/stabilization/')?'stabilization-commands':'character-commands'); assert.match(op['x-idempotency'].authorization, /before receipt replay/); assert.match(op['x-idempotency'].samePayload, /before stale.*no mutation or event/); }
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
validate(schemas.ModuleManifest,{...moduleManifest,formatVersion:3});
for(const invalid of [{...moduleManifest,moduleVersion:'01.2.3'},{...moduleManifest,formatVersion:4},{...moduleManifest,script:'run()'},{...moduleManifest,rules:'2024'}])assert.throws(()=>validate(schemas.ModuleManifest,invalid));
const moduleNode={id,kind:'SCENE',name:'Literal text',text:'<script>literal</script>',visibility:'DM',nodeIds:[],assetIds:[],dependencyIds:[]};validate(schemas.ModuleDocument,{nodes:[moduleNode]});
for(const invalid of [{...moduleNode,script:'x'},{...moduleNode,assetIds:[id,id]},{...moduleNode,text:'x'.repeat(10001)}])assert.throws(()=>validate(schemas.ModuleNode,invalid));
const moduleRoot=api.paths['/api/campaigns/{campaignId}/module-packages'];assert.match(moduleRoot.post['x-idempotency'].authorization,/current DM before receipt replay/);
for(const [suffix,fields]of [['',['command','file']],['/preview',['file']]]){const post=api.paths['/api/campaigns/{campaignId}/module-packages'+suffix].post;assert.deepEqual(post.requestBody.content['multipart/form-data'].schema.required.sort(),fields);assert.equal(post.requestBody.content['multipart/form-data'].schema.additionalProperties,false);}
assert.match(api['x-modules-policy'].validation,/no extraction or package code execution/);assert.match(api['x-modules-policy'].atomicity,/BYTEA.*one transaction.*P09/);
const moduleDefinition={nodes:[moduleNode],play:{scenes:[{nodeId:id,width:20,height:15,gridSize:48,map:null}],units:[],placements:[],admissionMode:'REVIEW',approvedDependencyIds:[]}};
validate(schemas.ModuleDefinition,moduleDefinition);
const portableDefinition={...moduleDefinition,operations:{formatVersion:1,definitions:[{nodeId:id,steps:[{kind:'REVEAL_TOKEN',placementId:id},{kind:'REVEAL_CHARACTER',unitId:id},{kind:'PUBLISH_MATERIAL',documentId:id},{kind:'START_ENCOUNTER',reason:'Explicit later binding'}]}]}};
validate(schemas.ModuleDefinitionV3,portableDefinition);
assert.throws(()=>validate(schemas.ModuleDefinition,portableDefinition));
for(const bad of [{...portableDefinition,operations:null},{...portableDefinition,operations:{formatVersion:2,definitions:[]}},{...portableDefinition,operations:{formatVersion:1,definitions:[{nodeId:id,steps:[]}]}}])assert.throws(()=>validate(schemas.ModuleDefinitionV3,bad));
for(const extra of [{expectedVersion:0},{tokenId:id},{actor:'forged'},{requestId:id},{initiativeVersion:1}])assert.throws(()=>validate(schemas.ModulePortableStep,{kind:'REVEAL_TOKEN',placementId:id,...extra}));
for(const kind of ['PUBLISH_TEXT','PUBLISH_MATERIAL'])validate(schemas.ModulePortableStep,{kind,documentId:id});
for(const key of Object.keys(moduleDefinition.play)){const play={...moduleDefinition.play};delete play[key];assert.throws(()=>validate(schemas.ModuleDefinition,{...moduleDefinition,play}));}
for(const name of ['ModuleCopyCommand','ModuleEditCommand','ModuleTemplateCommand','ModuleLaunchCommand','ModuleProgressCommand']){assert.equal(schemas[name].additionalProperties,false);assert.ok(schemas[name].required.includes('requestId'));assert.ok(schemas[name].required.includes('expectedVersion'));}
for(const suffix of ['/module-customizations','/module-customizations/{id}','/module-customizations/{id}/templates','/module-customizations/{id}/campaigns','/module-instance'])assert.match(api.paths['/api/campaigns/{campaignId}'+suffix].post['x-idempotency'].authorization,/current DM before receipt replay/);

assert.ok(api.paths["/api/campaigns/{campaignId}/attack-previews/{id}/monster-death-effect"]?.post,"A10 monster death route missing");

const deathSample={expectedVersion:0,previewHash:'a'.repeat(64),treatment:'MONSTER_DIES_AT_ZERO',reviewed:true};
for(const [schema,sample] of [[schemas.MonsterDeathReview,deathSample],[schemas.MonsterDeathConfirm,{...deathSample,requestId:id,reason:'Reviewed'}]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const extra of ['hp','tempHp','changes','actor','condition'])assert.throws(()=>validate(schema,{...sample,[extra]:0}));assert.throws(()=>validate(schema,{...sample,treatment:'CHARACTER'}));
}
const deathPlan={profile:'5R-SRD-5.2.1-MONSTER-DEATH-V1',status:'CANDIDATE',reasons:[],hp:0,tempHp:0,absorbed:1,hpLoss:1};validate(schemas.MonsterDeathEffect,deathPlan);
validate(schemas.MonsterDeathEffect,{...deathPlan,status:'MANUAL_REQUIRED',reasons:['LETHAL_DAMAGE_REQUIRED'],hp:null,tempHp:null,absorbed:null,hpLoss:null});
for(const bad of [{...deathPlan,hp:1},{...deathPlan,tempHp:1},{...deathPlan,status:'MANUAL_REQUIRED'},{...deathPlan,profile:'2014'}])assert.throws(()=>validate(schemas.MonsterDeathEffect,bad));
for(const suffix of ['effect','confirmations']){const route=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/monster-death-'+suffix].post;assert.equal(route['x-read-only'],suffix==='effect');assert.equal(route['x-required-role'],'DM');for(const response of Object.values(route.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');}
assert.ok(!schemas.AttackPreviewCommand.properties.kind.enum.includes('CONFIRM_MONSTER_DEATH'));

assert.ok(api.paths["/api/campaigns/{campaignId}/attack-previews/{id}/knockout-effect"]?.post,"A11 knockout route missing");

const knockoutSample={expectedVersion:0,previewHash:'a'.repeat(64),treatment:'KNOCK_OUT_MELEE',reviewed:true};
for(const [schema,sample] of [[schemas.KnockoutReview,knockoutSample],[schemas.KnockoutConfirm,{...knockoutSample,requestId:id,reason:'Reviewed'}]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const extra of ['hp','tempHp','changes','actor','condition'])assert.throws(()=>validate(schema,{...sample,[extra]:0}));assert.throws(()=>validate(schema,{...sample,treatment:'CHARACTER'}));
}
const knockoutPlan={profile:'5R-SRD-5.2.1-KNOCKOUT-V1',status:'CANDIDATE',reasons:[],hp:1,tempHp:0,absorbed:1,hpLoss:0};validate(schemas.KnockoutEffect,knockoutPlan);
validate(schemas.KnockoutEffect,{...knockoutPlan,status:'MANUAL_REQUIRED',reasons:['LETHAL_DAMAGE_REQUIRED'],hp:null,tempHp:null,absorbed:null,hpLoss:null});
for(const bad of [{...knockoutPlan,hp:0},{...knockoutPlan,tempHp:1},{...knockoutPlan,status:'MANUAL_REQUIRED'},{...knockoutPlan,profile:'2014'}])assert.throws(()=>validate(schemas.KnockoutEffect,bad));
for(const suffix of ['effect','confirmations']){const route=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/knockout-'+suffix].post;assert.equal(route['x-read-only'],suffix==='effect');assert.equal(route['x-required-role'],'DM');for(const response of Object.values(route.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');}
assert.ok(!schemas.AttackPreviewCommand.properties.kind.enum.includes('CONFIRM_KNOCKOUT'));



assert.ok(api.paths["/api/campaigns/{campaignId}/attack-previews/{id}/dagger-knockout-effect"]?.post,"A12 daggerKnockout route missing");

const daggerKnockoutSample={expectedVersion:0,previewHash:'a'.repeat(64),treatment:'KNOCK_OUT_MELEE',reviewed:true};
for(const [schema,sample] of [[schemas.DaggerKnockoutReview,daggerKnockoutSample],[schemas.DaggerKnockoutConfirm,{...daggerKnockoutSample,requestId:id,reason:'Reviewed'}]]){
 validate(schema,sample);for(const field of Object.keys(sample)){const missing={...sample};delete missing[field];assert.throws(()=>validate(schema,missing));assert.throws(()=>validate(schema,{...sample,[field]:null}));}
 for(const extra of ['hp','tempHp','changes','actor','condition'])assert.throws(()=>validate(schema,{...sample,[extra]:0}));assert.throws(()=>validate(schema,{...sample,treatment:'CHARACTER'}));
}
validate(schemas.DaggerKnockoutReview,{...daggerKnockoutSample,treatment:'THROWN'});
assert.throws(()=>validate(schemas.KnockoutReview,{...knockoutSample,treatment:'THROWN'}));
const daggerKnockoutPlan={profile:'5R-SRD-5.2.1-DAGGER-KNOCKOUT-V1',status:'CANDIDATE',reasons:[],hp:1,tempHp:0,absorbed:1,hpLoss:0};validate(schemas.DaggerKnockoutEffect,daggerKnockoutPlan);
validate(schemas.DaggerKnockoutEffect,{...daggerKnockoutPlan,status:'MANUAL_REQUIRED',reasons:['LETHAL_DAMAGE_REQUIRED'],hp:null,tempHp:null,absorbed:null,hpLoss:null});
for(const bad of [{...daggerKnockoutPlan,hp:0},{...daggerKnockoutPlan,tempHp:1},{...daggerKnockoutPlan,status:'MANUAL_REQUIRED'},{...daggerKnockoutPlan,profile:'2014'}])assert.throws(()=>validate(schemas.DaggerKnockoutEffect,bad));
for(const suffix of ['effect','confirmations']){const route=api.paths['/api/campaigns/{campaignId}/attack-previews/{id}/dagger-knockout-'+suffix].post;assert.equal(route['x-read-only'],suffix==='effect');assert.equal(route['x-required-role'],'DM');for(const response of Object.values(route.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');}
assert.ok(!schemas.AttackPreviewCommand.properties.kind.enum.includes('CONFIRM_DAGGER_KNOCKOUT'));



// A16: independent character scope; totals and final values are server-owned.
const stabilizationPath='/api/campaigns/{campaignId}/characters/{id}/stabilization';
for(const [suffix,method] of [['','get'],['/history','get'],['/rolls','post'],['/preview','post'],['/applications','post']]){
 const op=api.paths[stabilizationPath+suffix][method];assert.equal(op['x-required-role'],'DM');
 for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');
 if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));
}
const stabilizationSample={requestId:id,expectedVersion:0,targetVersion:0,helperId:id,helperVersion:0,reviewed:true,reason:'Reviewed Help'};
validate(schemas.StabilizationRollInput,stabilizationSample);
for(const field of ['total','dc','boundCheck','hp','actor'])assert.throws(()=>validate(schemas.StabilizationRollInput,{...stabilizationSample,[field]:20}));
for(const field of Object.keys(stabilizationSample)){const bad={...stabilizationSample};delete bad[field];assert.throws(()=>validate(schemas.StabilizationRollInput,bad));}
assert.throws(()=>validate(schemas.StabilizationRollInput,{...stabilizationSample,reviewed:'true'}));
validate(schemas.StabilizationState,{id,version:0,latest:null});

// A17: adjudicated healing has a separate explicit confirmation and no final values.
const healingSample={expectedVersion:2,targetVersion:1,attemptId:id,amount:3,reviewed:true};
validate(schemas.StabilizationHealingReview,healingSample);
for(const amount of [0,-1,100001,1.5,'3',null])assert.throws(()=>validate(schemas.StabilizationHealingReview,{...healingSample,amount}));
for(const field of ['hp','tempHp','removed','originVersion','boundOrigin','helperId','actor'])assert.throws(()=>validate(schemas.StabilizationHealingReview,{...healingSample,[field]:1}));
for(const field of Object.keys(healingSample)){const bad={...healingSample};delete bad[field];assert.throws(()=>validate(schemas.StabilizationHealingReview,bad));}
for(const [suffix,method]of [['/healing-preview','post'],['/healings','post'],['/healing-history','get']]){const op=api.paths[stabilizationPath+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
assert.ok(schemas.StabilizationAttempt.properties.status.enum.includes('HEALED'));

// A18: adjudicated final damage, not a client-supplied outcome or automatic death.
const stableDamage={expectedVersion:2,targetVersion:1,attemptId:id,amount:3,critical:true,reviewed:true};
validate(schemas.StabilizationDamageReview,stableDamage);validate(schemas.StabilizationDamageReview,{...stableDamage,amount:0});
for(const amount of [-1,220001,1.5,'3',null])assert.throws(()=>validate(schemas.StabilizationDamageReview,{...stableDamage,amount}));
for(const field of ['hp','tempHp','deathFailures','removed','boundOrigin','helperId','actor'])assert.throws(()=>validate(schemas.StabilizationDamageReview,{...stableDamage,[field]:1}));
for(const field of Object.keys(stableDamage)){const bad={...stableDamage};delete bad[field];assert.throws(()=>validate(schemas.StabilizationDamageReview,bad));}
for(const [suffix,method]of [['/damage-preview','post'],['/damages','post'],['/damage-history','get']]){const op=api.paths[stabilizationPath+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
assert.ok(schemas.StabilizationAttempt.properties.status.enum.includes('UNSTABLE'));

// A19 is a new independently versioned Help action, bound to an A18 origin.
const restabilizationRoot=stabilizationPath+'/restabilization',restSample={requestId:id,expectedVersion:0,originVersion:3,targetVersion:2,helperId:id,helperVersion:0,reviewed:true,reason:'Reviewed new Help'};
validate(schemas.RestabilizationRollInput,restSample);
for(const field of ['total','dc','boundOrigin','boundCheck','hp','annotations','actor'])assert.throws(()=>validate(schemas.RestabilizationRollInput,{...restSample,[field]:20}));
for(const field of Object.keys(restSample)){const bad={...restSample};delete bad[field];assert.throws(()=>validate(schemas.RestabilizationRollInput,bad));}
for(const [suffix,method]of [['','get'],['/history','get'],['/rolls','post'],['/preview','post'],['/applications','post']]){const op=api.paths[restabilizationRoot+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
assert.ok(schemas.StabilizationAttempt.properties.status.enum.includes('RESTABILIZED'));validate(schemas.RestabilizationState,{id,version:0,latest:null});

// A20: both source versions, no client final values, private explicit confirmation.
const rehealing={...healingSample,originVersion:4};validate(schemas.RestabilizationHealingReview,rehealing);
for(const amount of [0,-1,100001,1.5,'3',null])assert.throws(()=>validate(schemas.RestabilizationHealingReview,{...rehealing,amount}));
for(const field of ['hp','removed','boundOrigin','helperId','actor'])assert.throws(()=>validate(schemas.RestabilizationHealingReview,{...rehealing,[field]:1}));
for(const field of Object.keys(rehealing)){const bad={...rehealing};delete bad[field];assert.throws(()=>validate(schemas.RestabilizationHealingReview,bad));}
for(const [suffix,method]of [['/healing-preview','post'],['/healings','post'],['/healing-history','get']]){const op=api.paths[restabilizationRoot+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
assert.ok(schemas.RestabilizationAttempt.properties.status.enum.includes('HEALED'));

// A21: final adjudicated damage after A19, separately versioned and confirmed.
const redamage={...rehealing,amount:3,critical:true};validate(schemas.RestabilizationDamageReview,redamage);validate(schemas.RestabilizationDamageReview,{...redamage,amount:0});
for(const amount of [-1,220001,1.5,'3',null])assert.throws(()=>validate(schemas.RestabilizationDamageReview,{...redamage,amount}));
for(const field of ['hp','tempHp','deathFailures','removed','boundOrigin','helperId','actor'])assert.throws(()=>validate(schemas.RestabilizationDamageReview,{...redamage,[field]:1}));
for(const field of Object.keys(redamage)){const bad={...redamage};delete bad[field];assert.throws(()=>validate(schemas.RestabilizationDamageReview,bad));}
for(const [suffix,method]of [['/damage-preview','post'],['/damages','post'],['/damage-history','get']]){const op=api.paths[restabilizationRoot+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
assert.ok(schemas.RestabilizationAttempt.properties.status.enum.includes('UNSTABLE'));assert.ok(schemas.StabilizationAttempt.properties.status.enum.includes('RESTABILIZATION_UNSTABLE'));
const redamageOutcome={profile:'5R-SRD-5.2.1-RESTABILIZATION-DAMAGE-V1',status:'CANDIDATE',reasons:[],hp:0,tempHp:0,absorbed:0,deathSuccesses:0,deathFailures:1,removed:['a19-stable:'+id]};
validate(schemas.RestabilizationDamageEffect,redamageOutcome);
validate(schemas.RestabilizationDamageEffect,{...redamageOutcome,tempHp:1,absorbed:3,deathFailures:2});
validate(schemas.RestabilizationDamageEffect,{...redamageOutcome,status:'MANUAL_REQUIRED',reasons:['POSITIVE_DAMAGE_REQUIRED'],hp:null,tempHp:null,absorbed:null,deathSuccesses:null,deathFailures:null,removed:[]});
for(const bad of [{...redamageOutcome,hp:1},{...redamageOutcome,absorbed:-1},{...redamageOutcome,deathFailures:3},{...redamageOutcome,profile:'2014'}])assert.throws(()=>validate(schemas.RestabilizationDamageEffect,bad));


// A22: source damage version is server-derived, exact single-source healing.
const instabilityRoot=restabilizationRoot+'/instability';validate(schemas.InstabilityHealingReview,rehealing);
for(const amount of [0,-1,100001,1.5,'3',null])assert.throws(()=>validate(schemas.InstabilityHealingReview,{...rehealing,amount}));
for(const field of ['hp','removed','damageVersion','boundOrigin','actor','helperId'])assert.throws(()=>validate(schemas.InstabilityHealingReview,{...rehealing,[field]:1}));
for(const field of Object.keys(rehealing)){const bad={...rehealing};delete bad[field];assert.throws(()=>validate(schemas.InstabilityHealingReview,bad));}
for(const [suffix,method]of [['/healing-preview','post'],['/healings','post'],['/healing-history','get']]){const op=api.paths[instabilityRoot+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
const instabilityHealing={profile:'5R-SRD-5.2.1-INSTABILITY-HEALING-V1',status:'CANDIDATE',reasons:[],hp:1,tempHp:0,healed:1,deathSuccesses:0,deathFailures:0,removed:['a16-unconscious:'+id]};validate(schemas.InstabilityHealingEffect,instabilityHealing);
validate(schemas.InstabilityHealingEffect,{...instabilityHealing,status:'MANUAL_REQUIRED',reasons:['HEALING_REVIEW_REQUIRED'],hp:null,tempHp:null,healed:null,deathSuccesses:null,deathFailures:null,removed:[]});
for(const bad of [{...instabilityHealing,hp:0},{...instabilityHealing,healed:0},{...instabilityHealing,deathFailures:1},{...instabilityHealing,profile:'2014'}])assert.throws(()=>validate(schemas.InstabilityHealingEffect,bad));

// A23: direct original A18 source, no second ledger version or trusted source input.
const initialInstabilityRoot=stabilizationPath+'/instability';validate(schemas.InitialInstabilityHealingReview,healingSample);
for(const amount of [0,-1,100001,1.5,'3',null])assert.throws(()=>validate(schemas.InitialInstabilityHealingReview,{...healingSample,amount}));
for(const field of ['hp','removed','originVersion','damageVersion','boundOrigin','actor','helperId'])assert.throws(()=>validate(schemas.InitialInstabilityHealingReview,{...healingSample,[field]:1}));
for(const field of Object.keys(healingSample)){const bad={...healingSample};delete bad[field];assert.throws(()=>validate(schemas.InitialInstabilityHealingReview,bad));}
for(const [suffix,method]of [['/healing-preview','post'],['/healings','post'],['/healing-history','get']]){const op=api.paths[initialInstabilityRoot+suffix][method];assert.equal(op['x-required-role'],'DM');for(const response of Object.values(op.responses))assert.equal(response.headers['Cache-Control'].schema.const,'no-store, private');if(method==='post')assert.ok(op.parameters.some(p=>p.$ref==='#/components/parameters/CsrfHeader'));}
validate(schemas.InitialInstabilityHealingEffect,{...instabilityHealing,profile:'5R-SRD-5.2.1-INITIAL-INSTABILITY-HEALING-V1'});
console.log(`Contract checks passed: ${actualOperations.length} HTTP operations; A02 independent customizations, typed v2 templates and campaign instances. Previous operations retained; focused checker, not a general OpenAPI validator.`);
