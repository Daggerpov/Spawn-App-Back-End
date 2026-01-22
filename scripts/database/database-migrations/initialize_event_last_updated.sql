-- Add the last_updated column if it doesn't exist
ALTER TABLE Activity
ADD COLUMN IF NOT EXISTS last_updated TIMESTAMP;
-- Update all existing Activities' last_updated to current timestamp
UPDATE Activity
SET last_updated = CURRENT_TIMESTAMP
WHERE last_updated IS NULL;
-- For future records, ensure the column is NOT NULL
ALTER TABLE Activity
MODIFY COLUMN last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;