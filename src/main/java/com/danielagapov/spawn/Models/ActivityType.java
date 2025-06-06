package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Getter
public class ActivityType {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    private List<UserFriendTag> associatedFriends; // TODO: refactor to friend table when possible

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "creator_id", referencedColumnName = "id", nullable = false)
    private User creator;
    private int orderNum;

    private String icon;
    private String colorHexCode;

}
