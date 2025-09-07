package com.danielagapov.spawn.DTOs.ActivityType;

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
public class ActivityTypeFriendSuggestionDTO {
    private UUID activityTypeId;
    private String activityTypeTitle;
    private List<BaseUserDTO> suggestedFriends;
    private boolean shouldShowPrompt;
    
    public ActivityTypeFriendSuggestionDTO(UUID activityTypeId, String activityTypeTitle, List<BaseUserDTO> suggestedFriends) {
        this.activityTypeId = activityTypeId;
        this.activityTypeTitle = activityTypeTitle;
        this.suggestedFriends = suggestedFriends;
        this.shouldShowPrompt = suggestedFriends != null && !suggestedFriends.isEmpty();
    }
}
