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
        private String color; // TODO: investigate data type later | represents hex code?
}
