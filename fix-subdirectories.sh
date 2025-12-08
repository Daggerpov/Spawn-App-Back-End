#!/bin/bash

# The issue is that files in subdirectories have package declarations with the subdirectory name
# We need to remove the subdirectory from the package declaration

# Fix shared/exceptions files in subdirectories
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions\.Base;/package com.danielagapov.spawn.shared.exceptions;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions\.Token;/package com.danielagapov.spawn.shared.exceptions;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions\.Logger;/package com.danielagapov.spawn.shared.exceptions;/' {} \;

# Fix shared/util files in subdirectories
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Utils\.Cache;/package com.danielagapov.spawn.shared.util;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Utils\.Exceptions;/package com.danielagapov.spawn.shared.util;/' {} \;

# Fix user/api/dto files in subdirectories
find src/main/java/com/danielagapov/spawn/user/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.User\.FriendUser;/package com.danielagapov.spawn.user.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/user/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.User\.Profile;/package com.danielagapov.spawn.user.api.dto;/' {} \;

# Fix analytics/internal/services files in subdirectories (Cache)
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Report\.Cache;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;

# Fix user/internal/domain files in subdirectories
find src/main/java/com/danielagapov/spawn/user/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.User\.BlockedUser;/package com.danielagapov.spawn.user.internal.domain;/' {} \;

# Now update imports to not use the subdirectory paths
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.shared\.exceptions\.Base\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.shared\.exceptions\.Token\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.shared\.exceptions\.Logger\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.shared\.util\.Cache\./import com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.user\.api\.dto\.FriendUser\./import com.danielagapov.spawn.user.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.user\.api\.dto\.Profile\./import com.danielagapov.spawn.user.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.analytics\.internal\.services\.Cache\./import com.danielagapov.spawn.analytics.internal.services./g' {} \;

echo "Subdirectory package declarations fixed"
