alter table scene add column module_retired boolean not null default false;
alter table token add column module_retired boolean not null default false;
alter table module_lifecycle_receipt drop constraint module_lifecycle_receipt_result_kind_check;
alter table module_lifecycle_receipt add constraint module_lifecycle_receipt_result_kind_check
    check(result_kind in ('COPY','EDIT','TEMPLATE','CAMPAIGN','PROGRESS','UPDATE','LIVE','MIGRATE'));
create table module_instance_migration (
    campaign_id uuid not null references module_instance(campaign_id) on delete cascade,
    version bigint not null check(version>0),
    recovery_campaign_id uuid not null references campaign(id),
    evidence jsonb not null,
    primary key(campaign_id,version)
);
