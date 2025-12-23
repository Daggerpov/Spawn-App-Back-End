#!/bin/bash

echo "Updating Notification module imports..."

# Update imports for Notification module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.NotificationController/import com.danielagapov.spawn.notification.api.NotificationController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.PushNotification\./import com.danielagapov.spawn.notification.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IDeviceTokenRepository/import com.danielagapov.spawn.notification.internal.repositories.IDeviceTokenRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.INotificationPreferencesRepository/import com.danielagapov.spawn.notification.internal.repositories.INotificationPreferencesRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.DeviceToken;/import com.danielagapov.spawn.notification.internal.domain.DeviceToken;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.NotificationPreferences;/import com.danielagapov.spawn.notification.internal.domain.NotificationPreferences;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.Notification\./import com.danielagapov.spawn.notification.api.dto./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.DeviceTokenDTO/import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO/g' {} \;

echo "Notification imports updated"

echo "Updating Media module imports..."

# Update imports for Media module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.S3\./import com.danielagapov.spawn.media.internal.services./g' {} \;

echo "Media imports updated"

echo "Updating Analytics module imports..."

# Update imports for Analytics module
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.FeedbackSubmissionController/import com.danielagapov.spawn.analytics.api.FeedbackSubmissionController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.ShareLinkController/import com.danielagapov.spawn.analytics.api.ShareLinkController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.BetaAccessSignUpController/import com.danielagapov.spawn.analytics.api.BetaAccessSignUpController/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Controllers\.Analytics\./import com.danielagapov.spawn.analytics.api./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Report\./import com.danielagapov.spawn.analytics.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.FeedbackSubmission\./import com.danielagapov.spawn.analytics.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.ShareLinkService/import com.danielagapov.spawn.analytics.internal.services.ShareLinkService/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.ShareLinkCleanupService/import com.danielagapov.spawn.analytics.internal.services.ShareLinkCleanupService/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.Analytics\./import com.danielagapov.spawn.analytics.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Services\.BetaAccessSignUp\./import com.danielagapov.spawn.analytics.internal.services./g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IFeedbackSubmissionRepository/import com.danielagapov.spawn.analytics.internal.repositories.IFeedbackSubmissionRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.ShareLinkRepository/import com.danielagapov.spawn.analytics.internal.repositories.ShareLinkRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IReportedContentRepository/import com.danielagapov.spawn.analytics.internal.repositories.IReportedContentRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Repositories\.IBetaAccessSignUpRepository/import com.danielagapov.spawn.analytics.internal.repositories.IBetaAccessSignUpRepository/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ShareLink;/import com.danielagapov.spawn.analytics.internal.domain.ShareLink;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.FeedbackSubmission;/import com.danielagapov.spawn.analytics.internal.domain.FeedbackSubmission;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.ReportedContent;/import com.danielagapov.spawn.analytics.internal.domain.ReportedContent;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.Models\.BetaAccessSignUp;/import com.danielagapov.spawn.analytics.internal.domain.BetaAccessSignUp;/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.FetchReportedContentDTO/import com.danielagapov.spawn.analytics.api.dto.FetchReportedContentDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CreateReportedContentDTO/import com.danielagapov.spawn.analytics.api.dto.CreateReportedContentDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.FetchFeedbackSubmissionDTO/import com.danielagapov.spawn.analytics.api.dto.FetchFeedbackSubmissionDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.ReportedContentDTO/import com.danielagapov.spawn.analytics.api.dto.ReportedContentDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.CreateFeedbackSubmissionDTO/import com.danielagapov.spawn.analytics.api.dto.CreateFeedbackSubmissionDTO/g' {} \;
find src/main/java -name "*.java" -type f -exec sed -i '' 's/import com\.danielagapov\.spawn\.DTOs\.BetaAccessSignUpDTO/import com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO/g' {} \;

echo "Analytics imports updated"

echo "All imports updated successfully!"
