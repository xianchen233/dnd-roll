create table rp_attachment (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    actor varchar(80) not null references app_user(username),
    storage_key uuid not null unique,
    sha256 char(64) not null,
    data jsonb not null,
    unique(campaign_id,id)
);
create table rp_attachment_reference (
    campaign_id uuid not null,
    message_id uuid not null,
    version bigint not null,
    attachment_id uuid not null,
    primary key(message_id,version,attachment_id),
    foreign key(campaign_id,message_id) references rp_message(campaign_id,id) on delete cascade,
    foreign key(message_id,version) references rp_message_history(message_id,version) on delete cascade,
    foreign key(campaign_id,attachment_id) references rp_attachment(campaign_id,id)
);
create index rp_attachment_read on rp_attachment_reference(attachment_id,message_id);
create table timeline_session (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    name varchar(100) not null,
    archived boolean not null default false,
    unique(campaign_id,id)
);
create unique index timeline_one_active_session on timeline_session(campaign_id) where not archived;
alter table timeline_entry add foreign key(campaign_id,session_id) references timeline_session(campaign_id,id);
create function timeline_capture_session() returns trigger language plpgsql as $$
begin
    new.session_id := (select id from timeline_session where campaign_id=new.campaign_id and not archived);
    return new;
end $$;
create trigger timeline_session_capture before insert on timeline_entry for each row execute function timeline_capture_session();

-- Existing immutable source rows are retained, and projected through their own services.
insert into timeline_entry(id,campaign_id,kind,roll_id,action_id,at)
select id,campaign_id,kind,roll_id,action_id,at from (
    select id,campaign_id,'DICE' as kind,id as roll_id,null::uuid as action_id,coalesce((data->>'at')::timestamptz,'1970-01-01T00:00:00Z'::timestamptz) as at from dice_roll
    union all
    select id,campaign_id,'ACTION',null::uuid,id,coalesce((data->>'at')::timestamptz,'1970-01-01T00:00:00Z'::timestamptz) from combat_action
) sources order by at,id;
create function timeline_capture_source() returns trigger language plpgsql as $$
begin
    if TG_TABLE_NAME='dice_roll' then
        insert into timeline_entry(id,campaign_id,kind,roll_id,at) values (new.id,new.campaign_id,'DICE',new.id,(new.data->>'at')::timestamptz);
    else
        insert into timeline_entry(id,campaign_id,kind,action_id,at) values (new.id,new.campaign_id,'ACTION',new.id,(new.data->>'at')::timestamptz);
    end if;
    return new;
end $$;
create trigger timeline_dice after insert on dice_roll for each row execute function timeline_capture_source();
create trigger timeline_action after insert on combat_action for each row execute function timeline_capture_source();
