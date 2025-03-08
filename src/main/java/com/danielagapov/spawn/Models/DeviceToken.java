package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.util.UUID;

/**
 * DeviceToken stores push notification tokens for users' devices.
 * Used to send push notifications to specific devices for various events.
 */
@Entity
@Table(name = "device_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DeviceToken implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
} 