package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserIdExternalIdMap {
    @Id
    private String id; // the id (or sub) from external provider like Google OAuth

    @OneToOne // TODO: may need to revisit relationship type if google/apple calendars is a feature later
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
}
