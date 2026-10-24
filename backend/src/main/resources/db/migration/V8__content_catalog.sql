CREATE TABLE campaign_content_settings (
 campaign_id UUID PRIMARY KEY REFERENCES campaign(id) ON DELETE CASCADE,
 mode VARCHAR(10) NOT NULL CHECK(mode IN ('REVIEW','FREE')),
 approved_ids JSONB NOT NULL CHECK(jsonb_typeof(approved_ids)='array' AND jsonb_array_length(approved_ids)<=500)
);
CREATE TABLE campaign_content_entry (
 id UUID PRIMARY KEY, campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 data JSONB NOT NULL CHECK(octet_length(data::text)<=65536),
 UNIQUE(campaign_id,id)
);
CREATE INDEX campaign_content_entry_campaign ON campaign_content_entry(campaign_id);
CREATE TABLE content_command_receipt (
 actor VARCHAR(80) NOT NULL REFERENCES app_user(username), request_id UUID NOT NULL,
 campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 fingerprint CHAR(64) NOT NULL, result_id UUID NOT NULL, result_version BIGINT NOT NULL,
 PRIMARY KEY(actor,request_id)
);
