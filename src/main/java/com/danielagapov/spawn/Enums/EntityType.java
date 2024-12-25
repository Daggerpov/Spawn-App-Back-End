package com.danielagapov.spawn.Enums;

public enum EntityType {
    // Base entities
    ChatMessage("Chat Message"),
    Event("Event"),
    FriendTag("Friend Tag"),
    User("User"),

    // Relationships between entities
    ChatMessageLike("Chat Message Like");
    // TODO: add more here

    private final String description;

    EntityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
