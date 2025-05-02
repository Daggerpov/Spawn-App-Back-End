package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.CreateUserInterestDTO;
import com.danielagapov.spawn.DTOs.UserInterestDTO;
import com.danielagapov.spawn.Services.UserInterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/interests")
public class UserInterestController {

    private final UserInterestService userInterestService;

    @Autowired
    public UserInterestController(UserInterestService userInterestService) {
        this.userInterestService = userInterestService;
    }

    @GetMapping
    public ResponseEntity<List<UserInterestDTO>> getUserInterests(@PathVariable UUID userId) {
        List<UserInterestDTO> interests = userInterestService.getUserInterests(userId);
        return ResponseEntity.ok(interests);
    }

    @PostMapping
    public ResponseEntity<UserInterestDTO> addUserInterest(
            @RequestBody CreateUserInterestDTO createUserInterestDTO) {
        UserInterestDTO createdInterest = userInterestService.addUserInterest(createUserInterestDTO);
        return new ResponseEntity<>(createdInterest, HttpStatus.CREATED);
    }
} 