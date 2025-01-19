package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.LocationDTO;
import com.danielagapov.spawn.Services.Location.ILocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("api/v1/locations")
public class LocationController {
    private final ILocationService locationService;

    public LocationController(ILocationService locationService) {
        this.locationService = locationService;
    }

    // full path: /api/v1/locations
    @GetMapping
    public ResponseEntity<List<LocationDTO>> getLocations() {
        try {
            return new ResponseEntity<>(locationService.getAllLocations(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }