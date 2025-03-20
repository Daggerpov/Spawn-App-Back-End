package com.danielagapov.spawn.Enums;

public enum EntityType {
    // Base entities
    ChatMessage("Chat Message"),
    Event("Event"),
    FriendTag("Friend Tag"),
    User("User"),
    ReportedContent("Reported Content"),
    ExternalIdMap("External Id Map"),

    // Relationships between entities
    ChatMessageLike("Chat Message Like"),
    FriendRequest("Friend Request"),
    FeedbackSubmission("Feedback Submission"),
    Location("Location");

    private final String description;

    EntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
