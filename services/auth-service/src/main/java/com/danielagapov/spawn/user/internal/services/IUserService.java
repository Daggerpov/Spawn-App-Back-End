package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.user.internal.domain.User;

import java.util.UUID;

/**
 * Minimal user service interface for the auth microservice.
 * Contains only the user operations needed by auth services.
 */
public interface IUserService {

    User getUserEntityById(UUID id);

    User getUserEntityByUsername(String username);

    User getUserByEmail(String email);

    User createAndSaveUser(User user);

    User saveEntity(User user);

    UserDTO createAndSaveUserWithProfilePicture(UserDTO user, byte[] profilePicture);

    void deleteUserById(UUID id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndStatus(String email, UserStatus status);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByUserId(UUID userId);
}
