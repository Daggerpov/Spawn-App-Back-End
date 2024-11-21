package com.danielagapov.spawn.Models.EventInvited;

import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(
        name = "event_invited",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "user_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventInvited implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
