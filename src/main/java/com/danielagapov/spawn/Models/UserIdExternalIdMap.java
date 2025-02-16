package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserIdExternalIdMap {
    @Id
    private String id; // the id (or sub) from external provider like Google OAuth

    // TODO: may need to revisit relationship type if google/apple calendars is a feature later
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
}
