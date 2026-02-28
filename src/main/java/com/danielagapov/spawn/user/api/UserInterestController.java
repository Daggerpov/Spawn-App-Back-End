package com.danielagapov.spawn.user.api;

import com.danielagapov.spawn.user.internal.services.IUserInterestService;
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
        return ResponseEntity.ok(interests);
    }

    @PutMapping
    public ResponseEntity<List<String>> replaceUserInterests(
            @PathVariable UUID userId,
            @RequestBody List<String> interests) {
        List<String> saved = userInterestService.replaceUserInterests(userId, interests);
        return ResponseEntity.ok(saved);
    }

    @PostMapping
    public ResponseEntity<String> addUserInterest(
            @PathVariable UUID userId,
            @RequestBody String userInterestName) {
        // @RequestBody with a plain JSON string arrives wrapped in quotes; strip them.
        String cleaned = userInterestName.replaceAll("^\"|\"$", "").trim();
        if (cleaned.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String result = userInterestService.addUserInterest(userId, cleaned);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @DeleteMapping("/{interest}")
    public ResponseEntity<Void> removeUserInterest(
            @PathVariable UUID userId,
            @PathVariable String interest) {
        boolean removed = userInterestService.removeUserInterest(userId, interest);
        return removed
                ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
} 