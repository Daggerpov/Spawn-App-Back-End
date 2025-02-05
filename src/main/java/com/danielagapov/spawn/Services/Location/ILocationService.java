package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Models.Location;

import java.util.List;
import java.util.UUID;

public interface ILocationService {
    List<LocationDTO> getAllLocations();
    LocationDTO getLocationById(UUID id);
    Location getLocationEntityById(UUID id);
    Location save(Location location);
}