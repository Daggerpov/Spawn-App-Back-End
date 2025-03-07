package com.danielagapov.spawn.Exceptions;

import com.danielagapov.spawn.Enums.EntityType;

import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;

public class UsersNotFoundException extends BasesNotFoundException{
    // note: this is, and should be, used for all exceptions based on lists of users
    // this includes (not limited to) users and friends
    
    public UsersNotFoundException() {
        super(EntityType.User);
    }
}
