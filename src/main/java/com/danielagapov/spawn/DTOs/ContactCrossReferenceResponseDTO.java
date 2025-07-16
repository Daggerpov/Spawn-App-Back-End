package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import java.util.List;

public class ContactCrossReferenceResponseDTO {
    private List<BaseUserDTO> users;

    public ContactCrossReferenceResponseDTO() {}

    public ContactCrossReferenceResponseDTO(List<BaseUserDTO> users) {
        this.users = users;
    }

    public List<BaseUserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<BaseUserDTO> users) {
        this.users = users;
    }
} 