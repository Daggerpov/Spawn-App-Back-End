package com.danielagapov.spawn.Models.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public record Event(
        @Id
        Long id,
        String title,
        String startTime, // TODO: investigate data type later
        String endTime, // TODO: investigate data type later
        String location, // TODO: investigate data type later
        String note
) implements Serializable {
}
