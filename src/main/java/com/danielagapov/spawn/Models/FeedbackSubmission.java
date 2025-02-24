package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.FeedbackType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FeedbackSubmission implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private FeedbackType type;

    private UUID fromUserId;
    private OffsetDateTime submittedAt;

    @PrePersist
    public void prePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = OffsetDateTime.now();
        }
    }

}
