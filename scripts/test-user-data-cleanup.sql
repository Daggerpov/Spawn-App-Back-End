-- Test script to preview what the user data cleanup would do
-- Run this BEFORE running the actual migration to see what will be changed
-- This script makes NO changes to the database, only shows what WOULD change

-- Show current problematic data patterns
SELECT 
    CONCAT('User ', SUBSTRING(HEX(id), 1, 8)) as user_id_short,
    email,
    status,
    username,
    phone_number,
    name,
    CASE 
        WHEN username REGEXP '^[0-9]{15,}$' THEN 'OAUTH_NUMERIC_USERNAME'
        WHEN username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN 'APPLE_ID_USERNAME'
        WHEN username LIKE '%@%' THEN 'EMAIL_USERNAME'
        WHEN username = phone_number AND username = name THEN 'IDENTICAL_PLACEHOLDER'
        ELSE 'OK'
    END as username_issue,
    CASE 
        WHEN phone_number REGEXP '^[0-9]{15,}$' THEN 'OAUTH_NUMERIC_PHONE'
        WHEN phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN 'APPLE_ID_PHONE'
        WHEN phone_number LIKE '%@%' THEN 'EMAIL_PHONE'
        ELSE 'OK'
    END as phone_issue,
    CASE 
        WHEN name REGEXP '^[0-9]{15,}$' THEN 'OAUTH_NUMERIC_NAME'
        WHEN name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN 'APPLE_ID_NAME'
        ELSE 'OK'
    END as name_issue,
    CASE 
        WHEN email IS NULL THEN 'NULL_EMAIL_CRITICAL'
        ELSE 'OK'
    END as email_issue
FROM user 
WHERE 
    -- OAuth external IDs (long numeric strings)
    username REGEXP '^[0-9]{15,}$'
    OR phone_number REGEXP '^[0-9]{15,}$'
    OR name REGEXP '^[0-9]{15,}$'
    -- Apple IDs (long alphanumeric with dots)
    OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    -- Email addresses used as username/phone
    OR username LIKE '%@%'
    OR (phone_number LIKE '%@%' AND phone_number != email)
    -- Null email (critical issue)
    OR email IS NULL
    -- Same value for username, phone, and name (obvious placeholder)
    OR (username = phone_number AND username = name)
ORDER BY status, email;

-- Preview what changes would be made
SELECT 
    'PREVIEW_OF_CHANGES' as change_type,
    CONCAT('User ', SUBSTRING(HEX(id), 1, 8)) as user_id_short,
    email,
    status,
    username as current_username,
    CASE 
        WHEN username REGEXP '^[0-9]{15,}$' THEN NULL
        WHEN username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN NULL
        WHEN username LIKE '%@%' AND username != email THEN NULL
        WHEN username = phone_number AND username = name AND (
            username REGEXP '^[0-9]{15,}$' 
            OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
            OR username LIKE '%@%'
        ) THEN NULL
        ELSE username
    END as new_username,
    phone_number as current_phone,
    CASE 
        WHEN phone_number REGEXP '^[0-9]{15,}$' THEN NULL
        WHEN phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN NULL
        WHEN phone_number LIKE '%@%' THEN NULL
        WHEN username = phone_number AND username = name AND (
            username REGEXP '^[0-9]{15,}$' 
            OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
            OR username LIKE '%@%'
        ) THEN NULL
        ELSE phone_number
    END as new_phone,
    name as current_name,
    CASE 
        WHEN name REGEXP '^[0-9]{15,}$' THEN 
            CASE WHEN status = 0 THEN SUBSTRING_INDEX(COALESCE(email, 'unknown'), '@', 1) ELSE NULL END
        WHEN name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$' THEN 
            CASE WHEN status = 0 THEN SUBSTRING_INDEX(COALESCE(email, 'unknown'), '@', 1) ELSE NULL END
        ELSE name
    END as new_name,
    CASE 
        WHEN email IS NULL THEN CONCAT('user_', LOWER(HEX(id)), '@tempmail.spawn.com')
        ELSE email
    END as new_email
FROM user 
WHERE 
    -- OAuth external IDs (long numeric strings)
    username REGEXP '^[0-9]{15,}$'
    OR phone_number REGEXP '^[0-9]{15,}$'
    OR name REGEXP '^[0-9]{15,}$'
    -- Apple IDs (long alphanumeric with dots)
    OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    -- Email addresses used as username/phone
    OR username LIKE '%@%'
    OR (phone_number LIKE '%@%' AND phone_number != email)
    -- Null email (critical issue)
    OR email IS NULL
    -- Same value for username, phone, and name (obvious placeholder)
    OR (username = phone_number AND username = name)
ORDER BY status, email;

-- Summary of what would change
SELECT 
    'CHANGE_SUMMARY' as summary_type,
    COUNT(*) as total_users_affected,
    SUM(CASE 
        WHEN username REGEXP '^[0-9]{15,}$' 
        OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
        OR (username LIKE '%@%' AND username != email)
        OR (username = phone_number AND username = name) 
        THEN 1 ELSE 0 
    END) as usernames_to_null,
    SUM(CASE 
        WHEN phone_number REGEXP '^[0-9]{15,}$' 
        OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
        OR phone_number LIKE '%@%'
        OR (username = phone_number AND username = name)
        THEN 1 ELSE 0 
    END) as phones_to_null,
    SUM(CASE 
        WHEN name REGEXP '^[0-9]{15,}$' 
        OR name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
        THEN 1 ELSE 0 
    END) as names_to_fix,
    SUM(CASE WHEN email IS NULL THEN 1 ELSE 0 END) as emails_to_fix
FROM user 
WHERE 
    -- OAuth external IDs (long numeric strings)
    username REGEXP '^[0-9]{15,}$'
    OR phone_number REGEXP '^[0-9]{15,}$'
    OR name REGEXP '^[0-9]{15,}$'
    -- Apple IDs (long alphanumeric with dots)
    OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    OR name REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
    -- Email addresses used as username/phone
    OR username LIKE '%@%'
    OR (phone_number LIKE '%@%' AND phone_number != email)
    -- Null email (critical issue)
    OR email IS NULL
    -- Same value for username, phone, and name (obvious placeholder)
    OR (username = phone_number AND username = name);

-- Show what the final state would look like by status
SELECT 
    'FINAL_STATE_BY_STATUS' as report_type,
    status,
    COUNT(*) as total_users,
    SUM(CASE 
        WHEN username IS NULL 
        OR username REGEXP '^[0-9]{15,}$' 
        OR username REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
        OR (username LIKE '%@%' AND username != email)
        OR (username = phone_number AND username = name)
        THEN 1 ELSE 0 
    END) as users_with_null_username_after,
    SUM(CASE 
        WHEN phone_number IS NULL 
        OR phone_number REGEXP '^[0-9]{15,}$' 
        OR phone_number REGEXP '^[0-9]{6}\\.[a-f0-9]{32}\\.[0-9]{4}$'
        OR phone_number LIKE '%@%'
        OR (username = phone_number AND username = name)
        THEN 1 ELSE 0 
    END) as users_with_null_phone_after
FROM user 
GROUP BY status
ORDER BY status; 