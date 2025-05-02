package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.DTOs.User.Profile.CreateUserInterestDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserInterestDTO;
import com.danielagapov.spawn.Services.UserInterest.IUserInterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/{userId}/interests")
public class UserInterestController {

    private final IUserInterestService userInterestService;

    @Autowired
    public UserInterestController(IUserInterestService userInterestService) {
        this.userInterestService = userInterestService;
    }

    @GetMapping
    public ResponseEntity<List<UserInterestDTO>> getUserInterests(@PathVariable UUID userId) {
        List<UserInterestDTO> interests = userInterestService.getUserInterests(userId);
        return ResponseEntity.ok(interests);
    }

    @PostMapping
    public ResponseEntity<UserInterestDTO> addUserInterest(
            @PathVariable UUID userId,
            @RequestBody CreateUserInterestDTO createUserInterestDTO) {
        // Set the userId from the path parameter
        createUserInterestDTO.setUserId(userId);
        UserInterestDTO createdInterest = userInterestService.addUserInterest(createUserInterestDTO);
        return new ResponseEntity<>(createdInterest, HttpStatus.CREATED);
    }
} 