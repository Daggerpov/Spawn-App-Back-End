package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

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
    public FullUserDTO makeUser(UserDTO userDTO, String externalUserId, byte[] profilePicture, OAuthProvider provider) {
        try {
            // user dto -> entity & save user
            logger.log(String.format("Making user: {userDTO: %s}", userDTO));
            userDTO = userService.saveUserWithProfilePicture(userDTO, profilePicture);
            if (externalUserId != null) {
                // create and save mapping, if the user was created externally through Google or Apple
                logger.log(String.format("External user detected, saving mapping: {externalUserId: %s, userDTO: %s}", externalUserId, userDTO));
                saveMapping(externalUserId, userDTO, provider);
            }
            FullUserDTO fullUserDTO = userService.getFullUserByUser(userDTO);
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
        try {
            UserIdExternalIdMap mapping = getMapping(externalUserId);
            return mapping == null ? userService.getUserByEmail(email) : getFullUserDTO(mapping);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching user by external ID: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching user by external ID: " + e.getMessage());
            throw e;
        }
    }

//    private TempUserDTO unpackOAuthUser(OAuth2User oauthUser) {
//        try {
//            String given_name = oauthUser.getAttribute("given_name");
//            String family_name = oauthUser.getAttribute("family_name");
//            String picture = oauthUser.getAttribute("picture"); // TODO: may need to change once S3 is set
//            String email = oauthUser.getAttribute("email"); // to be used as username
//            String externalUserId = oauthUser.getAttribute("sub"); // sub is a unique identifier for google accounts
//            if (externalUserId == null) throw new ApplicationException("Subject was null");
//            return new TempUserDTO(externalUserId, given_name, family_name, email, picture);
//        } catch (Exception e) {
//            logger.log("Error unpacking OAuth user: " + e.getMessage());
//            throw e;
//        }
//    }

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

    private FullUserDTO getFullUserDTO(UserIdExternalIdMap mapping) {
        try {
            return userService.getFullUserById(mapping.getUser().getId());
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