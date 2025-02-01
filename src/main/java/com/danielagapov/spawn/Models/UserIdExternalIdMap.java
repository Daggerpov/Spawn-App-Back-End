package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Enums.ParticipationStatus;
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

    // many to one because of the duplicate case, may want to revisit
    @ManyToOne // TODO: may need to revisit relationship type if google/apple calendars is a feature later
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
}
