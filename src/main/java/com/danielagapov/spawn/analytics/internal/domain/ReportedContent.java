package com.danielagapov.spawn.analytics.internal.domain;

import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ReportType;
import com.danielagapov.spawn.shared.util.ResolutionStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/*
 * This entity represents a report that a user has made, against another user's account or content.
 * "Content" includes chat message, Activity, and user account.
 *
 * ReportType is used to categorize the report with how the reported content violates a policy (e.g. Bullying, Nudity, etc.)
 * ResolutionStatus is used to indicate whether the report has been investigated by a Spawn Admin and if so,
 * what decision was made regarding the report.
 */
public class ReportedContent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    // Even if the reporting user deletes their account, they might have filed a valid report that should be investigated
    private User reporter;

    @Column(nullable = false)
    private UUID contentId;

    @Column(nullable = false)
    private EntityType contentType;

    @Column(nullable = false)
    private OffsetDateTime timeReported;

    @Column(nullable = false)
    private ResolutionStatus resolution;

    @Column(nullable = false)
    private ReportType reportType;

    private String description;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false)
    private User contentOwner; // owner of the account/content that has been reported
}
