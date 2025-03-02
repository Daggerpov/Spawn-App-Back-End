package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.UUID;

@Service
public class OAuthService implements IOAuthService {
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final IUserService userService;
    private final ILogger logger;

    public OAuthService(IUserIdExternalIdMapRepository externalIdMapRepository, IUserService userService, ILogger logger) {
        this.externalIdMapRepository = externalIdMapRepository;
        this.userService = userService;
        this.logger = logger;
    }

    @Override
    public FullUserDTO createUser(UserCreationDTO userCreationDTO, String externalUserId, OAuthProvider provider) {
        UserDTO newUser = new UserDTO(
                userCreationDTO.getId(),
                null,
                userCreationDTO.getUsername(),
                null, // going to set within `makeUser()`
                userCreationDTO.getFirstName(),
                userCreationDTO.getLastName(),
                userCreationDTO.getBio(),
                null,
                userCreationDTO.getEmail()
        );

        return makeUser(newUser, externalUserId, userCreationDTO.getProfilePictureData(), provider);
    }

    @Override
    public FullUserDTO makeUser(UserDTO userDTO, String externalUserId, byte[] profilePicture, OAuthProvider provider) {
        try {
            // TODO: temporary solution
            if (mappingExistsByExternalId(externalUserId)) {
                logger.log(String.format("Existing user detected in makeUser, mapping already exists: {user: %s, externalUserId: %s}", userDTO.getEmail(), externalUserId));
                return userService.getFullUserById(getMapping(externalUserId).getUser().getId());
            }
            if (userDTO.getEmail() != null && userService.existsByEmail(userDTO.getEmail())) {
                logger.log(String.format("Existing user detected in makeUser, email already exists: {user: %s, email: %s}", userDTO.getEmail(), userDTO.getEmail()));
                return userService.getFullUserByEmail(userDTO.getEmail());
            }

            // user dto -> entity & save user
            logger.log(String.format("Making user: {userDTO: %s}", userDTO));
            userDTO = userService.saveUserWithProfilePicture(userDTO, profilePicture);

            // create and save mapping
            logger.log(String.format("External user detected, saving mapping: {externalUserId: %s, userDTO: %s}", externalUserId, userDTO));
            createAndSaveMapping(externalUserId, userDTO, provider);

            FullUserDTO fullUserDTO = userService.getFullUserByUser(userDTO, new HashSet<>());
            logger.log(String.format("Returning FullUserDTO of newly made user: {fullUserDTO: %s}", fullUserDTO));
            return fullUserDTO;
        } catch (DataAccessException e) {
            logger.log("Database error while creating user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while creating user: " + e.getMessage());
            throw e;
        }
    }


    @Override
    public FullUserDTO getUserIfExistsbyExternalId(String externalUserId, String email) {
        UserIdExternalIdMap mapping;
        try {
            mapping = getMapping(externalUserId);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching external user id <> spawn user id by externalUserId(" + externalUserId + ") :" + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching user by externalUserId (" + externalUserId + ") : " + e.getMessage());
            throw e;
        }
        if (mapping != null) {
            // if there is already a mapping (Spawn account exists, given externalUserId) -> get the associated `FullUserDTO`
            try {
                return getFullUserDTO(mapping.getUser().getId());
            } catch (DataAccessException e) {
                logger.log("Database error while fetching user by externalUserId(" + externalUserId + "): " + e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.log("Unexpected error while fetching user by externalUserId(" + externalUserId + "): " + e.getMessage());
                throw e;
            }
        }
        if (email != null) {
            // if not (signed in through external provider, but no external id <> user id mapping -> try finding by email
            try {
                return userService.getFullUserByEmail(email);
            } catch (DataAccessException e) {
                logger.log("Database error while fetching user by email(" + email + "): " + e.getMessage());
                throw e;
            } catch (Exception e) {
                logger.log("Unexpected error while fetching user by email(" + email + "): " + e.getMessage());
                throw e;
            }
        }
        // No existing user was found
        return null;
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private boolean mappingExistsByExternalId(String externalUserId) {
        return externalIdMapRepository.existsById(externalUserId);
    }

    private UserIdExternalIdMap getMapping(String externalId) {
        try {
            return externalIdMapRepository.findById(externalId).orElse(null);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching mapping for external ID: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching mapping for external ID: " + e.getMessage());
            throw e;
        }
    }


    private FullUserDTO getFullUserDTO(UUID externalUserId) {
        try {
            return userService.getFullUserById(externalUserId);
        } catch (BaseNotFoundException e) {
            logger.log("User not found while fetching full user DTO: " + e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            logger.log("Database error while fetching full user DTO: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching full user DTO: " + e.getMessage());
            throw e;
        }
    }

    private void createAndSaveMapping(String externalUserId, AbstractUserDTO userDTO, OAuthProvider provider) {
        try {
            User user = UserMapper.toEntity((BaseUserDTO) userDTO);
            UserIdExternalIdMap mapping = new UserIdExternalIdMap(externalUserId, user, provider);
            logger.log(String.format("Saving mapping: {mapping: %s}", mapping));
            externalIdMapRepository.save(mapping);
            logger.log("Mapping saved");
        } catch (Exception e) {
            logger.log(e.getMessage());
            throw e;
        }
    }
}