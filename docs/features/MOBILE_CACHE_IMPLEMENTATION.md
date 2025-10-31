## Mobile Cache Implementation

The app implements client-side caching to improve performance and reduce API calls. The caching system includes:

1. **AppCache Singleton**: A centralized cache store that persists data to disk and provides reactive updates
2. **Cache Validation API**: A backend API endpoint that validates cached data and informs the client when to refresh
3. **Push Notification Support**: Real-time updates when data changes server-side

### Overview

The Spawn App iOS client implements a sophisticated caching mechanism to reduce API calls, speed up the app's responsiveness, and provide a better user experience. This is achieved through:

1. **Client-side caching:** Storing frequently accessed data locally
2. **Cache invalidation:** Checking with the backend to determine if cached data is stale
3. **Push notifications:** Receiving real-time updates when relevant data changes

#### Cached Data Types

The app caches several types of data to enhance performance:

1. **Friends List**: A user's complete friends list
2. **Activities**: Activities the user created or was invited to
3. **Profile Pictures**: Both the user's own profile picture and those of friends
4. **Recommended Friends**: Potential friends recommended by the system
5. **Friend Requests**: Pending friend requests
6. **Tags**: User-created friend tags
7. **Tagged Friends**: Friends categorized in each tag

#### Special Considerations

- **User Blocking**: When a user blocks another user, relevant caches are invalidated to ensure they don't appear in each other's recommended friends lists, friend lists, or other profile views
- **Activity Creation**: New Activities trigger cache invalidation for invited users

#### Cache Validation API

The app makes a request to `/api/v1/cache/validate/:userId` on startup, sending a list of cached items and their timestamps:

```json
{
  "friends": "2025-04-01T10:00:00Z",
  "Activities": "2025-04-01T10:10:00Z",
  "profilePicture": "2025-04-01T10:15:00Z",
  "otherProfiles": "2025-04-01T10:20:00Z",
  "recommendedFriends": "2025-04-01T10:25:00Z",
  "friendRequests": "2025-04-01T10:30:00Z",
  "userTags": "2025-04-01T10:35:00Z",
  "tagFriends": "2025-04-01T10:40:00Z"
}
```

The backend responds with which items need to be refreshed:

```json
{
  "friends": {
    "invalidate": true,
    "updatedItems": [...] // Optional
  },
  "Activities": {
    "invalidate": false
  },
  "profilePicture": {
    "invalidate": true,
    "updatedItems": [...] // Optional
  }
}
```

#### Push Notification Handling

The app listens for push notifications with specific types that indicate data changes:

- `friend-accepted`: When a friend request is accepted
- `friend-blocked`: When a user is blocked
- `Activity-updated`: When an Activity is updated
- `Activity-created`: When a new Activity is created
- `profile-updated`: When a user's profile is updated
- `tag-updated`: When a user's tags are modified

When these notifications are received, the app refreshes the relevant cached data.

### How It Works

1. On app launch, `AppCache` loads cached data from disk
2. The app sends a request to validate the cache with the backend
3. For invalidated cache items:
    - If the backend provides updated data, it's used directly
    - Otherwise, the app fetches the data with a separate API call
4. As the user uses the app, they see data from the cache immediately
5. In the background, the app may update cache items based on push notifications

### Implementation Details

#### Data Flow

1. App loads cached data → UI renders immediately
2. App checks if cache is valid → Updates UI if needed
3. User interacts with fresh data → Great experience!

#### Benefits

- **Speed:** UI renders instantly from cache
- **Bandwidth:** Reduced API calls
- **Battery:** Less network activity
- **Offline Use:** Basic functionality without network

### Backend Implementation Requirements

The backend implements a cache validation endpoint at `/api/v1/cache/validate/:userId` that:

1. Receives a map of cache categories and their last update timestamps
2. Compares these timestamps to when data was last modified on the server
3. Responds with which categories need refreshing and optionally includes updated data
4. Sends push notifications when relevant data changes