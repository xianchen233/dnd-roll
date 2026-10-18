CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    username VARCHAR(80) UNIQUE NOT NULL,
    password_hash VARCHAR(200) NOT NULL
);
CREATE TABLE campaign (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);
CREATE TABLE campaign_member (
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    username VARCHAR(80) NOT NULL REFERENCES app_user(username),
    role VARCHAR(10) NOT NULL CHECK (role IN ('DM', 'PLAYER')),
    PRIMARY KEY (campaign_id, username)
);
CREATE TABLE scene (
    id UUID PRIMARY KEY,
    campaign_id UUID UNIQUE NOT NULL REFERENCES campaign(id),
    name VARCHAR(200) NOT NULL,
    width INTEGER NOT NULL CHECK (width > 0),
    height INTEGER NOT NULL CHECK (height > 0),
    grid_size INTEGER NOT NULL CHECK (grid_size > 0),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE TABLE token (
    id UUID PRIMARY KEY,
    scene_id UUID NOT NULL REFERENCES scene(id),
    name VARCHAR(200) NOT NULL,
    x INTEGER NOT NULL CHECK (x >= 0),
    y INTEGER NOT NULL CHECK (y >= 0),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    hidden BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX token_scene_idx ON token(scene_id);
CREATE TABLE token_controller (
    token_id UUID NOT NULL REFERENCES token(id),
    username VARCHAR(80) NOT NULL REFERENCES app_user(username),
    PRIMARY KEY (token_id, username)
);
CREATE TABLE movement_receipt (
    campaign_id UUID NOT NULL REFERENCES campaign(id),
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    token_id UUID NOT NULL REFERENCES token(id),
    expected_version BIGINT NOT NULL,
    requested_x INTEGER NOT NULL,
    requested_y INTEGER NOT NULL,
    result_name VARCHAR(200) NOT NULL,
    result_x INTEGER NOT NULL,
    result_y INTEGER NOT NULL,
    result_version BIGINT NOT NULL,
    result_can_move BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (campaign_id, actor, request_id)
);
