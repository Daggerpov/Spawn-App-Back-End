package com.danielagapov.spawn.Models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * This represents a location attached to
 * a particular Activity, with a 1-to-1 relationship.
 * A location cannot exist without an Activity,
 * and this Activity is essentially making an object
 * out of this sub-object to an Activity.
 * A user will be able to input a location with
 * its coordinates + a display name for their friends,
 * since we want it to be easily understandable.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Location implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(length = 200)
    private String name;

    private double latitude;
    private double longitude;
}