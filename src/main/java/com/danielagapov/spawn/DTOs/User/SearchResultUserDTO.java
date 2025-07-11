package com.danielagapov.spawn.DTOs.User;

import com.danielagapov.spawn.Enums.UserRelationshipType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SearchResultUserDTO {
    @JsonProperty("user")
    private BaseUserDTO user;
    
    @JsonProperty("relationshipType")
    private UserRelationshipType relationshipType;
    
    @JsonProperty("mutualFriendCount")
    private Integer mutualFriendCount; // Optional, only for recommended friends
    
    @JsonProperty("friendRequestId")
    private UUID friendRequestId; // Optional, only for incoming friend requests

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SearchResultUserDTO other = (SearchResultUserDTO) obj;
        return user.equals(other.user) && 
               relationshipType == other.relationshipType &&
               java.util.Objects.equals(mutualFriendCount, other.mutualFriendCount) &&
               java.util.Objects.equals(friendRequestId, other.friendRequestId);
    }
} 