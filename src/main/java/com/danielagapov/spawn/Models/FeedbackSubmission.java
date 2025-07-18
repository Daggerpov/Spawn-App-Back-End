package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.FeedbackStatus;
import com.danielagapov.spawn.Enums.FeedbackType;
import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private User fromUser;


    @Column
    private String fromUserEmail;
    private OffsetDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String resolutionComment;

    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column
    private String imageUrl;

    @PrePersist
    public void prePersist() {
        if (this.submittedAt == null) {
            this.submittedAt = OffsetDateTime.now();
        }
        if (this.status == null) {
            this.status = FeedbackStatus.PENDING;
        }
    }
}
