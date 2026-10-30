create table stabilization_healing_history (
 campaign_id uuid not null, target_id uuid not null, version bigint not null check(version>0),
 snapshot jsonb not null check(octet_length(snapshot::text)<=8388608),
 primary key(campaign_id,target_id,version),
 foreign key(campaign_id,target_id,version) references stabilization_history(campaign_id,target_id,version) on delete cascade
);
