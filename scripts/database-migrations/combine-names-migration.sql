-- Add the new name column
ALTER TABLE user
ADD COLUMN name VARCHAR(255);
-- Update the new column with the combined values
UPDATE user
SET name = CASE
        WHEN first_name IS NOT NULL
        AND last_name IS NOT NULL THEN CONCAT(first_name, ' ', last_name)
        WHEN first_name IS NOT NULL THEN first_name
        WHEN last_name IS NOT NULL THEN last_name
        ELSE NULL
    END;
-- Create a new index on the name column
CREATE INDEX idx_name ON user(name);
-- Drop the old indexes if they exist
DROP INDEX IF EXISTS idx_first_name ON user;
DROP INDEX IF EXISTS idx_last_name ON user;
-- Drop the old columns
ALTER TABLE user DROP COLUMN first_name;
ALTER TABLE user DROP COLUMN last_name;