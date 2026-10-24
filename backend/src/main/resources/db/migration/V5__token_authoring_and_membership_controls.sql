-- Extend ownership keys without rewriting or deleting existing valid controls.
ALTER TABLE token ADD COLUMN campaign_id UUID;
UPDATE token t SET campaign_id=s.campaign_id FROM scene s WHERE s.id=t.scene_id;
ALTER TABLE token ALTER COLUMN campaign_id SET NOT NULL;
ALTER TABLE token ADD CONSTRAINT token_id_campaign_unique UNIQUE(id,campaign_id);
ALTER TABLE token ADD CONSTRAINT token_scene_campaign_fk FOREIGN KEY(campaign_id,scene_id) REFERENCES scene(campaign_id,id);
ALTER TABLE token_controller ADD COLUMN campaign_id UUID;
UPDATE token_controller tc SET campaign_id=t.campaign_id FROM token t WHERE t.id=tc.token_id;
ALTER TABLE token_controller ALTER COLUMN campaign_id SET NOT NULL;
ALTER TABLE token_controller ADD CONSTRAINT controller_token_campaign_fk FOREIGN KEY(token_id,campaign_id) REFERENCES token(id,campaign_id);
ALTER TABLE token_controller ADD CONSTRAINT controller_member_campaign_fk FOREIGN KEY(campaign_id,username) REFERENCES campaign_member(campaign_id,username);
-- Older integrations may omit the new column. Explicit ownership is never overwritten.
CREATE FUNCTION fill_token_campaign() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.campaign_id IS NULL THEN SELECT campaign_id INTO NEW.campaign_id FROM scene WHERE id=NEW.scene_id; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER token_campaign_default BEFORE INSERT ON token FOR EACH ROW EXECUTE FUNCTION fill_token_campaign();
CREATE FUNCTION fill_controller_campaign() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.campaign_id IS NULL THEN SELECT campaign_id INTO NEW.campaign_id FROM token WHERE id=NEW.token_id; END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER controller_campaign_default BEFORE INSERT ON token_controller FOR EACH ROW EXECUTE FUNCTION fill_controller_campaign();
CREATE TABLE token_command_receipt (
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(actor,request_id)
);
