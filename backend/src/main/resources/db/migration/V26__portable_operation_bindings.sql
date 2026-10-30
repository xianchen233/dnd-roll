create table module_operation_binding (
    campaign_id uuid not null references module_instance(campaign_id) on delete cascade,
    node_id uuid not null,
    operation_id uuid not null,
    data jsonb not null,
    primary key(campaign_id,node_id),
    unique(campaign_id,operation_id)
);
create table module_operation_binding_history (
    campaign_id uuid not null,
    node_id uuid not null,
    version bigint not null check(version>=1),
    evidence jsonb not null,
    primary key(campaign_id,node_id,version),
    foreign key(campaign_id,node_id) references module_operation_binding(campaign_id,node_id) on delete cascade
);
create table module_operation_binding_receipt (
    campaign_id uuid not null,
    node_id uuid not null,
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    fingerprint char(64) not null,
    result jsonb not null,
    primary key(actor,request_id),
    foreign key(campaign_id,node_id) references module_operation_binding(campaign_id,node_id) on delete cascade
);
