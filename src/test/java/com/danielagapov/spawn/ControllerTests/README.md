# Controller Integration Tests

This directory contains comprehensive integration tests for all REST API controllers in the Spawn application. These tests use Spring Boot's `@SpringBootTest` and `MockMvc` to perform end-to-end testing of the HTTP endpoints.

## Test Configuration

### Base Test Configuration
- **BaseIntegrationTest**: Abstract base class providing common configuration and utilities
- **Application Profile**: Uses `test` profile with H2 in-memory database
- **Test Configuration**: Located in `src/test/resources/application-test.properties`

### Test Database
- H2 in-memory database for isolated testing
- Database schema created and dropped for each test
- No external dependencies required

## Test Classes

### 1. AuthControllerIntegrationTest
Tests authentication and authorization endpoints:
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/auth/sign-in` - OAuth sign-in
- `POST /api/v1/auth/make-user` - OAuth user creation
- `POST /api/v1/auth/refresh-token` - Token refresh
- `POST /api/v1/auth/change-password` - Password change
- `GET /api/v1/auth/quick-sign-in` - Quick authentication
- `GET /api/v1/auth/verify-email` - Email verification
- `GET /api/v1/auth/test-email` - Test email sending (deprecated)

**Coverage**: Registration validation, login authentication, OAuth flows, token management, password changes, email verification

### 2. UserControllerIntegrationTest
Tests user management endpoints:
- `GET /api/v1/users/{id}` - Get user by ID
- `DELETE /api/v1/users/{id}` - Delete user
- `GET /api/v1/users/friends/{id}` - Get user friends
- `GET /api/v1/users/recommended-friends/{id}` - Get recommended friends
- `PATCH /api/v1/users/update-pfp/{id}` - Update profile picture
- `GET /api/v1/users/default-pfp` - Get default profile picture
- `PATCH /api/v1/users/update/{id}` - Update user profile
- `GET /api/v1/users/filtered/{requestingUserId}` - Get filtered users
- `GET /api/v1/users/search` - Search users
- `GET /api/v1/users/{userId}/recent-users` - Get recent users
- `GET /api/v1/users/{userId}/is-friend/{potentialFriendId}` - Check friendship
- `POST /api/v1/users/s3/test-s3` - S3 upload test (deprecated)

**Coverage**: User CRUD operations, friend management, profile updates, search functionality, S3 integration

### 3. ActivityControllerIntegrationTest
Tests activity management endpoints:
- `GET /api/v1/Activities/user/{creatorUserId}` - Get user's activities (deprecated)
- `GET /api/v1/Activities/profile/{profileUserId}` - Get profile activities
- `GET /api/v1/Activities/friendTag/{friendTagFilterId}` - Get activities by friend tag
- `POST /api/v1/Activities` - Create activity
- `PUT /api/v1/Activities/{id}` - Update activity (deprecated)
- `DELETE /api/v1/Activities/{id}` - Delete activity
- `PUT /api/v1/Activities/{ActivityId}/toggleStatus/{userId}` - Toggle participation
- `GET /api/v1/Activities/feedActivities/{requestingUserId}` - Get feed activities
- `GET /api/v1/Activities/{id}` - Get activity by ID

**Coverage**: Activity CRUD operations, feed management, participation tracking, filtering by tags

### 4. FriendRequestControllerIntegrationTest
Tests friend request management:
- `GET /api/v1/friend-requests/incoming/{userId}` - Get incoming requests
- `POST /api/v1/friend-requests` - Create friend request
- `PUT /api/v1/friend-requests/{friendRequestId}` - Accept/reject requests

**Coverage**: Friend request lifecycle, validation, error handling

### 5. NotificationControllerIntegrationTest
Tests notification system endpoints:
- `POST /api/v1/notifications/device-tokens/register` - Register device token
- `DELETE /api/v1/notifications/device-tokens/unregister` - Unregister device token
- `GET /api/v1/notifications/preferences/{userId}` - Get notification preferences
- `POST /api/v1/notifications/preferences/{userId}` - Update preferences
- `GET /api/v1/notifications/notification` - Get notifications

**Coverage**: Push notification setup, preference management, device token handling

### 6. ChatMessageControllerIntegrationTest
Tests chat messaging endpoints:
- `POST /api/v1/chatMessages` - Create chat message
- `DELETE /api/v1/chatMessages/{id}` - Delete message (deprecated)
- `POST /api/v1/chatMessages/{chatMessageId}/likes/{userId}` - Like message (deprecated)
- `GET /api/v1/chatMessages/{chatMessageId}/likes` - Get message likes (deprecated)
- `DELETE /api/v1/chatMessages/{chatMessageId}/likes/{userId}` - Unlike message (deprecated)

**Coverage**: Message CRUD operations, like system, validation

### 7. FeedbackSubmissionControllerIntegrationTest
Tests feedback management endpoints:
- `POST /api/v1/feedback` - Submit feedback
- `PUT /api/v1/feedback/resolve/{id}` - Resolve feedback
- `PUT /api/v1/feedback/in-progress/{id}` - Mark as in progress
- `PUT /api/v1/feedback/status/{id}` - Update status
- `GET /api/v1/feedback` - Get all feedback
- `DELETE /api/v1/feedback/delete/{id}` - Delete feedback

**Coverage**: Feedback lifecycle, status management, admin operations

### 8. CacheControllerIntegrationTest
Tests cache validation endpoints:
- `POST /api/v1/cache/validate/{userId}` - Validate cache timestamps

**Coverage**: Cache validation logic, timestamp handling, error scenarios

## Running the Tests

### Individual Test Classes
```bash
# Run specific test class
mvn test -Dtest=AuthControllerIntegrationTest

