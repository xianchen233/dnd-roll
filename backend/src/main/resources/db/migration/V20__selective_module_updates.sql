alter table module_lifecycle_receipt drop constraint module_lifecycle_receipt_result_kind_check;
alter table module_lifecycle_receipt add constraint module_lifecycle_receipt_result_kind_check
    check(result_kind in ('COPY','EDIT','TEMPLATE','CAMPAIGN','PROGRESS','UPDATE'));
create table module_customization_archive (
    campaign_id uuid not null,
    customization_id uuid not null,
    bytes bytea not null check(octet_length(bytes) between 1 and 10485760),
    primary key(campaign_id,customization_id),
    foreign key(campaign_id,customization_id) references module_customization(campaign_id,id) on delete cascade
);
create table module_customization_update (
    campaign_id uuid not null,
    customization_id uuid not null,
    version bigint not null check(version>0),
    evidence jsonb not null,
    before_archive bytea not null check(octet_length(before_archive) between 1 and 10485760),
    created_at timestamptz not null default current_timestamp,
    primary key(campaign_id,customization_id,version),
    foreign key(campaign_id,customization_id) references module_customization(campaign_id,id) on delete cascade
);
