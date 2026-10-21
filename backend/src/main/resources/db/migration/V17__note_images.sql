create table note_attachment (
    id uuid primary key,
    campaign_id uuid not null references campaign(id) on delete cascade,
    actor varchar(80) not null references app_user(username),
    visibility varchar(4) not null check(visibility in ('TEAM','DM')),
    storage_key uuid not null unique,
    sha256 char(64) not null,
    data jsonb not null,
    unique(campaign_id,id,visibility)
);
create table note_attachment_reference (
    campaign_id uuid not null,
    note_id uuid not null,
    version bigint not null,
    attachment_id uuid not null,
    visibility varchar(4) not null,
    primary key(note_id,version,attachment_id),
    foreign key(campaign_id,note_id,visibility) references campaign_note(campaign_id,id,visibility) on delete cascade,
    foreign key(note_id,version) references note_history(note_id,version) on delete cascade,
    foreign key(campaign_id,attachment_id,visibility) references note_attachment(campaign_id,id,visibility)
);
create index note_attachment_read on note_attachment_reference(attachment_id,note_id);
