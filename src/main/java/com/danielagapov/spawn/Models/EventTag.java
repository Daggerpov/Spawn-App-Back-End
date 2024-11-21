package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(
        name = "event_tag",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "tag_id"})
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventTag implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "tag_id", referencedColumnName = "id", nullable = false)
    private FriendTag tag;
}
