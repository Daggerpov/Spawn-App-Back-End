package com.danielagapov.spawn.Models.CompositeKeys;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FriendId implements Serializable {
    private UUID friend1;
    private UUID friend2;

    public FriendId() {}

    public FriendId(UUID friend1, UUID friend2) {
        this.friend1 = friend1;
        this.friend2 = friend2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FriendId that)) return false;
        return Objects.equals(friend1, that.friend1) &&
                Objects.equals(friend2, that.friend2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(friend1, friend2);
    }
}