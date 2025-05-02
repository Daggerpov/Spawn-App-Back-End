CREATE TABLE IF NOT EXISTS user_social_media (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    whatsapp_number VARCHAR(255),
    instagram_username VARCHAR(255),
    last_updated TIMESTAMP,
    CONSTRAINT fk_social_media_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);