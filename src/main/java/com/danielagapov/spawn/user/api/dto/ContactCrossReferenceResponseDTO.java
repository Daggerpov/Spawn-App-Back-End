package com.danielagapov.spawn.user.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
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