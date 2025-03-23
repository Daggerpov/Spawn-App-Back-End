package com.danielagapov.spawn.DTOs.Notification;

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
     * Whether the user wants to receive event invite notifications
     */
    private boolean eventInvitesEnabled;
    
    /**
     * Whether the user wants to receive event update notifications
     */
    private boolean eventUpdatesEnabled;
    
    /**
     * Whether the user wants to receive chat message notifications
     */
    private boolean chatMessagesEnabled;
    
    /**
     * The user ID associated with these preferences
     */
    private UUID userId;
} 