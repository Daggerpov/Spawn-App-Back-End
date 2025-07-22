-- Comprehensive cleanup script for existing user data
-- This script identifies and cleans up problematic OAuth placeholder data
-- while preserving legitimate user information

-- First, let's create a backup summary of what we're about to change
CREATE TEMPORARY TABLE user_cleanup_log AS
SELECT 
    id,
    username,
    phone_number,
    name,
    email,
    status,
    'BEFORE_CLEANUP' as log_type,
    NOW() as log_timestamp
FROM user 
WHERE 
    -- OAuth external IDs (long numeric strings)
    username REGEXP '^[0-9]{15,}$'
    OR phone_number REGEXP '^[0-9]{15,}$'
    -- Apple IDs (long alphanumeric with dots)
    OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    -- Email addresses used as username/phone
    OR username LIKE '%@%'
    OR (phone_number LIKE '%@%' AND phone_number != email)
    -- Null email (critical issue)
    OR email IS NULL
    -- Same value for username, phone, and name (obvious placeholder)
    OR (username = phone_number AND username = name);

-- Display what we found
SELECT 
    'PROBLEMATIC_DATA_SUMMARY' as summary_type,
    COUNT(*) as total_users_to_clean,
    SUM(CASE WHEN username REGEXP '^[0-9]{15,}$' THEN 1 ELSE 0 END) as oauth_numeric_usernames,
    SUM(CASE WHEN phone_number REGEXP '^[0-9]{15,}$' THEN 1 ELSE 0 END) as oauth_numeric_phones,
    SUM(CASE WHEN username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN 1 ELSE 0 END) as apple_id_usernames,
    SUM(CASE WHEN username LIKE '%@%' THEN 1 ELSE 0 END) as email_usernames,
    SUM(CASE WHEN phone_number LIKE '%@%' AND phone_number != email THEN 1 ELSE 0 END) as email_phones,
    SUM(CASE WHEN email IS NULL THEN 1 ELSE 0 END) as null_emails,
    SUM(CASE WHEN username = phone_number AND username = name THEN 1 ELSE 0 END) as identical_placeholder_data
FROM user_cleanup_log;

-- 1. Handle the critical case: user with NULL email
-- This user appears to be an Apple ID user, we need to either delete or fix
-- Based on the data pattern, this seems like incomplete Apple OAuth data
UPDATE user 
SET email = CONCAT('user_', LOWER(HEX(id)), '@tempmail.spawn.com')
WHERE email IS NULL;

-- Log this critical fix
INSERT INTO user_cleanup_log 
SELECT 
    id, username, phone_number, name, email, status,
    'FIXED_NULL_EMAIL' as log_type,
    NOW() as log_timestamp
FROM user 
WHERE email LIKE '%@tempmail.spawn.com';

-- 2. Clean up OAuth external IDs used as usernames
UPDATE user 
SET username = NULL 
WHERE username REGEXP '^[0-9]{15,}$'  -- Google OAuth external IDs
   OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$';  -- Apple IDs

-- 3. Clean up OAuth external IDs used as phone numbers
UPDATE user 
SET phone_number = NULL 
WHERE phone_number REGEXP '^[0-9]{15,}$'  -- Google OAuth external IDs
   OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$';  -- Apple IDs

-- 4. Clean up email addresses used as usernames (but keep if it's their actual email)
UPDATE user 
SET username = NULL 
WHERE username LIKE '%@%' 
  AND username != email;  -- Don't clean if it matches their email

-- 5. Clean up email addresses used as phone numbers
UPDATE user 
SET phone_number = NULL 
WHERE phone_number LIKE '%@%';

-- 6. Clean up cases where username, phone_number, and name are identical (obvious placeholders)
UPDATE user 
SET 
    username = NULL,
    phone_number = NULL
WHERE username = phone_number 
  AND username = name 
  AND (
    username REGEXP '^[0-9]{15,}$' 
    OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR username LIKE '%@%'
  );

-- 7. Clean up names that are clearly OAuth external IDs
UPDATE user 
SET name = NULL 
WHERE name REGEXP '^[0-9]{15,}$'  -- Google OAuth external IDs
   OR name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$';  -- Apple IDs

-- 8. For users with status EMAIL_VERIFIED (0) and no proper name, set name from email prefix
UPDATE user 
SET name = SUBSTRING_INDEX(email, '@', 1)
WHERE status = 0 
  AND (name IS NULL OR name = '')
  AND email IS NOT NULL
  AND email != '';

-- Create final cleanup log
INSERT INTO user_cleanup_log 
SELECT 
    id, username, phone_number, name, email, status,
    'AFTER_CLEANUP' as log_type,
    NOW() as log_timestamp
FROM user;

-- Final summary report
SELECT 
    'CLEANUP_SUMMARY' as report_type,
    COUNT(DISTINCT u.id) as total_users_processed,
    SUM(CASE WHEN u.username IS NULL THEN 1 ELSE 0 END) as users_with_null_username,
    SUM(CASE WHEN u.phone_number IS NULL THEN 1 ELSE 0 END) as users_with_null_phone,
    SUM(CASE WHEN u.name IS NULL THEN 1 ELSE 0 END) as users_with_null_name,
    SUM(CASE WHEN u.email LIKE '%@tempmail.spawn.com' THEN 1 ELSE 0 END) as users_with_temp_email
FROM user u;

-- Show users by status after cleanup
SELECT 
    status,
    COUNT(*) as user_count,
    SUM(CASE WHEN username IS NULL THEN 1 ELSE 0 END) as null_usernames,
    SUM(CASE WHEN phone_number IS NULL THEN 1 ELSE 0 END) as null_phones,
    SUM(CASE WHEN name IS NULL THEN 1 ELSE 0 END) as null_names
FROM user 
GROUP BY status
ORDER BY status;

-- Show the specific users that were most affected by cleanup
SELECT 
    CONCAT('User ', SUBSTRING(HEX(u.id), 1, 8)) as user_id_short,
    u.email,
    u.status,
    u.username as current_username,
    u.phone_number as current_phone,
    u.name as current_name,
    before_log.username as old_username,
    before_log.phone_number as old_phone,
    before_log.name as old_name
FROM user u
JOIN user_cleanup_log before_log ON u.id = before_log.id AND before_log.log_type = 'BEFORE_CLEANUP'
WHERE (
    u.username != before_log.username 
    OR u.phone_number != before_log.phone_number 
    OR u.name != before_log.name
    OR (u.username IS NULL AND before_log.username IS NOT NULL)
    OR (u.phone_number IS NULL AND before_log.phone_number IS NOT NULL)
    OR (u.name IS NULL AND before_log.name IS NOT NULL)
)
ORDER BY u.status, u.email;

-- Cleanup: Drop the temporary log table
-- DROP TABLE user_cleanup_log; 