# Database Indexes Documentation

## Overview

This document explains all database indexes in the Spawn application, their purpose, and how they improve query performance. Indexes are critical for maintaining fast query response times as the database grows.

## Index Strategy

Our indexing strategy focuses on:
1. **Authentication & User Lookup**: Fast user identification via email, username, and phone
2. **Feed Generation**: Efficient activity retrieval for user feeds
3. **Relationship Queries**: Quick friend and participation lookups
4. **Time-Based Queries**: Fast filtering by activity dates and creation times

---

## Entity Indexes

### 1. User Entity

**Table**: `user`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_name` | `name` | Single | User search by name |
| `idx_email` | `email` | Single | Authentication and user lookup |
| `idx_username` | `username` | Single | Authentication and user lookup |
| `idx_phone_number` | `phoneNumber` | Single | Contact matching and user search |

#### Why These Indexes?

**Email Index (`idx_email`)**
- **Used By**: `findByEmail()`, OAuth authentication, login flows
- **Query Pattern**: `SELECT * FROM user WHERE email = ?`
- **Impact**: Critical for authentication speed. Without this index, every login would scan all users.
- **Frequency**: Very high - every login, user lookup, and OAuth flow

**Username Index (`idx_username`)**
- **Used By**: `findByUsername()`, profile lookups, user search
- **Query Pattern**: `SELECT * FROM user WHERE username = ?`
- **Impact**: Enables instant username lookups and prevents duplicate username checks from being slow
- **Frequency**: High - profile views, username availability checks

**Phone Number Index (`idx_phone_number`)**
- **Used By**: `findByPhoneNumber()`, `findByPhoneNumberIn()`, contact import
- **Query Pattern**: `SELECT * FROM user WHERE phoneNumber IN (?, ?, ...)`
- **Impact**: Essential for contact matching when users import their phone contacts
- **Frequency**: Medium - mainly during contact import and friend discovery

**Name Index (`idx_name`)**
- **Used By**: User search, fuzzy matching
- **Query Pattern**: `SELECT * FROM user WHERE name LIKE ?`
- **Impact**: Speeds up user search functionality
- **Frequency**: Medium - user search operations

---

### 2. Activity Entity

**Table**: `activity`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_creator_id` | `creator_id` | Single | Find activities by creator |
| `idx_start_time` | `startTime` | Single | Time-based filtering |
| `idx_end_time` | `endTime` | Single | Past activity detection |
| `idx_last_updated` | `last_updated` | Single | Recent activity sorting |
| `idx_created_at` | `created_at` | Single | Activity age calculations |
| `idx_activity_type_id` | `activity_type_id` | Single | Activity type filtering |
| `idx_participant_limit` | `participant_limit` | Single | Capacity-based queries |

#### Why These Indexes?

**Creator ID Index (`idx_creator_id`)**
- **Used By**: `findByCreatorId()`, `findByCreatorIdIn()`, calendar views
- **Query Pattern**: `SELECT * FROM activity WHERE creator_id = ?`
- **Impact**: **CRITICAL** - Used in every user's feed and calendar view
- **Frequency**: Extremely high - every feed load, calendar view
- **Example**: When loading a user's calendar, finds all activities they created

**Start Time Index (`idx_start_time`)**
- **Used By**: Calendar date filtering, feed sorting, past activity queries
- **Query Pattern**: `SELECT * FROM activity WHERE startTime BETWEEN ? AND ?`
- **Impact**: Enables fast date range queries for calendar views
- **Frequency**: Very high - every calendar load with date filters
- **Example**: "Show me activities between June 1 and June 30"

**End Time Index (`idx_end_time`)**
- **Used By**: `getPastActivitiesWhereUserInvited()`, expired activity detection
- **Query Pattern**: `SELECT * FROM activity WHERE endTime < NOW()`
- **Impact**: Fast filtering of past vs. upcoming activities
- **Frequency**: High - feed generation, activity cleanup
- **Example**: Filtering out past activities from the feed

**Last Updated Index (`idx_last_updated`)**
- **Used By**: `findTopByCreatorIdOrderByLastUpdatedDesc()`, activity sorting
- **Query Pattern**: `SELECT * FROM activity WHERE creator_id = ? ORDER BY last_updated DESC`
- **Impact**: Efficient "most recently modified" queries
- **Frequency**: Medium - cache invalidation, activity updates
- **Example**: Finding the most recently updated activity for cache busting

**Created At Index (`idx_created_at`)**
- **Used By**: Activity aging calculations, expiration logic
- **Query Pattern**: `SELECT * FROM activity WHERE created_at < ?`
- **Impact**: Helps determine activity staleness and expiration
- **Frequency**: Medium - background cleanup jobs

