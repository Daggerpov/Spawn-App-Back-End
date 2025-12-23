

-- First, identify and delete orphaned chat_message records
DELETE cm FROM chat_message cm
LEFT JOIN activity a ON cm.activity_id = a.id
WHERE a.id IS NULL;

-- Also clean up orphaned chat_message_like records that reference deleted chat messages
DELETE cml FROM chat_message_like cml
LEFT JOIN chat_message cm ON cml.chat_message_id = cm.id
WHERE cm.id IS NULL;

-- Add the foreign key constraint if it doesn't exist
-- Note: Hibernate will handle this, but we ensure data is clean first 