package com.danielagapov.spawn.Models;

import com.danielagapov.spawn.Models.CustomGenerators.GenerateIdIfNull;
import com.danielagapov.spawn.Models.CustomGenerators.UserID;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.HibernateException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.factory.internal.AutoGenerationTypeStrategy;
import org.hibernate.id.factory.internal.UUIDGenerationTypeStrategy;

import java.io.Serializable;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class User implements Serializable {
        @Id
        @GeneratedValue(generator = "generatedIdIfNull")
        @UserID
        private UUID id;

        @Column(nullable = false, unique = true) // Ensures the username is unique and not null
        private String username;

        private String firstName;
        private String lastName;
        private String bio;
        private String profilePicture; // TODO: reconsider data type later
}

