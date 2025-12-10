#!/bin/bash

# Fix BlockedUser package declaration (it was in a subdirectory)
find src/main/java/com/danielagapov/spawn/user/internal/domain -name "BlockedUser.java" -type f -exec sed -i '' 's/^    package com\.danielagapov\.spawn\.Models\.User;/package com.danielagapov.spawn.user.internal.domain;/' {} \;

# Fix Cache service package declarations
find src/main/java/com/danielagapov/spawn/analytics/internal/services/Cache -name "*.java" -type f -exec sed -i '' 's/^import com\.danielagapov\.spawn\.DTOs\.CacheValidationResponseDTO/import com.danielagapov.spawn.shared.config.CacheValidationResponseDTO/g' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services/Cache -name "*.java" -type f -exec sed -i '' 's/^import com\.danielagapov\.spawn\.DTOs\.CacheValidationRequestDTO/import com.danielagapov.spawn.shared.config.CacheValidationRequestDTO/g' {} \;

# Update any remaining old DTO imports in Cache service
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/package com\.danielagapov\.spawn\.DTOs/package com.danielagapov.spawn.shared.config/g' {} \;

# Fix UserIdExternalIdMap - it needs to import User from user module (but this crosses module boundaries!)
# For now, let me just fix the immediate import issue
find src/main/java/com/danielagapov/spawn/auth/internal/domain -name "UserIdExternalIdMap.java" -type f -exec sed -i '' '/^import/a\
import com.danielagapov.spawn.user.internal.domain.User;
' {} \;

echo "Remaining issues fixed"
