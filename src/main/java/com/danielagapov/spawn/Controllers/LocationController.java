package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.Event.LocationDTO;
import com.danielagapov.spawn.Services.Location.ILocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController()
@RequestMapping("api/v1/locations")
public class LocationController {
    private final ILocationService locationService;

    public LocationController(ILocationService locationService) {
        this.locationService = locationService;
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/locations
    @GetMapping
    public ResponseEntity<List<LocationDTO>> getLocations() {
        try {
            return new ResponseEntity<>(locationService.getAllLocations(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // TL;DR: Don't remove this endpoint; it may become useful.
    @Deprecated(since = "Not being used on mobile currently.")
    // full path: /api/v1/locations/{id}
    @GetMapping("{id}")
    public ResponseEntity<LocationDTO> getLocationById(@PathVariable UUID id) {
        if (id == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        try {
            return new ResponseEntity<>(locationService.getLocationById(id), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}