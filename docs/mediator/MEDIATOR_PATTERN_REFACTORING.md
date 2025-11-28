# Mediator Pattern Refactoring Plan

## Goal

Implement CQRS/MediatR-style mediator pattern for authentication and social modules using Spring's ApplicationEventPublisher, reducing tight coupling between controllers and services.

## Architecture Overview

### Core Components

1. **Mediator**: Central dispatcher using Spring's ApplicationEventPublisher
2. **Commands**: Represent write operations (create, update, delete)
3. **Queries**: Represent read operations (get, fetch, check)
4. **Handlers**: Process commands/queries and return results (extend existing services)
5. **Request/Response**: Base classes for type-safe messaging

### Modules in Scope

- **Authentication**: Login, registration, OAuth, email verification, password changes
- **Friend Requests**: Create, accept, reject, fetch incoming/sent requests
- **Blocked Users**: Block, unblock, check blocked status
- **Friendships**: Save/remove friendships (in UserService)

## Implementation Steps

### Phase 1: Core Mediator Infrastructure

**1.1 Create base request/response classes**

- `src/main/java/com/danielagapov/spawn/Mediator/IRequest.java` - Marker interface for all requests
- `src/main/java/com/danielagapov/spawn/Mediator/ICommand.java` - Extends IRequest for write ops
- `src/main/java/com/danielagapov/spawn/Mediator/IQuery.java` - Extends IRequest for read ops
- `src/main/java/com/danielagapov/spawn/Mediator/IRequestHandler.java` - Handler interface with `handle(TRequest request)` method

**1.2 Create Mediator service**

- `src/main/java/com/danielagapov/spawn/Mediator/Mediator.java` - Main dispatcher class
  - Uses `ApplicationEventPublisher` for async dispatch
  - Uses `ApplicationContext` to find handlers via type matching
  - Synchronous request/response via custom `@EventListener(condition = ...)` pattern
  - Method: `<TResponse> TResponse send(IRequest<TResponse> request)`

**1.3 Create handler registration**

- `@Component` annotation on handlers for auto-discovery
- Generic type resolution to match requests to handlers

### Phase 2: Friend Request Module

**2.1 Define Commands**

- `commands/friendrequest/CreateFriendRequestCommand` - wraps CreateFriendRequestDTO
- `commands/friendrequest/AcceptFriendRequestCommand` - takes friendRequestId
- `commands/friendrequest/DeleteFriendRequestCommand` - takes friendRequestId
- `commands/friendrequest/DeleteFriendRequestBetweenUsersCommand` - takes senderId, receiverId

**2.2 Define Queries**

- `queries/friendrequest/GetIncomingFriendRequestsQuery` - takes userId, returns List<FetchFriendRequestDTO>
- `queries/friendrequest/GetSentFriendRequestsQuery` - takes userId, returns List<FetchSentFriendRequestDTO>
- `queries/friendrequest/GetLatestFriendRequestTimestampQuery` - takes userId, returns Instant

**2.3 Create Handlers**

- `handlers/friendrequest/CreateFriendRequestHandler` - delegates to FriendRequestService.saveFriendRequest()
- `handlers/friendrequest/AcceptFriendRequestHandler` - delegates to FriendRequestService.acceptFriendRequest()
- `handlers/friendrequest/DeleteFriendRequestHandler` - delegates to FriendRequestService.deleteFriendRequest()
- Similar handlers for all queries

**2.4 Update FriendRequestController**

- Replace direct service calls with `mediator.send(new CreateFriendRequestCommand(...))`
- Keep same endpoints and signatures
- Controllers become thin dispatchers

### Phase 3: Blocked User Module

**3.1 Define Commands**

- `commands/blockeduser/BlockUserCommand` - blockerId, blockedId, reason
- `commands/blockeduser/UnblockUserCommand` - blockerId, blockedId

**3.2 Define Queries**

- `queries/blockeduser/GetBlockedUsersQuery` - blockerId, returns List<BlockedUserDTO>
- `queries/blockeduser/GetBlockedUserIdsQuery` - blockerId, returns List<UUID>
- `queries/blockeduser/IsBlockedQuery` - blockerId, blockedId, returns boolean
- `queries/blockeduser/FilterBlockedUsersQuery<T>` - generic filtering query

**3.3 Create Handlers**

- `handlers/blockeduser/BlockUserHandler` - delegates to BlockedUserService.blockUser()
- `handlers/blockeduser/UnblockUserHandler` - delegates to BlockedUserService.unblockUser()
- Similar handlers for all queries

**3.4 Update BlockedUserController**

- Replace service calls with mediator commands/queries

### Phase 4: Authentication Module

**4.1 Define Commands**

- `commands/auth/RegisterUserCommand` - wraps AuthUserDTO
- `commands/auth/RegisterViaOAuthCommand` - wraps OAuthRegistrationDTO
- `commands/auth/LoginUserCommand` - username/email, password
- `commands/auth/ChangePasswordCommand` - username, currentPassword, newPassword
- `commands/auth/UpdateUserDetailsCommand` - wraps UpdateUserDetailsDTO
- `commands/auth/SendEmailVerificationCommand` - email
- `commands/auth/CheckEmailVerificationCommand` - email, code
- `commands/auth/CompleteContactImportCommand` - userId
- `commands/auth/AcceptTermsOfServiceCommand` - userId

