package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "friend_tags"
)
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
        private String colorHexCode; // TODO: investigate data type later | represents hex code?
        @ManyToOne
        @JoinColumn(name = "owner_id", referencedColumnName = "id") //relate friendTag.id() to user.id()
        private User owner;
        //private UUID owner;
        @OneToMany(mappedBy = "id")
        private List<User> friends;
}
