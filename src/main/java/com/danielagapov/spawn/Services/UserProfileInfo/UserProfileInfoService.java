package com.danielagapov.spawn.Services.UserProfileInfo;

import com.danielagapov.spawn.DTOs.User.Profile.UserProfileInfoDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserProfileInfoService implements IUserProfileInfoService {

    private final IUserRepository userRepository;

    @Autowired
    public UserProfileInfoService(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileInfoDTO getUserProfileInfo(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));

        return new UserProfileInfoDTO(
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getBio(),
                user.getProfilePictureUrlString(),
                user.getDateCreated()
        );
    }
} 