**4.2 Define Queries**

- `queries/auth/SignInUserQuery` - idToken, email, provider, returns Optional<AuthResponseDTO>
- `queries/auth/GetUserByTokenQuery` - token, returns AuthResponseDTO
- `queries/auth/RefreshTokenQuery` - request, returns String

**4.3 Create Handlers**

- `handlers/auth/RegisterUserHandler` - delegates to AuthService.registerUser()
- `handlers/auth/LoginUserHandler` - delegates to AuthService.loginUser()
- Similar handlers for all auth operations (15+ handlers)

**4.4 Update AuthController**

- Replace service calls with mediator pattern
- Large controller with many endpoints - refactor incrementally

### Phase 5: Friendship Module

**5.1 Define Commands**

- `commands/friendship/SaveFriendshipCommand` - userId, friendId
- `commands/friendship/RemoveFriendshipCommand` - userId, friendId (delegates to BlockedUserService.removeFriendshipBetweenUsers)

**5.2 Define Queries**

- `queries/friendship/GetFriendUserIdsQuery` - userId, returns List<UUID>
- `queries/friendship/GetFullFriendUsersQuery` - userId, returns List<FullFriendUserDTO>
- `queries/friendship/IsUserFriendQuery` - userId, potentialFriendId, returns boolean

**5.3 Create Handlers**

- Handlers delegate to UserService friendship methods
- Keep cache eviction logic in handlers

**5.4 Update UserController**

- Replace friendship-related service calls with mediator

### Phase 6: Cross-Cutting Concerns

**6.1 Handler Decorators (Optional Enhancement)**

- Create decorator pattern for handlers
- `LoggingHandlerDecorator` - wraps handlers with logging
- `CachingHandlerDecorator` - handles @Cacheable logic
- `ValidationHandlerDecorator` - validates commands before execution
- Implementation: Use Spring AOP or manual decoration in Mediator

**6.2 Exception Handling**

- Handlers throw same exceptions as services
- Controllers catch and handle as before
- Mediator propagates exceptions transparently

### Phase 7: Testing & Migration

**7.1 Testing Strategy**

- Keep existing service tests (they test business logic)
- Add handler tests (verify delegation)
- Add mediator integration tests
- Update controller tests to verify mediator calls

**7.2 Service Layer**

- Keep existing services initially (handlers delegate to them)
- Services become implementation details
- Future: Move logic from services into handlers

**7.3 Gradual Rollout**

- Start with FriendRequest module (smallest, well-defined)
- Then BlockedUser module
- Then Friendship operations
- Finally Auth module (largest, most complex)
- Both patterns coexist temporarily

## File Structure

```
src/main/java/com/danielagapov/spawn/
├── Mediator/
│   ├── IRequest.java
│   ├── ICommand.java
│   ├── IQuery.java
│   ├── IRequestHandler.java
│   ├── Mediator.java
│   ├── commands/
│   │   ├── auth/
│   │   │   ├── RegisterUserCommand.java
│   │   │   ├── LoginUserCommand.java
│   │   │   └── ... (10+ auth commands)
│   │   ├── friendrequest/
│   │   │   ├── CreateFriendRequestCommand.java
│   │   │   ├── AcceptFriendRequestCommand.java
│   │   │   └── ... (4 commands)
│   │   ├── blockeduser/
│   │   │   ├── BlockUserCommand.java
│   │   │   └── UnblockUserCommand.java
│   │   └── friendship/
│   │       ├── SaveFriendshipCommand.java
│   │       └── RemoveFriendshipCommand.java
│   ├── queries/
│   │   ├── auth/
│   │   │   ├── SignInUserQuery.java
│   │   │   ├── GetUserByTokenQuery.java
│   │   │   └── RefreshTokenQuery.java
│   │   ├── friendrequest/
│   │   │   ├── GetIncomingFriendRequestsQuery.java
│   │   │   ├── GetSentFriendRequestsQuery.java
│   │   │   └── GetLatestFriendRequestTimestampQuery.java
│   │   ├── blockeduser/
│   │   │   ├── GetBlockedUsersQuery.java
│   │   │   ├── GetBlockedUserIdsQuery.java
│   │   │   ├── IsBlockedQuery.java
│   │   │   └── FilterBlockedUsersQuery.java
│   │   └── friendship/
│   │       ├── GetFriendUserIdsQuery.java
│   │       ├── GetFullFriendUsersQuery.java
│   │       └── IsUserFriendQuery.java
│   └── handlers/
│       ├── auth/ (15+ handlers)
│       ├── friendrequest/ (7 handlers)
│       ├── blockeduser/ (6 handlers)
│       └── friendship/ (5 handlers)
```

## Key Design Decisions

1. **Synchronous by default**: Use custom synchronous event listeners, not async
2. **Generic type safety**: `IRequest<TResponse>` ensures type safety at compile time
3. **Minimal change to services**: Services remain, handlers delegate to them
4. **Keep DTOs**: Reuse existing DTOs, Commands/Queries wrap them
5. **Cache annotations**: Move from services to handlers (or keep in services initially)
6. **Existing events**: Keep notification events separate from mediator pattern

