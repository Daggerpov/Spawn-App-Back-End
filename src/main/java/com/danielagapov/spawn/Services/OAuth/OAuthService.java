package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public AbstractUserDTO verifyUser(OAuth2User oauthUser) {
        try {
            TempUserDTO tempUser = unpackOAuthUser(oauthUser);
            UserIdExternalIdMap mapping = getMapping(tempUser);

            return mapping == null ? tempUser : getUserDTO(mapping);
        } catch (DataAccessException e) {
            logger.log("Database error while verifying OAuth user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while verifying OAuth user: " + e.getMessage());
            throw e;
        }
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
                logger.log(String.format("Existing user detected in makeUser, mapping already exists: {user: %s, externalUserId: %s}", userDTO.email(), externalUserId));
                return userService.getFullUserByEmail(userDTO.email());
            }
            if (userService.existsByEmail(userDTO.email())) {
                logger.log(String.format("Existing user detected in makeUser, email already exists: {user: %s, email: %s}", userDTO.email(), userDTO.email()));
                return userService.getFullUserByEmail(userDTO.email());
            }

            // user dto -> entity & save user
            logger.log(String.format("Making user: {userDTO: %s}", userDTO));
            userDTO = userService.saveUserWithProfilePicture(userDTO, profilePicture);
            if (externalUserId != null) {
                // create and save mapping, if the user was created externally through Google or Apple
                logger.log(String.format("External user detected, saving mapping: {externalUserId: %s, userDTO: %s}", externalUserId, userDTO));
                saveMapping(externalUserId, userDTO, provider);
            }
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

        if (mapping == null) {
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
        } else {
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
    }

    /**
     * Checks if user exists, first by externalUserId then by email
     * This is a temporary solution to duplicates occurring in database
     */
    private boolean userExistsByExternalIdOrEmail(String externalUserId, String email) {
        return mappingExistsByExternalId(externalUserId) || userService.existsByEmail(email);
    }

    private boolean mappingExistsByExternalId(String externalUserId) {
        return externalIdMapRepository.existsById(externalUserId);
    }

    private TempUserDTO unpackOAuthUser(OAuth2User oauthUser) {
        try {
            String given_name = oauthUser.getAttribute("given_name");
            String family_name = oauthUser.getAttribute("family_name");
            String picture = oauthUser.getAttribute("picture"); // TODO: may need to change once S3 is set
            String email = oauthUser.getAttribute("email"); // to be used as username
            String externalUserId = oauthUser.getAttribute("sub"); // sub is a unique identifier for google accounts
            if (externalUserId == null) throw new ApplicationException("Subject was null");
            return new TempUserDTO(externalUserId, given_name, family_name, email, picture);
        } catch (Exception e) {
            logger.log("Error unpacking OAuth user: " + e.getMessage());
            throw e;
        }
    }

    private UserIdExternalIdMap getMapping(TempUserDTO tempUser) {
        try {
            return externalIdMapRepository.findById(tempUser.id()).orElse(null);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching mapping for temp user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching mapping for temp user: " + e.getMessage());
            throw e;
        }
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

    private UserDTO getUserDTO(UserIdExternalIdMap mapping) {
        try {
            return userService.getUserById(mapping.getUser().getId());
        } catch (BaseNotFoundException e) {
            logger.log("User not found while fetching user DTO: " + e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            logger.log("Database error while fetching user DTO: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching user DTO: " + e.getMessage());
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

    private void saveMapping(String externalUserId, IOnboardedUserDTO userDTO, OAuthProvider provider) {
        try {
            User user;
            if (userDTO instanceof FullUserDTO) {
                user = UserMapper.convertFullUserToUserEntity((FullUserDTO) userDTO);
            } else {
                user = UserMapper.toEntity((UserDTO) userDTO);
            }
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