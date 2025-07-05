-- Manual script to remove the problematic ActivityType constraint
-- Run this directly on the database if the initialization is still failing

-- Check current constraints
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_TYPE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME
WHERE k.TABLE_NAME = 'activity_type' 
AND k.TABLE_SCHEMA = DATABASE();

-- Drop the specific problematic constraint
ALTER TABLE activity_type DROP CONSTRAINT IF EXISTS UKg65vbnmntlk2562ko8bg3kmbi;

-- Drop any other unique constraints on order_num only
-- Check for indexes on order_num
SELECT 
    INDEX_NAME,
    COLUMN_NAME,
    NON_UNIQUE
FROM information_schema.statistics 
WHERE TABLE_NAME = 'activity_type' 
AND COLUMN_NAME = 'order_num'
AND TABLE_SCHEMA = DATABASE();

-- Drop any unique index on order_num only (not composite)
-- Note: This will need to be run with the specific index name found above
-- Example: DROP INDEX index_name ON activity_type;

-- Create the proper composite unique constraint
ALTER TABLE activity_type 
ADD CONSTRAINT UK_activity_type_creator_order 
UNIQUE (creator_id, order_num);

-- Verify the final state
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_TYPE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME
WHERE k.TABLE_NAME = 'activity_type' 
AND k.TABLE_SCHEMA = DATABASE(); 