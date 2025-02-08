package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

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
    private String firstName;
    private String lastName;
    private OffsetDateTime signedUpAt;
    private String additionalComments;
    private String instagramUsername;

    @PrePersist
    public void prePersist() {
        // this happens upon persistence to our database in `BetaAccessSignUpService::signUp()`
        if (this.signedUpAt == null) {
            this.signedUpAt = OffsetDateTime.now();
        }
    }
}