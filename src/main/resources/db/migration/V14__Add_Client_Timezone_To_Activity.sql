-- Add client_timezone column to activity table
-- This column stores the timezone of the client that created the activity
-- Used for timezone-aware expiration of activities without explicit end times

ALTER TABLE activity 
ADD COLUMN client_timezone VARCHAR(255);

-- Add comment to explain the column purpose
ALTER TABLE activity 
MODIFY COLUMN client_timezone VARCHAR(255) COMMENT 'Timezone of the client creating the activity (e.g., America/New_York). Used for timezone-aware expiration of indefinite activities.';
