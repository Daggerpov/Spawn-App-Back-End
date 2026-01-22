#!/bin/bash

# Fix Enums imports
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import static com\.danielagapov\.spawn\.Enums\./import static com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Enums\./import com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/package com\.danielagapov\.spawn\.Enums/package com.danielagapov.spawn.shared.util/g' {} \;

# Fix Activity import in ChatMessage
find src/main/java/com/danielagapov/spawn/chat -name "ChatMessage.java" -type f -exec sed -i '' '/^import/a\
import com.danielagapov.spawn.activity.internal.domain.Activity;
' {} \;

# Fix UserSocialMedia and UserInterest imports - they're in subdirectories but should be flat
find src/main/java/com/danielagapov/spawn/user/internal/repositories -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.user\.internal\.domain\.Profile\./import com.danielagapov.spawn.user.internal.domain./g' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/repositories -name "*.java" -type f -exec sed -i '' 's/package com\.danielagapov\.spawn\.Repositories\.User\.Profile/package com.danielagapov.spawn.user.internal.repositories/g' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.user\.internal\.domain\.Profile\./import com.danielagapov.spawn.user.internal.domain./g' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.user\.internal\.repositories\.Profile\./import com.danielagapov.spawn.user.internal.repositories./g' {} \;

# Fix CacheController imports
find src/main/java/com/danielagapov/spawn/shared/config -name "CacheController.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CacheValidationRequestDTO/import com.danielagapov.spawn.shared.config.CacheValidationRequestDTO/g' {} \;
find src/main/java/com/danielagapov/spawn/shared/config -name "CacheController.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CacheValidationResponseDTO/import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO/g' {} \;

echo "Remaining import issues fixed"