**Activity Type ID Index (`idx_activity_type_id`)**
- **Used By**: Activity type filtering, categorization
- **Query Pattern**: `SELECT * FROM activity WHERE activity_type_id = ?`
- **Impact**: Fast filtering by activity type (sports, dining, etc.)
- **Frequency**: Medium - type-specific queries
- **Added Via**: Migration V5__Add_Activity_Type_Id_To_Activity.sql

**Participant Limit Index (`idx_participant_limit`)**
- **Used By**: Capacity-based filtering
- **Query Pattern**: `SELECT * FROM activity WHERE participant_limit IS NOT NULL`
- **Impact**: Finding activities with limited capacity
- **Frequency**: Low-Medium
- **Added Via**: Migration V12__Add_Participant_Limit_To_Activity.sql

---

### 3. ActivityUser Entity (Join Table)

**Table**: `activity_user`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_activity_id` | `activity_id` | Single | Find users for an activity |
| `idx_user_id` | `user_id` | Single | Find activities for a user |
| `idx_status` | `status` | Single | Filter by participation status |
| `idx_user_status` | `user_id, status` | Composite | User's activities by status |
| `idx_activity_status` | `activity_id, status` | Composite | Activity's users by status |

#### Why These Indexes?

**Activity ID Index (`idx_activity_id`)**
- **Used By**: `findByActivity_Id()`, `findByActivity_IdAndStatus()`
- **Query Pattern**: `SELECT * FROM activity_user WHERE activity_id = ?`
- **Impact**: Find all participants/invitees for an activity
- **Frequency**: Very high - every activity detail view

**User ID Index (`idx_user_id`)**
- **Used By**: `findByUser_Id()`, `findByUser_IdAndStatus()`
- **Query Pattern**: `SELECT * FROM activity_user WHERE user_id = ?`
- **Impact**: Find all activities a user is involved in
- **Frequency**: Very high - feed generation

**Status Index (`idx_status`)**
- **Used By**: General status filtering
- **Query Pattern**: `SELECT * FROM activity_user WHERE status = 'participating'`
- **Impact**: Broad status-based queries
- **Frequency**: Medium

**User-Status Composite Index (`idx_user_status`)**
- **Used By**: `findByUser_IdAndStatus()` - **MOST CRITICAL QUERY**
- **Query Pattern**: `SELECT * FROM activity_user WHERE user_id = ? AND status = 'participating'`
- **Impact**: **CRITICAL** - Powers feed generation for activities user is participating in or invited to
- **Frequency**: Extremely high - every feed load
- **Example**: "Show me all activities I'm participating in"
- **Why Composite**: MySQL can use this single index for both `WHERE user_id = ?` and `WHERE user_id = ? AND status = ?`

**Activity-Status Composite Index (`idx_activity_status`)**
- **Used By**: `findByActivity_IdAndStatus()`, participant lists
- **Query Pattern**: `SELECT * FROM activity_user WHERE activity_id = ? AND status = 'participating'`
- **Impact**: Fast retrieval of participants vs. invitees for an activity
- **Frequency**: Very high - activity detail views, participant counts
- **Example**: "Show me who's participating vs. who's just invited"

---

### 4. Friendship Entity

**Table**: `friendship`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_user_a_id` | `user_a_id` | Single | Find friendships for user A |
| `idx_user_b_id` | `user_b_id` | Single | Find friendships for user B |

#### Why These Indexes?

**User A Index (`idx_user_a_id`)**
- **Used By**: Friend list queries, friendship checks
- **Query Pattern**: `SELECT * FROM friendship WHERE user_a_id = ?`
- **Impact**: Fast friend list retrieval
- **Frequency**: High - friend list views, permission checks

**User B Index (`idx_user_b_id`)**
- **Used By**: Bidirectional friendship queries
- **Query Pattern**: `SELECT * FROM friendship WHERE user_b_id = ?`
- **Impact**: Enables efficient bidirectional friendship lookups
- **Frequency**: High - friend list views
- **Note**: Both indexes are needed because friendship is bidirectional

---

### 5. FriendRequest Entity

