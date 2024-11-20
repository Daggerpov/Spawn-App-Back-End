package com.danielagapov.spawn.Models.FriendTag;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.io.Serializable;

@Entity
public class FriendTag implements Serializable {
        private @Id
        @GeneratedValue Long id;
        private String displayName;

        public FriendTag(Long id, String displayName, String color) {
                this.id = id;
                this.displayName = displayName;
                this.color = color;
        }

        public String getColor() {
                return color;
        }

        public void setColor(String color) {
                this.color = color;
        }

        public String getDisplayName() {
                return displayName;
        }

        public void setDisplayName(String displayName) {
                this.displayName = displayName;
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        private String color; // TODO: investigate data type later
}
