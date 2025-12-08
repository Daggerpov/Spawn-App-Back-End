#!/bin/bash

# Update package declarations for Chat module
find src/main/java/com/danielagapov/spawn/chat/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.chat.api;/' {} \;
find src/main/java/com/danielagapov/spawn/chat/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.ChatMessage;/package com.danielagapov.spawn.chat.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/chat/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.ChatMessage;/package com.danielagapov.spawn.chat.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/chat/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.chat.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/chat/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.chat.internal.domain;/' {} \;
find src/main/java/com/danielagapov/spawn/chat/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.CompositeKeys;/package com.danielagapov.spawn.chat.internal.domain;/' {} \;

# Update package declarations for User module
find src/main/java/com/danielagapov/spawn/user/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers\.User;/package com.danielagapov.spawn.user.api;/' {} \;
find src/main/java/com/danielagapov/spawn/user/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers\.User\.Profile;/package com.danielagapov.spawn.user.api;/' {} \;
find src/main/java/com/danielagapov/spawn/user/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.User;/package com.danielagapov.spawn.user.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/user/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.BlockedUser;/package com.danielagapov.spawn.user.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/user/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.user.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.User;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.UserSearch;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.UserStats;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.UserInterest;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.UserSocialMedia;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.UserDetails;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.FuzzySearch;/package com.danielagapov.spawn.user.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories\.User;/package com.danielagapov.spawn.user.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories\.User\.Profile;/package com.danielagapov.spawn.user.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.User;/package com.danielagapov.spawn.user.internal.domain;/' {} \;
find src/main/java/com/danielagapov/spawn/user/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models\.User\.Profile;/package com.danielagapov.spawn.user.internal.domain;/' {} \;

echo "Package declarations updated for Chat and User modules"
