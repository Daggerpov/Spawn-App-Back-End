-- Fix Activity Type Unique Constraint Issue
-- This migration removes the problematic unique constraint that prevents activity type initialization
-- The constraint name UKg65vbnmntlk2562ko8bg3kmbi is auto-generated and blocks orderNum=0 for multiple users

-- Method 1: Drop the specific constraint if it exists
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.table_constraints 
     WHERE table_schema = DATABASE() 
     AND table_name = 'activity_type' 
     AND constraint_name = 'UKg65vbnmntlk2562ko8bg3kmbi') > 0,
    'ALTER TABLE activity_type DROP CONSTRAINT UKg65vbnmntlk2562ko8bg3kmbi',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Method 2: Drop any unique constraint on orderNum column (fallback)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics 
     WHERE table_schema = DATABASE() 
     AND table_name = 'activity_type' 
     AND column_name = 'order_num'
     AND non_unique = 0) > 0,
    CONCAT('DROP INDEX ', 
           (SELECT index_name FROM information_schema.statistics 
            WHERE table_schema = DATABASE() 
            AND table_name = 'activity_type' 
            AND column_name = 'order_num'
            AND non_unique = 0
            LIMIT 1), 
           ' ON activity_type'),
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Method 3: Generic approach - drop any unique constraint containing orderNum
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.key_column_usage k
     JOIN information_schema.table_constraints c ON k.constraint_name = c.constraint_name
     WHERE k.table_schema = DATABASE() 
     AND k.table_name = 'activity_type' 
     AND k.column_name = 'order_num'
     AND c.constraint_type = 'UNIQUE') > 0,
    CONCAT('ALTER TABLE activity_type DROP CONSTRAINT ',
           (SELECT c.constraint_name FROM information_schema.key_column_usage k
            JOIN information_schema.table_constraints c ON k.constraint_name = c.constraint_name
            WHERE k.table_schema = DATABASE() 
            AND k.table_name = 'activity_type' 
            AND k.column_name = 'order_num'
            AND c.constraint_type = 'UNIQUE'
            LIMIT 1)),
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 