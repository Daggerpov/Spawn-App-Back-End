package com.danielagapov.spawn.Enums;

public enum EntityType {
    // Base Entities
    ChatMessage("Chat Message"),
    Activity("Activity"),
    FriendTag("Friend Tag"),
    User("User"),
    FriendRequest("Friend Request"),
    BetaAccessSignUp("Beta Access Sign Up"),

    // Related to Base Entities
    Location("Location"),
    ChatMessageLike("Chat Message Like"),
    ActivityUser("Activity User"),

    // Unrelated to Base Entities
    ExternalIdMap("External Id Map"),
    ReportedContent("Reported Content"),
    FeedbackSubmission("Feedback Submission");

    private final String description;

    EntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
