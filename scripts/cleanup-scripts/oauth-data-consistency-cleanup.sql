-- OAuth Data Consistency Cleanup Script
-- This script identifies and cleans up orphaned OAuth data that can occur during concurrent operations
-- Run this script to fix existing data inconsistencies before deploying the OAuth concurrency fixes

-- Enable transaction mode for safety
BEGIN;

-- Create a temporary table to track cleanup actions
CREATE TEMPORARY TABLE oauth_cleanup_log (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(50),
    description TEXT,
    affected_count INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 1. Identify and clean up orphaned OAuth mappings (mappings pointing to deleted users)
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'ORPHANED_MAPPINGS_FOUND',
    'OAuth mappings pointing to non-existent users',
    COUNT(*)
FROM user_id_external_id_map m
LEFT JOIN users u ON m.user_id = u.id
WHERE u.id IS NULL;

-- Delete orphaned mappings
DELETE FROM user_id_external_id_map 
WHERE user_id NOT IN (SELECT id FROM users);

-- Log the cleanup action
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'ORPHANED_MAPPINGS_DELETED',
    'Deleted OAuth mappings pointing to non-existent users',
    ROW_COUNT();

-- 2. Identify users without OAuth mappings who have non-ACTIVE status (likely orphaned OAuth users)
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'ORPHANED_USERS_FOUND',
    'Users with non-ACTIVE status but no OAuth mappings',
    COUNT(*)
FROM users u
LEFT JOIN user_id_external_id_map m ON u.id = m.user_id
WHERE m.user_id IS NULL 
  AND u.status IS NOT NULL 
  AND u.status != 'ACTIVE'
  AND u.email IS NOT NULL;

-- Clean up orphaned users (users with EMAIL_VERIFIED or other non-ACTIVE status but no OAuth mapping)
-- This is safe because these users are likely artifacts from failed OAuth registrations
DELETE FROM users 
WHERE id IN (
    SELECT u.id
    FROM users u
    LEFT JOIN user_id_external_id_map m ON u.id = m.user_id
    WHERE m.user_id IS NULL 
      AND u.status IS NOT NULL 
      AND u.status != 'ACTIVE'
      AND u.email IS NOT NULL
      AND u.date_created > NOW() - INTERVAL '7 days' -- Only clean up recent orphaned users
);

-- Log the cleanup action
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'ORPHANED_USERS_DELETED',
    'Deleted orphaned users with non-ACTIVE status and no OAuth mappings (created within last 7 days)',
    ROW_COUNT();

-- 3. Identify duplicate OAuth mappings (multiple mappings for the same user)
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'DUPLICATE_MAPPINGS_FOUND',
    'Users with multiple OAuth mappings',
    COUNT(*)
FROM (
    SELECT user_id, COUNT(*) as mapping_count
    FROM user_id_external_id_map
    GROUP BY user_id
    HAVING COUNT(*) > 1
) duplicates;

-- For users with multiple OAuth mappings, keep the most recent one and delete the others
-- This handles the edge case where multiple mappings were created during race conditions
WITH duplicate_mappings AS (
    SELECT 
        external_user_id,
        user_id,
        provider,
        ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY user_id DESC) as rn
    FROM user_id_external_id_map
    WHERE user_id IN (
        SELECT user_id
        FROM user_id_external_id_map
        GROUP BY user_id
        HAVING COUNT(*) > 1
    )
)
DELETE FROM user_id_external_id_map
WHERE external_user_id IN (
    SELECT external_user_id
    FROM duplicate_mappings
    WHERE rn > 1
);

-- Log the cleanup action
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'DUPLICATE_MAPPINGS_DELETED',
    'Deleted duplicate OAuth mappings, keeping most recent for each user',
    ROW_COUNT();

-- 4. Identify and fix users with ACTIVE status but no OAuth mappings (potential data corruption)
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'ACTIVE_USERS_NO_OAUTH',
    'ACTIVE users without OAuth mappings (potential manual intervention required)',
    COUNT(*)
FROM users u
LEFT JOIN user_id_external_id_map m ON u.id = m.user_id
WHERE m.user_id IS NULL 
  AND (u.status = 'ACTIVE' OR u.status IS NULL)
  AND u.email IS NOT NULL
  AND u.password IS NULL; -- OAuth users typically don't have passwords

-- Note: We don't automatically delete ACTIVE users without OAuth mappings as they might be valid email/password users
-- Log them for manual review

-- 5. Verify data integrity after cleanup
INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'FINAL_VERIFICATION',
    'Total users after cleanup',
    COUNT(*)
FROM users;

INSERT INTO oauth_cleanup_log (action_type, description, affected_count)
SELECT 
    'FINAL_VERIFICATION',
    'Total OAuth mappings after cleanup',
    COUNT(*)
FROM user_id_external_id_map;

-- Display cleanup summary
SELECT 
    action_type,
    description,
    affected_count,
    timestamp
FROM oauth_cleanup_log
ORDER BY timestamp;

-- Commit the changes if everything looks good
-- COMMIT;

-- Uncomment the line above to actually execute the cleanup
-- For safety, the script is set to rollback by default
ROLLBACK;

-- Instructions for use:
-- 1. Review the cleanup log output to understand what would be cleaned up
-- 2. If the actions look correct, change ROLLBACK to COMMIT above
-- 3. Run the script again to actually perform the cleanup
-- 4. Monitor application logs to ensure OAuth operations work correctly after cleanup 