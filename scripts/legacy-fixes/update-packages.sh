#!/bin/bash

# Update package declarations for Auth module
find src/main/java/com/danielagapov/spawn/auth/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.auth.api;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.auth.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Auth;/package com.danielagapov.spawn.auth.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.OAuth;/package com.danielagapov.spawn.auth.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Email;/package com.danielagapov.spawn.auth.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.JWT;/package com.danielagapov.spawn.auth.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories\.User;/package com.danielagapov.spawn.auth.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.auth.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.User;/package com.danielagapov.spawn.auth.internal.domain;/' {} \;
find src/main/java/com/danielagapov/spawn/auth/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.auth.internal.domain;/' {} \;

# Update package declarations for Activity module
find src/main/java/com/danielagapov/spawn/activity/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.activity.api;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.Activity;/package com.danielagapov.spawn.activity.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.ActivityType;/package com.danielagapov.spawn.activity.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.activity.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Activity;/package com.danielagapov.spawn.activity.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.ActivityType;/package com.danielagapov.spawn.activity.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Location;/package com.danielagapov.spawn.activity.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Calendar;/package com.danielagapov.spawn.activity.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.activity.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.activity.internal.domain;/' {} \;
find src/main/java/com/danielagapov/spawn/activity/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.CompositeKeys;/package com.danielagapov.spawn.activity.internal.domain;/' {} \;

echo "Package declarations updated for Auth and Activity modules"
