package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.EventUsersId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;

@Entity
@Table(name = "event_users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventUser implements Serializable {
    @EmbeddedId
    private EventUsersId id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    private ParticipationStatus status;
}
