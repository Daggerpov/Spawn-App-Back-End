package com.danielagapov.spawn.shared.util;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
// Represents a tuple of 3 items
public class Triple<X, Y, Z> {
    private X x;
    private Y y;
    private Z z;

    public X getFirst() {
        return this.x;
    }

    public Y getSecond() {
        return this.y;
    }

    public Z getThird() {
        return this.z;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Triple other))
            return false;
        return other.getFirst().equals(this.getFirst()) && other.getSecond().equals(this.getSecond()) && other.getThird().equals(this.getThird());
    }
}
