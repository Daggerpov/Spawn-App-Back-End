package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity for storing user notification preferences
 */
@Entity
@Table(name = "notification_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NotificationPreferences {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    /**
     * User who owns these preferences
     */
    @OneToOne(cascade = {CascadeType.ALL, CascadeType.REMOVE})
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;
    
    /**
     * Whether the user wants to receive friend request notifications
     */
    @Column(nullable = false)
    private boolean friendRequestsEnabled = true;
    
    /**
     * Whether the user wants to receive Activity invite notifications
     */
    @Column(nullable = false)
    private boolean ActivityInvitesEnabled = true;
    
    /**
     * Whether the user wants to receive Activity update notifications
     */
    @Column(nullable = false)
    private boolean ActivityUpdatesEnabled = true;
    
    /**
     * Whether the user wants to receive chat message notifications
     */
    @Column(nullable = false)
    private boolean chatMessagesEnabled = true;
} 