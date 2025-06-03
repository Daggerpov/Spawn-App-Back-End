package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@TestConfiguration
@Profile("test")
public class TestS3Config {

    @Bean
    @Primary
    public IS3Service mockS3Service() {
        return new IS3Service() {
            
            private static final String MOCK_CDN_BASE = "https://test-cdn.example.com/";
            private static final String MOCK_DEFAULT_PFP = "https://test-cdn.example.com/default-profile.jpg";
            
            @Override
            public String putObjectWithKey(byte[] file, String key) {
                // Mock implementation - just return a mock URL
                return MOCK_CDN_BASE + key;
            }

            @Override
            public void deleteObjectByUserId(UUID userId) {
                // Mock implementation - do nothing
            }

            @Override
            public String putObject(byte[] file) {
                // Mock implementation - return a mock URL with random key
                String key = UUID.randomUUID().toString();
                return MOCK_CDN_BASE + key;
            }

            @Override
            public UserDTO putProfilePictureWithUser(byte[] file, UserDTO user) {
                // Mock implementation - set a mock profile picture URL
                String profilePictureUrl = file == null ? MOCK_DEFAULT_PFP : putObject(file);
                return new UserDTO(
                    user.getId(),
                    user.getFriendUserIds(),
                    user.getUsername(),
                    profilePictureUrl,
                    user.getName(),
                    user.getBio(),
                    user.getFriendTagIds(),
                    user.getEmail()
                );
            }

            @Override
            public UserDTO updateProfilePicture(byte[] file, UUID userId) {
                // Mock implementation - this would typically involve user service calls
                // For tests, just return a basic UserDTO with mock data
                String profilePictureUrl = file == null ? MOCK_DEFAULT_PFP : putObject(file);
                return new UserDTO(
                    userId,
                    null,
                    "test-user",
                    profilePictureUrl,
                    "Test User",
                    "Test bio",
                    null,
                    "test@example.com"
                );
            }

            @Override
            public String getDefaultProfilePicture() {
                return MOCK_DEFAULT_PFP;
            }

            @Override
            public void deleteObjectByURL(String urlString) {
                // Mock implementation - do nothing
            }
        };
    }
} 