# User Logging Implementation Guide

This document outlines how to implement consistent user information logging throughout the Spawn backend codebase.

## Overview

We've created a utility class `LoggingUtils` that provides consistent formatting for user information in log messages. All services that perform logging related to users should use this utility class to ensure that logs include user's first name, last name, and username whenever a user ID is mentioned.

## The LoggingUtils Class

The `LoggingUtils` class provides the following methods:

- `formatUserInfo(User user)`: Formats user information including ID, first name, last name, and username
- `formatUserIdInfo(UUID userId)`: Formats user ID information when the full user object isn't available

## Implementation Steps

### Step 1: Use LoggingUtils for User-Related Log Messages

Find all log messages that include user IDs and update them to use the appropriate LoggingUtils method:

1. For logs that already have access to the User entity:
   ```java
   // Before
   logger.info("User action for user: " + userId);
   
   // After
   logger.info("User action for user: " + LoggingUtils.formatUserInfo(user));
   ```

2. For logs that only have access to the user ID:
   ```java
   // Before
   logger.error("Error processing for user: " + userId);
   
   // After
   logger.error("Error processing for user: " + LoggingUtils.formatUserIdInfo(userId));
   ```

3. For informational logs, consider fetching the User entity first:
   ```java
   // Before
   logger.info("Processing data for user ID: " + userId);
   
   // After
   User user = userService.getUserEntityById(userId);
   logger.info("Processing data for user: " + LoggingUtils.formatUserInfo(user));
   ```

### Step 2: Update Error Handling

When an exception occurs, make sure to include detailed user information in the error logs:

```java
try {
    // User-related operations
} catch (Exception e) {
    logger.error("Error processing for user: " + LoggingUtils.formatUserInfo(user) + ": " + e.getMessage());
    throw e;
}
```

If the User entity isn't available in the catch block:

```java
try {
    // User-related operations
} catch (Exception e) {
    logger.error("Error processing for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
    throw e;
}
```

### Step 3: Update Services

The following services should be updated to use the LoggingUtils class:

- UserService
- AuthService
- FriendRequestService
- EventService
- BlockedUserService
- JWTService
- OAuthService
- CleanUnverifiedService

## Example Implementation

See NotificationService.java for a complete example of how to implement these changes. Key points:

1. Import the utils class:
   ```java
   import com.danielagapov.spawn.Utils.LoggingUtils;
   ```

2. Replace direct user ID logging:
   ```java
   // Before
   logger.info("Getting data for user: " + userId);
   
   // After
   logger.info("Getting data for user: " + LoggingUtils.formatUserInfo(user));
   ```

3. In error handling:
   ```java
   // Before
   catch (Exception e) {
       logger.error("Error processing for user " + userId + ": " + e.getMessage());
       throw e;
   }
   
   // After
   catch (Exception e) {
       logger.error("Error processing for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
       throw e;
   }
   ```

## Testing

After implementing these changes:

1. Run the application and test various user operations
2. Check the logs to confirm that user information (ID, first name, last name, username) appears consistently
3. Verify that error logs include detailed user information

## Additional Considerations

- For performance-critical sections, you may want to avoid fetching the User entity solely for logging purposes
- In such cases, use LoggingUtils.formatUserIdInfo() instead
- For batch operations involving multiple users, consider logging individual user details at the appropriate level 