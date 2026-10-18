ALTER TABLE scene DROP CONSTRAINT scene_campaign_id_key;
ALTER TABLE scene ADD CONSTRAINT scene_campaign_id_id_key UNIQUE (campaign_id,id);
CREATE INDEX scene_campaign_idx ON scene(campaign_id);
ALTER TABLE campaign ADD COLUMN current_scene_id UUID;
UPDATE campaign c SET current_scene_id=s.id FROM scene s WHERE s.campaign_id=c.id;
-- Nullable during initial campaign creation; all application creation paths set it
-- in the same transaction. A nonnull pointer can only reference its own campaign.
ALTER TABLE campaign ADD CONSTRAINT campaign_current_scene_fk
    FOREIGN KEY (id,current_scene_id) REFERENCES scene(campaign_id,id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE scene_command_receipt (
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('CREATE','ACTIVATE')),
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    target UUID,
    name VARCHAR(200) NOT NULL,
    expected_version BIGINT NOT NULL CHECK (expected_version >= 0),
    result_id UUID NOT NULL,
    result_name VARCHAR(200) NOT NULL,
    result_version BIGINT NOT NULL CHECK (result_version >= 0),
    PRIMARY KEY (actor,request_id)
);
