package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.user.api.dto.AuthResponseDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserCreationDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.internal.domain.User;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UserMapper - Critical core functionality component.
 * Tests all mapping methods between User entity and various DTOs.
 */
@DisplayName("User Mapper Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Order(2)
class UserMapperTests {

    private User testUser;
    private UUID testUserId;
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_NAME = "Test User";
    private static final String TEST_BIO = "Test bio";
    private static final String TEST_PROFILE_PIC = "https://example.com/pic.jpg";

    @BeforeEach
    void setup() {
        testUserId = UUID.randomUUID();
        testUser = createTestUser(testUserId, TEST_USERNAME, TEST_EMAIL, TEST_NAME, TEST_BIO, TEST_PROFILE_PIC);
    }

    private User createTestUser(UUID id, String username, String email, String name, String bio, String profilePic) {
        User user = new User(id, username, profilePic, name, bio, email);
        user.setStatus(UserStatus.ACTIVE);
        user.setHasCompletedOnboarding(true);
        user.setDateCreated(new Date());
        user.setPhoneNumber("+1234567890");
        return user;
    }

    @Nested
    @DisplayName("User to BaseUserDTO Mapping Tests")
    class UserToBaseUserDTOTests {

        @Test
        @DisplayName("Should map User to BaseUserDTO with all fields")
        void shouldMapUserToBaseUserDTO() {
            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(testUser.getId());
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(dto.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(dto.getName()).isEqualTo(testUser.getName());
            assertThat(dto.getBio()).isEqualTo(testUser.getBio());
            assertThat(dto.getProfilePicture()).isEqualTo(testUser.getProfilePictureUrlString());
            assertThat(dto.getHasCompletedOnboarding()).isEqualTo(testUser.getHasCompletedOnboarding());
        }

        @Test
        @DisplayName("Should handle User with null optional fields")
        void shouldHandleUserWithNullFields() {
            // Given
            User userWithNulls = new User();
            userWithNulls.setId(testUserId);
            userWithNulls.setEmail(TEST_EMAIL);

            // When
            BaseUserDTO dto = UserMapper.toDTO(userWithNulls);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(testUserId);
            assertThat(dto.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(dto.getUsername()).isNull();
            assertThat(dto.getName()).isNull();
            assertThat(dto.getBio()).isNull();
            assertThat(dto.getProfilePicture()).isNull();
        }

        @Test
        @DisplayName("Should handle User with hasCompletedOnboarding as false")
        void shouldHandleOnboardingFalse() {
            // Given
            testUser.setHasCompletedOnboarding(false);

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto.getHasCompletedOnboarding()).isFalse();
        }
    }

    @Nested
    @DisplayName("User to AuthResponseDTO Mapping Tests")
    class UserToAuthResponseDTOTests {

        @Test
        @DisplayName("Should map User to AuthResponseDTO with status")
        void shouldMapUserToAuthResponseDTO() {
            // Given
            testUser.setStatus(UserStatus.ACTIVE);

            // When
            AuthResponseDTO dto = UserMapper.toAuthResponseDTO(testUser);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getUser()).isNotNull();
            assertThat(dto.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(dto.getIsOAuthUser()).isNull();
        }

        @Test
        @DisplayName("Should map User to AuthResponseDTO with OAuth flag")
        void shouldMapUserToAuthResponseDTOWithOAuthFlag() {
            // Given
            testUser.setStatus(UserStatus.EMAIL_VERIFIED);

            // When
            AuthResponseDTO dto = UserMapper.toAuthResponseDTO(testUser, true);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getUser()).isNotNull();
            assertThat(dto.getStatus()).isEqualTo(UserStatus.EMAIL_VERIFIED);
            assertThat(dto.getIsOAuthUser()).isTrue();
        }

        @Test
        @DisplayName("Should map User to AuthResponseDTO with non-OAuth flag")
        void shouldMapUserToAuthResponseDTOWithNonOAuthFlag() {
            // When
            AuthResponseDTO dto = UserMapper.toAuthResponseDTO(testUser, false);

            // Then
            assertThat(dto.getIsOAuthUser()).isFalse();
        }

        @Test
        @DisplayName("Should map User with different statuses")
        void shouldMapUserWithDifferentStatuses() {
            // Test all user statuses
            for (UserStatus status : UserStatus.values()) {
                // Given
                testUser.setStatus(status);

                // When
                AuthResponseDTO dto = UserMapper.toAuthResponseDTO(testUser);

                // Then
                assertThat(dto.getStatus()).isEqualTo(status);
            }
        }
    }

    @Nested
    @DisplayName("User to UserDTO Mapping Tests")
    class UserToUserDTOTests {

        @Test
        @DisplayName("Should map User to UserDTO with friend IDs")
        void shouldMapUserToUserDTOWithFriends() {
            // Given
            List<UUID> friendIds = List.of(UUID.randomUUID(), UUID.randomUUID());

            // When
            UserDTO dto = UserMapper.toDTO(testUser, friendIds);

            // Then
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(testUser.getId());
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(dto.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(dto.getName()).isEqualTo(testUser.getName());
            assertThat(dto.getBio()).isEqualTo(testUser.getBio());
            assertThat(dto.getProfilePicture()).isEqualTo(testUser.getProfilePictureUrlString());
            assertThat(dto.getFriendUserIds()).hasSize(2);
            assertThat(dto.getFriendUserIds()).containsExactlyElementsOf(friendIds);
        }

        @Test
        @DisplayName("Should map User to UserDTO with empty friend list")
        void shouldMapUserToUserDTOWithEmptyFriends() {
            // When
            UserDTO dto = UserMapper.toDTO(testUser, List.of());

            // Then
            assertThat(dto.getFriendUserIds()).isEmpty();
        }

        @Test
        @DisplayName("Should map User to UserDTO with null friend list")
        void shouldMapUserToUserDTOWithNullFriends() {
            // When
            UserDTO dto = UserMapper.toDTO(testUser, null);

            // Then
            assertThat(dto.getFriendUserIds()).isNull();
        }
    }

    @Nested
    @DisplayName("List Mapping Tests")
    class ListMappingTests {

        @Test
        @DisplayName("Should map list of Users to BaseUserDTO list")
        void shouldMapUserListToBaseUserDTOList() {
            // Given
            User user2 = createTestUser(UUID.randomUUID(), "user2", "user2@test.com", "User 2", "Bio 2", "pic2.jpg");
            User user3 = createTestUser(UUID.randomUUID(), "user3", "user3@test.com", "User 3", "Bio 3", "pic3.jpg");
            List<User> users = List.of(testUser, user2, user3);

            // When
            List<BaseUserDTO> dtos = UserMapper.toDTOList(users);

            // Then
            assertThat(dtos).hasSize(3);
            assertThat(dtos.get(0).getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(dtos.get(1).getUsername()).isEqualTo("user2");
            assertThat(dtos.get(2).getUsername()).isEqualTo("user3");
        }

        @Test
        @DisplayName("Should return empty list when mapping empty user list")
        void shouldReturnEmptyListForEmptyInput() {
            // When
            List<BaseUserDTO> dtos = UserMapper.toDTOList(Collections.emptyList());

            // Then
            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("Should map list of Users to UserDTO list with friend map")
        void shouldMapUserListToUserDTOListWithFriendMap() {
            // Given
            User user2 = createTestUser(UUID.randomUUID(), "user2", "user2@test.com", "User 2", "Bio 2", "pic2.jpg");
            List<User> users = List.of(testUser, user2);
            
            UUID friend1 = UUID.randomUUID();
            UUID friend2 = UUID.randomUUID();
            Map<User, List<UUID>> friendMap = new HashMap<>();
            friendMap.put(testUser, List.of(friend1));
            friendMap.put(user2, List.of(friend1, friend2));

            // When
            List<UserDTO> dtos = UserMapper.toDTOList(users, friendMap);

            // Then
            assertThat(dtos).hasSize(2);
            assertThat(dtos.get(0).getFriendUserIds()).hasSize(1);
            assertThat(dtos.get(1).getFriendUserIds()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle missing entries in friend map")
        void shouldHandleMissingEntriesInFriendMap() {
            // Given
            User user2 = createTestUser(UUID.randomUUID(), "user2", "user2@test.com", "User 2", "Bio 2", "pic2.jpg");
            List<User> users = List.of(testUser, user2);
            
            Map<User, List<UUID>> friendMap = new HashMap<>();
            friendMap.put(testUser, List.of(UUID.randomUUID()));
            // user2 is not in the map

            // When
            List<UserDTO> dtos = UserMapper.toDTOList(users, friendMap);

            // Then
            assertThat(dtos).hasSize(2);
            assertThat(dtos.get(0).getFriendUserIds()).hasSize(1);
            assertThat(dtos.get(1).getFriendUserIds()).isEmpty(); // Should default to empty list
        }
    }

    @Nested
    @DisplayName("DTO to Entity Mapping Tests")
    class DTOToEntityTests {

        @Test
        @DisplayName("Should map BaseUserDTO to User entity")
        void shouldMapBaseUserDTOToEntity() {
            // Given
            BaseUserDTO dto = new BaseUserDTO(testUserId, TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_BIO, TEST_PROFILE_PIC);

            // When
            User user = UserMapper.toEntity(dto);

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(dto.getId());
            assertThat(user.getUsername()).isEqualTo(dto.getUsername());
            assertThat(user.getEmail()).isEqualTo(dto.getEmail());
            assertThat(user.getName()).isEqualTo(dto.getName());
            assertThat(user.getBio()).isEqualTo(dto.getBio());
            assertThat(user.getProfilePictureUrlString()).isEqualTo(dto.getProfilePicture());
        }

        @Test
        @DisplayName("Should map BaseUserDTO with null fields to User entity")
        void shouldMapBaseUserDTOWithNullFields() {
            // Given
            BaseUserDTO dto = new BaseUserDTO();
            dto.setId(testUserId);
            dto.setEmail(TEST_EMAIL);

            // When
            User user = UserMapper.toEntity(dto);

            // Then
            assertThat(user.getId()).isEqualTo(testUserId);
            assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(user.getUsername()).isNull();
            assertThat(user.getName()).isNull();
        }

        @Test
        @DisplayName("Should map list of BaseUserDTOs to User entities")
        void shouldMapDTOListToEntityList() {
            // Given
            BaseUserDTO dto1 = new BaseUserDTO(testUserId, TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_BIO, TEST_PROFILE_PIC);
            BaseUserDTO dto2 = new BaseUserDTO(UUID.randomUUID(), "Name 2", "email2@test.com", "user2", "Bio 2", "pic2.jpg");
            List<BaseUserDTO> dtos = List.of(dto1, dto2);

            // When
            List<User> users = UserMapper.toEntityListFromBaseUserDTOs(dtos);

            // Then
            assertThat(users).hasSize(2);
            assertThat(users.get(0).getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(users.get(1).getUsername()).isEqualTo("user2");
        }
    }

    @Nested
    @DisplayName("UserDTO to BaseUserDTO Conversion Tests")
    class UserDTOToBaseUserDTOTests {

        @Test
        @DisplayName("Should convert UserDTO to BaseUserDTO")
        void shouldConvertUserDTOToBaseUserDTO() {
            // Given
            UserDTO userDTO = new UserDTO(testUserId, List.of(UUID.randomUUID()), TEST_USERNAME, TEST_PROFILE_PIC, TEST_NAME, TEST_BIO, TEST_EMAIL, true);

            // When
            BaseUserDTO baseDTO = UserMapper.toBaseDTO(userDTO);

            // Then
            assertThat(baseDTO).isNotNull();
            assertThat(baseDTO.getId()).isEqualTo(userDTO.getId());
            assertThat(baseDTO.getUsername()).isEqualTo(userDTO.getUsername());
            assertThat(baseDTO.getName()).isEqualTo(userDTO.getName());
            assertThat(baseDTO.getBio()).isEqualTo(userDTO.getBio());
            assertThat(baseDTO.getEmail()).isEqualTo(userDTO.getEmail());
            assertThat(baseDTO.getProfilePicture()).isEqualTo(userDTO.getProfilePicture());
            assertThat(baseDTO.getHasCompletedOnboarding()).isTrue();
        }

        @Test
        @DisplayName("Should convert list of UserDTOs to BaseUserDTO list")
        void shouldConvertUserDTOListToBaseUserDTOList() {
            // Given
            UserDTO dto1 = new UserDTO(testUserId, List.of(), TEST_USERNAME, TEST_PROFILE_PIC, TEST_NAME, TEST_BIO, TEST_EMAIL, true);
            UserDTO dto2 = new UserDTO(UUID.randomUUID(), List.of(), "user2", "pic2.jpg", "Name 2", "Bio 2", "email2@test.com", false);
            List<UserDTO> userDTOs = List.of(dto1, dto2);

            // When
            List<BaseUserDTO> baseDTOs = UserMapper.toBaseDTOList(userDTOs);

            // Then
            assertThat(baseDTOs).hasSize(2);
            assertThat(baseDTOs.get(0).getHasCompletedOnboarding()).isTrue();
            assertThat(baseDTOs.get(1).getHasCompletedOnboarding()).isFalse();
        }
    }

    @Nested
    @DisplayName("UserCreationDTO Conversion Tests")
    class UserCreationDTOTests {

        @Test
        @DisplayName("Should convert UserCreationDTO to UserDTO")
        void shouldConvertUserCreationDTOToUserDTO() {
            // Given
            UserCreationDTO creationDTO = new UserCreationDTO(testUserId, TEST_USERNAME, null, TEST_NAME, TEST_BIO, TEST_EMAIL);

            // When
            UserDTO userDTO = UserMapper.toDTOFromCreationUserDTO(creationDTO);

            // Then
            assertThat(userDTO).isNotNull();
            assertThat(userDTO.getId()).isEqualTo(testUserId);
            assertThat(userDTO.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(userDTO.getName()).isEqualTo(TEST_NAME);
            assertThat(userDTO.getBio()).isEqualTo(TEST_BIO);
            assertThat(userDTO.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(userDTO.getProfilePicture()).isNull(); // Profile picture URL is null in conversion
            assertThat(userDTO.getFriendUserIds()).isNull(); // New users have no friends
            assertThat(userDTO.getHasCompletedOnboarding()).isFalse(); // Default for new users
        }

        @Test
        @DisplayName("Should handle UserCreationDTO with minimal fields")
        void shouldHandleMinimalUserCreationDTO() {
            // Given
            UserCreationDTO creationDTO = new UserCreationDTO();
            creationDTO.setId(testUserId);
            creationDTO.setEmail(TEST_EMAIL);

            // When
            UserDTO userDTO = UserMapper.toDTOFromCreationUserDTO(creationDTO);

            // Then
            assertThat(userDTO.getId()).isEqualTo(testUserId);
            assertThat(userDTO.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(userDTO.getUsername()).isNull();
            assertThat(userDTO.getName()).isNull();
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle user with special characters in fields")
        void shouldHandleSpecialCharacters() {
            // Given
            testUser.setUsername("user_with-special.chars@123");
            testUser.setBio("Bio with émojis 🎉 and spëcial çhars!");
            testUser.setName("José María García-López");

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(dto.getBio()).isEqualTo(testUser.getBio());
            assertThat(dto.getName()).isEqualTo(testUser.getName());
        }

        @Test
        @DisplayName("Should handle very long bio")
        void shouldHandleVeryLongBio() {
            // Given
            String longBio = "A".repeat(1000);
            testUser.setBio(longBio);

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto.getBio()).isEqualTo(longBio);
            assertThat(dto.getBio().length()).isEqualTo(1000);
        }

        @Test
        @DisplayName("Should handle empty strings vs null")
        void shouldHandleEmptyStrings() {
            // Given
            testUser.setBio("");
            testUser.setName("");

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto.getBio()).isEmpty();
            assertThat(dto.getName()).isEmpty();
        }

        @Test
        @DisplayName("Should preserve UUID correctly through mapping")
        void shouldPreserveUUID() {
            // Given
            UUID specificId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
            testUser.setId(specificId);

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);
            User mappedBack = UserMapper.toEntity(dto);

            // Then
            assertThat(dto.getId()).isEqualTo(specificId);
            assertThat(mappedBack.getId()).isEqualTo(specificId);
        }

        @Test
        @DisplayName("Should handle URL with query parameters")
        void shouldHandleProfilePictureURL() {
            // Given
            String complexUrl = "https://example.com/pic.jpg?size=large&format=webp&token=abc123";
            testUser.setProfilePictureUrlString(complexUrl);

            // When
            BaseUserDTO dto = UserMapper.toDTO(testUser);

            // Then
            assertThat(dto.getProfilePicture()).isEqualTo(complexUrl);
        }
    }

    @Nested
    @DisplayName("Consistency Tests")
    class ConsistencyTests {

        @Test
        @DisplayName("Should maintain data consistency through round-trip")
        void shouldMaintainDataConsistencyThroughRoundTrip() {
            // When - Map to DTO and back to entity
            BaseUserDTO dto = UserMapper.toDTO(testUser);
            User mappedUser = UserMapper.toEntity(dto);

            // Then - Core fields should be consistent
            assertThat(mappedUser.getId()).isEqualTo(testUser.getId());
            assertThat(mappedUser.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(mappedUser.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(mappedUser.getName()).isEqualTo(testUser.getName());
            assertThat(mappedUser.getBio()).isEqualTo(testUser.getBio());
            assertThat(mappedUser.getProfilePictureUrlString()).isEqualTo(testUser.getProfilePictureUrlString());
        }

        @Test
        @DisplayName("Should convert between UserDTO and BaseUserDTO consistently")
        void shouldConvertBetweenUserDTOAndBaseUserDTO() {
            // Given
            List<UUID> friendIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            UserDTO userDTO = UserMapper.toDTO(testUser, friendIds);

            // When
            BaseUserDTO baseDTO = UserMapper.toBaseDTO(userDTO);

            // Then - All shared fields should be preserved
            assertThat(baseDTO.getId()).isEqualTo(userDTO.getId());
            assertThat(baseDTO.getUsername()).isEqualTo(userDTO.getUsername());
            assertThat(baseDTO.getEmail()).isEqualTo(userDTO.getEmail());
            assertThat(baseDTO.getName()).isEqualTo(userDTO.getName());
            assertThat(baseDTO.getBio()).isEqualTo(userDTO.getBio());
            assertThat(baseDTO.getProfilePicture()).isEqualTo(userDTO.getProfilePicture());
        }
    }
}

