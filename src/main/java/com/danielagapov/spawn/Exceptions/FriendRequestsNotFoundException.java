package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class FriendRequestsNotFoundException extends BasesNotFoundException{
    public FriendRequestsNotFoundException() {
        super(EntityType.FriendRequest);
    }
}