## Benefits

- **Decoupling**: Controllers don't know about services
- **Testability**: Handlers are isolated, easily mocked
- **Single Responsibility**: Each handler has one job
- **Consistency**: All operations follow same pattern
- **Extensibility**: Easy to add decorators, pipelines, validation

## Risks & Mitigation

- **Learning curve**: Team needs to learn pattern → Document with examples
- **Boilerplate**: More classes → Use code generation or IDE templates
- **Performance**: Extra layer → Negligible with proper caching
- **Debugging**: More indirection → Good logging in mediator

## Example Usage

### Before (Direct Service Call)

```java
@PostMapping
public ResponseEntity<CreateFriendRequestDTO> createFriendRequest(@RequestBody CreateFriendRequestDTO friendRequest) {
    try {
        CreateFriendRequestDTO createdRequest = friendRequestService.saveFriendRequest(friendRequest);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    } catch (Exception e) {
        logger.error("Error creating friend request: " + e.getMessage());
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### After (Mediator Pattern)

```java
@PostMapping
public ResponseEntity<CreateFriendRequestDTO> createFriendRequest(@RequestBody CreateFriendRequestDTO friendRequest) {
    try {
        CreateFriendRequestDTO createdRequest = mediator.send(new CreateFriendRequestCommand(friendRequest));
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    } catch (Exception e) {
        logger.error("Error creating friend request: " + e.getMessage());
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

### Handler Implementation

```java
@Component
public class CreateFriendRequestHandler implements IRequestHandler<CreateFriendRequestCommand, CreateFriendRequestDTO> {
    private final IFriendRequestService friendRequestService;
    
    public CreateFriendRequestHandler(IFriendRequestService friendRequestService) {
        this.friendRequestService = friendRequestService;
    }
    
    @Override
    public CreateFriendRequestDTO handle(CreateFriendRequestCommand command) {
        return friendRequestService.saveFriendRequest(command.getFriendRequest());
    }
}
```

## Implementation Checklist

- [ ] Phase 1: Core Mediator Infrastructure
  - [ ] Create IRequest interface
  - [ ] Create ICommand interface
  - [ ] Create IQuery interface
  - [ ] Create IRequestHandler interface
  - [ ] Implement Mediator service with handler discovery
  - [ ] Add unit tests for Mediator

- [ ] Phase 2: Friend Request Module
  - [ ] Create 4 command classes
  - [ ] Create 3 query classes
  - [ ] Create 7 handler classes
  - [ ] Refactor FriendRequestController
  - [ ] Add integration tests

- [ ] Phase 3: Blocked User Module
  - [ ] Create 2 command classes
  - [ ] Create 4 query classes
  - [ ] Create 6 handler classes
  - [ ] Refactor BlockedUserController
  - [ ] Add integration tests

- [ ] Phase 4: Friendship Module
  - [ ] Create 2 command classes
  - [ ] Create 3 query classes
  - [ ] Create 5 handler classes
  - [ ] Refactor UserController (friendship endpoints)
  - [ ] Add integration tests

- [ ] Phase 5: Authentication Module
  - [ ] Create 9 command classes
  - [ ] Create 3 query classes
  - [ ] Create 12+ handler classes
  - [ ] Refactor AuthController
  - [ ] Add integration tests

- [ ] Phase 6: Cross-Cutting Concerns
  - [ ] Implement logging decorator (optional)
  - [ ] Implement caching decorator (optional)
  - [ ] Implement validation decorator (optional)

- [ ] Phase 7: Testing & Documentation
  - [ ] Verify all existing tests still pass
  - [ ] Add handler-specific tests
  - [ ] Add mediator integration tests
  - [ ] Update API documentation
  - [ ] Create team training materials

## Related Resources

- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)
- [Mediator Pattern](https://refactoring.guru/design-patterns/mediator)
- [MediatR Library (.NET)](https://github.com/jbogard/MediatR)
- [Spring Events Documentation](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)

## Timeline Estimate

- **Phase 1 (Infrastructure)**: 1-2 days
- **Phase 2 (Friend Requests)**: 2-3 days
- **Phase 3 (Blocked Users)**: 2-3 days
- **Phase 4 (Friendships)**: 2-3 days
- **Phase 5 (Authentication)**: 4-5 days
- **Phase 6 (Cross-Cutting)**: 2-3 days (optional)
- **Phase 7 (Testing)**: 3-4 days

**Total**: ~3-4 weeks for complete implementation

## Future Enhancements

1. **Command Validation Pipeline**: Add automatic validation before handlers execute
2. **Audit Logging**: Log all commands for compliance/debugging
3. **Performance Monitoring**: Track handler execution times
4. **Retry Logic**: Add automatic retry for transient failures
5. **Circuit Breaker**: Prevent cascading failures
6. **Request Caching**: Cache query results automatically
7. **Batch Operations**: Support batch command execution
8. **Event Sourcing**: Store command history for audit trail

