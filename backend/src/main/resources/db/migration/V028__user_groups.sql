-- Household-scoped membership groups. system=true rows are dynamically evaluated
-- (Everyone = all active users, Adults = is_child=false, Kids = is_child=true)
-- and cannot be edited or deleted. Custom groups carry an explicit member list.
CREATE TABLE user_groups (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80)  NOT NULL UNIQUE,
    kind        VARCHAR(32)  NOT NULL DEFAULT 'CUSTOM',
    is_system   BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE user_group_members (
    group_id BIGINT NOT NULL REFERENCES user_groups(id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);

CREATE INDEX idx_user_group_members_user ON user_group_members(user_id);

-- Seed the three derived groups. Their member lists stay empty in the table —
-- the service computes them on demand from users.is_child / is_active.
INSERT INTO user_groups (name, kind, is_system) VALUES
    ('Everyone',  'SYSTEM_EVERYONE', true),
    ('Adults',    'SYSTEM_ADULTS',   true),
    ('Kids',      'SYSTEM_KIDS',     true);
