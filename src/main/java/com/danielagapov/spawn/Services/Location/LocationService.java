package com.danielagapov.spawn.Services.Location;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    public List<LocationDTO> getAllLocations() {
        try {
            return LocationMapper.toDTOList(repository.findAll());
        } catch (DataAccessException e) {
            logger.log(e.getMessage());
            throw new BasesNotFoundException(EntityType.FriendTag);
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }

    public Location getLocationById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new BaseNotFoundException(EntityType.Location, id));
    }
}