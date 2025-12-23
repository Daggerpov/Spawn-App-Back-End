package com.danielagapov.spawn.activity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BatchActivityTypeUpdateDTO implements Serializable {
    private List<ActivityTypeDTO> updatedActivityTypes; // List of updated/newly created Activity Types
    private List<UUID> deletedActivityTypeIds; // List of Activity Type IDs that were deleted
}
