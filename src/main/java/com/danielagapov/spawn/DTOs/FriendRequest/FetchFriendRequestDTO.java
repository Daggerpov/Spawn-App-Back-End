package com.danielagapov.spawn.DTOs.FriendRequest;

import com.danielagapov.spawn.DTOs.User.UserDTO;
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
public class FetchFriendRequestDTO implements Serializable {
    UUID id;
    UserDTO senderUser;
}
