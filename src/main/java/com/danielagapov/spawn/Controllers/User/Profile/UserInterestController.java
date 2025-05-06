package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.Services.UserInterest.IUserInterestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}/interests")
public class UserInterestController {

    private final IUserInterestService userInterestService;

    @Autowired
    public UserInterestController(IUserInterestService userInterestService) {
        this.userInterestService = userInterestService;
    }

    @GetMapping
    public ResponseEntity<List<String>> getUserInterests(@PathVariable UUID userId) {
        List<String> interests = userInterestService.getUserInterests(userId);
        interests = interests.stream()
                .map(interest -> interest.replaceAll("^\"|\"$", ""))
                .toList();
        return ResponseEntity.ok(interests);
    }

    @PostMapping
    public ResponseEntity<String> addUserInterest(
            @PathVariable UUID userId,
            @RequestBody String userInterestName) {
        userInterestName = userInterestName.replaceAll("^\"|\"$", "");
        return new ResponseEntity<>(userInterestService.addUserInterest(userId, userInterestName), HttpStatus.CREATED);
    }
} 