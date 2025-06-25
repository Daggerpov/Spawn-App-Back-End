package com.danielagapov.spawn.DTOs.ActivityType;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ActivityTypeDTO implements Serializable {
    private UUID id;
    private String title;
    private List<BaseUserDTO> associatedFriends;
    private String icon;
    private int orderNum;
    private UUID ownerUserId;
    private Boolean isPinned;
}
