package com.danielagapov.spawn.Services.UserSocialMedia;

import com.danielagapov.spawn.DTOs.User.Profile.UpdateUserSocialMediaDTO;
import com.danielagapov.spawn.DTOs.User.Profile.UserSocialMediaDTO;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Models.User.Profile.UserSocialMedia;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Repositories.User.Profile.IUserSocialMediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserSocialMediaService implements IUserSocialMediaService {

    private final IUserSocialMediaRepository userSocialMediaRepository;
    private final IUserRepository userRepository;

    @Autowired
    public UserSocialMediaService(
            IUserSocialMediaRepository userSocialMediaRepository,
            IUserRepository userRepository) {
        this.userSocialMediaRepository = userSocialMediaRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Cacheable(value = "userSocialMediaByUserId", key = "#userId")
    public UserSocialMediaDTO getUserSocialMedia(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        UserSocialMedia socialMedia = userSocialMediaRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserSocialMedia newSocialMedia = new UserSocialMedia(user);
                    return userSocialMediaRepository.save(newSocialMedia);
                });
        
        return convertToDTO(socialMedia);
    }

    @Override
    @CacheEvict(value = "userSocialMediaByUserId", key = "#userId")
    public UserSocialMediaDTO updateUserSocialMedia(UUID userId, UpdateUserSocialMediaDTO updateDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserSocialMedia socialMedia = userSocialMediaRepository.findByUserId(userId)
                .orElse(new UserSocialMedia(user));

        if (updateDTO.getWhatsappNumber() != null) {
            socialMedia.setWhatsappNumber(updateDTO.getWhatsappNumber());
        }

        if (updateDTO.getInstagramUsername() != null) {
            socialMedia.setInstagramUsername(updateDTO.getInstagramUsername());
        }

        socialMedia = userSocialMediaRepository.save(socialMedia);
        return convertToDTO(socialMedia);
    }

    private UserSocialMediaDTO convertToDTO(UserSocialMedia socialMedia) {
        if (socialMedia == null || socialMedia.getUser() == null) {
            return new UserSocialMediaDTO();
        }

        String whatsappLink = null;
        if (socialMedia.getWhatsappNumber() != null && !socialMedia.getWhatsappNumber().isEmpty()) {
            // Format the WhatsApp number for a direct chat link
            String formattedNumber = socialMedia.getWhatsappNumber().replaceAll("[^0-9]", "");
            whatsappLink = "https://wa.me/" + formattedNumber;
        }

        String instagramLink = null;
        if (socialMedia.getInstagramUsername() != null && !socialMedia.getInstagramUsername().isEmpty()) {
            // Format the Instagram username for a profile link
            String formattedUsername = socialMedia.getInstagramUsername().replaceAll("^@", "");
            instagramLink = "https://www.instagram.com/" + formattedUsername;
        }

        return new UserSocialMediaDTO(
                socialMedia.getId(),
                socialMedia.getUser().getId(),
                whatsappLink,
                instagramLink
        );
    }
} 