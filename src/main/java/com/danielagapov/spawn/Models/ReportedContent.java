package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.EntityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReportedContent {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne()
    @JoinColumn(nullable = false)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User reporter;

    @Column(nullable = false)
    private UUID contentId;

    @Column(nullable = false)
    private EntityType contentType;

    @Column(nullable = false)
    private Instant timeReported;

    @Column(nullable = false)
    /* TODO: do we want bool isResolved OR we have resolveType which indicates a decision made about this report
        resolveType would be an enum that might take on values BAN, SUSPEND, FALSE, WARN, etc.
     */
    private boolean isResolved;

}
