package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.CompositeKeys.FriendId;
import com.danielagapov.spawn.Models.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "friend")
@IdClass(FriendId.class)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Friend implements Serializable {

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id_1", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User friend1;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id_2", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User friend2;

    private Instant lastUpdated;
}