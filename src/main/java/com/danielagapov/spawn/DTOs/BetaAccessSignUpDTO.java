package com.danielagapov.spawn.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BetaAccessSignUpDTO implements Serializable {
    private UUID id;
    private String email;
    private OffsetDateTime signedUpAt;
    @JsonProperty("hasSubscribedToNewsletter") // Explicitly define JSON property name
    private Boolean hasSubscribedToNewsletter;
    @JsonProperty("hasBeenEmailed") // Explicitly define JSON property name
    private Boolean hasBeenEmailed;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; // Check if the same reference
        if (obj == null || getClass() != obj.getClass()) return false; // Null check and class check
        BetaAccessSignUpDTO that = (BetaAccessSignUpDTO) obj; // Safe cast
        return id != null && id.equals(that.id); // Compare IDs
    }
}
