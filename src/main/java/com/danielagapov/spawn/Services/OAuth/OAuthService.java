package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.TempUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Helpers.Logger.ILogger;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    // REQUIRES: tempUserDTO.id() == externalUserId from unpackOAuthUser()
    @Override
    public FullUserDTO makeUser(UserDTO userDTO, String externalUserId, byte[] profilePicture) {
        try {
            // user dto -> entity & save user
            userDTO = userService.saveUserWithProfilePicture(userDTO, profilePicture);

            if (externalUserId != null) {
                // create and save mapping, if the user was created externally through Google or Apple
                externalIdMapRepository.save(new UserIdExternalIdMap(externalUserId, UserMapper.toEntity(userDTO)));
            }

            return userService.getFullUserByUser(userDTO);
        } catch (DataAccessException e) {
            logger.log("Database error while creating user: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while creating user: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public FullUserDTO getUserIfExistsbyExternalId(String externalUserId) {
        try {
            UserIdExternalIdMap mapping = getMapping(externalUserId);
            return mapping == null ? null : getFullUserDTO(mapping);
        } catch (DataAccessException e) {
            logger.log("Database error while fetching user by external ID: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.log("Unexpected error while fetching user by external ID: " + e.getMessage());
            throw e;
        }
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
}