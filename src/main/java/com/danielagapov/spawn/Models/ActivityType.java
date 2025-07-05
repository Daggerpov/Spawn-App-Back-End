package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "UK_activity_type_creator_order", columnNames = {"creator_id", "order_num"})
})
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
    private Integer orderNum;
    @Column(length = 100, columnDefinition = "VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci") // For Emojis
    private String icon = "⭐"; // Default value
    
    @Column(nullable = false)
    private Boolean isPinned = false; // Default value for pinned status

    // Constructor for basic creation
    public ActivityType(User creator, String title, String icon) {
        this.creator = creator;
        this.title = title;
        this.icon = icon;
        this.associatedFriends = new ArrayList<>(); // Initialize with empty list
        this.isPinned = false; // Default to unpinned
    }
    
    // Comprehensive constructor for mapper usage
    public ActivityType(UUID id, String title, List<User> associatedFriends, User creator, Integer orderNum, String icon, Boolean isPinned) {
        this.id = id;
        this.title = title;
        this.associatedFriends = associatedFriends != null ? associatedFriends : new ArrayList<>();
        this.creator = creator;
        this.orderNum = orderNum;
        this.icon = icon != null ? icon : "⭐";
        this.isPinned = isPinned != null ? isPinned : false;
    }
}
