package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.FullUserDTO;
import com.danielagapov.spawn.DTOs.TempUserDTO;
import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

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

    // REQUIRES: tempUserDTO.id() == externId from unpackOAuthUser()
    @Override
    public UserDTO makeUser(UserDTO userDTO, String externId) {
        // user dto -> entity & save user
        userDTO = userService.saveUser(userDTO);
        // create and save mapping
        externalIdMapRepository.save(new UserIdExternalIdMap(externId, UserMapper.toEntity(userDTO)));

        return userDTO;
    }

    @Override
    public Map<Boolean, FullUserDTO> isOnboardedUser(String externId) {
        UserIdExternalIdMap mapping = getMapping(externId);
        if (mapping == null) {
            return Collections.singletonMap(false, null);
        } else {
            return Collections.singletonMap(true, getFullUserDTO(mapping));
        }
    }


    private TempUserDTO unpackOAuthUser(OAuth2User oauthUser) {
        String given_name = oauthUser.getAttribute("given_name");
        String family_name = oauthUser.getAttribute("family_name");
        String picture = oauthUser.getAttribute("picture"); // TODO: may need to change once S3 is set
        String email = oauthUser.getAttribute("email"); // to be used as username
        String externId = oauthUser.getAttribute("sub"); // sub is a unique identifier for google accounts
        if (externId == null) throw new ApplicationException("Subject was null");
        return new TempUserDTO(externId, given_name, family_name, email, picture);
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
}
