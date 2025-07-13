package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.ActivityUsersId;
import com.danielagapov.spawn.Models.User.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

/**
 * An `ActivityUser` represents either a participant or
 * invited user to an activity. Upon creation, the activity's
 * creator can invite another user to an activity, during which
 * they're added into this table with a status = ParticipationStatus.invited.
 * Once they've chosen to participate, their status is flipped to .participating.
 */
@Entity
@Table(name = "activity_user")
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
