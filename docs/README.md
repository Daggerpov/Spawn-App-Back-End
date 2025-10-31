# Documentation Organization

This directory contains all documentation for the Spawn App backend, organized into logical categories for better maintainability and discoverability.

## Directory Structure

### `/docs/implementation-guides/`
Contains detailed guides explaining how specific features and fixes were implemented:
- `OAUTH_CONCURRENCY_FIX_SUMMARY.md` - OAuth concurrency issue resolution
- `FUZZY_SEARCH_IMPLEMENTATION.md` - Fuzzy search algorithm implementation
- `PHONE_NUMBER_MATCHING_FIX.md` - Phone number matching improvements

### `/docs/migration-scripts/`
Contains documentation for data migration procedures:
- `README-name-migration.md` - Guide for name field migration

### `/docs/database-fixes/`
Contains SQL scripts and documentation for database issue resolution:
- `diagnose_user_constraint_issue.sql` - User constraint diagnostic script

### `/docs/guides/`
General setup and operational guides (linked from main README)

### `/docs/` (Root Level Documentation)
Performance optimization and system-level documentation:
- `RAM_OPTIMIZATION_README.md` - **Main entry point** for RAM optimization documentation
- `RAM_OPTIMIZATION_STRATEGIES.md` - Comprehensive RAM reduction strategies (all phases)
- `QUICK_START_RAM_OPTIMIZATION.md` - Step-by-step implementation guide (Phase 1)
- `RAM_OPTIMIZATION_IMPACT_SUMMARY.md` - Expected impact and metrics analysis
- `PERFORMANCE_OPTIMIZATION_SUMMARY.md` - General performance improvements
- `MOBILE_CACHE_IMPLEMENTATION.md` - Mobile caching strategy
- `AUTH_FLOW_FIXES_SUMMARY.md` - Authentication flow improvements
- `FRIENDSHIP_REFACTOR_PLAN.md` - Friendship system refactoring

## Scripts Organization

### `/scripts/database-migrations/`
SQL scripts for schema changes and data migrations:
- Database initialization scripts
- Column addition and modification scripts
- Data migration scripts

### `/scripts/cleanup-scripts/`
Scripts for cleaning up data and removing orphaned records:
- Chat message cleanup
- User data cleanup
- Database wiping utilities

### `/scripts/constraint-fixes/`
Scripts and documentation for fixing database constraints:
- Activity type constraint fixes
- Constraint diagnostic tools
- Constraint removal scripts

### `/scripts/validation-tools/`
Tools for validating code and database integrity:
- JPQL validation tools
- Syntax checking scripts

### `/scripts/` (Root Level Scripts)
System monitoring and optimization tools:
- `monitor-ram.sh` - Real-time RAM usage monitoring with CSV logging
- `analyze-ram-logs.sh` - Analyze and compare RAM monitoring logs
- `start-production.sh` - Production startup script with JVM optimizations (to be created)

## Best Practices

1. **Documentation Placement**: Place implementation guides in `docs/implementation-guides/`
2. **Script Organization**: Categorize scripts by their primary function
3. **Naming Conventions**: Use descriptive names that indicate the script's purpose
4. **Version Control**: Keep documentation alongside code changes in version control

## Tech Debt Resolution

✅ **File Organization**: Moved scattered documentation files to structured directories  
✅ **Scripts Organization**: Categorized scripts into logical subdirectories  
✅ **Naming Consistency**: Verified Util directory naming is consistent throughout codebase  
✅ **Calendar Structure**: Confirmed Calendar services and controllers are properly organized  

## Calendar Directory Structure (Verified Correct)

The Calendar functionality is properly organized across layers:

**Services Layer**: `src/main/java/com/danielagapov/spawn/Services/Calendar/`
- `CalendarService.java` - Main calendar business logic
- `ICalendarService.java` - Calendar service interface
- `CalendarEventHandler.java` - Event handling for calendar updates

**Controller Layer**: `src/main/java/com/danielagapov/spawn/Controllers/User/Profile/CalendarController.java`
- Placed under User/Profile as calendar is a user profile feature
- Follows REST API patterns for calendar endpoints

This structure follows proper separation of concerns with service layer handling business logic and controller layer managing HTTP requests. 