package com.danielagapov.spawn.Controllers.User.Profile;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.UserInterest.IUserInterestService;
import com.danielagapov.spawn.Util.LoggingUtils;
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
    private final ILogger logger;

    @Autowired
    public UserInterestController(IUserInterestService userInterestService, ILogger logger) {
        this.userInterestService = userInterestService;
        this.logger = logger;
    }

    @GetMapping
    public ResponseEntity<List<String>> getUserInterests(@PathVariable UUID userId) {
        try {
            List<String> interests = userInterestService.getUserInterests(userId);
            interests = interests.stream()
                    .map(interest -> interest.replaceAll("^\"|\"$", ""))
                    .toList();
            return ResponseEntity.ok(interests);
        } catch (Exception e) {
            logger.error("Error getting user interests for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @PostMapping
    public ResponseEntity<String> addUserInterest(
            @PathVariable UUID userId,
            @RequestBody String userInterestName) {
        userInterestName = userInterestName.replaceAll("^\"|\"$", "");
        try {
            String result = userInterestService.addUserInterest(userId, userInterestName);
            return new ResponseEntity<>(result, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error adding user interest for user: " + LoggingUtils.formatUserIdInfo(userId) + " - interest: " + userInterestName + ": " + e.getMessage());
            throw e;
        }
    }

    @DeleteMapping("/{interest}")
    public ResponseEntity<Void> removeUserInterest(
            @PathVariable UUID userId,
            @PathVariable String interest) {
        try {
            boolean removed = userInterestService.removeUserInterest(userId, interest);
            
            if (removed) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error removing user interest for user: " + LoggingUtils.formatUserIdInfo(userId) + " - interest: " + interest + ": " + e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 