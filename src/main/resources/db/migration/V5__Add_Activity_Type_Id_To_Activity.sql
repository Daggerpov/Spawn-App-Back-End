-- Add activity_type_id foreign key column to Activity table
-- This establishes a proper foreign key relationship between Activity and ActivityType

-- Add the activity_type_id column
ALTER TABLE activity ADD COLUMN activity_type_id BINARY(16) NULL;

-- Add foreign key constraint
ALTER TABLE activity ADD CONSTRAINT FK_activity_activity_type 
FOREIGN KEY (activity_type_id) REFERENCES activity_type(id) ON DELETE SET NULL;

-- Add index for better query performance
CREATE INDEX idx_activity_activity_type_id ON activity(activity_type_id);

-- Verify the column was added
SELECT 
    CASE 
        WHEN EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = DATABASE() 
            AND table_name = 'activity' 
            AND column_name = 'activity_type_id'
        ) THEN 'SUCCESS: activity_type_id column added to Activity table'
        ELSE 'ERROR: activity_type_id column not found'
    END as migration_status; 