#!/bin/bash
# Final fixes for remaining test import issues

TEST_DIR="src/test/java"

echo "Applying final test import fixes..."

# Fix FriendUser imports - they're in parent package not a subpackage
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.user\.api\.dto\.FriendUser\.FullFriendUserDTO|com.danielagapov.spawn.user.api.dto.FullFriendUserDTO|g' \
  {} +

# Fix Location - it's in activity module, not a separate location module
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.location\.internal\.domain\.Location|com.danielagapov.spawn.activity.internal.domain.Location|g' \
  -e 's|com\.danielagapov\.spawn\.location\.internal\.services\.|com.danielagapov.spawn.activity.internal.services.|g' \
  {} +

# Fix Config imports
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Config\.|com.danielagapov.spawn.shared.config.|g' \
  {} +

# Fix remaining Services that might not have a dedicated module
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Services\.Email\.|com.danielagapov.spawn.communication.internal.services.|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.JWT\.|com.danielagapov.spawn.auth.internal.services.|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.OAuth\.|com.danielagapov.spawn.auth.internal.services.|g' \
  -e 's|com\.danielagapov\.spawn\.Services\.BetaAccessSignUp\.|com.danielagapov.spawn.analytics.internal.services.|g' \
  {} +

# Fix remaining Repository references
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Repositories\.IVerificationCodeRepository|com.danielagapov.spawn.auth.internal.repositories.IVerificationCodeRepository|g' \
  {} +

# Fix remaining DTO references
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.DTOs\.BetaAccessSignUpDTO|com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO|g' \
  {} +

# Fix remaining Model references
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.Models\.BetaAccessSignUp|com.danielagapov.spawn.analytics.internal.domain.BetaAccessSignUp|g' \
  {} +

echo "Final test import fixes applied!"



