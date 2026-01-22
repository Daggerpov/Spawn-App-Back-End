package com.danielagapov.spawn.notification.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data transfer object for notification preferences
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDTO {
    /**
     * Whether the user wants to receive friend request notifications
     */
    private boolean friendRequestsEnabled;
    
    /**
     * Whether the user wants to receive Activity invite notifications
     */
    private boolean ActivityInvitesEnabled;
    
    /**
     * Whether the user wants to receive Activity update notifications
     */
    private boolean ActivityUpdatesEnabled;
    
    /**
     * Whether the user wants to receive chat message notifications
     */
    private boolean chatMessagesEnabled;
    
    /**
     * The user ID associated with these preferences
     */
    private UUID userId;
} 