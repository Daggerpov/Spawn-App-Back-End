# Email Notification Implementation

## Overview
This document describes the email notification system implemented for team notifications when users submit beta access sign-ups, feedback, or report content.

## Implementation Summary

### 1. Created Notification Configuration (`NotificationConfig.java`)
- **Location**: `src/main/java/com/danielagapov/spawn/shared/config/NotificationConfig.java`
- **Purpose**: Reads notification email addresses from environment variables for security
- **Configuration Property**: `notification.emails` (comma-separated list)
- **Default**: Falls back to `spawnappmarketing@gmail.com` if not configured

### 2. Created Notification Type Enum (`NotificationType.java`)
- **Location**: `src/main/java/com/danielagapov/spawn/shared/util/NotificationType.java`
- **Types**:
  - `BETA_ACCESS_SIGNUP`
  - `FEEDBACK_SUBMISSION`
  - `REPORTED_CONTENT`

### 3. Enhanced Email Service
- **Updated**: `IEmailService` interface and `EmailService` implementation
- **New Method**: `sendTeamNotificationEmail(String to, NotificationType notificationType, Map<String, String> parameters)`
- **Features**:
  - Builder pattern for email templates
  - Type-specific subject lines and content
  - HTML formatted emails with detailed information
  - Includes links to admin dashboard

### 4. Updated Services with Email Notifications

#### BetaAccessSignUpService
- Sends notification when a new user signs up for beta access
- Includes: signup email address

#### FeedbackSubmissionService
- Sends notification when feedback is submitted
- Includes: 
  - Feedback type
  - User name, email, and ID
  - Feedback ID
  - Message content
  - Image URL (if provided)

#### ReportContentService
- Sends notification when content is reported
- Includes:
  - Report type and content type
  - Reporter information (name, email, ID)
  - Content owner information (name, ID)
  - Report ID and description

### 5. Environment Variable Configuration

#### Production (`application.properties`)
```properties
# Notification Configuration
# Comma-separated list of email addresses to receive team notifications
notification.emails=${NOTIFICATION_EMAILS:spawnappmarketing@gmail.com}
```

#### Development (`application-dev.properties`)
```properties
# Notification Configuration (dev)
notification.emails=test@example.com
```

## Deployment Instructions

### Setting Up Email Notifications

1. **Set the environment variable** `NOTIFICATION_EMAILS` with comma-separated email addresses:
   ```bash
   export NOTIFICATION_EMAILS="spawnappmarketing@gmail.com,danielagapov1@gmail.com,shane.mander31@gmail.com,danieluhlee@gmail.com"
   ```

2. **For Docker/Cloud deployments**, add the environment variable to your deployment configuration:
   ```yaml
   environment:
     NOTIFICATION_EMAILS: "spawnappmarketing@gmail.com,danielagapov1@gmail.com,shane.mander31@gmail.com,danieluhlee@gmail.com"
   ```

3. **For local testing**, you can add it to your `.env` file or set it directly in your IDE run configuration.

## Email Template Structure

### Beta Access Sign-Up
```
Subject: New Beta Access Sign Up

Body:
- Signup email address
- Link to admin dashboard
```

### Feedback Submission
```
Subject: New Feedback Submission - {Type}

Body:
- Feedback type (Bug Report, Feature Request, etc.)
- User information (name, email, ID)
- Feedback ID
- Message content
- Image link (if applicable)
- Link to admin dashboard
```

### Reported Content
```
Subject: New Content Report - {ReportType}

Body:
- Report type (Bullying, Nudity, etc.)
- Content type (User, Activity, ChatMessage)
- Report ID
- Reporter information (name, email, ID)
- Content owner information (name, ID)
- Description
- Link to admin dashboard
```

## Error Handling

- All email sending is wrapped in try-catch blocks
- Failed email sends are logged but don't prevent the main operation from succeeding
- If one recipient fails, the system continues sending to other recipients
- Uses `MessagingException` for email-specific errors

## Testing

To test the email notifications:

1. **Beta Access**: Submit a new beta access sign-up through the application
2. **Feedback**: Submit feedback through the feedback form
3. **Report Content**: Report a user, activity, or chat message

Check that emails are received at all configured addresses.

## Benefits

1. **Security**: Email addresses are hidden in environment variables, not hardcoded
2. **Flexibility**: Easy to add/remove team members by updating environment variable
3. **Maintainability**: Centralized email template logic in EmailService
4. **Scalability**: Easy to add new notification types
5. **Reliability**: Error handling ensures notifications don't break core functionality

