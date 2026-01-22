# Scripts Directory

This directory contains all utility scripts for the Spawn App backend, organized by function.

## Directory Structure

### 📦 build/
Build scripts for compiling and packaging the application.
- `build.sh` - Standard build script
- `build-with-java17.sh` - Build specifically with Java 17

### 💾 database/
Database management, migrations, and maintenance scripts.

#### Subdirectories:
- **cleanup-scripts/** - SQL scripts for cleaning up data
  - `clean-null-id-friend-requests.sql`
  - `clean-orphaned-chat-messages.sql`
  - `oauth-data-consistency-cleanup.sql`
  - `test-user-data-cleanup.sql`
  - `wipe_db.sql`

- **constraint-fixes/** - Database constraint fixes and diagnostics
  - `ACTIVITY_TYPE_CONSTRAINT_FIX.md`
  - `diagnose-activity-type-constraints.sql`
  - `README-ActivityType-Constraint-Fix.md`
  - `remove-activity-type-constraint.sql`

- **database-migrations/** - Database schema migration scripts
  - `clean-phone-numbers-migration.sql`
  - `combine-names-migration.sql`
  - `fix-missing-date-created.sql`
  - `initialize_event_last_updated.sql`
  - `initialize_last_updated.sql`
  - `update_event_icon_defaults.sql`

- **validation-tools/** - Tools for validating database queries
  - `README-JPQL-Validation.md`
  - `validate-jpql-syntax.sh`

#### Root Files:
- `diagnose_user_constraint_issue.sql` - Diagnostic query for user constraints

### 🗄️ cache/
Cache management and maintenance scripts.
- `clear-activity-type-cache.sh` - Clear activity type cache
- `clear-corrupted-caches.sh` - Clear corrupted cache entries

### 🚀 deployment/
Deployment and production startup scripts.
- `start-production.sh` - Production environment startup script

### 📊 monitoring/
System monitoring and analysis scripts.
- `analyze-ram-logs.sh` - Analyze RAM usage logs
- `monitor-ram.sh` - Real-time RAM monitoring

### 🔧 legacy-fixes/
Legacy scripts from previous refactoring efforts. These scripts were used during codebase restructuring and are kept for reference.
- Various import fix scripts
- Package update scripts
- Subdirectory fix scripts

> **Note:** Scripts in `legacy-fixes/` are kept for historical reference and should generally not be needed for ongoing development.

## Usage Guidelines

1. **Build Scripts**: Run from the project root directory
   ```bash
   ./scripts/build/build.sh
   ```

2. **Database Scripts**: Review before executing, especially cleanup scripts
   ```bash
   # Always back up your database first!
   psql -U username -d database_name -f scripts/database/cleanup-scripts/script.sql
   ```

3. **Monitoring Scripts**: Can be run in the background
   ```bash
   ./scripts/monitoring/monitor-ram.sh &
   ```

4. **Cache Scripts**: Safe to run when cache issues occur
   ```bash
   ./scripts/cache/clear-corrupted-caches.sh
   ```

## Best Practices

- Always review SQL scripts before running in production
- Keep scripts executable: `chmod +x script-name.sh`
- Document new scripts in this README
- Place new scripts in the appropriate subdirectory
- Include error handling and logging in new scripts
- Test scripts in development before production use



