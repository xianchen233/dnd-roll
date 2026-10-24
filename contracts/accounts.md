# P01: Accounts, campaigns, invitations and members

Status: implementation contract, 2026-10-18. Extends contract 0.1 in README.md.
Baseline CI c57c35a passed before this work began. All JSON uses camelCase.
Existing scene, login and movement endpoints remain compatible.

## Account lifecycle

- Fixed accounts persist across sessions. No public unrestricted registration.
- Optional `BOOTSTRAP_USERNAME` + `BOOTSTRAP_PASSWORD` initialize one initial account
  without the demo profile. Supply both or neither; fail fast on partial/invalid
  configuration. Existing accounts/passwords/permissions are never reset.
- This initial account has no global access to other people's campaigns. Any
  authenticated account may create its own campaign and becomes its DM.
- New accounts can be registered only while accepting an active invitation.
  New usernames match `[a-z][a-z0-9_-]{2,31}`. Passwords are at least 12 characters
  and at most 72 UTF-8 bytes, and must not be silently trimmed or truncated.
  Existing demo accounts remain login-compatible.
- Bootstrap is an operator setup operation, not a public HTTP endpoint.

## Shared command behavior

Every POST uses the current session CSRF token, including anonymous invitation
preview and registration. Actor identity comes only from the authenticated session.
Non-DM campaign management returns 403; identifiers scoped to another campaign
return 404. Recheck authorization before replaying receipts.

Campaign membership/invitation edits serialize with the existing campaign row lock.
Add an independent `campaign.version`, separate from scene/token movement versions.
Member/invitation list responses include `campaignVersion`. Changing a membership or
invitation increments it once; no-op/replayed commands do not. Stale management
commands return 409 `VERSION_CONFLICT`, and the UI refreshes before the user retries.

Creation commands carry a UUID `requestId`; reuse with a different payload is 409
`IDEMPOTENCY_CONFLICT`. Removal/revocation commands do too. Store mutation receipts
durably. Secret invite tokens are the deliberate response exception below: retries
must not create another invitation, but the plaintext token is returned only once.

## Endpoints

### POST /api/campaigns

Authenticated. Body `{requestId,name}`; trim name, require 1–100 characters.
Return 201 `{id,name,role:"DM"}`; an identical creation retry returns the same
campaign (200 or 201). A different name with the same requestId conflicts.
Serialize creation by actor so concurrent retries cannot create two campaigns.
Create one empty 24×16 scene, gridSize 48, in the same transaction. New campaigns
contain no demo units or members. Scene authoring is P02.

Existing `GET /api/campaigns` stays the same membership-filtered array.

### GET /api/campaigns/{campaignId}/members

Any current member. Return
`{campaignVersion,members:[{username,role:"DM"|"PLAYER"}]}`. Never expose password
hashes, account identifiers outside the campaign or a global user directory.

### POST /api/campaigns/{campaignId}/invitations

DM only. Body `{requestId,expectedVersion}`. Return 201
`{id,token,expiresAt,campaignVersion}`. The server generates 32 cryptographically
random bytes as unpadded Base64URL, stores only SHA-256(token), and expires the invite
after seven days. `expiresAt` is an ISO-8601 UTC instant. Use injectable Clock.

An identical retry returns 200 with the same invitation metadata and `token:null`;
never regenerate a secret or re-create a revoked/consumed invite. The UI explains
that a lost one-time link can be revoked and replaced. Do not persist plaintext
tokens in receipts, logs, list responses or errors.

### GET /api/campaigns/{campaignId}/invitations

DM only. Return
`{campaignVersion,invitations:[{id,createdBy,expiresAt,status,usedBy}]}`.
status is `ACTIVE|EXPIRED|REVOKED|USED`; usedBy is username or null. No token/hash.

### POST /api/campaigns/{campaignId}/invitations/{invitationId}/revoke

