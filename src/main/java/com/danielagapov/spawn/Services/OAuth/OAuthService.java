package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FullUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.IncorrectProviderException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
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
    public Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email) {
        boolean existsByExternalId = mappingExistsByExternalId(externalUserId);
        boolean existsByEmail = userService.existsByEmail(email);

        if (existsByExternalId) { // A Spawn account exists with this external id, return the associated `FullUserDTO`
            User user = getMapping(externalUserId).getUser();
            return Optional.of(UserMapper.toDTO(user));
        } else if (existsByEmail) { // A Spawn account exists with this email but not with the external id which indicates a sign-in with incorrect provider
            UserIdExternalIdMap externalIdMap = externalIdMapRepository.findByUserEmail(email).orElseThrow(() -> new BaseNotFoundException(EntityType.ExternalIdMap, email, "email"));
            String provider = String.valueOf(externalIdMap.getProvider()).equals("google") ? "Google" : "Apple";
            throw new IncorrectProviderException("The email: " + email + " is already associated to a " + provider + " account. Please login through " + provider + " instead");
        } else { // No account exists for this external id or email
            return Optional.empty();
        }
    }

    /* ------------------------------ HELPERS ------------------------------ */

    private boolean mappingExistsByExternalId(String externalUserId) {
        return externalIdMapRepository.existsById(externalUserId);
    }

    private UserIdExternalIdMap getMapping(String externalId) {
        try {
            return externalIdMapRepository.findById(externalId).orElse(null);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching mapping for externalUserId( " + externalId + ") : " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching mapping for externalUserId( " + externalId + ") : " + e.getMessage());
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

    private void createAndSaveMapping(String externalUserId, UserDTO userDTO, OAuthProvider provider) {
        try {
            User user = UserMapper.toEntity(userDTO);
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