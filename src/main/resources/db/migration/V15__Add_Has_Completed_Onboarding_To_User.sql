-- Add hasCompletedOnboarding column to user table
-- This field tracks whether a user has completed the onboarding process (created their first activity)

ALTER TABLE "user" 
ADD COLUMN has_completed_onboarding BOOLEAN NOT NULL DEFAULT FALSE;

-- Update existing users who have created activities to mark onboarding as completed
UPDATE "user" 
SET has_completed_onboarding = TRUE 
WHERE id IN (
    SELECT DISTINCT creator_id 
    FROM activity 
    WHERE creator_id IS NOT NULL
);
