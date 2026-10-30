CREATE TABLE campaign_initiative (
 campaign_id UUID PRIMARY KEY REFERENCES campaign(id) ON DELETE CASCADE,
 data JSONB NOT NULL CHECK(octet_length(data::text)<=1048576)
);
CREATE TABLE initiative_history (
 campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 version BIGINT NOT NULL CHECK(version>0),
 data JSONB NOT NULL CHECK(octet_length(data::text)<=1048576), PRIMARY KEY(campaign_id,version)
);
CREATE TABLE initiative_command_receipt (
 actor VARCHAR(80) NOT NULL REFERENCES app_user(username), request_id UUID NOT NULL,
 campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
 fingerprint CHAR(64) NOT NULL, result_version BIGINT NOT NULL,
 PRIMARY KEY(actor,request_id)
);
