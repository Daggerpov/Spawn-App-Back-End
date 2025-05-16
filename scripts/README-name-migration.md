# Name Field Migration

This migration combines the previously separate `first_name` and `last_name` fields into a single `name` field across the application.

## Database Changes

The `combine-names-migration.sql` script performs the following operations:

1. Adds a new `name` column to the `user` table
2. Populates the `name` column by combining `first_name` and `last_name` values
3. Creates a new index on the `name` column
4. Drops the old indexes for `first_name` and `last_name`
5. Drops the old `first_name` and `last_name` columns

## Running the Migration

Execute the migration script with:

```bash
mysql -u [username] -p [database_name] < scripts/combine-names-migration.sql
```

## Code Changes

The following components were updated:

### Backend (Java)
- Updated the `User` entity to use a single `name` field
- Modified all DTOs (`AbstractUserDTO`, `UserCreationDTO`, `BaseUserDTO`, etc.) to use `name` instead of `firstName`/`lastName`
- Updated the user search functionality to search on the `name` field
- Updated logging and utilities to display the full name

### Frontend (iOS)
- Updated the `Nameable` protocol to have a single `name` property
- Modified `UserDTO` and related models to use `name`
- Removed the `formatName` method that previously combined first and last name
- Updated registration and profile editing forms to have a single name input instead of separate first/last name inputs

## Verification

After running the migration:

1. Verify that existing users display properly in the app
2. Test creating a new user with just a name field
3. Test editing a user's profile
4. Verify that user search functionality works correctly 