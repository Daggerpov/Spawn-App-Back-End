package com.danielagapov.spawn.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for notification emails.
 * Reads the notification email list from environment variables for security.
 */
@Configuration
public class NotificationConfig {
    
    @Value("${notification.emails:spawnappmarketing@gmail.com}")
    private String notificationEmailsString;
    
    /**
     * Get the list of notification email addresses.
     * Emails should be comma-separated in the environment variable.
     * 
     * @return List of email addresses to send notifications to
     */
    public List<String> getNotificationEmails() {
        return Arrays.stream(notificationEmailsString.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .toList();
    }
}

