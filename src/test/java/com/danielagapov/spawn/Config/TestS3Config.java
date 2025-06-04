package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import com.danielagapov.spawn.Services.OAuth.OAuthStrategy;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
@Profile("test")
public class TestS3Config {

    // Simple in-memory storage for test data
    private static final Map<UUID, BaseUserDTO> testUsers = new ConcurrentHashMap<>();
    private static final Map<String, BaseUserDTO> testUsersByUsername = new ConcurrentHashMap<>();

    // Static method to add test users from test classes
    public static void addTestUser(BaseUserDTO user) {
        testUsers.put(user.getId(), user);
        testUsersByUsername.put(user.getUsername(), user);
    }

    // Static method to clear test data between tests
    public static void clearTestData() {
        testUsers.clear();
        testUsersByUsername.clear();
    }

    @Bean
    @Primary
    public IS3Service mockS3Service() {
        return new IS3Service() {
            @Override
            public String putObjectWithKey(byte[] file, String key) {
                return "https://test-cdn.example.com/" + key;
            }

            @Override
            public void deleteObjectByUserId(UUID userId) {
                // Mock implementation - do nothing
            }

            @Override
            public String putObject(byte[] file) {
                return "https://test-cdn.example.com/mock-uploaded-file.jpg";
            }

            @Override
            public UserDTO putProfilePictureWithUser(byte[] file, UserDTO user) {
                String profilePictureUrl = file == null ? getDefaultProfilePicture() : putObject(file);
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
                // Mock implementation - return a UserDTO with updated profile picture
                return new UserDTO(
                    userId,
                    List.of(),
                    "testuser",
                    "https://test-cdn.example.com/mock-updated-profile.jpg",
                    "Test User",
                    "Test bio",
                    List.of(),
                    "test@example.com"
                );
            }

            @Override
            public String getDefaultProfilePicture() {
                return "https://test-cdn.example.com/default-profile.jpg";
            }

            @Override
            public void deleteObjectByURL(String urlString) {
                // Mock implementation - do nothing
            }
        };
    }

    @Bean
    @Primary
    public IUserSearchService mockUserSearchService() {
        return new IUserSearchService() {
            @Override
            public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
                // Return empty result to avoid Redis dependency
                return new SearchedUserResult(List.of(), List.of(), List.of());
            }

            @Override
            public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
                return List.of();
            }

            @Override
            public List<BaseUserDTO> searchByQuery(String searchQuery) {
                return List.of();
            }

            @Override
            public java.util.Set<UUID> getExcludedUserIds(UUID userId) {
                return java.util.Set.of();
            }
        };
    }

    @Bean
    @Primary
    public IJWTService mockJWTService() {
        return new IJWTService() {
            @Override
            public String generateAccessToken(String username) {
                return "mock-jwt-token-for-" + username;
            }

            @Override
            public String generateRefreshToken(String username) {
                return "mock-refresh-token-for-" + username;
            }

            @Override
            public String extractUsername(String token) {
                // Extract username from mock token format
                if (token.startsWith("mock-jwt-token-for-")) {
                    return token.substring("mock-jwt-token-for-".length());
                }
                throw new RuntimeException("Invalid mock token format");
            }

            @Override
            public boolean isValidToken(String token, UserDetails userDetails) {
                try {
                    String username = extractUsername(token);
                    return username.equals(userDetails.getUsername());
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public String refreshAccessToken(HttpServletRequest request) {
                // Mock implementation - simulate real behavior for testing
                final String authHeader = request.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    throw new com.danielagapov.spawn.Exceptions.Token.TokenNotFoundException("No authorization token found");
                }
                return "mock-refreshed-token";
            }

            @Override
            public String generateEmailToken(String username) {
                return "mock-email-token-for-" + username;
            }

            @Override
            public boolean isValidEmailToken(String token) {
                return token.startsWith("mock-email-token-for-");
            }
        };
    }

    @Bean
    @Primary
    public IOAuthService mockOAuthService() {
        return new IOAuthService() {
            @Override
            public BaseUserDTO makeUser(UserDTO user, String externalUserId, byte[] profilePicture, OAuthProvider provider) {
                // Mock implementation - just return a BaseUserDTO
                return new BaseUserDTO(
                    UUID.randomUUID(),
                    user.getName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getBio(),
                    "https://test-cdn.example.com/mock-profile.jpg"
                );
            }

            @Override
            public Optional<BaseUserDTO> signInUser(String idToken, String email, OAuthProvider provider) {
                // Mock implementation - return empty for new users (testing OAuth sign-in)
                return Optional.empty();
            }

            @Override
            public Optional<BaseUserDTO> getUserIfExistsbyExternalId(String externalUserId, String email) {
                // Mock implementation - return empty for testing
                return Optional.empty();
            }

            @Override
            public BaseUserDTO createUserFromOAuth(UserCreationDTO userCreationDTO, String idToken, OAuthProvider provider) {
                // Mock implementation - create a user from OAuth
                return new BaseUserDTO(
                    UUID.randomUUID(),
                    userCreationDTO.getName(),
                    userCreationDTO.getEmail(),
                    userCreationDTO.getUsername(),
                    userCreationDTO.getBio(),
                    "https://test-cdn.example.com/mock-oauth-profile.jpg"
                );
            }
        };
    }

    @Bean
    @Primary
    public List<OAuthStrategy> mockOAuthStrategies() {
        return List.of(
            new OAuthStrategy() {
                @Override
                public OAuthProvider getOAuthProvider() {
                    return OAuthProvider.google;
                }

                @Override
                public String verifyIdToken(String idToken) {
                    return "mock-google-user-id";
                }
            },
            new OAuthStrategy() {
                @Override
                public OAuthProvider getOAuthProvider() {
                    return OAuthProvider.apple;
                }

                @Override
                public String verifyIdToken(String idToken) {
                    return "mock-apple-user-id";
                }
            }
        );
    }

    @Bean
    @Primary
    public com.danielagapov.spawn.Services.Email.IEmailService mockEmailService() {
        return new com.danielagapov.spawn.Services.Email.IEmailService() {
            @Override
            public void sendEmail(String to, String subject, String text) {
                // Mock implementation - do nothing
            }

            @Override
            public void sendVerifyAccountEmail(String to, String token) {
                // Mock implementation - do nothing
            }
        };
    }
} 