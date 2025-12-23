package com.danielagapov.spawn.shared.util;

import com.danielagapov.spawn.analytics.api.dto.BetaAccessSignUpDTO;
import com.danielagapov.spawn.analytics.internal.domain.BetaAccessSignUp;

import java.util.List;
import java.util.stream.Collectors;

public final class BetaAccessSignUpMapper {

    public static BetaAccessSignUpDTO toDTO(BetaAccessSignUp entity) {
        return new BetaAccessSignUpDTO(
                entity.getId(),
                entity.getEmail(),
                entity.getSignedUpAt(),
                entity.getHasSubscribedToNewsletter(),
                entity.getHasBeenEmailed()
        );
    }

    public static BetaAccessSignUp toEntity(BetaAccessSignUpDTO dto) {
        return new BetaAccessSignUp(
                dto.getId(),
                dto.getEmail(),
                dto.getSignedUpAt(),
                dto.getHasSubscribedToNewsletter(),
                dto.getHasBeenEmailed()
        );
    }

    public static List<BetaAccessSignUpDTO> toDTOList(List<BetaAccessSignUp> entities) {
        return entities.stream()
                .map(BetaAccessSignUpMapper::toDTO)
                .collect(Collectors.toList());
    }
}
