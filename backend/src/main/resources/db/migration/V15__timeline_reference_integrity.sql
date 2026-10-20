alter table combat_action add unique(campaign_id,id);
alter table timeline_entry add foreign key(campaign_id,message_id) references rp_message(campaign_id,id) on delete cascade;
alter table timeline_entry add foreign key(campaign_id,roll_id) references dice_roll(campaign_id,id) on delete cascade;
alter table timeline_entry add foreign key(campaign_id,action_id) references combat_action(campaign_id,id) on delete cascade;
alter table rp_message add constraint rp_message_kind_identity check((kind='OOC')=(character_id is null));
create index timeline_campaign_session_page on timeline_entry(campaign_id,session_id,ordinal desc);
create index timeline_campaign_time on timeline_entry(campaign_id,at);
