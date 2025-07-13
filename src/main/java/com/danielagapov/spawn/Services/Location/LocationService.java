package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LocationService implements ILocationService {
    private final ILocationRepository repository;
    private final ILogger logger;

    @Autowired
    public LocationService(ILocationRepository repository, ILogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    @Override
    @Cacheable(value = "locations")
    public List<LocationDTO> getAllLocations() {
        try {
            return LocationMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.FriendTag);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "locationById", key = "#id")
    public LocationDTO getLocationById(UUID id) {
        return LocationMapper.toDTO(repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.Location, id)));
    }

    @Override
    public Location getLocationEntityById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.Location, id));
    }

    @Override
    public Location save(Location location) {
        try {
            return repository.save(location);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new ApplicationException("Failed to save location", e);
        }
    }
}