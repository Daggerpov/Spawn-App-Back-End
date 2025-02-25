package com.danielagapov.spawn.Utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
// Represents a tuple of 3 items
public class Triple<X, Y, Z> {
    public X x;
    public Y y;
    public Z z;
    public X first() {
        return this.x;
    }
    public Y second() {
        return this.y;
    }
    public Z third() {
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
        return other.first().equals(this.first()) && other.second().equals(this.second()) && other.third().equals(this.third());
    }
}
