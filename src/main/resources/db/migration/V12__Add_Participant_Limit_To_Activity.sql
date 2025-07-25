-- Add participant_limit column to Activity table
-- This allows activities to have a maximum number of participants
-- NULL value means unlimited participants

-- Add the participant_limit column
ALTER TABLE activity ADD COLUMN participant_limit INT NULL;

-- Add index for better query performance when filtering by participant limit
CREATE INDEX idx_activity_participant_limit ON activity(participant_limit);

-- Verify the column was added
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'activity' 
            AND column_name = 'participant_limit'
        ) THEN 'SUCCESS: participant_limit column added to Activity table'
        ELSE 'ERROR: participant_limit column not found'
    END as migration_status; 