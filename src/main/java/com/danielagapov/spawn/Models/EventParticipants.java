package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.CompositeKeys.EventParticipantsId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "event_participants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventParticipants implements Serializable {
    @EmbeddedId
    private EventParticipantsId id;

    @ManyToOne
    @MapsId("eventId")
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
