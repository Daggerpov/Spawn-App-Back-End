# ActivityType Constraint Fix

## Problem
The ActivityType initialization fails with constraint violation:
```
Duplicate entry '0' for key 'activity_type.UKg65vbnmntlk2562ko8bg3kmbi'
```

## Root Cause
A unique constraint exists on the `order_num` column alone, preventing multiple users from having activity types with the same order numbers (0, 1, 2, etc.).

## Automatic Fix
The application has been updated to:
1. Enable Flyway migrations to run constraint removal scripts
2. Improve error handling in the initialization process
3. Use individual saves instead of batch saves to handle constraint violations

## Manual Fix (if needed)
If the automatic fix doesn't work, you can run the manual constraint removal script:

1. Connect to your MySQL database
2. Run the commands in `remove-activity-type-constraint.sql`
3. Restart your application

## Expected Behavior After Fix
- Each user should be able to have activity types with order numbers starting from 0
- The constraint should be changed to a composite unique constraint on `(creator_id, order_num)`
- Activity type initialization should succeed for all users

## Verification
After applying the fix, check the database:
```sql
-- Should show the new composite constraint
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_TYPE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.TABLE_CONSTRAINTS t ON k.CONSTRAINT_NAME = t.CONSTRAINT_NAME
WHERE k.TABLE_NAME = 'activity_type' 
AND k.TABLE_SCHEMA = DATABASE();
```

You should see a constraint named `UK_activity_type_creator_order` with both `creator_id` and `order_num` columns. 