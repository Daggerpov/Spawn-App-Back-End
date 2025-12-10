#!/bin/bash

# Update package declarations for Social module
find src/main/java/com/danielagapov/spawn/social/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.social.api;/' {} \;
find src/main/java/com/danielagapov/spawn/social/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.FriendRequest;/package com.danielagapov.spawn.social.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/social/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.FriendRequest;/package com.danielagapov.spawn.social.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/social/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.BlockedUser;/package com.danielagapov.spawn.social.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/social/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.social.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/social/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.social.internal.domain;/' {} \;

# Update package declarations for Notification module
find src/main/java/com/danielagapov/spawn/notification/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.notification.api;/' {} \;
find src/main/java/com/danielagapov/spawn/notification/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs\.Notification;/package com.danielagapov.spawn.notification.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/notification/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.notification.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/notification/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.PushNotification;/package com.danielagapov.spawn.notification.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/notification/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.notification.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/notification/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.notification.internal.domain;/' {} \;

# Update package declarations for Media module
find src/main/java/com/danielagapov/spawn/media/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.S3;/package com.danielagapov.spawn.media.internal.services;/' {} \;

# Update package declarations for Analytics module
find src/main/java/com/danielagapov/spawn/analytics/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.analytics.api;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/api -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers\.Analytics;/package com.danielagapov.spawn.analytics.api;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/api/dto -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.analytics.api.dto;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Report;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.FeedbackSubmission;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.Analytics;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services\.BetaAccessSignUp;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/services -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Services;/package com.danielagapov.spawn.analytics.internal.services;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/repositories -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Repositories;/package com.danielagapov.spawn.analytics.internal.repositories;/' {} \;
find src/main/java/com/danielagapov/spawn/analytics/internal/domain -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Models;/package com.danielagapov.spawn.analytics.internal.domain;/' {} \;

# Update package declarations for Shared module
find src/main/java/com/danielagapov/spawn/shared/events -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Events;/package com.danielagapov.spawn.shared.events;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions\.Base;/package com.danielagapov.spawn.shared.exceptions;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions\.Logger;/package com.danielagapov.spawn.shared.exceptions;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/exceptions -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Exceptions;/package com.danielagapov.spawn.shared.exceptions;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/config -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Config;/package com.danielagapov.spawn.shared.config;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/config -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Controllers;/package com.danielagapov.spawn.shared.config;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/config -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.DTOs;/package com.danielagapov.spawn.shared.config;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Util;/package com.danielagapov.spawn.shared.util;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Utils;/package com.danielagapov.spawn.shared.util;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Enums;/package com.danielagapov.spawn.shared.util;/' {} \;
find src/main/java/com/danielagapov/spawn/shared/util -name "*.java" -type f -exec sed -i '' 's/^package com\.danielagapov\.spawn\.Mappers;/package com.danielagapov.spawn.shared.util;/' {} \;

echo "Package declarations updated for Social, Notification, Media, Analytics, and Shared modules"
