-- Add the last_updated column if it doesn't exist
ALTER TABLE user
ADD COLUMN IF NOT EXISTS last_updated TIMESTAMP;
-- Update all existing users' last_updated to current timestamp
UPDATE user
SET last_updated = CURRENT_TIMESTAMP
WHERE last_updated IS NULL;
-- For future records, ensure the column is NOT NULL
ALTER TABLE user
MODIFY COLUMN last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;