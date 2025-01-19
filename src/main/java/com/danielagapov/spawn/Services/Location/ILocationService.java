package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.LocationDTO;

import java.util.List;

public interface ILocationService {
    List<LocationDTO> getAllLocations();
}