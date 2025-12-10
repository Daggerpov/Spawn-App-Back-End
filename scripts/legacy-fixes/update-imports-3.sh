#!/bin/bash

echo "Updating User module imports..."

# Update imports for User module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.User\./import com.danielagapov.spawn.user.api./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.User\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.UserSearch\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.UserStats\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.UserInterest\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.UserSocialMedia\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.UserDetails\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.FuzzySearch\./import com.danielagapov.spawn.user.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.User\./import com.danielagapov.spawn.user.internal.repositories./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.User\./import com.danielagapov.spawn.user.internal.domain./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.User\.User;/import com.danielagapov.spawn.user.internal.domain.User;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.User\.BlockedUser;/import com.danielagapov.spawn.user.internal.domain.BlockedUser;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.User\./import com.danielagapov.spawn.user.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.BlockedUser\./import com.danielagapov.spawn.user.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.ContactCrossReferenceRequestDTO/import com.danielagapov.spawn.user.api.dto.ContactCrossReferenceRequestDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.ContactCrossReferenceResponseDTO/import com.danielagapov.spawn.user.api.dto.ContactCrossReferenceResponseDTO/g' {} \;

echo "User imports updated"

echo "Updating Social module imports..."

# Update imports for Social module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.FriendRequestController/import com.danielagapov.spawn.social.api.FriendRequestController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.FriendRequest\./import com.danielagapov.spawn.social.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.BlockedUser\./import com.danielagapov.spawn.social.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IFriendRequestsRepository/import com.danielagapov.spawn.social.internal.repositories.IFriendRequestsRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IFriendshipRepository/import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.Friendship;/import com.danielagapov.spawn.social.internal.domain.Friendship;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.FriendRequest;/import com.danielagapov.spawn.social.internal.domain.FriendRequest;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.FriendRequest\./import com.danielagapov.spawn.social.api.dto./g' {} \;

echo "Social imports updated"
