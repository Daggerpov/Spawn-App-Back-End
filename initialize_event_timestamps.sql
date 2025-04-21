-- Add the created_timestamp and updated_timestamp columns if they don't exist
ALTER TABLE event
ADD COLUMN IF NOT EXISTS created_timestamp TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_timestamp TIMESTAMP;
-- Update all existing events' timestamps to current timestamp
UPDATE event
SET created_timestamp = CURRENT_TIMESTAMP,
    updated_timestamp = CURRENT_TIMESTAMP
WHERE created_timestamp IS NULL
    OR updated_timestamp IS NULL;
-- For future records, ensure the columns are NOT NULL
ALTER TABLE event
MODIFY COLUMN created_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    MODIFY COLUMN updated_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;