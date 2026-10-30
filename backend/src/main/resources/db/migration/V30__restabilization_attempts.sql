create table restabilization_state (
 campaign_id uuid not null, target_id uuid not null, version bigint not null check(version>0), origin_version bigint not null,
 snapshot jsonb not null check(octet_length(snapshot::text)<=8388608),
 primary key(campaign_id,target_id),
 foreign key(campaign_id,target_id,origin_version) references stabilization_damage_history(campaign_id,target_id,version) on delete cascade
);
create table restabilization_history (
 campaign_id uuid not null, target_id uuid not null, version bigint not null check(version>0), origin_version bigint not null,
 snapshot jsonb not null check(octet_length(snapshot::text)<=8388608),
 primary key(campaign_id,target_id,version),
 foreign key(campaign_id,target_id) references restabilization_state(campaign_id,target_id) on delete cascade,
 foreign key(campaign_id,target_id,origin_version) references stabilization_damage_history(campaign_id,target_id,version) on delete cascade
);