**Table**: `friend_request`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_sender_id` | `sender_id` | Single | Find requests sent by user |
| `idx_receiver_id` | `receiver_id` | Single | Find requests received by user |
| `idx_created_at` | `created_at` | Single | Sort by request age |

#### Why These Indexes?

**Sender ID Index (`idx_sender_id`)**
- **Used By**: Outgoing friend request queries
- **Query Pattern**: `SELECT * FROM friend_request WHERE sender_id = ?`
- **Impact**: Show user their pending sent requests
- **Frequency**: Medium - friend request management

**Receiver ID Index (`idx_receiver_id`)**
- **Used By**: Incoming friend request queries
- **Query Pattern**: `SELECT * FROM friend_request WHERE receiver_id = ?`
- **Impact**: **PRIMARY** - Show user their pending friend requests (most common)
- **Frequency**: High - notification badge, friend request list

**Created At Index (`idx_created_at`)**
- **Used By**: Request sorting, expiration logic
- **Query Pattern**: `SELECT * FROM friend_request ORDER BY created_at DESC`
- **Impact**: Sort requests by age, identify stale requests
- **Frequency**: Medium - sorted friend request lists

---

### 6. ShareLink Entity

**Table**: `share_link`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_share_code` | `shareCode` | Unique | Lookup by share code |
| `idx_target_id_type` | `targetId, type` | Composite | Find links for entity |
| `idx_expires_at` | `expiresAt` | Single | Expiration cleanup |

#### Why These Indexes?

**Share Code Index (`idx_share_code`)**
- **Used By**: Share link resolution (e.g., "spawn.app/join/happy-dolphin")
- **Query Pattern**: `SELECT * FROM share_link WHERE shareCode = ?`
- **Impact**: **CRITICAL** - Every share link click needs instant resolution
- **Frequency**: High - every shared link access
- **Unique**: Enforces one code per link

**Target ID + Type Composite Index (`idx_target_id_type`)**
- **Used By**: Finding existing share links for activities/profiles
- **Query Pattern**: `SELECT * FROM share_link WHERE targetId = ? AND type = 'ACTIVITY'`
- **Impact**: Prevents duplicate share codes, finds existing links
- **Frequency**: Medium - share link creation/retrieval

**Expires At Index (`idx_expires_at`)**
- **Used By**: Cleanup jobs, expiration checking
- **Query Pattern**: `SELECT * FROM share_link WHERE expiresAt < NOW()`
- **Impact**: Efficient expired link cleanup
- **Frequency**: Low - background jobs

---

### 7. EmailVerification Entity

**Table**: `email_verification`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_email_verification_user_id` | `user_id` | Single | Find verification by user |
| `idx_email_verification_email` | `email` | Single | Find verification by email |

#### Why These Indexes?

**User ID Index (`idx_email_verification_user_id`)**
- **Used By**: User verification status lookups
- **Query Pattern**: `SELECT * FROM email_verification WHERE user_id = ?`
- **Impact**: Fast verification status checks during registration
- **Frequency**: High - registration flow

**Email Index (`idx_email_verification_email`)**
- **Used By**: `findByEmail()`, verification code validation
- **Query Pattern**: `SELECT * FROM email_verification WHERE email = ?`
- **Impact**: **CRITICAL** - Used during email verification flow
- **Frequency**: High - every verification attempt

---

### 8. UserIdExternalIdMap Entity (OAuth)

**Table**: `user_id_external_id_map`

#### Indexes:
| Index Name | Columns | Type | Purpose |
|------------|---------|------|---------|
| `idx_oauth_provider` | `provider` | Single | Find mappings by provider |
| `idx_oauth_user_provider` | `user_id, provider` | Composite | User's OAuth connections |

#### Why These Indexes?

**Provider Index (`idx_oauth_provider`)**
- **Used By**: Provider-specific queries
- **Query Pattern**: `SELECT * FROM user_id_external_id_map WHERE provider = 'GOOGLE'`
- **Impact**: Find all users using a specific OAuth provider
- **Frequency**: Low-Medium - analytics, provider-specific operations

**User-Provider Composite Index (`idx_oauth_user_provider`)**
- **Used By**: OAuth authentication, account linking
- **Query Pattern**: `SELECT * FROM user_id_external_id_map WHERE user_id = ? AND provider = ?`
- **Impact**: Fast OAuth authentication lookup
- **Frequency**: High - every OAuth login

---

## Performance Impact

### Critical Indexes (Must Have)
These indexes are essential for basic application functionality:
1. **`User.idx_email`** - Authentication would be extremely slow without this
2. **`User.idx_username`** - Profile lookups and auth fallback
3. **`Activity.idx_creator_id`** - Feed and calendar generation
4. **`ActivityUser.idx_user_status`** - Feed generation (participating activities)
5. **`ActivityUser.idx_activity_status`** - Participant lists
6. **`ShareLink.idx_share_code`** - Share link resolution

### High-Impact Indexes (Strongly Recommended)
These significantly improve performance:
1. **`Activity.idx_start_time`** - Calendar date filtering
2. **`Activity.idx_end_time`** - Past activity filtering
3. **`FriendRequest.idx_receiver_id`** - Pending request notifications
4. **`EmailVerification.idx_email`** - Verification flow
5. **`User.idx_phone_number`** - Contact import

### Medium-Impact Indexes (Optimization)
These help with specific features:
1. **`Activity.idx_last_updated`** - Cache invalidation
2. **`FriendRequest.idx_created_at`** - Request sorting
3. **`ShareLink.idx_expires_at`** - Cleanup jobs

---

## Index Maintenance

### Automatic Creation
These indexes are automatically created by Hibernate when:
- The application starts with `spring.jpa.hibernate.ddl-auto=update`
- Deploying to a fresh database
- The entity annotations are processed

### Manual Creation (Migrations)
Some indexes were added via SQL migrations:
- V5: Email verification indexes
- V5: Activity type index
- V11: OAuth mapping indexes
- V12: Participant limit index

### Monitoring
To check index usage in production:
```sql
-- Show all indexes on a table
SHOW INDEX FROM activity;

