package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ReportType;
import com.danielagapov.spawn.Enums.ResolutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportedContent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL)
    // Even if the reporting user deletes their account, they might have filed a valid report
    private User reporter;

    @Column(nullable = false)
    private UUID contentId;

    @Column(nullable = false)
    private EntityType contentType;

    @Column(nullable = false)
    private Instant timeReported;

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
