package com.danielagapov.spawn.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@Setter
public class RecommendedFriendUserDTO extends FullUserDTO implements Serializable, IOnboardedUserDTO {
    int mutualFriendCount;
}
