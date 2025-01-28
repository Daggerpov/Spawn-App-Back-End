package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Models.Location;

import java.util.List;
import java.util.UUID;

public interface ILocationService {
    List<LocationDTO> getAllLocations();
    Location getLocationById(UUID id);
    LocationDTO getLocationDTOById(UUID id);

}