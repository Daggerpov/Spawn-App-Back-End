#!/bin/bash
#Fix test imports script

TEST_DIR="src/test/java/com/danielagapov/spawn"

# Controllers
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Controllers\.ActivityTypeController|com.danielagapov.spawn.activity.api.ActivityTypeController|g' \
  -e 's|com\.danielagapov\.spawn\.Controllers\.User\.BlockedUserController|com.danielagapov.spawn.user.api.BlockedUserController|g' \
  -e 's|com\.danielagapov\.spawn\.Controllers\.FriendRequestController|com.danielagapov.spawn.social.api.FriendRequestController|g' \
  -e 's|com\.danielagapov\.spawn\.Controllers\.User\.UserController|com.danielagapov.spawn.user.api.UserController|g' \
  {} +

# DTOs - Activity
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.ActivityType\.ActivityTypeDTO|com.danielagapov.spawn.activity.api.dto.ActivityTypeDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.ActivityType\.BatchActivityTypeUpdateDTO|com.danielagapov.spawn.activity.api.dto.BatchActivityTypeUpdateDTO|g' \
  {} +

# DTOs - User
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.User\.AbstractUserDTO|com.danielagapov.spawn.user.api.dto.AbstractUserDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.User\.BaseUserDTO|com.danielagapov.spawn.user.api.dto.BaseUserDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.User\.FriendUser\.FullFriendUserDTO|com.danielagapov.spawn.user.api.dto.FriendUser.FullFriendUserDTO|g' \
  {} +

# DTOs - BlockedUser
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.BlockedUser\.BlockedUserCreationDTO|com.danielagapov.spawn.user.api.dto.BlockedUserCreationDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.BlockedUser\.BlockedUserDTO|com.danielagapov.spawn.user.api.dto.BlockedUserDTO|g' \
  {} +

# DTOs - FriendRequest
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.FriendRequest\.CreateFriendRequestDTO|com.danielagapov.spawn.social.api.dto.CreateFriendRequestDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.FriendRequest\.FetchFriendRequestDTO|com.danielagapov.spawn.social.api.dto.FetchFriendRequestDTO|g' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.FriendRequest\.FetchSentFriendRequestDTO|com.danielagapov.spawn.social.api.dto.FetchSentFriendRequestDTO|g' \
  {} +

# Services - Activity
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Services\.ActivityType\.IActivityTypeService|com.danielagapov.spawn.activity.internal.services.IActivityTypeService|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.ActivityType\.ActivityTypeService|com.danielagapov.spawn.activity.internal.services.ActivityTypeService|g' \
  {} +

# Services - User
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Services\.User\.IUserService|com.danielagapov.spawn.user.internal.services.IUserService|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.BlockedUser\.IBlockedUserService|com.danielagapov.spawn.user.internal.services.IBlockedUserService|g' \
  {} +

# Services - Social/Friend
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Services\.FriendRequest\.IFriendRequestService|com.danielagapov.spawn.social.internal.services.IFriendRequestService|g' \
  {} +

# Services - Auth and S3
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Services\.Auth\.IAuthService|com.danielagapov.spawn.auth.internal.services.IAuthService|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.S3\.IS3Service|com.danielagapov.spawn.storage.internal.services.IS3Service|g' \
  {} +

# Models/Domain
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Models\.ActivityType|com.danielagapov.spawn.activity.internal.domain.ActivityType|g' \
  -e 's|com\.danielagapov\.spawn\.Models\.User\.User|com.danielagapov.spawn.user.internal.domain.User|g' \
  -e 's|com\.danielagapov\.spawn\.Models\.Friendship|com.danielagapov.spawn.social.internal.domain.Friendship|g' \
  -e 's|com\.danielagapov\.spawn\.Models\.FriendRequest|com.danielagapov.spawn.social.internal.domain.FriendRequest|g' \
  {} +

# Repositories
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Repositories\.IActivityTypeRepository|com.danielagapov.spawn.activity.internal.repositories.IActivityTypeRepository|g' \
  -e 's|com\.danielagapov\.spawn\.Repositories\.User\.IUserRepository|com.danielagapov.spawn.user.internal.repositories.IUserRepository|g' \
  -e 's|com\.danielagapov\.spawn\.Repositories\.IFriendshipRepository|com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository|g' \
  -e 's|com\.danielagapov\.spawn\.Repositories\.IFriendRequestsRepository|com.danielagapov.spawn.social.internal.repositories.IFriendRequestsRepository|g' \
  {} +

# Exceptions
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Exceptions\.Logger\.ILogger|com.danielagapov.spawn.shared.exceptions.Logger.ILogger|g' \
  -e 's|com\.danielagapov\.spawn\.Exceptions\.ActivityTypeValidationException|com.danielagapov.spawn.shared.exceptions.ActivityTypeValidationException|g' \
  -e 's|com\.danielagapov\.spawn\.Exceptions\.Base\.BaseNotFoundException|com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException|g' \
  -e 's|com\.danielagapov\.spawn\.Exceptions\.Base\.BasesNotFoundException|com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException|g' \
  {} +

# Enums/Utils
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Enums\.EntityType|com.danielagapov.spawn.shared.util.EntityType|g' \
  -e 's|com\.danielagapov\.spawn\.Enums\.FriendRequestAction|com.danielagapov.spawn.shared.util.FriendRequestAction|g' \
  -e 's|com\.danielagapov\.spawn\.Enums\.UserStatus|com.danielagapov.spawn.shared.util.UserStatus|g' \
  {} +

echo "Test imports fixed successfully!"

