-- Remove unique constraint from order_num column in activity_type table
-- This allows temporary duplicate orderNum values during batch updates for reordering
-- Application-level validation will ensure uniqueness is maintained after updates

-- MySQL-compatible approach to drop unique constraint
-- Check for different possible constraint/index names that MySQL might use

-- First attempt: Drop by column name (most common)
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics 
     WHERE table_schema = DATABASE() 
     AND table_name = 'activity_type' 
     AND index_name = 'order_num') > 0,
    'DROP INDEX order_num ON activity_type',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Second attempt: Drop by UK_ prefixed name
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics 
     WHERE table_schema = DATABASE() 
     AND table_name = 'activity_type' 
     AND index_name = 'UK_activity_type_order_num') > 0,
    'DROP INDEX UK_activity_type_order_num ON activity_type',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt; 