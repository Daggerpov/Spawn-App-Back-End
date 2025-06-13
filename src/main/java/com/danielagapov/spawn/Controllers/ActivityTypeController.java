package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/activity-type")
@AllArgsConstructor
public class ActivityTypeController {
    private final IActivityTypeService activityTypeService;
    private final ILogger logger;


    @GetMapping("{userId}")
    public ResponseEntity<List<ActivityTypeDTO>> getActivityTypes(@PathVariable UUID userId) {
        try {
            return ResponseEntity.ok(activityTypeService.getActivityTypesByUserId(userId));
        } catch (Exception e) {
            logger.error("Error getting activity types for user: " + userId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("{userId}")
    public ResponseEntity<?> updateActivityTypes(@PathVariable UUID userId, @RequestBody BatchActivityTypeUpdateDTO batchUpdateDTO) {
        try {
            activityTypeService.updateActivityTypes(userId, batchUpdateDTO);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error updating or deleting activity types for user: " + userId + ": " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
