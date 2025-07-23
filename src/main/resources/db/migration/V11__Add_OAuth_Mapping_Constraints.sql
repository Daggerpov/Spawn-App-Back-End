-- Migration to add proper constraints for OAuth mappings to prevent race conditions
-- This migration ensures data integrity and prevents duplicate OAuth mappings

-- First, clean up any potential duplicate mappings if they exist
-- This is a safety measure in case there are existing duplicates
DELETE m1 FROM user_id_external_id_map m1
INNER JOIN user_id_external_id_map m2 
WHERE m1.id = m2.id 
AND m1.user_id != m2.user_id 
AND m1.user_id < m2.user_id;

-- Add unique constraint on the external ID (which is the primary key)
-- This ensures each external OAuth ID can only map to one user
-- The constraint should already exist since 'id' is the primary key, but let's verify

-- Add unique constraint on user_id to ensure each user can only have one OAuth mapping
-- This prevents a user from having multiple OAuth provider mappings
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.table_constraints 
     WHERE table_schema = DATABASE() 
     AND table_name = 'user_id_external_id_map' 
     AND constraint_name = 'UK_oauth_user_unique') = 0,
    'ALTER TABLE user_id_external_id_map ADD CONSTRAINT UK_oauth_user_unique UNIQUE (user_id)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add an index on provider column for better query performance
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics 
     WHERE table_schema = DATABASE() 
     AND table_name = 'user_id_external_id_map' 
     AND index_name = 'idx_oauth_provider') = 0,
    'CREATE INDEX idx_oauth_provider ON user_id_external_id_map (provider)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add composite index on (user_id, provider) for efficient lookups
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.statistics 
     WHERE table_schema = DATABASE() 
     AND table_name = 'user_id_external_id_map' 
     AND index_name = 'idx_oauth_user_provider') = 0,
    'CREATE INDEX idx_oauth_user_provider ON user_id_external_id_map (user_id, provider)',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verify the constraints were created successfully
SELECT 
    'CONSTRAINT_VERIFICATION' as verification_type,
    COUNT(*) as oauth_mapping_count,
    COUNT(DISTINCT user_id) as unique_users_with_oauth,
    COUNT(DISTINCT id) as unique_external_ids
FROM user_id_external_id_map;

-- Show the constraints that were added
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME,
    COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME
WHERE k.TABLE_NAME = 'user_id_external_id_map' 
AND k.TABLE_SCHEMA = DATABASE()
AND t.CONSTRAINT_TYPE IN ('PRIMARY KEY', 'UNIQUE'); 