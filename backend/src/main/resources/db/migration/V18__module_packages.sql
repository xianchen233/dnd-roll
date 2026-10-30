create table module_package (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    module_id uuid not null,
    module_version varchar(20) not null,
    sha256 char(64) not null check(sha256 ~ '^[a-f0-9]{64}$'),
    manifest jsonb not null check(octet_length(manifest::text)<=1048576),
    archive bytea not null check(octet_length(archive) between 1 and 10485760),
    created_at timestamptz not null default current_timestamp,
    unique(campaign_id,id)
);
create index module_package_identity on module_package(campaign_id,module_id,module_version);
create table module_command_receipt (
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint char(64) not null,
    result_id uuid not null,
    result_version bigint not null check(result_version>=0),
    primary key(actor,request_id),
    foreign key(campaign_id,result_id) references module_package(campaign_id,id) on delete cascade
);
