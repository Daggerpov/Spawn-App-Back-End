## Friendship Refactor Plan

### Objective
- Replace friend-tag-based friendships with a first-class, symmetric `Friendship` model.
- Keep friend request APIs intact; acceptance creates friendships directly.
- Minimize API breakage; keep caches stable where possible.

### Current State (brief)
- Friendships inferred via `FriendTag` (“Everyone”) and `UserFriendTag`.
- `FriendRequestService.acceptFriendRequest` → `UserService.saveFriendToUser` → adds each user to other’s “Everyone” tag.
- Feeds and mutuals derive from tag-based friends; activity filters by friend tag.

### Target State
- Direct `Friendship` table, 1 row per pair, undirected.
- `UserService` friendship methods use `Friendship` repo (signatures unchanged).
- Feed filters become friend-based (drop tag filter and tag color).

### Data Model
- New table: `friendship`
  - Columns: `id` UUID, `user_a_id` UUID, `user_b_id` UUID, `created_at` TIMESTAMP.
  - Enforce canonical ordering (a < b) and unique `(user_a_id, user_b_id)`.
  - Indexes on `user_a_id`, `user_b_id`.
- Deprecate friendship use of `FriendTag`/`UserFriendTag`. Remove “Everyone” later.

### Services/Repos
- New `IFriendshipRepository`
  - `existsBetween(u1,u2)`, `findFriendIdsOf(userId)`, `findFriendsOf(userId)`,
    `countMutualFriends(u1,u2)`, `createIfAbsent(u1,u2)`, `deleteBetween(u1,u2)`.
- `UserService` (keep interface)
  - `saveFriendToUser` → create friendship (no tags).
  - `isUserFriendOfUser` → via `existsBetween`.
  - `getFriendUserIdsByUserId`/`getFriendUsersByUserId` → via friendship.
  - Mutual count via friendship joins.
- `FriendRequestService`
  - No signature change; still calls `saveFriendToUser` and evicts caches.

### Controllers/APIs
- Keep friend-requests endpoints unchanged.
- Friends list endpoints unchanged (backed by friendship).
- Activities/Feed
  - Replace `getFilteredFeedActivitiesByFriendTagId` with friend-only filtering, keyed by `userId`.
  - Remove tag color in feed DTOs (use neutral/default if needed).

### Caching
- Keep: `friendsByUserId`, `incomingFetchFriendRequests`, `sentFetchFriendRequests`, `recommendedFriends`.
- Change keying for `filteredFeedActivities` from friendTagId → userId (or `userId:friendOnly`).
- Remove: `ActivitiesByFriendTagId`, `ActivitiesInvitedToByFriendTagId`.

### Migration (Flyway)
- VXX__Create_Friendship_Table.sql
  - Create table, FKs, unique constraint, indexes.
- VXX__Backfill_Friendships_From_UserFriendTag.sql
  - Insert distinct pairs from `user_friend_tag` joined to `friend_tag` where `is_everyone = true`, using `(LEAST(owner_id,user_id), GREATEST(...))`.
- VXX__Cleanup_Tag_Based_Friendship.sql (later)
  - Remove “Everyone” usage; optionally drop `user_friend_tag`/`friend_tag` if no other dependencies remain.

### Feature Impacts
- Friends list/mutuals/recommendations: re-backed by friendship, interfaces unchanged.
- Feed: becomes friend-only filter; tag-filter and tag color removed.
- Invites: if tag-based bulk-invite existed, defer or reintroduce as user-defined lists (separate from friendship).

### Rollout
- Phase 1: Add friendship + backfill; switch `UserService` to friendship; keep tag tables in place; update feed to friend-based.
- Phase 2: Remove tag-based code paths/caches; drop “Everyone”; optional table cleanup.

### Risks
- Duplicate creation under concurrency → unique constraint + upsert/no-op.
- Backfill correctness → verify counts equal distinct friend pairs.
- Feed behavior change → coordinate mobile to use friend-only filter.

### Tests
- Unit: `FriendRequestService` acceptance creates friendship; `UserService` friend queries/mutuals; recommendations.
- Integration: create FR → accept → friendship exists; friends list/mutuals correct; feed returns friend-only items.
- Performance: validate joins on large graphs; adjust indexes if needed.

### Out of Scope (now)
- User-defined lists to replace advanced tag workflows.
- Restoring per-tag colors in feed.