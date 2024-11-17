package com.danielagapov.spawn.Models.FriendTag;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public record FriendTag(
        @Id
        Long id,
        String displayName,
        String color // TODO: investigate data type later
) implements Serializable {
}
