package com.danielagapov.spawn.chat.internal.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal reference to the shared activity table for JPA FK from chat_message.
 * Chat-service uses shared DB and does not own activity data.
 */
@Entity
@Table(name = "activity")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActivityRef {
    @Id
    private UUID id;
}
