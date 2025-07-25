-- Fix missing dateCreated values for existing users
-- This script updates any users that don't have a dateCreated value set

UPDATE `user` 
SET date_created = CURRENT_TIMESTAMP 
WHERE date_created IS NULL;

-- Verify the update
SELECT COUNT(*) AS users_without_date_created 
FROM `user` 
WHERE date_created IS NULL;

-- Show sample of users with their creation dates
SELECT id, username, name, date_created, last_updated 
FROM `user` 
ORDER BY date_created DESC 
LIMIT 10; 