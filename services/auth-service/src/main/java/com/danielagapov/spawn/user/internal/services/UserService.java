package com.danielagapov.spawn.user.internal.services;

import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Minimal user service implementation for the auth microservice.
 * Provides only the user operations needed by auth services.
 * This service directly accesses the shared user database table.
 */
@Service
@AllArgsConstructor
public class UserService implements IUserService {

    private final IUserRepository userRepository;
    private final ILogger logger;

    @Override
    public User getUserEntityById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User));
    }

    @Override
    public User getUserEntityByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User));
    }

    @Override
    public User createAndSaveUser(User user) {
        logger.info("Creating new user with email: " + user.getEmail());
        return userRepository.save(user);
    }

    @Override
    public User saveEntity(User user) {
        return userRepository.save(user);
    }

    @Override
    public UserDTO createAndSaveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
        // In the auth service, we save the user without S3 profile picture upload
        // The full profile picture handling is done by the main service
        User userEntity = UserMapper.toEntity(user);
        userEntity = userRepository.save(userEntity);
        return UserMapper.toDTO(userEntity, List.of());
    }

    @Override
    public void deleteUserById(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.User);
        }
        userRepository.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndStatus(String email, UserStatus status) {
        return userRepository.existsByEmailAndStatus(email, status);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return userRepository.existsById(userId);
    }
}
