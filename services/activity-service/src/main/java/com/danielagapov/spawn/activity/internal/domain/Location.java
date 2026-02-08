package com.danielagapov.spawn.activity.internal.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Location implements Serializable {
    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Column(length = 200)
    private String name;
    private double latitude;
    private double longitude;
}
