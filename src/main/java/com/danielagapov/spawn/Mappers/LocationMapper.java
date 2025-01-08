package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Models.Location;

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
                dto.id(),
                dto.name(),
                dto.latitude(),
                dto.longitude()
        );
    }
}
