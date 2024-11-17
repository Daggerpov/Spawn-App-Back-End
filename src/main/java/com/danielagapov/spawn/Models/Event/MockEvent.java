package com.danielagapov.spawn.Models.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MockEvent{
    @Id
    Long id;
    String title;
    String startTime; // TODO: investigate data type later
    String endTime; // TODO: investigate data type later
    String location; // TODO: investigate data type later
    String note;

    public MockEvent() {
    }

    public MockEvent(Long id, String title, String startTime, String endTime, String location, String note) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
