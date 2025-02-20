package com.danielagapov.spawn.DTOs;

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
    private Boolean hasSubscribedToNewsletter;
}
