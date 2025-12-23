-- Clean up friend request records with null IDs
-- This script identifies and removes friend request entries that have null IDs,
-- which can cause JSON decoding errors on the client side.

-- Check if there are any friend requests with null IDs
SELECT COUNT(*) as null_id_count
FROM friend_request
WHERE id IS NULL;

-- Display the problematic records for review before deletion
SELECT 
    id,
    sender_id,
    receiver_id,
    created_at
FROM friend_request
WHERE id IS NULL;

-- Remove friend requests with null IDs
-- Note: This should be safe since ID is the primary key and should never be null
DELETE FROM friend_request
WHERE id IS NULL;

-- Verify cleanup completed successfully
SELECT COUNT(*) as remaining_null_id_count
FROM friend_request
WHERE id IS NULL;

-- Show total friend request count after cleanup
SELECT COUNT(*) as total_friend_requests
FROM friend_request; 