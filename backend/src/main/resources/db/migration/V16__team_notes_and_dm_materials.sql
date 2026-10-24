create table campaign_note (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    ordinal bigint generated always as identity unique,
    visibility varchar(4) not null check(visibility in ('TEAM','DM')),
    data jsonb not null,
    unique(campaign_id,id),
    unique(campaign_id,id,visibility),
    check(data->>'visibility'=visibility)
);
create index note_campaign_page on campaign_note(campaign_id,ordinal desc);
create table note_history (
    note_id uuid not null references campaign_note(id) on delete cascade,
    version bigint not null check(version>=0),
    data jsonb not null,
    primary key(note_id,version)
);
create table note_command_receipt (
    namespace varchar(24) not null,
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint char(64) not null,
    result_id uuid not null,
    result_version bigint not null check(result_version>=0),
    primary key(namespace,actor,request_id)
);
