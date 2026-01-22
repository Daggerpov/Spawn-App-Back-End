-- Fix User Status Check Constraint Issue
-- This migration fixes the user_chk_1 constraint that doesn't recognize the CONTACT_IMPORT status
-- The constraint was auto-generated and needs to be updated to include all valid UserStatus enum values

-- First, let's see what constraints exist
SELECT 
    CONSTRAINT_NAME, 
    CHECK_CLAUSE 
FROM information_schema.CHECK_CONSTRAINTS 
WHERE CONSTRAINT_SCHEMA = DATABASE() 
  AND CONSTRAINT_NAME LIKE '%user%';

-- Drop the existing check constraint if it exists
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.table_constraints 
     WHERE table_schema = DATABASE() 
     AND table_name = 'user' 
     AND constraint_name = 'user_chk_1') > 0,
    'ALTER TABLE user DROP CHECK user_chk_1',
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Also check for any other user check constraints and drop them
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.table_constraints 
     WHERE table_schema = DATABASE() 
     AND table_name = 'user' 
     AND constraint_type = 'CHECK') > 0,
    CONCAT('ALTER TABLE user DROP CHECK ', 
           (SELECT constraint_name FROM information_schema.table_constraints 
            WHERE table_schema = DATABASE() 
            AND table_name = 'user' 
            AND constraint_type = 'CHECK'
            LIMIT 1)),
    'SELECT 1'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Recreate the constraint with all valid UserStatus enum values
-- UserStatus enum values: EMAIL_VERIFIED, USERNAME_AND_PHONE_NUMBER, NAME_AND_PHOTO, CONTACT_IMPORT, ACTIVE
ALTER TABLE user ADD CONSTRAINT user_status_chk 
CHECK (status IN ('EMAIL_VERIFIED', 'USERNAME_AND_PHONE_NUMBER', 'NAME_AND_PHOTO', 'CONTACT_IMPORT', 'ACTIVE'));

-- Verify the constraint was created
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.table_constraints 
            WHERE table_schema = DATABASE() 
            AND table_name = 'user' 
            AND constraint_name = 'user_status_chk'
        ) THEN 'SUCCESS: User status check constraint updated with CONTACT_IMPORT'
        ELSE 'WARNING: User status check constraint not found'
    END as migration_status;

-- Show all user statuses to verify they're valid
SELECT DISTINCT status, COUNT(*) as count 
FROM user 
GROUP BY status 
ORDER BY status; 