create table rp_message (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    character_id uuid,
    kind varchar(6) not null check(kind in ('RP','OOC')),
    data jsonb not null,
    unique(campaign_id,id),
    foreign key(campaign_id,character_id) references character_card(campaign_id,id) on delete cascade
);
create table timeline_entry (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    ordinal bigint generated always as identity unique,
    kind varchar(6) not null check(kind in ('RP','OOC','DICE','ACTION')),
    message_id uuid references rp_message(id) on delete cascade,
    roll_id uuid references dice_roll(id) on delete cascade,
    action_id uuid references combat_action(id) on delete cascade,
    at timestamptz not null,
    session_id uuid,
    check(num_nonnulls(message_id,roll_id,action_id)=1),
    check((kind in ('RP','OOC'))=(message_id is not null)),
    check((kind='DICE')=(roll_id is not null)),
    check((kind='ACTION')=(action_id is not null)),
    unique(campaign_id,id)
);
create index timeline_campaign_page on timeline_entry(campaign_id,ordinal desc);
create table rp_message_history (
    message_id uuid not null references rp_message(id) on delete cascade,
    version bigint not null check(version>=0),
    data jsonb not null,
    primary key(message_id,version)
);
create table timeline_command_receipt (
    namespace varchar(24) not null,
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint char(64) not null,
    result_id uuid not null,
    result_version bigint not null check(result_version>=0),
    primary key(namespace,actor,request_id)
);
