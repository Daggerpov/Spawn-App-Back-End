package com.danielagapov.spawn.DTOs;

import java.util.List;
import java.util.UUID;

public class ContactCrossReferenceRequestDTO {
    private List<String> phoneNumbers;
    private UUID requestingUserId;

    public ContactCrossReferenceRequestDTO() {}

    public ContactCrossReferenceRequestDTO(List<String> phoneNumbers, UUID requestingUserId) {
        this.phoneNumbers = phoneNumbers;
        this.requestingUserId = requestingUserId;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public UUID getRequestingUserId() {
        return requestingUserId;
    }

    public void setRequestingUserId(UUID requestingUserId) {
        this.requestingUserId = requestingUserId;
    }
} 