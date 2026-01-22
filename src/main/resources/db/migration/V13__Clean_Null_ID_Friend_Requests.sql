-- Migration to clean up friend request records with null IDs
-- This addresses JSON decoding errors on the client side caused by invalid data

-- Check if there are any friend requests with null IDs (for logging purposes)
-- SELECT COUNT(*) as null_id_count FROM friend_request WHERE id IS NULL;

-- Remove friend requests with null IDs
-- This is safe because ID is the primary key and should never be null
DELETE FROM friend_request WHERE id IS NULL;

-- Add a NOT NULL constraint to prevent future null IDs
-- This ensures data integrity going forward
ALTER TABLE friend_request MODIFY COLUMN id BINARY(16) NOT NULL;

-- Verify the cleanup was successful
-- SELECT COUNT(*) as remaining_null_id_count FROM friend_request WHERE id IS NULL;
