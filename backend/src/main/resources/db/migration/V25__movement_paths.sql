create table movement_budget (
    campaign_id uuid not null references campaign(id) on delete cascade,
    character_id uuid not null,
    data jsonb not null,
    primary key(campaign_id,character_id),
    foreign key(campaign_id,character_id) references character_card(campaign_id,id) on delete cascade
);
create table movement_journal (
    id uuid primary key,
    seq bigint generated always as identity unique,
    campaign_id uuid not null references campaign(id) on delete cascade,
    character_id uuid not null,
    data jsonb not null
);
create index movement_journal_character on movement_journal(campaign_id,character_id,seq desc);
create table movement_path_receipt (
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint varchar(64) not null,
    result jsonb not null,
    primary key(actor,request_id)
);
