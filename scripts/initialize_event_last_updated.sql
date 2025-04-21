-- Add the last_updated column if it doesn't exist
ALTER TABLE event
ADD COLUMN IF NOT EXISTS last_updated TIMESTAMP;
-- Update all existing events' last_updated to current timestamp
UPDATE event
SET last_updated = CURRENT_TIMESTAMP
WHERE last_updated IS NULL;
-- For future records, ensure the column is NOT NULL
ALTER TABLE event
MODIFY COLUMN last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;