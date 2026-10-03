-- V10: one-to-one chat (Step 11).

CREATE TABLE conversations (
    id               BIGSERIAL    PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_message_at  TIMESTAMPTZ
);

CREATE TABLE conversation_members (
    conversation_id  BIGINT       NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    last_read_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (conversation_id, user_id)
);
CREATE INDEX idx_conversation_members_user ON conversation_members (user_id);

CREATE TABLE messages (
    id               BIGSERIAL     PRIMARY KEY,
    conversation_id  BIGINT        NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body             VARCHAR(2000) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, id DESC);
