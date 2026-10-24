# A03e: Chinese module review and manual controls

The existing A03a comparison remains read-only. A separate selective-update
panel reads A03b options, shows baseline/current/incoming and conflict status,
and requires an explicit KEEP or INCOMING for every path. There is no default
choice, including local-only edits. More than 500 changes block review. Missing exact
dependencies in the assembled result block application; candidate-only missing dependencies remain advisory because KEEP can exclude them. Any choice, candidate, saved version or
realtime revision change invalidates the reviewed impact. Unsaved customization
drafts must be saved or explicitly reloaded before selection/application.

The current target instance has separate preparation-text and structural review
controls. Each reads the current original source customization version under
its original campaign identity, then requests a server review pinning both
versions. All blockers remain visible. Structural application requires checking
every exact returned confirmation individually; no truncated list or inferred
confirmation is sent. The runtime snapshot and independent DM-only recovery
semantics are shown, with immutable evidence and the recovery campaign entry.
Neither path automatically adopts future source versions or rewrites play.

The live-operation editor uses five existing discriminated step shapes, named
current-campaign targets and explicit ordering. Token and card hiding are separate.
Only unattached DM prose can be copied into a TEAM note. Preparation configures
individual participants, kinds, groups, surprise and reasoned advantage/disadvantage
sources; it leaves a DRAFT candidate. START binds the exact confirmed initiative
and idle combat versions. One encounter step per combination, no duplicate target,
1..20 steps, 100 definitions and 64 KiB complete save bounds are retained.
Refreshing targets does not silently rebase a saved definition or unsaved draft;
adopting current target versions is an explicit editing action requiring save.
Editing, saving, importing and reading an impact never execute a combination.
Triggering requires a new authoritative impact of the saved version.

Every mutating UI action captures its route, domain, request UUID, complete body,
versions and hash before sending. Transport/5xx uncertainty locks editing and
keeps exactly that request across reads/revisions for retry. Confirmation clears
the pending request before refreshing; a failed read is not a failed write and
must not offer another mutation retry. Known rejection requires another review.
Concurrent clicks cannot issue another command while the first is outstanding.

All private data, drafts, impacts, confirmations, evidence, download Blob URLs
and pending commands are cleared on campaign/copy/API lifetime changes or
current-domain 401/403. Each parallel request observes authorization separately,
even after an earlier request failed or its successful result became obsolete.
Old-domain success/denial cannot restore or revoke a newly selected domain.
Recovered ZIPs use authenticated no-store reads and revoke URLs on invalidation.
Player views do not mount these controls or fetch their private endpoints.

Wire contracts, old routes and migrations remain unchanged. Actual TDD and
acceptance evidence are recorded in `docs/a03e-tdd.md`; pending checks are not
passing. Cloud, physical cross-host recovery and real phones remain deferred.
Continue the existing branch and draft PR #1 without merging or publishing.
