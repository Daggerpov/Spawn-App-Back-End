package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.UserParticipationStatus;
import com.danielagapov.spawn.Models.CompositeKeys.EventInvitedId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "event_users")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventUser implements Serializable {
    // @EmbeddedId
    // private EventParticipantsId id;

    // TODO: switch to embeddedID
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private UserParticipationStatus status;
}