DM only. Body `{requestId,expectedVersion}`. Return 204. Scope invite to campaign.
An already revoked/used invite is a no-op; repeated original requests never revoke
a different invite or increment the version again. Revocation never removes an
already joined member; membership removal is explicit below.

### POST /api/invitations/preview

Anonymous allowed, CSRF required. Body `{token}`. Return 200
`{campaignId,campaignName,expiresAt}` for an active invite, 404 `INVITE_NOT_FOUND`,
410 `INVITE_EXPIRED`, or 409 `INVITE_UNAVAILABLE` for revoked/used invites. No unit,
member-list or private campaign data. Invalid token format is 400 `INVALID_INPUT`.

Use the POST body for the token; no token in backend URL paths or access logs.
Frontend share URL is `${location.origin}/#invite=TOKEN`, so fragments are not sent
to the server. Clear the fragment after success and never log it.

### POST /api/invitations/accept

Authenticated existing account, CSRF required. Body `{token}`. Return 200
`{campaignId}`. Join as PLAYER only; never trust a client role. Claim the invitation
and add membership atomically. An already-member DM keeps DM status. Same actor
replaying an already consumed invite while still a member returns the same result;
after membership removal, replay must fail and must not recreate access.
Other actors cannot claim a used invite; expired/revoked invites cannot add members.
Concurrent acceptance has one winner. Keep existing-member handling explicit and
do not downgrade roles.

### POST /api/invitations/register

Anonymous allowed, CSRF required. Body `{token,username,password}`. Return 201
`{username,campaignId}`. Validate the active invitation and credentials, create the
account, claim the invite and join as PLAYER in one transaction. Existing username
is 409 `USERNAME_TAKEN`; never overwrite its password or partially consume the
invite on failure. Authenticated users should use accept (400 if they use register).
The client explicitly logs in afterward using the existing login flow, then clears
the invite fragment and loads campaigns. Registration itself does not create a
partially authenticated session. A lost registration response can be recovered by
normal login and same-actor accept; it must not create a second account.

### POST /api/campaigns/{campaignId}/members/{username}/remove

DM only. Body `{requestId,expectedVersion}`. Return 204. Remove PLAYER membership
and every token control grant for that account within this campaign, atomically.
Do not touch another campaign's grants, the account itself, or game history.
P02c additionally advances each affected token's version and each affected scene's
version once when deleting controls. This invalidates stale token authoring forms
after a member rejoins; no-op/removal receipt replay advances neither version.
Removing a DM is 409 `DM_REMOVAL_DENIED`; DM promotion/transfer is outside P01.
Already-absent members are a no-op. Receipt replay cannot remove a membership that
was later newly granted. Reusing requestId for another target conflicts.

After commit, close the removed user's existing sockets (other campaigns may
reconnect normally). HTTP always rechecks membership; a session remains a valid
account login but no longer authorizes this campaign. Publish only safe invalidation
events. The client clears its map on 401/403 instead of keeping a playable stale
snapshot, and provides a campaign refresh action.

## Required evidence

- Pure policy/application tests RED → GREEN for DM-only management, expiry,
  single-use claim, strict credentials, replay and version checks.
- Real PostgreSQL/HTTP tests: bootstrap without demo; create campaign idempotently;
  anonymous preview/register with CSRF; existing-account join; invalid/expired/
  revoked/used invite rejection; parallel acceptance; username conflict rollback;
  member removal and cross-campaign grant isolation; removed member cannot replay
  an accepted invite or move receipt to regain access; restart does not restore it.
- Real WS test: removed member socket closes, and further scene reads fail.
- Frontend tests before implementation: invite signup/login/accept, one-time link,
  DM management visibility, conflict refresh, and clearing forbidden scene data.
- Real browser test using the existing demo identities as setup: DM creates a new
  campaign, shares a fragment invite, a fresh context creates a fixed account,
  joins, DM sees/removes the member, and revoked access disappears.
- Keep CHANGELOG, work-packages, verification and continuation accurate. P01 does
  not imply P02 map authoring or the entire M2 product has been delivered.
