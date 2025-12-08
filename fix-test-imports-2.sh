#!/bin/bash
# Fix remaining test import issues

TEST_DIR="src/test/java/com/danielagapov/spawn"

# Fix ILogger - it's in shared.exceptions not shared.exceptions.Logger
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.shared\.exceptions\.Logger\.ILogger|com.danielagapov.spawn.shared.exceptions.ILogger|g' \
  {} +

# Fix Base exceptions - they're in shared.exceptions not shared.exceptions.Base
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.shared\.exceptions\.Base\.BaseNotFoundException|com.danielagapov.spawn.shared.exceptions.BaseNotFoundException|g' \
  -e 's|com\.danielagapov\.spawn\.shared\.exceptions\.Base\.BasesNotFoundException|com.danielagapov.spawn.shared.exceptions.BasesNotFoundException|g' \
  {} +

# Fix IBlockedUserService - it's in social module not user module
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.user\.internal\.services\.IBlockedUserService|com.danielagapov.spawn.social.internal.services.IBlockedUserService|g' \
  {} +

# Fix IS3Service - it's in media module not storage module
find "$TEST_DIR" -type f -name "*.java" -exec sed -i '' \
  -e 's|com\.danielagapov\.spawn\.storage\.internal\.services\.IS3Service|com.danielagapov.spawn.media.internal.services.IS3Service|g' \
  {} +

echo "Remaining test import issues fixed!"

