package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.Models.ActivityType;

import java.util.List;
import java.util.UUID;

public interface IActivityTypeService {

    List<ActivityType> getActivityTypesByUserId(UUID userId);

    void updateActivityTypes(UUID userId, List<ActivityTypeDTO> activityTypeDTOs);
}
