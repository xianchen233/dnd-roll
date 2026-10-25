create table attack_preview (
    id uuid primary key,
    seq bigint generated always as identity unique,
    campaign_id uuid not null references campaign(id) on delete cascade,
    character_id uuid not null,
    target_id uuid not null,
    published boolean not null default false,
    data jsonb not null,
    foreign key(campaign_id,character_id) references character_card(campaign_id,id),
    foreign key(campaign_id,target_id) references character_card(campaign_id,id)
);
create index attack_preview_campaign_page on attack_preview(campaign_id,seq desc);
create table attack_preview_receipt (
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint varchar(64) not null,
    result jsonb not null,
    primary key(actor,request_id)
);
