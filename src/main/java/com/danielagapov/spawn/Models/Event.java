package com.danielagapov.spawn.Models;

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
public class Event implements Serializable {
        private @Id
        @GeneratedValue UUID id;

        private String title;
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;

        @ManyToOne
        @JoinColumn(name = "location_id", referencedColumnName = "id", nullable = false)
        private Location location;

        private String note;

        @ManyToOne
        @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
        private User creator;
}
