create table module_customization (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    source_package_id uuid not null,
    source_sha256 char(64) not null,
    name varchar(100) not null,
    version bigint not null default 0 check(version>=0),
    baseline jsonb not null check(octet_length(baseline::text)<=2097152),
    document jsonb not null check(octet_length(document::text)<=2097152),
    created_at timestamptz not null default current_timestamp,
    unique(campaign_id,id),
    foreign key(campaign_id,source_package_id) references module_package(campaign_id,id)
);
create table module_instance (
    campaign_id uuid primary key references campaign(id) on delete cascade,
    version bigint not null default 0 check(version>=0),
    source jsonb not null,
    document jsonb not null check(octet_length(document::text)<=2097152),
    mapping jsonb not null,
    progress jsonb not null default '{}' check(octet_length(progress::text)<=2097152)
);
create table module_lifecycle_receipt (
    actor varchar(80) not null references app_user(username),
    request_id uuid not null,
    campaign_id uuid not null references campaign(id) on delete cascade,
    fingerprint char(64) not null,
    result_id uuid not null,
    result_version bigint not null check(result_version>=0),
    result_kind varchar(16) not null check(result_kind in ('COPY','EDIT','TEMPLATE','CAMPAIGN','PROGRESS')),
    primary key(actor,request_id)
);
