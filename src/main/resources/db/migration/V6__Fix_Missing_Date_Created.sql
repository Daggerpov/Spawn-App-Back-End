-- Fix missing dateCreated values for existing users
-- This migration ensures all users have a dateCreated value set

-- Update any users that don't have a dateCreated value set
UPDATE user 
SET date_created = CURRENT_TIMESTAMP 
WHERE date_created IS NULL;

-- Ensure the column is properly constrained to not be null going forward
-- (This is optional since the @PrePersist annotation will handle new users)
-- ALTER TABLE user MODIFY COLUMN date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP; 