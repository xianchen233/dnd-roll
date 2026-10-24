CREATE TABLE dice_roll (
 id UUID PRIMARY KEY, campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 character_id UUID, visibility VARCHAR(10) NOT NULL CHECK(visibility IN ('PUBLIC','DM')),
 ordinal BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
 data JSONB NOT NULL CHECK(octet_length(data::text)<=65536), UNIQUE(campaign_id,id),
 FOREIGN KEY(campaign_id,character_id) REFERENCES character_card(campaign_id,id)
);
CREATE INDEX dice_roll_campaign_page ON dice_roll(campaign_id,ordinal DESC);
CREATE TABLE dice_command_receipt (
 actor VARCHAR(80) NOT NULL REFERENCES app_user(username), request_id UUID NOT NULL,
 campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 fingerprint CHAR(64) NOT NULL, result_id UUID NOT NULL,
 PRIMARY KEY(actor,request_id),
 FOREIGN KEY(campaign_id,result_id) REFERENCES dice_roll(campaign_id,id) ON DELETE CASCADE
);
