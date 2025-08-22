package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.ResolutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing system error logs that are automatically captured
 * and sent to administrators for monitoring and resolution.
 */
@Entity
@Table(name = "error_log")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorLog implements Serializable {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false, length = 500)
    private String sourceClass;

    @Column(nullable = false, length = 200)
    private String sourceMethod;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(name = "error_level", nullable = false, length = 50)
    private String errorLevel; // ERROR, WARN, etc.

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResolutionStatus status;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @Column(name = "email_sent", nullable = false)
    private Boolean emailSent = false;

    @Column(name = "user_context", length = 1000)
    private String userContext; // Any relevant user/request context

    @PrePersist
    protected void onCreate() {
        occurredAt = Instant.now();
        if (status == null) {
            status = ResolutionStatus.PENDING;
        }
        if (emailSent == null) {
            emailSent = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == ResolutionStatus.RESOLVED && resolvedAt == null) {
            resolvedAt = Instant.now();
        }
    }

    /**
     * Constructor for creating error logs from exceptions
     */
    public ErrorLog(String errorMessage, String stackTrace, String sourceClass, 
                   String sourceMethod, Integer lineNumber, String errorLevel, 
                   String userContext) {
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.sourceClass = sourceClass;
        this.sourceMethod = sourceMethod;
        this.lineNumber = lineNumber;
        this.errorLevel = errorLevel;
        this.userContext = userContext;
        this.status = ResolutionStatus.PENDING;
        this.emailSent = false;
        this.occurredAt = Instant.now();
    }
}

