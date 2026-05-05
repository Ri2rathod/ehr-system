CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN DEFAULT FALSE,
    revoked_at  TIMESTAMP,
    user_agent  VARCHAR(255),
    ip_address  VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_rt_user_id   ON refresh_tokens(user_id);
CREATE INDEX idx_rt_token     ON refresh_tokens(token_hash);
CREATE INDEX idx_rt_revoked   ON refresh_tokens(revoked);