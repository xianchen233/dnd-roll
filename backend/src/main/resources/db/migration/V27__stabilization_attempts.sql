create table stabilization_state (
 campaign_id uuid not null references campaign(id) on delete cascade,
 target_id uuid not null references character_card(id) on delete cascade,
 version bigint not null check(version>0),
 snapshot jsonb not null check(octet_length(snapshot::text)<=8388608),
 primary key(campaign_id,target_id)
);
create table stabilization_history (
 campaign_id uuid not null, target_id uuid not null, version bigint not null check(version>0),
 snapshot jsonb not null check(octet_length(snapshot::text)<=8388608),
 primary key(campaign_id,target_id,version),
 foreign key(campaign_id,target_id) references stabilization_state(campaign_id,target_id) on delete cascade
);
create table stabilization_receipt (
 actor varchar(80) not null references app_user(username),request_id uuid not null,
 campaign_id uuid not null references campaign(id) on delete cascade,fingerprint varchar(64) not null,
 result_id uuid not null references character_card(id) on delete cascade,result_version bigint not null check(result_version>0),
 primary key(actor,request_id)
);
