CREATE TABLE character_card (
 id UUID PRIMARY KEY, campaign_id UUID NOT NULL REFERENCES campaign(id),
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0), hidden BOOLEAN NOT NULL DEFAULT FALSE,
 share_full BOOLEAN NOT NULL DEFAULT FALSE, import_review BOOLEAN NOT NULL DEFAULT FALSE,
 sheet JSONB NOT NULL CHECK(jsonb_typeof(sheet)='object' AND octet_length(sheet::text)<=2097152),
 UNIQUE(campaign_id,id)
);
CREATE TABLE character_controller (
 campaign_id UUID NOT NULL, character_id UUID NOT NULL, username VARCHAR(80) NOT NULL,
 PRIMARY KEY(character_id,username),
 FOREIGN KEY(campaign_id,character_id) REFERENCES character_card(campaign_id,id) ON DELETE CASCADE,
 FOREIGN KEY(campaign_id,username) REFERENCES campaign_member(campaign_id,username)
);
CREATE TABLE character_history (
 character_id UUID NOT NULL REFERENCES character_card(id) ON DELETE CASCADE,
 version BIGINT NOT NULL, actor VARCHAR(100) NOT NULL, kind VARCHAR(40) NOT NULL,
 reason TEXT NOT NULL, at TIMESTAMPTZ NOT NULL, before_state JSONB, after_state JSONB NOT NULL,
 PRIMARY KEY(character_id,version)
);
CREATE TABLE character_command_receipt (
 actor VARCHAR(80) NOT NULL REFERENCES app_user(username), request_id UUID NOT NULL,
 campaign_id UUID NOT NULL REFERENCES campaign(id), fingerprint CHAR(64) NOT NULL,
 result_id UUID NOT NULL, result_version BIGINT NOT NULL, PRIMARY KEY(actor,request_id)
);
ALTER TABLE token ADD COLUMN character_id UUID;
ALTER TABLE token ADD CONSTRAINT token_character_campaign_fk FOREIGN KEY(campaign_id,character_id)
 REFERENCES character_card(campaign_id,id);
CREATE INDEX character_card_campaign ON character_card(campaign_id);
CREATE INDEX character_controller_member ON character_controller(campaign_id,username);
