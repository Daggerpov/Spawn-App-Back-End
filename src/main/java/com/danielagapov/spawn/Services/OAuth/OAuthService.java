package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.*;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuthService implements IOAuthService {
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final IUserService userService;


    public OAuthService(IUserIdExternalIdMapRepository externalIdMapRepository, IUserService userService) {
        this.externalIdMapRepository = externalIdMapRepository;
        this.userService = userService;
    }

    @Override
    public AbstractUserDTO verifyUser(OAuth2User oauthUser) {
        TempUserDTO tempUser = unpackOAuthUser(oauthUser);
        UserIdExternalIdMap mapping = getMapping(tempUser);

        return mapping == null ? tempUser : getUserDTO(mapping);
    }

    @Override
    public UserDTO makeUser(UserDTO userDTO, String externalUserId, byte[] profilePicture, OAuthProvider provider) {
        // user dto -> entity & save user
        userDTO = userService.saveUserWithProfilePicture(userDTO, profilePicture);

        if (externalUserId != null) {
            // create and save mapping, if the user was created externally through Google or Apple
            saveMapping(externalUserId, userDTO, provider);
        }

        return userDTO;
    }

    @Override
    public FullUserDTO getUserIfExistsbyExternalId(String externalUserId, String email, OAuthProvider provider) {
        UserIdExternalIdMap mapping = getMapping(externalUserId);
        if (mapping != null) {
            return getFullUserDTO(mapping);
        } else {
            FullUserDTO fullUserDTO = userService.getUserByEmail(email);
            if (fullUserDTO != null) {
                saveMapping(externalUserId, fullUserDTO, provider);
            }
            return fullUserDTO;
        }
    }


    private TempUserDTO unpackOAuthUser(OAuth2User oauthUser) {
        String given_name = oauthUser.getAttribute("given_name");
        String family_name = oauthUser.getAttribute("family_name");
        String picture = oauthUser.getAttribute("picture"); // TODO: may need to change once S3 is set
        String email = oauthUser.getAttribute("email"); // to be used as username
        String externalUserId = oauthUser.getAttribute("sub"); // sub is a unique identifier for google accounts
        if (externalUserId == null) throw new ApplicationException("Subject was null");
        return new TempUserDTO(externalUserId, given_name, family_name, email, picture);
    }

    private UserIdExternalIdMap getMapping(TempUserDTO tempUser) {
        return externalIdMapRepository.findById(tempUser.id()).orElse(null);
    }

    private UserIdExternalIdMap getMapping(String externalId) {
        return externalIdMapRepository.findById(externalId).orElse(null);
    }

    private UserDTO getUserDTO(UserIdExternalIdMap mapping) {
        return userService.getUserById(mapping.getUser().getId());
    }

    private FullUserDTO getFullUserDTO(UserIdExternalIdMap mapping) {
        return userService.getFullUserById(mapping.getUser().getId());
    }

    private void saveMapping(String externalUserId, IOnboardedUserDTO userDTO, OAuthProvider provider) {
        User user;
        if (userDTO instanceof FullUserDTO) {
            user = UserMapper.toEntityFromFullDTO((FullUserDTO) userDTO);
        } else {
            user = UserMapper.toEntity((UserDTO) userDTO);
        }
        externalIdMapRepository.save(new UserIdExternalIdMap(externalUserId, user, provider));
    }
}
