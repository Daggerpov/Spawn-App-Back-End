package com.danielagapov.spawn.activity.internal.domain;

import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Entity
@Table(
        name = "activity_user",
        indexes = {
                @Index(name = "idx_activity_id", columnList = "activity_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_activity_status", columnList = "activity_id, status")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ActivityUser implements Serializable {
    @EmbeddedId
    private ActivityUsersId id;

    @ManyToOne
    @MapsId("activityId")
    @JoinColumn(name = "activity_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Activity activity;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User user;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;
}
