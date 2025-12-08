package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.social.internal.domain.Friendship;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.social.internal.repositories.IFriendshipRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class FriendshipRepositoryTests {

    @Autowired
    private IFriendshipRepository friendshipRepository;

    @Autowired
    private IUserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // Create test users
        userA = createAndSaveUser("userA", "userA@example.com");
        userB = createAndSaveUser("userB", "userB@example.com");
        userC = createAndSaveUser("userC", "userC@example.com");
    }

    @Test
    void existsByUserA_IdAndUserB_Id_ShouldReturnTrue_WhenFriendshipExists() {
        // Given
        Friendship friendship = createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // When
        boolean exists = friendshipRepository.existsByUserA_IdAndUserB_Id(userA.getId(), userB.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUserA_IdAndUserB_Id_ShouldReturnFalse_WhenFriendshipDoesNotExist() {
        // When
        boolean exists = friendshipRepository.existsByUserA_IdAndUserB_Id(userA.getId(), userB.getId());

        // Then
        assertFalse(exists);
    }

    @Test
    void existsBidirectionally_ShouldReturnTrue_WhenFriendshipExistsInEitherDirection() {
        // Given - Create friendship with userA as userA and userB as userB
        Friendship friendship = createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // When - Check both directions
        boolean existsAB = friendshipRepository.existsBidirectionally(userA.getId(), userB.getId());
        boolean existsBA = friendshipRepository.existsBidirectionally(userB.getId(), userA.getId());

        // Then
        assertTrue(existsAB);
        assertTrue(existsBA);
    }

    @Test
    void findByUserA_Id_ShouldReturnFriendships_WhenUserHasFriends() {
        // Given
        Friendship friendship1 = createFriendship(userA, userB);
        Friendship friendship2 = createFriendship(userA, userC);
        friendshipRepository.save(friendship1);
        friendshipRepository.save(friendship2);

        // When
        List<Friendship> friendships = friendshipRepository.findByUserA_Id(userA.getId());

        // Then
        assertEquals(2, friendships.size());
        assertTrue(friendships.stream().anyMatch(f -> f.getUserB().getId().equals(userB.getId())));
        assertTrue(friendships.stream().anyMatch(f -> f.getUserB().getId().equals(userC.getId())));
    }

    @Test
    void findAllByUserIdBidirectional_ShouldReturnAllFriendships_RegardlessOfDirection() {
        // Given - Create friendships where userA is sometimes userA, sometimes userB
        UUID smallerId = userA.getId().compareTo(userB.getId()) < 0 ? userA.getId() : userB.getId();
        UUID largerId = userA.getId().compareTo(userB.getId()) > 0 ? userA.getId() : userB.getId();
        User smallerUser = smallerId.equals(userA.getId()) ? userA : userB;
        User largerUser = largerId.equals(userA.getId()) ? userA : userB;

        Friendship friendship1 = createFriendship(smallerUser, largerUser);
        Friendship friendship2 = createFriendship(userA, userC); // Assuming userA < userC or userC < userA
        friendshipRepository.save(friendship1);
        friendshipRepository.save(friendship2);

        // When
        List<Friendship> friendships = friendshipRepository.findAllByUserIdBidirectional(userA.getId());

        // Then
        assertEquals(2, friendships.size());
    }

    @Test
    void deleteBidirectionally_ShouldDeleteFriendship_RegardlessOfDirection() {
        // Given
        Friendship friendship = createFriendship(userA, userB);
        friendshipRepository.save(friendship);
        assertTrue(friendshipRepository.existsBidirectionally(userA.getId(), userB.getId()));

        // When
        friendshipRepository.deleteBidirectionally(userB.getId(), userA.getId());

        // Then
        assertFalse(friendshipRepository.existsBidirectionally(userA.getId(), userB.getId()));
    }

    @Test
    void save_ShouldEnforceUniqueConstraint_WhenDuplicateFriendshipCreated() {
        // Given
        Friendship friendship1 = createFriendship(userA, userB);
        friendshipRepository.save(friendship1);

        // When & Then - Attempt to create duplicate friendship
        Friendship friendship2 = createFriendship(userA, userB);
        assertThrows(DataIntegrityViolationException.class, () -> {
            friendshipRepository.save(friendship2);
            friendshipRepository.flush(); // Force immediate constraint check
        });
    }

    @Test
    void save_ShouldSetCreatedAt_WhenFriendshipCreated() {
        // Given
        Friendship friendship = createFriendship(userA, userB);

        // When
        Friendship saved = friendshipRepository.save(friendship);

        // Then
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByUserB_Id_ShouldReturnFriendships_WhenUserIsFriendB() {
        // Given
        Friendship friendship = createFriendship(userA, userB);
        friendshipRepository.save(friendship);

        // When
        List<Friendship> friendships = friendshipRepository.findByUserB_Id(userB.getId());

        // Then
        assertEquals(1, friendships.size());
        assertEquals(userA.getId(), friendships.get(0).getUserA().getId());
    }

    @Test
    void canonicalOrdering_ShouldWork_WhenUsersAreInDifferentOrder() {
        // Test that the canonical ordering logic works correctly
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        // Ensure we have different UUIDs
        while (id1.equals(id2)) {
            id2 = UUID.randomUUID();
        }
        
        // Test canonical ordering logic without using variables
        
        // Test the helper method
        boolean exists1 = friendshipRepository.existsBidirectionally(id1, id2);
        boolean exists2 = friendshipRepository.existsBidirectionally(id2, id1);
        
        // Both should return the same result (false in this case since no friendship exists)
        assertEquals(exists1, exists2);
    }

    @Test
    void cascadeDelete_ShouldDeleteFriendships_WhenUserIsDeleted() {
        // Given
        Friendship friendship = createFriendship(userA, userB);
        friendshipRepository.save(friendship);
        entityManager.flush(); // Ensure the friendship is persisted
        assertTrue(friendshipRepository.existsByUserA_IdAndUserB_Id(userA.getId(), userB.getId()));

        // When - Delete one of the users
        userRepository.delete(userA);
        entityManager.flush(); // Force the delete to execute
        entityManager.clear(); // Clear the persistence context

        // Then - Friendship should be deleted due to cascade
        assertFalse(friendshipRepository.existsByUserA_IdAndUserB_Id(userA.getId(), userB.getId()));
    }

    @Test
    void multipleUsers_ShouldHandleManyFriendships_Correctly() {
        // Given - Create multiple users and friendships
        User user1 = createAndSaveUser("user1", "user1@example.com");
        User user2 = createAndSaveUser("user2", "user2@example.com");
        User user3 = createAndSaveUser("user3", "user3@example.com");
        User user4 = createAndSaveUser("user4", "user4@example.com");

        // Create friendships: user1-user2, user1-user3, user2-user3, user3-user4
        friendshipRepository.save(createFriendship(user1, user2));
        friendshipRepository.save(createFriendship(user1, user3));
        friendshipRepository.save(createFriendship(user2, user3));
        friendshipRepository.save(createFriendship(user3, user4));

        // When - Get all friendships for user3
        List<Friendship> user3Friendships = friendshipRepository.findAllByUserIdBidirectional(user3.getId());

        // Then - user3 should have 3 friendships
        assertEquals(3, user3Friendships.size());
    }

    private User createAndSaveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(username);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Friendship createFriendship(User userA, User userB) {
        Friendship friendship = new Friendship();
        friendship.setUserA(userA);
        friendship.setUserB(userB);
        return friendship;
    }
}
