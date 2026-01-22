#!/bin/bash

# This script updates imports across the entire codebase
# We'll process all Java files recursively

echo "Starting import updates..."

# Update imports for Shared module (do this first as it's referenced everywhere)
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Events\./import com.danielagapov.spawn.shared.events./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Exceptions\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Exceptions\.Base\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Exceptions\.Logger\./import com.danielagapov.spawn.shared.exceptions./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Config\./import com.danielagapov.spawn.shared.config./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Util\./import com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Utils\./import com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Enums\./import com.danielagapov.spawn.shared.util./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Mappers\./import com.danielagapov.spawn.shared.util./g' {} \;

echo "Shared imports updated"

# Update imports for Auth module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.AuthController/import com.danielagapov.spawn.auth.api.AuthController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Auth\./import com.danielagapov.spawn.auth.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.OAuth\./import com.danielagapov.spawn.auth.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Email\./import com.danielagapov.spawn.auth.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.JWT\./import com.danielagapov.spawn.auth.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.User\.IUserIdExternalIdMapRepository/import com.danielagapov.spawn.auth.internal.repositories.IUserIdExternalIdMapRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IEmailVerificationRepository/import com.danielagapov.spawn.auth.internal.repositories.IEmailVerificationRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.User\.UserIdExternalIdMap/import com.danielagapov.spawn.auth.internal.domain.UserIdExternalIdMap/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.EmailVerification/import com.danielagapov.spawn.auth.internal.domain.EmailVerification/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.OAuthRegistrationDTO/import com.danielagapov.spawn.auth.api.dto.OAuthRegistrationDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.SendEmailVerificationRequestDTO/import com.danielagapov.spawn.auth.api.dto.SendEmailVerificationRequestDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CheckEmailVerificationRequestDTO/import com.danielagapov.spawn.auth.api.dto.CheckEmailVerificationRequestDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.EmailVerificationResponseDTO/import com.danielagapov.spawn.auth.api.dto.EmailVerificationResponseDTO/g' {} \;

echo "Auth imports updated"
