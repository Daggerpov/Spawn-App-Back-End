package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.BetaAccessSignUpDTO;
import com.danielagapov.spawn.Models.BetaAccessSignUp;

import java.util.List;
import java.util.stream.Collectors;

public class BetaAccessSignUpMapper {

    public static BetaAccessSignUpDTO toDTO(BetaAccessSignUp entity) {
        return new BetaAccessSignUpDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getSignedUpAt(),
                entity.getAdditionalComments(),
                entity.getInstagramUsername()
        );
    }

    public static BetaAccessSignUp toEntity(BetaAccessSignUpDTO dto) {
        return new BetaAccessSignUp(
                dto.getId(),
                dto.getEmail(),
                dto.getFirstName(),
                dto.getLastName(),
                dto.getSignedUpAt(),
                dto.getAdditionalComments(),
                dto.getInstagramUsername()
        );
    }

    public static List<BetaAccessSignUpDTO> toDTOList(List<BetaAccessSignUp> entities) {
        return entities.stream()
                .map(BetaAccessSignUpMapper::toDTO)
                .collect(Collectors.toList());
    }
}
