package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.Models.Location;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing Location entities and operations.
 * Provides methods for retrieving, saving, and converting location data.
 */
public interface ILocationService {
    
    /**
     * Retrieves all locations from the database and converts them to DTOs.
     * 
     * @return List of LocationDTO objects representing all locations
     * @throws com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException if database access fails
     */
    List<LocationDTO> getAllLocations();
    
    /**
     * Retrieves a specific location by its unique identifier and converts it to a DTO.
     * 
     * @param id The unique identifier of the location to retrieve
     * @return LocationDTO object representing the requested location
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if location with given ID is not found
     */
    LocationDTO getLocationById(UUID id);
    
    /**
     * Retrieves a specific location entity by its unique identifier.
     * This method returns the actual Location model object, not a DTO.
     * 
     * @param id The unique identifier of the location to retrieve
     * @return Location entity object
     * @throws com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException if location with given ID is not found
     */
    Location getLocationEntityById(UUID id);
    
    /**
     * Saves a location entity to the database.
     * 
     * @param location The Location entity to save
     * @return The saved Location entity with updated information
     * @throws com.danielagapov.spawn.Exceptions.ApplicationException if saving fails due to database issues
     */
    Location save(Location location);
}