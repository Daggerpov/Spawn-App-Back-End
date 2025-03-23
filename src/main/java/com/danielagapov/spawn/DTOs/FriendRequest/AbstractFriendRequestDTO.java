package com.danielagapov.spawn.DTOs.FriendRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public abstract class AbstractFriendRequestDTO {
    private UUID id;

    @Override
    public boolean equals(Object obj) {
        AbstractFriendRequestDTO other = (AbstractFriendRequestDTO) obj;
        return this.id.equals(other.getId());
    }
}
