package com.danielagapov.spawn.Mappers;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Models.Location.Location;

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
        Location location = new Location();
        location.setId(dto.id());
        location.setName(dto.name());
        location.setLatitude(dto.latitude());
        location.setLongitude(dto.longitude());
        return location;
    }
}
