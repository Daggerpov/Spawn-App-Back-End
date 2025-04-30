-- Add created_at column to friend_request table if it doesn't exist
ALTER TABLE friend_request
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
-- For existing rows, set created_at to current timestamp
UPDATE friend_request
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;