-- Check index usage statistics (MySQL 5.7+)
SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'spawn_db';

-- Analyze query performance
EXPLAIN SELECT * FROM activity WHERE creator_id = 'some-uuid';
```

---

## Query Optimization Examples

### Example 1: Feed Generation (Before vs. After)

**Without Indexes:**
```sql
-- Full table scan on activity_user (slow with 100K+ rows)
SELECT * FROM activity_user WHERE user_id = ? AND status = 'participating';
-- Then full table scan on activity for each result
SELECT * FROM activity WHERE id = ?;
```
**Time**: ~500ms for user with 50 activities

**With Indexes (`idx_user_status`, `idx_creator_id`):**
```sql
-- Index seek on idx_user_status (instant)
SELECT * FROM activity_user WHERE user_id = ? AND status = 'participating';
-- Primary key lookups (instant)
SELECT * FROM activity WHERE id = ?;
```
**Time**: ~10ms for same query

### Example 2: Authentication (Before vs. After)

**Without Index on `user.email`:**
```sql
-- Full table scan (checks every user)
SELECT * FROM user WHERE email = 'user@example.com';
```
**Time**: ~200ms with 100K users

**With `idx_email`:**
```sql
-- Index seek (direct lookup)
SELECT * FROM user WHERE email = 'user@example.com';
```
**Time**: ~2ms

---

## Composite Index Details

### Why Composite Indexes?

A composite index on `(user_id, status)` can satisfy these queries efficiently:
1. `WHERE user_id = ?` (uses leftmost prefix)
2. `WHERE user_id = ? AND status = ?` (uses full index)

But NOT:
- `WHERE status = ?` (would need separate index on status)

### Our Composite Indexes:
1. **`activity_user(user_id, status)`** - Most common feed query pattern
2. **`activity_user(activity_id, status)`** - Participant list pattern
3. **`share_link(targetId, type)`** - Entity share link lookup
4. **`user_id_external_id_map(user_id, provider)`** - OAuth lookup

---

## Best Practices

### Do's ✅
- Index foreign keys that are frequently joined
- Index columns used in WHERE, ORDER BY, and JOIN clauses
- Use composite indexes for common multi-column queries
- Monitor index usage and remove unused indexes

### Don'ts ❌
- Don't index everything (indexes slow down writes)
- Don't index low-cardinality columns (e.g., boolean with only 2 values)
- Don't create duplicate indexes (e.g., index on `user_id` when `(user_id, status)` exists)
- Don't forget to update this doc when adding new indexes!

---

## Future Considerations

### Potential Additional Indexes
As the application grows, consider:
1. **Full-text indexes** on `activity.title` and `activity.note` for search
2. **Spatial indexes** on `location` coordinates for proximity searches
3. **Covering indexes** that include commonly selected columns to avoid table lookups

### Index Bloat
Monitor index size as data grows:
```sql
SELECT 
    table_name,
    index_name,
    ROUND(stat_value * @@innodb_page_size / 1024 / 1024, 2) AS size_mb
FROM mysql.innodb_index_stats
WHERE database_name = 'spawn_db'
ORDER BY stat_value DESC;
```

---

## Summary

Our database has **26 indexes** across 8 entities, carefully chosen to optimize the most common query patterns:
- **Authentication**: Email, username, phone lookups
- **Feed Generation**: User activities, participation status
- **Calendar**: Date-based activity filtering
- **Social**: Friend relationships, friend requests
- **Sharing**: Share code resolution

These indexes ensure the application remains fast and responsive even as the user base grows to millions of users and activities.

