-- Diagnostic script for Activity Type constraint issues
-- This script helps identify and fix the unique constraint problems

-- Step 1: Show all constraints on the activity_type table
SELECT 
    tc.constraint_name,
    tc.constraint_type,
    kcu.column_name,
    tc.table_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.table_schema = DATABASE() 
    AND tc.table_name = 'activity_type'
    AND tc.constraint_type = 'UNIQUE'
ORDER BY tc.constraint_name, kcu.ordinal_position;

-- Step 2: Show all indexes on the activity_type table
SELECT 
    index_name,
    column_name,
    non_unique,
    seq_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() 
    AND table_name = 'activity_type'
ORDER BY index_name, seq_in_index;

-- Step 3: Show current orderNum distribution
SELECT 
    order_num,
    COUNT(*) as count_users,
    GROUP_CONCAT(DISTINCT SUBSTRING(creator_id, 1, 8)) as creator_ids_sample
FROM activity_type 
GROUP BY order_num 
ORDER BY order_num;

-- Step 4: Check for users without activity types
SELECT 
    u.id,
    u.username,
    COUNT(at.id) as activity_type_count
FROM user u
LEFT JOIN activity_type at ON u.id = at.creator_id
GROUP BY u.id, u.username
HAVING activity_type_count = 0
ORDER BY u.username;

-- Step 5: Manual constraint removal (if needed)
-- Uncomment the following lines to manually remove the problematic constraint

-- SET @constraint_name = (
--     SELECT constraint_name 
--     FROM information_schema.table_constraints 
--     WHERE table_schema = DATABASE() 
--         AND table_name = 'activity_type' 
--         AND constraint_type = 'UNIQUE'
--         AND constraint_name LIKE 'UK%'
--     LIMIT 1
-- );

-- SET @sql = CONCAT('ALTER TABLE activity_type DROP CONSTRAINT ', @constraint_name);
-- PREPARE stmt FROM @sql;
-- EXECUTE stmt;
-- DEALLOCATE PREPARE stmt; 