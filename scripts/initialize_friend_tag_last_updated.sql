ALTER TABLE friend_tag
ADD COLUMN last_modified TIMESTAMP;
-- Update existing records to have a default value
UPDATE friend_tag
SET last_modified = NOW();