create table module_operation (
    campaign_id uuid not null references module_instance(campaign_id) on delete cascade,
    id uuid not null,
    definition jsonb not null,
    primary key(campaign_id,id)
);
create table module_operation_receipt (
    campaign_id uuid not null references module_instance(campaign_id) on delete cascade,
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    fingerprint varchar(64) not null,
    result jsonb not null,
    primary key(actor,request_id)
);
create table module_operation_execution (
    campaign_id uuid not null,
    operation_id uuid not null,
    id uuid primary key,
    at timestamptz not null,
    evidence jsonb not null,
    foreign key(campaign_id,operation_id) references module_operation(campaign_id,id) on delete cascade
);