# Run all controller tests
mvn test -Dtest="*ControllerIntegrationTest"
```

### Maven Test Execution
```bash
# Run all tests
mvn test

# Run tests with specific profile
mvn test -Dspring.profiles.active=test
```

### IDE Execution
- All test classes can be run individually in any IDE
- Right-click on test class and select "Run Tests"
- Debug mode available for troubleshooting

## Test Data

### Mock Data
- Tests use UUID.randomUUID() for test IDs
- JSON strings for request bodies to avoid DTO constructor issues
- Mock authentication tokens via `createMockJwtToken()` helper

### Test Isolation
- Each test method is wrapped in `@Transactional` for automatic rollback
- H2 database is recreated for each test run
- No shared state between tests

## Error Scenarios Covered

### Common Error Cases
- **404 Not Found**: Non-existent resources
- **400 Bad Request**: Invalid input data, missing parameters
- **401 Unauthorized**: Invalid authentication
- **409 Conflict**: Duplicate data (e.g., duplicate username)
- **500 Internal Server Error**: Unexpected errors

### Validation Testing
- Null/empty required fields
- Invalid data formats
- Missing request parameters
- Malformed JSON requests

## Best Practices

### Test Structure
- Descriptive test method names with `@DisplayName`
- Clear test scenarios covering happy path and edge cases
- Consistent URL constants and test data setup

### Assertions
- Status code verification
- Response body validation using JSONPath
- Header presence checks for authentication tokens

### Maintainability
- Shared base class for common functionality
- Helper methods for repetitive operations
- Clear separation of concerns

## Configuration Files

### Test Properties
- `src/test/resources/application-test.properties` - Test-specific configuration
- H2 database configuration
- Disabled external services (Redis, Flyway, Email)
- Mock OAuth and APNS configurations

### Dependencies
- Spring Boot Test Starter
- MockMvc for HTTP testing
- H2 Database for testing
- JUnit 5 for test framework

## Future Enhancements

### Additional Controllers
Tests can be added for any missing controllers following the same patterns:
- FriendTagController
- BlockedUserController  
- ReportController
- BetaAccessSignUpController
- User Profile Controllers (Calendar, Interests, Social Media, Stats)

### Test Coverage
- Integration with code coverage tools
- Performance testing for endpoints
- Security testing for authentication
- API contract testing

### Test Data Management
- Test data builders for complex DTOs
- Fixture data for common test scenarios
- Database seeding for integration tests 