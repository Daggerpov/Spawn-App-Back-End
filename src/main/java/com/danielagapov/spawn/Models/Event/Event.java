package com.danielagapov.spawn.Models.Event;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
// these two annotations are in place of writing out constructors manually (for readability)
@NoArgsConstructor
@AllArgsConstructor
// these two annotations are in place of writing out getters and setters manually (for readability):
@Getter
@Setter
public class Event implements Serializable {
        private @Id
        @GeneratedValue Long id;
        private String title;
        private String startTime; // TODO: investigate data type later
        private String endTime; // TODO: investigate data type later
        private String location; // TODO: investigate data type later
        private String note;
}
