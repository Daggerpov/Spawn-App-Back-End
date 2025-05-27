package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.Models.Location;

import java.util.List;
import java.util.stream.Collectors;

public class LocationMapper {

    public static LocationDTO toDTO(Location entity) {
        return new LocationDTO(
                entity.getId(),
                entity.getName(),
                entity.getLatitude(),
                entity.getLongitude()
        );
    }

    public static Location toEntity(LocationDTO dto) {
        return new Location(
                dto.getId(),
                dto.getName(),
                dto.getLatitude(),
                dto.getLongitude()
        );
    }

    public static List<LocationDTO> toDTOList(List<Location> entities) {
        return entities.stream()
                .map(LocationMapper::toDTO)
                .collect(Collectors.toList());
    }
}
