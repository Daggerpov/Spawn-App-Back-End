package com.danielagapov.spawn.activity.api.dto;

import com.danielagapov.spawn.user.api.dto.MinimalFriendDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for activity types.
 * 
 * Note: associatedFriends uses MinimalFriendDTO instead of BaseUserDTO to reduce memory usage.
 * MinimalFriendDTO only contains essential fields (id, username, name, profilePicture)
 * needed for displaying friends in activity type selection UI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTypeDTO implements Serializable {
    private UUID id;
    private String title;
    private List<MinimalFriendDTO> associatedFriends;
    private String icon;
    private int orderNum;
    private UUID ownerUserId;
    private Boolean isPinned;
}
