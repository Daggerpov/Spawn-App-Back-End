-- Migration to make user fields nullable for better OAuth user handling
-- This allows OAuth users to have null values for username, phoneNumber, and name
-- until they provide them during onboarding

-- Make username nullable (keep unique constraint)
ALTER TABLE user MODIFY COLUMN username VARCHAR(255) NULL;

-- Make phone_number nullable (keep unique constraint)
ALTER TABLE user MODIFY COLUMN phone_number VARCHAR(255) NULL;

-- Make name nullable
ALTER TABLE user MODIFY COLUMN name VARCHAR(255) NULL;

-- Clean up any existing placeholder data from OAuth users
-- Remove obviously placeholder usernames and phone numbers
UPDATE user 
SET username = NULL 
WHERE username LIKE 'temp_%' 
   OR username REGEXP '^[a-f0-9-]{30,}$'
   OR username REGEXP '^[0-9]{15,}$';

UPDATE user 
SET phone_number = NULL 
WHERE phone_number LIKE 'temp_%' 
   OR phone_number REGEXP '^[a-f0-9-]{30,}$'
   OR phone_number REGEXP '^[0-9]{15,}$';

-- Log the number of users affected
SELECT 
    COUNT(*) as total_users,
    SUM(CASE WHEN username IS NULL THEN 1 ELSE 0 END) as users_without_username,
    SUM(CASE WHEN phone_number IS NULL THEN 1 ELSE 0 END) as users_without_phone,
    SUM(CASE WHEN name IS NULL THEN 1 ELSE 0 END) as users_without_name
FROM user; 