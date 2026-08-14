CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_preferences (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    investor_type  VARCHAR(20) NOT NULL CHECK (investor_type IN ('HODLER','DAY_TRADER','NFT_COLLECTOR')),
    interests      JSONB NOT NULL,      -- e.g. ["bitcoin","ethereum"]
    content_types  JSONB NOT NULL,      -- e.g. ["MARKET_NEWS","FUN"]
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE daily_content (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content_type  VARCHAR(20) NOT NULL CHECK (content_type IN ('AI_INSIGHT','MEME')),
    content_date  DATE NOT NULL,
    payload       JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, content_type, content_date)
);

CREATE TABLE feedback (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_type   VARCHAR(20) NOT NULL CHECK (item_type IN ('NEWS','AI_INSIGHT','MEME')),
    item_ref    VARCHAR(255) NOT NULL,
    vote        SMALLINT NOT NULL CHECK (vote IN (-1, 1)),
    item_date   DATE NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, item_type, item_ref)
);

CREATE INDEX idx_daily_content_lookup ON daily_content (user_id, content_type, content_date);
CREATE INDEX idx_feedback_user ON feedback (user_id);
