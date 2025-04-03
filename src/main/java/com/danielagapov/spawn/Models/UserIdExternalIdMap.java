package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Enums.OAuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * This maps an external user id (from Apple or Google)
 * to a spawn's user id, so we can keep track.
 * For now, we're limiting spawn accounts to just one
 * external mapping. So, if you create an account through
 * Google and make a corresponding Spawn user, your Apple
 * account must link to a new Spawn user. 
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class UserIdExternalIdMap {
    @Id
    private String id; // the id (or sub) from external provider like Google OAuth

    // TODO: may need to revisit relationship type if google/apple calendars is a feature later
    @OneToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;
}
