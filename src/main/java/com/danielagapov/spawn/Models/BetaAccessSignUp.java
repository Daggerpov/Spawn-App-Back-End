package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * This entity represents what we'll store from our
 * beta access sign up site. Namely, the `email` field
 * will be most useful, since that's how we'll be sending
 * out our beta through Apple TestFlight for installation.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BetaAccessSignUp implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String email;
    private OffsetDateTime signedUpAt;
    private Boolean hasSubscribedToNewsletter;

    @PrePersist
    public void prePersist() {
        // this happens upon persistence to our database in `BetaAccessSignUpService::signUp()`
        if (this.signedUpAt == null) {
            this.signedUpAt = OffsetDateTime.now();
        }
    }
}