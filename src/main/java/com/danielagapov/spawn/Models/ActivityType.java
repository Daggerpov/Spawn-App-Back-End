package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
@Setter
public class ActivityType {
    @Id
    @GeneratedValue
    private UUID id;
    private String title;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<User> associatedFriends = new ArrayList<>(); // Initialize with empty list

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
    private User creator;
    @Column(unique = true)
    private Integer orderNum;
    @Column(length = 100, columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci") // For Emojis
    private String icon = "⭐"; // Default value


    public ActivityType(User creator, String title, String icon) {
        this.creator = creator;
        this.title = title;
        this.icon = icon;
        this.associatedFriends = new ArrayList<>(); // Initialize with empty list
    }
}
