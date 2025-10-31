-- Diagnostic script for user constraint issue
-- Run this to understand the current database state and constraint problem

-- 1. Show all constraints on the user table
SELECT 
    tc.CONSTRAINT_NAME,
    tc.CONSTRAINT_TYPE,
    cc.CHECK_CLAUSE,
    tc.TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS tc
LEFT JOIN information_schema.CHECK_CONSTRAINTS cc ON tc.CONSTRAINT_NAME = cc.CONSTRAINT_NAME
WHERE tc.TABLE_SCHEMA = DATABASE() 
  AND tc.TABLE_NAME = 'user'
ORDER BY tc.CONSTRAINT_TYPE, tc.CONSTRAINT_NAME;

-- 2. Show current user statuses in the database
SELECT 
    status,
    COUNT(*) as user_count
FROM user 
GROUP BY status 
ORDER BY status;

-- 3. Show the problematic user mentioned in the error
SELECT 
    id,
    email,
    username,
    phone_number,
    name,
    status,
    date_created
FROM user 
WHERE id = '92c76848-a908-4459-95c0-297b12445a2e'
   OR email LIKE '%ubc%'
   OR name LIKE '%ubc mmhc%';

-- 4. Show all users with CONTACT_IMPORT status (if any)
SELECT 
    id,
    email,
    username,
    phone_number,
    name,
    status,
    date_created
FROM user 
WHERE status = 'CONTACT_IMPORT';

-- 5. Check for any users that might be causing constraint issues
SELECT 
    'Potential constraint issues' as check_type,
    COUNT(*) as count
FROM user 
WHERE status NOT IN ('EMAIL_VERIFIED', 'USERNAME_AND_PHONE_NUMBER', 'NAME_AND_PHOTO', 'CONTACT_IMPORT', 'ACTIVE')
UNION ALL
SELECT 
    'Users with null required fields for ACTIVE status' as check_type,
    COUNT(*) as count
FROM user 
WHERE status = 'ACTIVE' 
  AND (email IS NULL OR username IS NULL OR phone_number IS NULL OR name IS NULL);

-- 6. Manual fix for the specific constraint (if you want to run this manually)
/*
-- MANUAL FIX - UNCOMMENT AND RUN IF NEEDED:

-- Drop the problematic constraint
ALTER TABLE user DROP CHECK user_chk_1;

-- Recreate with all valid enum values
ALTER TABLE user ADD CONSTRAINT user_status_chk 
CHECK (status IN ('EMAIL_VERIFIED', 'USERNAME_AND_PHONE_NUMBER', 'NAME_AND_PHOTO', 'CONTACT_IMPORT', 'ACTIVE'));
*/ 