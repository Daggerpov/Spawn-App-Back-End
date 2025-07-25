-- Phone Number Data Cleanup Migration
-- This script cleans up phone number data before deploying the new phone number matching system
-- It handles cases where phone numbers are placeholder values (emails, external IDs, etc.)

-- Step 1: Backup current phone number data for safety
-- CREATE TABLE user_phone_backup AS 
-- SELECT id, username, email, phone_number, status, date_created 
-- FROM user 
-- WHERE phone_number IS NOT NULL;

-- Step 2: Identify and log problematic phone numbers
SELECT 
    'BEFORE CLEANUP - Problematic phone numbers:' as analysis_step,
    COUNT(*) as total_count
FROM user 
WHERE phone_number IS NOT NULL 
AND (
    -- Phone numbers that are emails
    phone_number LIKE '%@%' 
    -- Phone numbers that are too short (less than 10 digits when cleaned)
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{0,9}$'
    -- Phone numbers that are UUIDs or external IDs (contain hyphens and are long)
    OR (phone_number LIKE '%-%' AND CHAR_LENGTH(phone_number) > 20)
    -- Phone numbers that are clearly not phone numbers (contain letters other than in emails)
    OR (phone_number REGEXP '[a-zA-Z]' AND phone_number NOT LIKE '%@%')
    -- Phone numbers that are just numbers but too long (more than 15 digits)
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{16,}$'
);

-- Step 3: Show examples of problematic data
SELECT 
    'EXAMPLES - Before cleanup:' as analysis_step,
    id,
    username,
    email,
    phone_number,
    status,
    CASE 
        WHEN phone_number LIKE '%@%' THEN 'EMAIL_AS_PHONE'
        WHEN phone_number LIKE '%-%' AND CHAR_LENGTH(phone_number) > 20 THEN 'UUID_AS_PHONE'
        WHEN REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{0,9}$' THEN 'TOO_SHORT'
        WHEN REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{16,}$' THEN 'TOO_LONG'
        WHEN phone_number REGEXP '[a-zA-Z]' AND phone_number NOT LIKE '%@%' THEN 'CONTAINS_LETTERS'
        ELSE 'OTHER'
    END as issue_type
FROM user 
WHERE phone_number IS NOT NULL 
AND (
    phone_number LIKE '%@%' 
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{0,9}$'
    OR (phone_number LIKE '%-%' AND CHAR_LENGTH(phone_number) > 20)
    OR (phone_number REGEXP '[a-zA-Z]' AND phone_number NOT LIKE '%@%')
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{16,}$'
)
LIMIT 10;

-- Step 4: Clean up phone numbers by setting invalid ones to NULL
UPDATE user 
SET phone_number = NULL 
WHERE phone_number IS NOT NULL 
AND (
    -- Phone numbers that are emails
    phone_number LIKE '%@%' 
    -- Phone numbers that are too short (less than 10 digits when cleaned)
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{0,9}$'
    -- Phone numbers that are UUIDs or external IDs (contain hyphens and are long)
    OR (phone_number LIKE '%-%' AND CHAR_LENGTH(phone_number) > 20)
    -- Phone numbers that are clearly not phone numbers (contain letters other than in emails)
    OR (phone_number REGEXP '[a-zA-Z]' AND phone_number NOT LIKE '%@%')
    -- Phone numbers that are just numbers but too long (more than 15 digits)
    OR REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^.{16,}$'
);

-- Step 5: Normalize remaining valid phone numbers to E.164 format
-- Note: This assumes most numbers are US numbers. Adjust as needed for your user base.
UPDATE user 
SET phone_number = CASE
    -- If it already starts with +, keep as is (assuming it's already international format)
    WHEN phone_number LIKE '+%' THEN phone_number
    -- If it's 11 digits starting with 1, add + prefix (US format)
    WHEN REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^1[0-9]{10}$' THEN 
        CONCAT('+', REGEXP_REPLACE(phone_number, '[^0-9]', ''))
    -- If it's 10 digits, assume US and add +1 prefix
    WHEN REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^[0-9]{10}$' THEN 
        CONCAT('+1', REGEXP_REPLACE(phone_number, '[^0-9]', ''))
    -- For other cases, try to add +1 if it looks like a reasonable number
    WHEN REGEXP_REPLACE(phone_number, '[^0-9]', '') REGEXP '^[0-9]{10,14}$' THEN 
        CONCAT('+1', REGEXP_REPLACE(phone_number, '[^0-9]', ''))
    -- Otherwise, set to NULL for manual review
    ELSE NULL
END
WHERE phone_number IS NOT NULL
AND phone_number NOT LIKE '+%';

-- Step 6: Handle duplicate phone numbers by keeping the most recent user
-- First, find duplicates
CREATE TEMPORARY TABLE phone_duplicates AS
SELECT phone_number, COUNT(*) as count_users, MAX(date_created) as latest_date
FROM user 
WHERE phone_number IS NOT NULL 
GROUP BY phone_number 
HAVING COUNT(*) > 1;

-- For duplicates, keep only the most recent user and NULL the others
UPDATE user u1
SET phone_number = NULL
WHERE u1.phone_number IS NOT NULL
AND EXISTS (
    SELECT 1 FROM phone_duplicates pd 
    WHERE pd.phone_number = u1.phone_number 
    AND u1.date_created < pd.latest_date
);

-- Step 7: Post-cleanup analysis
SELECT 
    'AFTER CLEANUP - Summary:' as analysis_step,
    COUNT(*) as total_users,
    SUM(CASE WHEN phone_number IS NOT NULL THEN 1 ELSE 0 END) as users_with_phone,
    SUM(CASE WHEN phone_number IS NULL THEN 1 ELSE 0 END) as users_without_phone,
    SUM(CASE WHEN phone_number LIKE '+%' THEN 1 ELSE 0 END) as users_with_international_format
FROM user;

-- Step 8: Show sample of cleaned data
SELECT 
    'EXAMPLES - After cleanup (valid phones):' as analysis_step,
    id,
    username,
    email,
    phone_number,
    status
FROM user 
WHERE phone_number IS NOT NULL 
LIMIT 10;

-- Step 9: Show users who lost their phone numbers (for manual review if needed)
SELECT 
    'USERS WHO LOST PHONE NUMBERS - For manual review:' as analysis_step,
    COUNT(*) as count_users_lost_phone
FROM user 
WHERE phone_number IS NULL 
AND status IN ('USERNAME_AND_PHONE_NUMBER', 'NAME_AND_PHOTO', 'CONTACT_IMPORT', 'ACTIVE');

-- Cleanup temporary table
DROP TEMPORARY TABLE IF EXISTS phone_duplicates;

-- Step 10: Optional - Create a report of the changes for administrators
-- SELECT 
--     'MIGRATION COMPLETE' as status,
--     'Phone number cleanup completed successfully' as message,
--     NOW() as completed_at; 