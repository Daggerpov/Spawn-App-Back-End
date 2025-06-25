package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTypeDTO {
    private UUID id;
    private String title;
    private String icon;
    private List<BaseUserDTO> associatedFriends;
    private Integer orderNum;
    private Boolean isPinned;
} 