package com.danielagapov.spawn.Services.OAuth;

import com.danielagapov.spawn.DTOs.UserDTO;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Models.UserIdExternalIdMap;
import com.danielagapov.spawn.Repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.Repositories.IUserRepository;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuthService implements IOAuthService {
    private final IUserRepository userRepository;
    private final IUserIdExternalIdMapRepository externalIdMapRepository;
    private final IUserService userService;

    public OAuthService(IUserRepository userRepository, IUserIdExternalIdMapRepository externalIdMapRepository, IUserService userService) {
        this.userRepository = userRepository;
        this.externalIdMapRepository = externalIdMapRepository;
        this.userService = userService;
    }

    @Override
    public UserDTO verifyUser(OAuth2User oauthUser) {
        UserDTO user = unpackOAuthUser(oauthUser);

        String externId = oauthUser.getAttribute("sub");
        if (externId == null) throw new ApplicationException("Subject was null");

        UserIdExternalIdMap mapping = externalIdMapRepository.findById(externId).orElse(null);
        if (mapping == null) {
            User userEntity = UserMapper.toEntity(user);
            User savedEntity = userRepository.save(userEntity);

            UserIdExternalIdMap newMapping = new UserIdExternalIdMap(externId, savedEntity);
            externalIdMapRepository.save(newMapping);

            return UserMapper.toDTO(savedEntity, null, null);
        }
        return userService.getUserById(mapping.getUser().getId());
    }

    private UserDTO unpackOAuthUser(OAuth2User oauthUser) {
        String given_name = oauthUser.getAttribute("given_name");
        String family_name = oauthUser.getAttribute("family_name");
        String picture = oauthUser.getAttribute("picture"); // TODO: may need to change once S3 is set
        String email = oauthUser.getAttribute("email"); // to be used as username
        String externId = oauthUser.getAttribute("sub"); // sub is a unique identifier for google accounts
        if (externId == null) throw new ApplicationException("Subject was null");
        return new UserDTO(null, null, null, picture, given_name, family_name, null, null, email);
    }
}
