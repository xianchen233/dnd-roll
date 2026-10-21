# First playable slice: HTTP and realtime contract

Contract version: 0.1. All application paths are same-origin under `/api`. Production uses HTTPS. Identifiers are opaque UUID strings; coordinates are integer grid cells, with origin at the top left. JSON field names use camelCase.

## Authentication

- `GET /api/auth/csrf` → `{ "token": "...", "headerName": "X-CSRF-TOKEN" }`. Obtain a new token after login or logout. Send it on every POST using the returned header name. Requests include session cookies (`credentials: include`).
- `POST /api/auth/login` body `{ "username": "...", "password": "..." }` → `{ "username": "..." }`.
- `GET /api/auth/me` → `{ "username": "..." }`; unauthenticated requests return 401.
- `POST /api/auth/logout` → 204. Invalidates the session.

## Campaign and scene

- `GET /api/campaigns` → array of `{ "id": "...", "name": "...", "role": "DM" | "PLAYER" }`, restricted to memberships.
- `GET /api/campaigns/{campaignId}/scene` → scene snapshot:

```json
{
  "id": "UUID", "campaignId": "UUID", "name": "First scene",
  "width": 24, "height": 16, "gridSize": 48, "version": 0,
  "tokens": [{ "id": "UUID", "name": "Adventurer", "x": 2, "y": 3, "version": 0, "canMove": true }]
}
```

- `POST /api/campaigns/{campaignId}/tokens/{tokenId}/moves`, body `{ "requestId": "UUID", "expectedVersion": 0, "x": 4, "y": 5 }` → updated visible token with `id,name,x,y,version,canMove` fields. Read a new scene snapshot after success.
- A member who cannot view a token receives 404. A visible token without control returns 403. DM may move any token in their campaign. Grid bounds, role, control and membership are enforced by the backend. Exploration remains free; active combat also checks current character control and turn/DM movement grants as specified in [P06](combat.md), before historical receipt replay.
- Repeating the same request ID, actor and payload returns the original response without moving twice, including when the expected version is now stale. Reusing that key with a different payload returns 409. Actor identity comes from the session.
- A stale object version returns 409; the client refreshes the authoritative snapshot and explains the conflict. Invalid coordinates return 400.

## Errors

