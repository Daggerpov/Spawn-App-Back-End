package com.danielagapov.spawn.Utils;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
// Represents a tuple of 3 items
// TODO: overriding equals and hashcode might be useful
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
}
