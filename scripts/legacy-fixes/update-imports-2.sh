#!/bin/bash

echo "Updating Activity module imports..."

# Update imports for Activity module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.ActivityController/import com.danielagapov.spawn.activity.api.ActivityController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.ActivityTypeController/import com.danielagapov.spawn.activity.api.ActivityTypeController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Activity\./import com.danielagapov.spawn.activity.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.ActivityType\./import com.danielagapov.spawn.activity.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Location\./import com.danielagapov.spawn.activity.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Calendar\./import com.danielagapov.spawn.activity.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IActivityRepository/import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IActivityTypeRepository/import com.danielagapov.spawn.activity.internal.repositories.IActivityTypeRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IActivityUserRepository/import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.ILocationRepository/import com.danielagapov.spawn.activity.internal.repositories.ILocationRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.Activity;/import com.danielagapov.spawn.activity.internal.domain.Activity;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ActivityType;/import com.danielagapov.spawn.activity.internal.domain.ActivityType;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ActivityUser;/import com.danielagapov.spawn.activity.internal.domain.ActivityUser;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.Location;/import com.danielagapov.spawn.activity.internal.domain.Location;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.CompositeKeys\.ActivityUsersId/import com.danielagapov.spawn.activity.internal.domain.ActivityUsersId/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.Activity\./import com.danielagapov.spawn.activity.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.ActivityType\./import com.danielagapov.spawn.activity.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CalendarActivityDTO/import com.danielagapov.spawn.activity.api.dto.CalendarActivityDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.UserIdActivityTimeDTO/import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO/g' {} \;

echo "Activity imports updated"

echo "Updating Chat module imports..."

# Update imports for Chat module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.ChatMessageController/import com.danielagapov.spawn.chat.api.ChatMessageController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.ChatMessage\./import com.danielagapov.spawn.chat.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IChatMessageRepository/import com.danielagapov.spawn.chat.internal.repositories.IChatMessageRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IChatMessageLikesRepository/import com.danielagapov.spawn.chat.internal.repositories.IChatMessageLikesRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ChatMessage;/import com.danielagapov.spawn.chat.internal.domain.ChatMessage;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ChatMessageLikes;/import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikes;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.CompositeKeys\.ChatMessageLikesId/import com.danielagapov.spawn.chat.internal.domain.ChatMessageLikesId/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.ChatMessage\./import com.danielagapov.spawn.chat.api.dto./g' {} \;

echo "Chat imports updated"
