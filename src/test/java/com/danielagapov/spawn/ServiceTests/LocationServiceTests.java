package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.activity.internal.domain.Location;
import com.danielagapov.spawn.activity.internal.repositories.ILocationRepository;
import com.danielagapov.spawn.activity.internal.services.LocationService;
import com.danielagapov.spawn.shared.exceptions.ApplicationException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LocationService
 * Tests location management operations with edge cases
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTests {

    @Mock
    private ILocationRepository locationRepository;

    @Mock
    private ILogger logger;

    @InjectMocks
    private LocationService locationService;

    private UUID locationId;
    private Location testLocation;

    @BeforeEach
    void setUp() {
        locationId = UUID.randomUUID();
        testLocation = new Location();
        testLocation.setId(locationId);
        testLocation.setName("Test Location");
        testLocation.setLatitude(40.7128);
        testLocation.setLongitude(-74.0060);
    }

    // MARK: - Save Location Tests

    @Test
    void save_ShouldReturnLocation_WhenValidLocation() {
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertNotNull(result);
        assertEquals(locationId, result.getId());
        assertEquals("Test Location", result.getName());
        assertEquals(40.7128, result.getLatitude());
        assertEquals(-74.0060, result.getLongitude());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldThrowApplicationException_WhenDatabaseError() {
        when(locationRepository.save(any(Location.class)))
                .thenThrow(new DataAccessException("Database error") {});

        // LocationService wraps DataAccessException into ApplicationException
        assertThrows(ApplicationException.class, () -> locationService.save(testLocation));
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleNullName_WhenNameNotProvided() {
        testLocation.setName(null);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertNotNull(result);
        assertNull(result.getName());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleZeroCoordinates_WhenAtOrigin() {
        testLocation.setLatitude(0.0);
        testLocation.setLongitude(0.0);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(0.0, result.getLatitude());
        assertEquals(0.0, result.getLongitude());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleNegativeCoordinates_WhenInSouthernOrWesternHemisphere() {
        testLocation.setLatitude(-33.8688);
        testLocation.setLongitude(-151.2093);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(-33.8688, result.getLatitude());
        assertEquals(-151.2093, result.getLongitude());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleExtremeLatitude_WhenNearPoles() {
        testLocation.setLatitude(89.9);
        testLocation.setLongitude(0.0);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(89.9, result.getLatitude());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleExtremeLongitude_WhenNearDateLine() {
        testLocation.setLatitude(0.0);
        testLocation.setLongitude(179.9);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(179.9, result.getLongitude());
        verify(locationRepository, times(1)).save(testLocation);
    }

    // MARK: - Get Location Tests

    @Test
    void getLocationEntityById_ShouldReturnLocation_WhenLocationExists() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(testLocation));

        Location result = locationService.getLocationEntityById(locationId);

        assertNotNull(result);
        assertEquals(locationId, result.getId());
        verify(locationRepository, times(1)).findById(locationId);
    }

    @Test
    void getLocationEntityById_ShouldThrowException_WhenLocationNotFound() {
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(
            BaseNotFoundException.class,
            () -> locationService.getLocationEntityById(locationId)
        );

        assertEquals(EntityType.Location, exception.entityType);
        verify(locationRepository, times(1)).findById(locationId);
    }

    // MARK: - Delete Location Tests
    // Note: Delete methods are not currently implemented in LocationService
    // Commented out until delete functionality is added

    /*
    @Test
    void delete_ShouldReturnTrue_WhenLocationExists() {
        when(locationRepository.existsById(locationId)).thenReturn(true);
        doNothing().when(locationRepository).deleteById(locationId);

        boolean result = locationService.delete(locationId);

        assertTrue(result);
        verify(locationRepository, times(1)).existsById(locationId);
        verify(locationRepository, times(1)).deleteById(locationId);
    }

    @Test
    void delete_ShouldThrowException_WhenLocationNotFound() {
        when(locationRepository.existsById(locationId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(
            BaseNotFoundException.class,
            () -> locationService.delete(locationId)
        );

        assertEquals(EntityType.Location, exception.entityType);
        verify(locationRepository, times(1)).existsById(locationId);
        verify(locationRepository, never()).deleteById(any());
    }

    @Test
    void delete_ShouldReturnFalse_WhenDatabaseError() {
        when(locationRepository.existsById(locationId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {})
                .when(locationRepository).deleteById(locationId);

        boolean result = locationService.delete(locationId);

        assertFalse(result);
        verify(locationRepository, times(1)).deleteById(locationId);
    }
    */

    // MARK: - Edge Case Tests

    @Test
    void save_ShouldHandleLongLocationName_WhenNameIsVeryLong() {
        String longName = "A".repeat(255);
        testLocation.setName(longName);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(longName, result.getName());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleSpecialCharacters_WhenNameHasUnicode() {
        testLocation.setName("Café ☕ & Restaurant 🍽️");
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals("Café ☕ & Restaurant 🍽️", result.getName());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleHighPrecisionCoordinates_WhenManyDecimalPlaces() {
        testLocation.setLatitude(40.712775897);
        testLocation.setLongitude(-74.006008914);
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals(40.712775897, result.getLatitude(), 0.000000001);
        assertEquals(-74.006008914, result.getLongitude(), 0.000000001);
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleEmptyName_WhenNameIsEmptyString() {
        testLocation.setName("");
        when(locationRepository.save(any(Location.class))).thenReturn(testLocation);

        Location result = locationService.save(testLocation);

        assertEquals("", result.getName());
        verify(locationRepository, times(1)).save(testLocation);
    }

    @Test
    void save_ShouldHandleMultipleSaves_WhenSavingDifferentLocations() {
        Location location1 = new Location();
        location1.setName("Location 1");
        location1.setLatitude(40.0);
        location1.setLongitude(-74.0);

        Location location2 = new Location();
        location2.setName("Location 2");
        location2.setLatitude(41.0);
        location2.setLongitude(-75.0);

        when(locationRepository.save(location1)).thenReturn(location1);
        when(locationRepository.save(location2)).thenReturn(location2);

        Location result1 = locationService.save(location1);
        Location result2 = locationService.save(location2);

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("Location 1", result1.getName());
        assertEquals("Location 2", result2.getName());
        verify(locationRepository, times(1)).save(location1);
        verify(locationRepository, times(1)).save(location2);
    }

    @Test
    void getLocationEntityById_ShouldHandleNullId_WhenIdIsNull() {
        when(locationRepository.findById(null)).thenReturn(Optional.empty());

        assertThrows(BaseNotFoundException.class, () -> locationService.getLocationEntityById(null));
        verify(locationRepository, times(1)).findById(null);
    }

    /*
    @Test
    void delete_ShouldHandleNullId_WhenIdIsNull() {
        when(locationRepository.existsById(null)).thenReturn(false);

        assertThrows(BaseNotFoundException.class, () -> locationService.delete(null));
        verify(locationRepository, times(1)).existsById(null);
    }
    */

    @Test
    void save_ShouldHandleSameCoordinates_WhenMultipleLocationsSamePlace() {
        Location location1 = new Location();
        location1.setName("Building A");
        location1.setLatitude(40.7128);
        location1.setLongitude(-74.0060);

        Location location2 = new Location();
        location2.setName("Building B");
        location2.setLatitude(40.7128);
        location2.setLongitude(-74.0060);

        when(locationRepository.save(location1)).thenReturn(location1);
        when(locationRepository.save(location2)).thenReturn(location2);

        Location result1 = locationService.save(location1);
        Location result2 = locationService.save(location2);

        assertEquals(40.7128, result1.getLatitude());
        assertEquals(40.7128, result2.getLatitude());
        assertEquals(-74.0060, result1.getLongitude());
        assertEquals(-74.0060, result2.getLongitude());
        verify(locationRepository, times(2)).save(any(Location.class));
    }
}
