CREATE TABLE campaign_asset (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    storage_key VARCHAR(36) NOT NULL UNIQUE,
    sha256 CHAR(64) NOT NULL,
    media_type VARCHAR(10) NOT NULL CHECK (media_type IN ('image/png','image/jpeg')),
    byte_size BIGINT NOT NULL CHECK (byte_size BETWEEN 1 AND 10485760),
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 8192),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 8192),
    CHECK (width::BIGINT * height <= 32000000),
    UNIQUE (campaign_id,id)
);
CREATE TABLE scene_map (
    campaign_id UUID NOT NULL,
    scene_id UUID PRIMARY KEY,
    asset_id UUID NOT NULL,
    grid_size INTEGER NOT NULL CHECK (grid_size BETWEEN 8 AND 512),
    offset_x INTEGER NOT NULL CHECK (offset_x BETWEEN -8192 AND 8192),
    offset_y INTEGER NOT NULL CHECK (offset_y BETWEEN -8192 AND 8192),
    distance_per_cell DOUBLE PRECISION NOT NULL CHECK (distance_per_cell BETWEEN 0.01 AND 10000),
    FOREIGN KEY (campaign_id,scene_id) REFERENCES scene(campaign_id,id),
    FOREIGN KEY (campaign_id,asset_id) REFERENCES campaign_asset(campaign_id,id)
);
-- Independent globally actor-keyed command namespace; responses contain no storage data.
CREATE TABLE asset_command_receipt (
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    fingerprint TEXT NOT NULL,
    response_json TEXT NOT NULL,
    PRIMARY KEY (actor,request_id)
);
