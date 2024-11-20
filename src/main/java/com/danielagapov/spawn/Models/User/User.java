package com.danielagapov.spawn.Models.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import java.io.Serializable;

@Entity
public class User  implements Serializable {
        private @Id
        @GeneratedValue Long id;
        private String username;
        private String firstName;
        private String lastName;
        private String bio;

        public User(Long id, String username, String firstName, String lastName, String bio) {
                this.id = id;
                this.username = username;
                this.firstName = firstName;
                this.lastName = lastName;
                this.bio = bio;
        }

        public Long getId() {
                return id;
        }

        public void setId(Long id) {
                this.id = id;
        }

        public String getUsername() {
                return username;
        }

        public void setUsername(String username) {
                this.username = username;
        }

        public String getFirstName() {
                return firstName;
        }

        public void setFirstName(String firstName) {
                this.firstName = firstName;
        }

        public String getLastName() {
                return lastName;
        }

        public void setLastName(String lastName) {
                this.lastName = lastName;
        }

        public String getBio() {
                return bio;
        }

        public void setBio(String bio) {
                this.bio = bio;
        }
}
