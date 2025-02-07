package com.danielagapov.spawn.DTOs;

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
}
