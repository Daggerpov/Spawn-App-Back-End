package com.danielagapov.spawn.Config;

import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserCreationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.DTOs.User.UserUpdateDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.RecentlySpawnedUserDTO;
import com.danielagapov.spawn.Enums.OAuthProvider;
import com.danielagapov.spawn.Models.FriendTag;
import com.danielagapov.spawn.Services.JWT.IJWTService;
import com.danielagapov.spawn.Services.OAuth.IOAuthService;
import com.danielagapov.spawn.Services.OAuth.OAuthStrategy;
import com.danielagapov.spawn.Services.S3.IS3Service;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserDetails.UserInfoService;
import com.danielagapov.spawn.Services.UserSearch.IUserSearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
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
    public IUserService mockUserService() {
        return new IUserService() {
            @Override
            public List<UserDTO> getAllUsers() {
                return List.of();
            }

            @Override
            public UserDTO getUserById(UUID id) {
                BaseUserDTO baseUser = testUsers.get(id);
                if (baseUser != null) {
                    return new UserDTO(baseUser.getId(), List.of(), baseUser.getUsername(), 
                                     baseUser.getProfilePicture(), baseUser.getName(), baseUser.getBio(), 
                                     List.of(), baseUser.getEmail());
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, id);
            }

            @Override
            public com.danielagapov.spawn.Models.User.User getUserEntityById(UUID id) {
                BaseUserDTO baseUser = testUsers.get(id);
                if (baseUser != null) {
                    com.danielagapov.spawn.Models.User.User user = new com.danielagapov.spawn.Models.User.User();
                    user.setId(baseUser.getId());
                    user.setName(baseUser.getName());
                    user.setEmail(baseUser.getEmail());
                    user.setUsername(baseUser.getUsername());
                    user.setBio(baseUser.getBio());
                    user.setProfilePictureUrlString(baseUser.getProfilePicture());
                    return user;
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, id);
            }

            @Override
            public UserDTO saveUser(UserDTO user) {
                return user;
            }

            @Override
            public void deleteUserById(UUID id) {
                if (!testUsers.containsKey(id)) {
                    throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                        com.danielagapov.spawn.Enums.EntityType.User, id);
                }
                testUsers.remove(id);
            }

            @Override
            public com.danielagapov.spawn.Models.User.User saveEntity(com.danielagapov.spawn.Models.User.User user) {
                return user;
            }

            @Override
            public UserDTO saveUserWithProfilePicture(UserDTO user, byte[] profilePicture) {
                return user;
            }

            @Override
            public UserDTO getUserDTOByEntity(com.danielagapov.spawn.Models.User.User user) {
                return new UserDTO(user.getId(), List.of(), user.getUsername(), 
                                 user.getProfilePictureUrlString(), user.getName(), user.getBio(), 
                                 List.of(), user.getEmail());
            }

            @Override
            public List<UUID> getFriendUserIdsByUserId(UUID id) {
                return List.of();
            }

            @Override
            public List<FullFriendUserDTO> getFullFriendUsersByUserId(UUID requestingUserId) {
                return List.of();
            }

            @Override
            public List<com.danielagapov.spawn.Models.User.User> getFriendUsersByUserId(UUID requestingUserId) {
                return List.of();
            }

            @Override
            public boolean isUserFriendOfUser(UUID userId, UUID potentialFriendId) {
                return false; // Default to false for testing
            }

            @Override
            public Map<FriendTag, UUID> getOwnerUserIdsMap() {
                return Map.of();
            }

            @Override
            public Map<FriendTag, List<UUID>> getFriendUserIdsMap() {
                return Map.of();
            }

            @Override
            public List<BaseUserDTO> getFriendsByFriendTagId(UUID friendTagId) {
                return List.of();
            }

            @Override
            public List<UUID> getFriendUserIdsByFriendTagId(UUID friendTagId) {
                return List.of();
            }

            @Override
            public void saveFriendToUser(UUID userId, UUID friendId) {
                // Mock implementation - do nothing
            }

            @Override
            public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
                return List.of(); // Empty list to avoid Redis dependency
            }

            @Override
            public Instant getLatestFriendProfileUpdateTimestamp(UUID userId) {
                return Instant.now();
            }

            @Override
            public List<BaseUserDTO> getParticipantsByActivityId(UUID ActivityId) {
                return List.of();
            }

            @Override
            public List<BaseUserDTO> getInvitedByActivityId(UUID ActivityId) {
                return List.of();
            }

            @Override
            public List<UUID> getParticipantUserIdsByActivityId(UUID ActivityId) {
                return List.of();
            }

            @Override
            public List<UUID> getInvitedUserIdsByActivityId(UUID ActivityId) {
                return List.of();
            }

            @Override
            public boolean existsByEmail(String email) {
                return false;
            }

            @Override
            public boolean existsByUsername(String username) {
                return testUsersByUsername.containsKey(username);
            }

            @Override
            public void verifyUserByUsername(String username) {
                // Mock implementation - do nothing
            }

            @Override
            public int getMutualFriendCount(UUID receiverId, UUID id) {
                return 0;
            }

            @Override
            public BaseUserDTO getBaseUserById(UUID id) {
                BaseUserDTO user = testUsers.get(id);
                if (user != null) {
                    return user;
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, id);
            }

            @Override
            public BaseUserDTO updateUser(UUID id, UserUpdateDTO updateDTO) {
                BaseUserDTO existingUser = testUsers.get(id);
                if (existingUser != null) {
                    BaseUserDTO updatedUser = new BaseUserDTO(
                        existingUser.getId(),
                        updateDTO.getName(),
                        existingUser.getEmail(),
                        updateDTO.getUsername(),
                        updateDTO.getBio(),
                        existingUser.getProfilePicture()
                    );
                    testUsers.put(id, updatedUser);
                    return updatedUser;
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, id);
            }

            @Override
            public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
                // Return empty result to avoid Redis dependency
                return new SearchedUserResult(List.of(), List.of(), List.of());
            }

            @Override
            public com.danielagapov.spawn.Models.User.User getUserEntityByUsername(String username) {
                BaseUserDTO baseUser = testUsersByUsername.get(username);
                if (baseUser != null) {
                    com.danielagapov.spawn.Models.User.User user = new com.danielagapov.spawn.Models.User.User();
                    user.setId(baseUser.getId());
                    user.setName(baseUser.getName());
                    user.setEmail(baseUser.getEmail());
                    user.setUsername(baseUser.getUsername());
                    user.setBio(baseUser.getBio());
                    user.setProfilePictureUrlString(baseUser.getProfilePicture());
                    return user;
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, username, "username");
            }

            @Override
            public List<BaseUserDTO> searchByQuery(String searchQuery) {
                return List.of(); // Return empty list for testing
            }

            @Override
            public com.danielagapov.spawn.Models.User.User getUserByEmail(String email) {
                return testUsersByUsername.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst()
                    .map(baseUser -> {
                        com.danielagapov.spawn.Models.User.User user = new com.danielagapov.spawn.Models.User.User();
                        user.setId(baseUser.getId());
                        user.setName(baseUser.getName());
                        user.setEmail(baseUser.getEmail());
                        user.setUsername(baseUser.getUsername());
                        user.setBio(baseUser.getBio());
                        user.setProfilePictureUrlString(baseUser.getProfilePicture());
                        return user;
                    })
                    .orElseThrow(() -> new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                        com.danielagapov.spawn.Enums.EntityType.User, email, "email"));
            }

            @Override
            public List<RecentlySpawnedUserDTO> getRecentlySpawnedWithUsers(UUID requestingUserId) {
                // Check if user exists first
                if (!testUsers.containsKey(requestingUserId)) {
                    throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                        com.danielagapov.spawn.Enums.EntityType.User, requestingUserId);
                }
                return List.of(); // Return empty list for testing
            }

            @Override
            public BaseUserDTO getBaseUserByUsername(String username) {
                BaseUserDTO user = testUsersByUsername.get(username);
                if (user != null) {
                    return user;
                }
                throw new com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException(
                    com.danielagapov.spawn.Enums.EntityType.User, username, "username");
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
    public UserDetailsService mockUserDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                // Return a mock user for any username
                return org.springframework.security.core.userdetails.User.builder()
                        .username(username)
                        .password("password") // Not used in JWT validation
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                        .build();
            }
        };
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