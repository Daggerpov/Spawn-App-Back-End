package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class FriendRequestDTO implements Serializable {
    UUID id;
    UUID senderUserId;
    UUID receiverUserId;
}
