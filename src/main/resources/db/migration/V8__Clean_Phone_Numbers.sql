-- V8__Clean_Phone_Numbers.sql
-- Flyway migration to clean up phone number data for proper contact matching
-- This migration handles placeholder phone numbers (emails, external IDs) and normalizes valid ones

-- Clean up phone numbers by setting invalid ones to NULL
-- This handles cases where phone numbers are placeholder values
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

-- Normalize remaining valid phone numbers to E.164 format
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

-- Handle duplicate phone numbers by keeping the most recent user
-- Create a temporary table to track duplicates
CREATE TEMPORARY TABLE phone_duplicates_temp AS
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
    SELECT 1 FROM phone_duplicates_temp pd 
    WHERE pd.phone_number = u1.phone_number 
    AND u1.date_created < pd.latest_date
);

-- Cleanup temporary table
DROP TEMPORARY TABLE phone_duplicates_temp; 