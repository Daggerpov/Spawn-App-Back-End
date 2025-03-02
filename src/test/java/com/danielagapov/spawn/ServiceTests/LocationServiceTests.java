package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.Event.LocationDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Services.Location.LocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LocationServiceTests {

    @Mock
    private ILocationRepository locationRepository;

    @Mock
    private com.danielagapov.spawn.Exceptions.Logger.ILogger logger;

    @InjectMocks
    private LocationService locationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllLocations_ShouldReturnLocationDTOList_WhenLocationsExist() {
        // Arrange
        Location location1 = new Location(UUID.randomUUID(), "Location 1", 10.0, 20.0);
        Location location2 = new Location(UUID.randomUUID(), "Location 2", 15.0, 25.0);
        when(locationRepository.findAll()).thenReturn(List.of(location1, location2));

        // Act
        List<LocationDTO> result = locationService.getAllLocations();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(locationRepository, times(1)).findAll();
    }

    @Test
    void getAllLocations_ShouldThrowBasesNotFoundException_WhenDataAccessExceptionOccurs() {
        // Arrange
        when(locationRepository.findAll()).thenThrow(new DataAccessException("Database error") {});

        // Act & Assert
        assertThrows(BasesNotFoundException.class, () -> locationService.getAllLocations());
        verify(logger, times(1)).log("Database error");
    }

    @Test
    void getLocationById_ShouldReturnLocationDTO_WhenLocationExists() {
        // Arrange
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Location 1", 10.0, 20.0);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        // Act
        LocationDTO result = locationService.getLocationById(locationId);

        // Assert
        assertNotNull(result);
        assertEquals(locationId, result.getId());
        verify(locationRepository, times(1)).findById(locationId);
    }

    @Test
    void getLocationById_ShouldThrowBaseNotFoundException_WhenLocationDoesNotExist() {
        // Arrange
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BaseNotFoundException.class, () -> locationService.getLocationById(locationId));
        verify(locationRepository, times(1)).findById(locationId);
    }

    @Test
    void getLocationEntityById_ShouldReturnLocation_WhenLocationExists() {
        // Arrange
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Location 1", 10.0, 20.0);
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        // Act
        Location result = locationService.getLocationEntityById(locationId);

        // Assert
        assertNotNull(result);
        assertEquals(locationId, result.getId());
        verify(locationRepository, times(1)).findById(locationId);
    }

    @Test
    void getLocationEntityById_ShouldThrowBaseNotFoundException_WhenLocationDoesNotExist() {
        // Arrange
        UUID locationId = UUID.randomUUID();
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BaseNotFoundException.class, () -> locationService.getLocationEntityById(locationId));
        verify(locationRepository, times(1)).findById(locationId);
    }

    @Test
    void save_ShouldReturnSavedLocation_WhenSuccessful() {
        // Arrange
        Location location = new Location(UUID.randomUUID(), "Location 1", 10.0, 20.0);
        when(locationRepository.save(location)).thenReturn(location);

        // Act
        Location result = locationService.save(location);

        // Assert
        assertNotNull(result);
        assertEquals(location.getId(), result.getId());
        verify(locationRepository, times(1)).save(location);
    }

    @Test
    void save_ShouldThrowApplicationException_WhenDataAccessExceptionOccurs() {
        // Arrange
        Location location = new Location(UUID.randomUUID(), "Location 1", 10.0, 20.0);
        when(locationRepository.save(location)).thenThrow(new DataAccessException("Database error") {});

        // Act & Assert
        assertThrows(ApplicationException.class, () -> locationService.save(location));
        verify(logger, times(1)).log("Database error");
    }
}