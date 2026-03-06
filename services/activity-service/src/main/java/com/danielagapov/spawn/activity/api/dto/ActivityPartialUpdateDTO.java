package com.danielagapov.spawn.activity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for partial activity updates using PATCH requests.
 * This DTO allows for updating specific fields of an activity without requiring all fields.
 * All fields are optional and only non-null values will be processed.
 */
public class ActivityPartialUpdateDTO {
    
    private String title;
    private String icon;
    
    @JsonProperty("startTime")
    private String startTime; // ISO8601 formatted string
    
    @JsonProperty("endTime") 
    private String endTime;   // ISO8601 formatted string
    
    @JsonProperty("participantLimit")
    private Integer participantLimit;
    
    private String note;

    // Default constructor
    public ActivityPartialUpdateDTO() {}

    // Constructor with all fields
    public ActivityPartialUpdateDTO(String title, String icon, String startTime, String endTime, 
                                  Integer participantLimit, String note) {
        this.title = title;
        this.icon = icon;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantLimit = participantLimit;
        this.note = note;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getParticipantLimit() {
        return participantLimit;
    }

    public void setParticipantLimit(Integer participantLimit) {
        this.participantLimit = participantLimit;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "ActivityPartialUpdateDTO{" +
                "title='" + title + '\'' +
                ", icon='" + icon + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", participantLimit=" + participantLimit +
                ", note='" + note + '\'' +
                '}';
    }
}
