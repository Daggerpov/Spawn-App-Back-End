package com.danielagapov.spawn.DTOs.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO that represents a response for checking if two users are friends.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IsFriendResponseDTO {
    private boolean isFriend;
} 