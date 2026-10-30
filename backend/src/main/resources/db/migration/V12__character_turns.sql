create table campaign_combat (
    campaign_id uuid primary key references campaign(id) on delete cascade,
    data jsonb not null
);
create table combat_turn_history (
    campaign_id uuid not null references campaign(id) on delete cascade,
    version bigint not null check(version>0),
    data jsonb not null,
    primary key(campaign_id,version)
);
