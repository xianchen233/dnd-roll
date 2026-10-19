-- Absence of rows is fully covered, including all existing maps. V1-V5 stay untouched.
CREATE TABLE scene_revealed_cell (
    campaign_id UUID NOT NULL,
    scene_id UUID NOT NULL,
    x INTEGER NOT NULL CHECK (x>=0),
    y INTEGER NOT NULL CHECK (y>=0),
    PRIMARY KEY(scene_id,x,y),
    FOREIGN KEY(campaign_id,scene_id) REFERENCES scene(campaign_id,id) ON DELETE CASCADE
);
CREATE TABLE fog_command_receipt (
    actor VARCHAR(80) NOT NULL REFERENCES app_user(username),
    request_id UUID NOT NULL,
    campaign_id UUID NOT NULL REFERENCES campaign(id) ON DELETE CASCADE,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(actor,request_id)
);
