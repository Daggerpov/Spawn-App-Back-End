package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class FriendTagsNotFoundException extends BasesNotFoundException{
    public FriendTagsNotFoundException() {
        super(EntityType.FriendTag);
    }
}
