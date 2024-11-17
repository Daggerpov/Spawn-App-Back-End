package com.danielagapov.spawn.Models.FriendTag;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MockFriendTag {
    @Id
    Long id;
    String displayName;
    String color;

    public MockFriendTag() {}

    public MockFriendTag(Long id, String displayName, String color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
