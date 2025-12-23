package com.danielagapov.spawn.activity.api.dto;

import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
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
public class ActivityTypeDTO implements Serializable {
    private UUID id;
    private String title;
    private List<BaseUserDTO> associatedFriends;
    private String icon;
    private int orderNum;
    private UUID ownerUserId;
    private Boolean isPinned;
}
