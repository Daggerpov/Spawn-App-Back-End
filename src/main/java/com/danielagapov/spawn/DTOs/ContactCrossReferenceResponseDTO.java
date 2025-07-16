package com.danielagapov.spawn.DTOs;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactCrossReferenceResponseDTO implements Serializable {
    private List<BaseUserDTO> users;
} 