Application error JSON: `{ "code": "VERSION_CONFLICT", "message": "..." }`. Codes include `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `VERSION_CONFLICT`, `IDEMPOTENCY_CONFLICT`, `INVALID_INPUT`. HTTP status is authoritative; clients tolerate non-JSON transport errors. Never include hidden token data in errors.

## Realtime

- Native WebSocket STOMP endpoint `/ws` (same session); no SockJS requirement. STOMP CONNECT sends the current CSRF header/token.
- Subscribe only to `/user/queue/scene-changes`.
- Events contain only `{ "type": "SCENE_CHANGED", "campaignId": "UUID" }`. They invalidate the cache; the client fetches a permission-filtered snapshot. Hidden token IDs, names, coordinates and counts are never broadcast.
- The server sends invalidations only to campaign members. Reject client SEND and subscriptions to arbitrary destinations. Events are hints, not storage: reconnect and a periodic refresh always fetch current snapshots. UI must display offline/reconnecting/error states and never present a failed move as saved.

## Initial data

Optional paired `BOOTSTRAP_USERNAME` and `BOOTSTRAP_PASSWORD` insert one initial account without granting global access or resetting existing credentials. An opt-in `demo` profile bootstraps one campaign, one DM account, one player account, a player-controlled token and a hidden DM token. Passwords are supplied by environment variables, never committed defaults. This preserves the first two-client acceptance flow.

Account invitations, campaign creation and member management are specified in [P01](accounts.md). P01 uses a separate campaign management version, single-use hashed invitation secrets and durable command receipts. Multiple scenes and the shared current scene are specified in [P02a](scenes.md). Private durable PNG/JPEG assets, authenticated downloads and nullable scene map calibration are specified in [P02b](assets.md). DM token authoring, hidden state, shared control and revocation-safe historical receipts are specified in [P02c](tokens.md). Player-safe derived maps, manual cell visibility and Pixi interaction are specified in [P02d](map-visibility.md). The machine-readable [OpenAPI contract](openapi.json) preserves existing scene snapshots and secret-free realtime events.

The [P02e integrated acceptance contract](p02-acceptance.md) composes these existing
boundaries across both clients, disconnection, revocation and real service restart.

[P03 character sheets](characters.md) add independent long-term character identity,
typed sheets, sources/calculations, histories, resources, conditions, transfer and
map placement references. A token configuration does not constitute a character card.
The synthetic [version 1 example](character.example.json) contains no private data.

[P04 searchable content and explicit fill](content.md) adds licensed selected SRD
data, immutable campaign entries, separate admission and authoritative previews.

`GET /api/campaigns/{campaignId}/content`
`POST /api/campaigns/{campaignId}/content/custom`
`POST /api/campaigns/{campaignId}/content/settings`
`POST /api/campaigns/{campaignId}/characters/{id}/content-preview`
`POST /api/campaigns/{campaignId}/characters/{id}/content`

[P05 dice and 5R initiative](dice-initiative.md) adds server-owned immutable rolls,
DM-only dice, shared command/button behavior and explicit initiative confirmation.

`GET /api/campaigns/{campaignId}/rolls`
`GET /api/campaigns/{campaignId}/rolls/{id}`
`POST /api/campaigns/{campaignId}/rolls`
`POST /api/campaigns/{campaignId}/roll-commands`
`GET /api/campaigns/{campaignId}/initiative`
`POST /api/campaigns/{campaignId}/initiative`
`POST /api/campaigns/{campaignId}/initiative/confirm`
`GET /api/campaigns/{campaignId}/initiative/history`

[P06 manual combat](combat.md) links saved attacks, targets and server rolls,
explicit DM application/corrections, independent character turns and movement grants.

`GET /api/campaigns/{campaignId}/actions`
`POST /api/campaigns/{campaignId}/actions`
`GET /api/campaigns/{campaignId}/actions/{id}`
`POST /api/campaigns/{campaignId}/actions/{id}/events`
`GET /api/campaigns/{campaignId}/combat`
`POST /api/campaigns/{campaignId}/combat`
`GET /api/campaigns/{campaignId}/combat/history`

[P07 timeline](timeline.md) adds durable RP/OOC, current-permission references,
images, immutable edits, unified dice/action search, sessions and record exports.

`GET /api/campaigns/{campaignId}/timeline`
`GET /api/campaigns/{campaignId}/timeline/{id}`
`GET /api/campaigns/{campaignId}/timeline/export`
`GET /api/campaigns/{campaignId}/timeline/sessions`
`POST /api/campaigns/{campaignId}/timeline/sessions`
`POST /api/campaigns/{campaignId}/messages`
`POST /api/campaigns/{campaignId}/messages/{id}`
`GET /api/campaigns/{campaignId}/messages/{id}/history`
`POST /api/campaigns/{campaignId}/attachments`
`GET /api/campaigns/{campaignId}/attachments/{id}/content`

[P08 team notes and DM preparation](notes.md) adds collaborative TEAM notes and
immutable DM-only materials, histories, persistent images and authorized exports.

`GET /api/campaigns/{campaignId}/notes`
`POST /api/campaigns/{campaignId}/notes`
`GET /api/campaigns/{campaignId}/notes/{id}`
`POST /api/campaigns/{campaignId}/notes/{id}`
`GET /api/campaigns/{campaignId}/notes/{id}/history`
`GET /api/campaigns/{campaignId}/notes/export`
`POST /api/campaigns/{campaignId}/note-attachments`
`GET /api/campaigns/{campaignId}/note-attachments/{id}/content`

[P09 deployment backups](backups.md) require separately configured operator authority.

`GET /api/admin/backups`
`POST /api/admin/backups`
`POST /api/admin/backups/migration`
`GET /api/admin/backups/{id}/content`

[P10 integrated acceptance](p10-acceptance.md) composes the first-edition campaign,
phone navigation, restart/recovery and bounded capacity measurements. Deferred
physical cloud deployment/recovery and release gates remain separately tracked.

[A01 module packages](module-packages.md) add an inert current-DM-only template
library, bounded archive validation, exact dependencies and explicit import.

`GET /api/campaigns/{campaignId}/module-packages`
`POST /api/campaigns/{campaignId}/module-packages`
`POST /api/campaigns/{campaignId}/module-packages/preview`
`GET /api/campaigns/{campaignId}/module-packages/{id}/content`

[A02 module lifecycle](module-lifecycle.md) adds immutable editing baselines,
independent editable definitions, save-as v2 templates and new playable campaigns.

`GET /api/campaigns/{campaignId}/module-customizations`
`POST /api/campaigns/{campaignId}/module-customizations`
`GET /api/campaigns/{campaignId}/module-customizations/{id}`
`POST /api/campaigns/{campaignId}/module-customizations/{id}`
`POST /api/campaigns/{campaignId}/module-customizations/{id}/templates`
`POST /api/campaigns/{campaignId}/module-customizations/{id}/campaigns`
`GET /api/campaigns/{campaignId}/module-instance`
`POST /api/campaigns/{campaignId}/module-instance`
