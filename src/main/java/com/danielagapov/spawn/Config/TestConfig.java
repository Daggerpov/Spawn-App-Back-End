package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Services.S3.IS3Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

@Configuration
@Profile("test")
public class TestConfig {
    
    @Bean
    @Primary
    public IS3Service mockS3Service() {
        return new MockS3Service();
    }
    
    /**
     * Mock implementation of S3Service for testing
     * Returns default/mock values without actually connecting to S3
     */
    private static class MockS3Service implements IS3Service {
        
        private static final String MOCK_DEFAULT_PFP = "https://mock-cdn.com/default.jpg";
        private static final String MOCK_CDN_BASE = "https://mock-cdn.com/";
        
        @Override
        public String putObjectWithKey(byte[] file, String key) {
            return MOCK_CDN_BASE + key;
        }
        
        @Override
        public void deleteObjectByUserId(UUID userId) {
            // No-op for test
        }
        
        @Override
        public String putObject(byte[] file) {
            return MOCK_CDN_BASE + UUID.randomUUID().toString();
        }
        
        @Override
        public UserDTO putProfilePictureWithUser(byte[] file, UserDTO user) {
            // Return the same user for testing purposes
            return user;
        }
        
        @Override
        public UserDTO updateProfilePicture(byte[] file, UUID userId) {
            // For test, return a simple mock UserDTO
            return new UserDTO(
                    userId,
                    null,
                    "testuser",
                    file == null ? MOCK_DEFAULT_PFP : putObject(file),
                    "Test User",
                    "Test Bio",
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
            // No-op for test
        }
    }
} 