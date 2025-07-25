package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCrossReferenceRequestDTO implements Serializable {
    private List<String> phoneNumbers;
    private UUID requestingUserId;
} 