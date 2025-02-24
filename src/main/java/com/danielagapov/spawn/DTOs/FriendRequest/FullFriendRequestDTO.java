package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FullFriendRequestDTO implements Serializable {
    UUID id;
    FullUserDTO senderUser;
    FullUserDTO receiverUser;

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof FullFriendRequestDTO other))
            return false;
        return other.getId().equals(this.id) && other.getSenderUser().equals(this.senderUser) && other.getReceiverUser().equals(this.receiverUser);
    }
}
