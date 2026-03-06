package com.danielagapov.spawn.activity.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for activity creation that wraps the created activity
 * and optionally includes friend suggestions for activity types.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityCreationResponseDTO {
    private FullFeedActivityDTO activity;
    private ActivityTypeFriendSuggestionDTO friendSuggestion;
    
    /**
     * Create a response with just the activity (no friend suggestion)
     */
    public ActivityCreationResponseDTO(FullFeedActivityDTO activity) {
        this.activity = activity;
        this.friendSuggestion = null;
    }
}
