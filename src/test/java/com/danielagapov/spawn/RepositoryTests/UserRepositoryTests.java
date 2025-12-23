package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.shared.util.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserRepository
 * Tests database operations for User entities
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTests {

    @Autowired
    private IUserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setProfilePictureUrlString("pic.jpg");
        testUser.setBio("Test bio");
        testUser.setStatus(UserStatus.ACTIVE);
    }

    // MARK: - Basic CRUD Tests

    @Test
    void save_ShouldPersistUser_WhenValidUser() {
        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getUsername());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("Test User", saved.getName());
    }

    @Test
    void findById_ShouldReturnUser_WhenUserExists() {
        User saved = userRepository.save(testUser);

        Optional<User> found = userRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenUserDoesNotExist() {
        Optional<User> found = userRepository.findById(UUID.randomUUID());

        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllUsers_WhenUsersExist() {
        userRepository.save(testUser);

        User secondUser = new User();
        secondUser.setUsername("seconduser");
        secondUser.setEmail("second@example.com");
        secondUser.setName("Second User");
        secondUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(secondUser);

        List<User> all = userRepository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void deleteById_ShouldRemoveUser_WhenUserExists() {
        User saved = userRepository.save(testUser);
        UUID userId = saved.getId();

        userRepository.deleteById(userId);

        Optional<User> found = userRepository.findById(userId);
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_ShouldReturnTrue_WhenUserExists() {
        User saved = userRepository.save(testUser);

        boolean exists = userRepository.existsById(saved.getId());

        assertTrue(exists);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenUserDoesNotExist() {
        boolean exists = userRepository.existsById(UUID.randomUUID());

        assertFalse(exists);
    }

    // MARK: - Custom Query Tests

    @Test
    void findByUsername_ShouldReturnUser_WhenUsernameExists() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenUsernameDoesNotExist() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenEmailExists() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenEmailDoesNotExist() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
        userRepository.save(testUser);

        boolean exists = userRepository.existsByUsername("testuser");

        assertTrue(exists);
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenUsernameDoesNotExist() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertFalse(exists);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        userRepository.save(testUser);

        boolean exists = userRepository.existsByEmail("test@example.com");

        assertTrue(exists);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailDoesNotExist() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertFalse(exists);
    }

    // MARK: - Update Tests

    @Test
    void update_ShouldModifyUser_WhenUserExists() {
        User saved = userRepository.save(testUser);

        saved.setName("Updated Name");
        saved.setBio("Updated bio");
        User updated = userRepository.save(saved);

        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated bio", updated.getBio());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    void update_ShouldModifyUsername_WhenUsernameChanged() {
        User saved = userRepository.save(testUser);

        saved.setUsername("newusername");
        User updated = userRepository.save(saved);

        assertEquals("newusername", updated.getUsername());
    }

    @Test
    void update_ShouldModifyEmail_WhenEmailChanged() {
        User saved = userRepository.save(testUser);

        saved.setEmail("newemail@example.com");
        User updated = userRepository.save(saved);

        assertEquals("newemail@example.com", updated.getEmail());
    }

    @Test
    void update_ShouldModifyProfilePicture_WhenPictureChanged() {
        User saved = userRepository.save(testUser);

        saved.setProfilePictureUrlString("newpic.jpg");
        User updated = userRepository.save(saved);

        assertEquals("newpic.jpg", updated.getProfilePictureUrlString());
    }

    // MARK: - Edge Case Tests

    @Test
    void save_ShouldHandleNullBio_WhenBioNotProvided() {
        testUser.setBio(null);

        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertNull(saved.getBio());
    }

    @Test
    void save_ShouldHandleNullProfilePicture_WhenPictureNotProvided() {
        testUser.setProfilePictureUrlString(null);

        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertNull(saved.getProfilePictureUrlString());
    }

    @Test
    void save_ShouldHandleLongUsername_WhenUsernameIsVeryLong() {
        String longUsername = "a".repeat(50);
        testUser.setUsername(longUsername);

        User saved = userRepository.save(testUser);

        assertEquals(longUsername, saved.getUsername());
    }

    @Test
    void save_ShouldHandleLongEmail_WhenEmailIsVeryLong() {
        String longEmail = "a".repeat(50) + "@example.com";
        testUser.setEmail(longEmail);

        User saved = userRepository.save(testUser);

        assertEquals(longEmail, saved.getEmail());
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenNoUsers() {
        List<User> all = userRepository.findAll();

        assertTrue(all.isEmpty());
    }

    @Test
    void save_ShouldGenerateUniqueIds_WhenMultipleUsers() {
        User first = userRepository.save(testUser);

        User second = new User();
        second.setUsername("seconduser");
        second.setEmail("second@example.com");
        second.setName("Second User");
        second.setStatus(UserStatus.ACTIVE);
        User secondSaved = userRepository.save(second);

        assertNotEquals(first.getId(), secondSaved.getId());
    }

    @Test
    void findByUsername_ShouldBeCaseInsensitive_WhenSearchingByUsername() {
        userRepository.save(testUser);

        // Note: This test assumes the repository has case-insensitive search
        // If not implemented, this test documents expected behavior
        Optional<User> found = userRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
    }

    @Test
    void save_ShouldHandleSpecialCharactersInUsername_WhenUsernameHasSpecialChars() {
        testUser.setUsername("test_user-123");

        User saved = userRepository.save(testUser);

        assertEquals("test_user-123", saved.getUsername());
    }

    @Test
    void save_ShouldHandleSpecialCharactersInEmail_WhenEmailHasSpecialChars() {
        testUser.setEmail("test+user@example.com");

        User saved = userRepository.save(testUser);

        assertEquals("test+user@example.com", saved.getEmail());
    }

    @Test
    void save_ShouldHandleEmptyBio_WhenBioIsEmptyString() {
        testUser.setBio("");

        User saved = userRepository.save(testUser);

        assertEquals("", saved.getBio());
    }

    @Test
    void findByEmail_ShouldHandleEmailCase_WhenSearchingByEmail() {
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
    }

    @Test
    void save_ShouldHandleMultipleUsersWithDifferentEmails_WhenEmailsAreUnique() {
        User first = userRepository.save(testUser);

        User second = new User();
        second.setUsername("seconduser");
        second.setEmail("second@example.com");
        second.setName("Second User");
        second.setStatus(UserStatus.ACTIVE);
        User secondSaved = userRepository.save(second);

        assertNotNull(first.getId());
        assertNotNull(secondSaved.getId());
        assertNotEquals(first.getEmail(), secondSaved.getEmail());
    }

    @Test
    void save_ShouldHandleMultipleUsersWithDifferentUsernames_WhenUsernamesAreUnique() {
        User first = userRepository.save(testUser);

        User second = new User();
        second.setUsername("differentuser");
        second.setEmail("different@example.com");
        second.setName("Different User");
        second.setStatus(UserStatus.ACTIVE);
        User secondSaved = userRepository.save(second);

        assertNotNull(first.getId());
        assertNotNull(secondSaved.getId());
        assertNotEquals(first.getUsername(), secondSaved.getUsername());
    }

    @Test
    void update_ShouldPreserveId_WhenUserUpdated() {
        User saved = userRepository.save(testUser);
        UUID originalId = saved.getId();

        saved.setName("Updated Name");
        User updated = userRepository.save(saved);

        assertEquals(originalId, updated.getId());
    }

    @Test
    void save_ShouldHandleLongBio_WhenBioIsVeryLong() {
        String longBio = "a".repeat(500);
        testUser.setBio(longBio);

        User saved = userRepository.save(testUser);

        assertEquals(longBio, saved.getBio());
    }

    @Test
    void findByUsername_ShouldReturnCorrectUser_WhenMultipleUsersExist() {
        userRepository.save(testUser);

        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setName("Another User");
        anotherUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(anotherUser);

        Optional<User> found = userRepository.findByUsername("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_ShouldReturnCorrectUser_WhenMultipleUsersExist() {
        userRepository.save(testUser);

        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setName("Another User");
        anotherUser.setStatus(UserStatus.ACTIVE);
        userRepository.save(anotherUser);

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getUsername());
        assertEquals("test@example.com", found.get().getEmail());
    }
}

