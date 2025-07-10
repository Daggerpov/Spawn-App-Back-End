package com.danielagapov.spawn.DTOs.Activity;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.Enums.ActivityCategory;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for external activity invites - contains only essential information
 * needed for the activity invite page without requiring authentication
 */
@Getter
@Setter
public class ActivityInviteDTO extends AbstractActivityDTO {
    private String location; // Just the location name, not full LocationDTO
    private String creatorName;
    private String creatorUsername;
    private String description; // Maps to 'note' field in the database
    private List<BaseUserDTO> attendees; // Participating and invited users combined
    private int totalAttendees; // Total count of attendees
    
    public ActivityInviteDTO(UUID id,
                           String title,
                           OffsetDateTime startDateTime,
                           OffsetDateTime endTime,
                           String note,
                           String icon,
                           ActivityCategory category,
                           Instant createdAt,
                           String location,
                           String creatorName,
                           String creatorUsername,
                           List<BaseUserDTO> attendees,
                           int totalAttendees) {
        super(id, title, startDateTime, endTime, note, icon, category, createdAt);
        this.location = location;
        this.creatorName = creatorName;
        this.creatorUsername = creatorUsername;
        this.description = note; // Map note to description for clarity
        this.attendees = attendees;
        this.totalAttendees = totalAttendees;
    }
} 