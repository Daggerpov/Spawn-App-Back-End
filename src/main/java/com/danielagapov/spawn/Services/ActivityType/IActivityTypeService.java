package com.danielagapov.spawn.Services.ActivityType;

import com.danielagapov.spawn.DTOs.ActivityType.ActivityTypeDTO;
import com.danielagapov.spawn.DTOs.ActivityType.BatchActivityTypeUpdateDTO;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.User.User;

import java.util.List;
import java.util.UUID;

public interface IActivityTypeService {

    List<ActivityTypeDTO> getActivityTypesByUserId(UUID userId);

    void updateActivityTypes(UUID userId, BatchActivityTypeUpdateDTO activityTypeDTOs);

    void initializeDefaultActivityTypesForUser(User user);

    void setOrderNumber(ActivityType activityType);
}
