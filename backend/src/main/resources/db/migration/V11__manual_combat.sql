create table combat_action (
    id uuid primary key,
    seq bigint generated always as identity unique,
    campaign_id uuid not null references campaign(id) on delete cascade,
    character_id uuid not null,
    target_id uuid not null,
    visibility varchar(6) not null check (visibility in ('PUBLIC','DM')),
    data jsonb not null,
    foreign key(campaign_id,character_id) references character_card(campaign_id,id),
    foreign key(campaign_id,target_id) references character_card(campaign_id,id)
);
create index combat_action_campaign_page on combat_action(campaign_id,seq desc);
create table combat_command_receipt (
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint varchar(64) not null,
    result_id uuid not null,
    result_version bigint not null check(result_version>=0),
    primary key(actor,request_id)
);
