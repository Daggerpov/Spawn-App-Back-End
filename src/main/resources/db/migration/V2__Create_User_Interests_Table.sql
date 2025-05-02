CREATE TABLE IF NOT EXISTS user_interests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    interest VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);