ALTER TABLE campaign ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0);
CREATE TABLE campaign_invitation (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    token_hash CHAR(64) UNIQUE NOT NULL,
    created_by VARCHAR(80) NOT NULL REFERENCES app_user(username),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    used_by VARCHAR(80) REFERENCES app_user(username),
    CHECK (NOT (revoked AND used_by IS NOT NULL))
);
CREATE INDEX campaign_invitation_campaign_idx ON campaign_invitation(campaign_id);
-- Deliberately contains no plaintext invitation token, nor password credentials.
CREATE TABLE account_command_receipt (
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('CREATE','INVITE','REVOKE','REMOVE')),
    target VARCHAR(180) NOT NULL,
    expected_version BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    result_id UUID NOT NULL,
    expires_at TIMESTAMPTZ,
    result_version BIGINT NOT NULL,
    PRIMARY KEY (actor, request_id)
);
