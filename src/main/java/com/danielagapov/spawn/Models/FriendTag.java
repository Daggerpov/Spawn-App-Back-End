package com.danielagapov.spawn.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/*
 * Represents a unique FriendTag entity. Each FriendTag entity is crucially
 * associated to one User by the ownerId field. Furthermore, each User contains
 * at least one FriendTag, being the "Everyone" friend tag that is added by
 * default.
 */
@Entity
// these two annotations are in place of writing out constructors manually (for readability):
@NoArgsConstructor
@AllArgsConstructor
// these two annotations are in place of writing out getters and setters manually (for readability):
@Getter
@Setter
public class FriendTag implements Serializable {
        private @Id
        @GeneratedValue UUID id;
        private String displayName;
        private String colorHexCode;
        private UUID ownerId;
}
