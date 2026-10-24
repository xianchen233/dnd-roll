alter table module_lifecycle_receipt drop constraint module_lifecycle_receipt_result_kind_check;
alter table module_lifecycle_receipt add constraint module_lifecycle_receipt_result_kind_check
    check(result_kind in ('COPY','EDIT','TEMPLATE','CAMPAIGN','PROGRESS','UPDATE','LIVE'));
create table module_instance_update (
    campaign_id uuid not null references module_instance(campaign_id) on delete cascade,
    version bigint not null check(version>0),
    evidence jsonb not null,
    created_at timestamptz not null default current_timestamp,
    primary key(campaign_id,version)
);
