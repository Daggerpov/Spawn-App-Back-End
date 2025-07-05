# Activity Type Constraint Issue Fix

## Problem Description

The application was experiencing a SQL constraint violation error during activity type initialization for existing users:

```
SQL Error: 1062, SQLState: 23000
Duplicate entry '0' for key 'activity_type.UKg65vbnmntlk2562ko8bg3kmbi'
```

## Root Cause

1. **Unique Constraint Issue**: The database had a unique constraint named `UKg65vbnmntlk2562ko8bg3kmbi` on the `activity_type` table
2. **Previous Migration Failed**: The existing migration `V2__Remove_Unique_Constraint_From_Activity_Type_Order_Num.sql` didn't successfully remove the constraint because:
   - It only checked for specific constraint names like `order_num` and `UK_activity_type_order_num`
   - The actual constraint had an auto-generated name `UKg65vbnmntlk2562ko8bg3kmbi`
3. **Initialization Conflict**: When `ActivityTypeInitializer` tried to create default activity types for multiple users, each user would start with `orderNum = 0`, causing constraint violations

## Solution Components

### 1. New Migration Script (`V3__Fix_Activity_Type_Unique_Constraint.sql`)

This migration uses multiple approaches to remove the problematic constraint:

- **Method 1**: Targets the specific constraint name `UKg65vbnmntlk2562ko8bg3kmbi`
- **Method 2**: Finds and removes any unique constraint on the `order_num` column
- **Method 3**: Generic approach to find and remove unique constraints containing `order_num`

### 2. Enhanced ActivityTypeInitializer

The initializer now includes:

- **Specific Exception Handling**: Catches `DataIntegrityViolationException` separately
- **Recovery Logic**: Checks if partial initialization succeeded
- **Better Logging**: Tracks users with errors and provides detailed diagnostics
- **Graceful Degradation**: Continues processing other users even if some fail

## Database Schema Expectation

The `orderNum` field should allow:
- Each user to have their own sequence of activity types starting from 0
- Multiple users to have activity types with the same `orderNum` value
- The constraint should be on `(creator_id, orderNum)` combination, not just `orderNum` alone

## Testing and Validation

### Before Running the Fix

1. **Diagnose Current State**:
   ```sql
   -- Run the diagnostic script
   source scripts/diagnose-activity-type-constraints.sql
   ```

2. **Check Affected Users**:
   ```sql
   SELECT u.username, COUNT(at.id) as activity_type_count
   FROM user u
   LEFT JOIN activity_type at ON u.id = at.creator_id
   GROUP BY u.id, u.username
   HAVING activity_type_count = 0;
   ```

### After Running the Fix

1. **Verify Constraint Removal**:
   ```sql
   SELECT constraint_name, constraint_type, column_name
   FROM information_schema.table_constraints tc
   JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
   WHERE tc.table_schema = DATABASE() 
     AND tc.table_name = 'activity_type'
     AND tc.constraint_type = 'UNIQUE';
   ```

2. **Test Application Startup**: The initialization should complete without constraint violations

## Rollback Plan

If issues occur, you can:

1. **Manually re-add the constraint** (if needed for data integrity):
   ```sql
   ALTER TABLE activity_type ADD CONSTRAINT UK_activity_type_creator_order 
   UNIQUE (creator_id, order_num);
   ```

2. **Check application behavior** and adjust initialization logic if needed

## Prevention

To prevent similar issues in the future:

1. **Use Descriptive Constraint Names**: Always name constraints explicitly rather than relying on auto-generated names
2. **Test Migrations**: Test constraint removal migrations against actual database schema
3. **Robust Error Handling**: Ensure initialization code handles constraint violations gracefully
4. **Database Schema Documentation**: Maintain clear documentation of intended constraints

## Files Modified

- `src/main/resources/db/migration/V3__Fix_Activity_Type_Unique_Constraint.sql` - New migration to remove constraint
- `src/main/java/com/danielagapov/spawn/Config/ActivityTypeInitializer.java` - Enhanced error handling
- `scripts/diagnose-activity-type-constraints.sql` - Diagnostic script for future troubleshooting 