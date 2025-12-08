package com.danielagapov.spawn.user.internal.domain;

    import jakarta.persistence.*;
    import lombok.*;
    import org.hibernate.annotations.OnDelete;
    import org.hibernate.annotations.OnDeleteAction;

    import java.util.UUID;

    /**
     * Represents a user that has been blocked by another user.
     */

    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Table(uniqueConstraints = {
            @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
    })
    public class BlockedUser {

        @Id
        @GeneratedValue
        private UUID id;

        @ManyToOne(optional = false)
        @JoinColumn(name = "blocker_id", nullable = false)
        @OnDelete(action = OnDeleteAction.CASCADE)
        private User blocker;

        @ManyToOne(optional = false)
        @JoinColumn(name = "blocked_id", nullable = false)
        @OnDelete(action = OnDeleteAction.CASCADE)
        private User blocked;

        //optional reason for blocking
        @Column(nullable = true, length = 500)
        private String reason;
    }
