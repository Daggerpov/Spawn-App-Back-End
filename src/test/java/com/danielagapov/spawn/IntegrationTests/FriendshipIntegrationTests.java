package com.danielagapov.spawn.IntegrationTests;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.Enums.UserStatus;
import com.danielagapov.spawn.Models.Friendship;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Repositories.IFriendshipRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FriendshipIntegrationTests {

    @Autowired
    private IFriendRequestService friendRequestService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IFriendshipRepository friendshipRepository;

    @Autowired
    private IFriendRequestsRepository friendRequestRepository;

    @Autowired
    private IUserRepository userRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        friendshipRepository.deleteAll();
        friendRequestRepository.deleteAll();
        
        // Create test users
        userA = createAndSaveUser("userA", "userA@example.com");
        userB = createAndSaveUser("userB", "userB@example.com");
        userC = createAndSaveUser("userC", "userC@example.com");
    }

    @Test
    void completeFriendshipFlow_ShouldWork_WhenFriendRequestAccepted() {
        // Step 1: User A sends friend request to User B
        CreateFriendRequestDTO requestDTO = new CreateFriendRequestDTO(
            null, userA.getId(), userB.getId()
        );
        CreateFriendRequestDTO savedRequest = friendRequestService.saveFriendRequest(requestDTO);
        
        assertNotNull(savedRequest);
        assertNotNull(savedRequest.getId());

        // Step 2: Verify friend request exists
        List<FriendRequest> incomingRequests = friendRequestRepository.findByReceiverId(userB.getId());
        assertEquals(1, incomingRequests.size());
        assertEquals(userA.getId(), incomingRequests.get(0).getSender().getId());

        // Step 3: Verify users are not yet friends
        assertFalse(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        assertFalse(userService.isUserFriendOfUser(userB.getId(), userA.getId()));

        // Step 4: User B accepts the friend request
        friendRequestService.acceptFriendRequest(savedRequest.getId());

        // Step 5: Verify friendship was created
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        assertTrue(userService.isUserFriendOfUser(userB.getId(), userA.getId()));

        // Step 6: Verify friend request was deleted
        List<FriendRequest> remainingRequests = friendRequestRepository.findByReceiverId(userB.getId());
        assertTrue(remainingRequests.isEmpty());

        // Step 7: Verify friendship exists in repository
        assertTrue(friendshipRepository.existsBidirectionally(userA.getId(), userB.getId()));

        // Step 8: Verify friends appear in each other's friend lists
        List<UUID> userAFriends = userService.getFriendUserIdsByUserId(userA.getId());
        List<UUID> userBFriends = userService.getFriendUserIdsByUserId(userB.getId());
        
        assertTrue(userAFriends.contains(userB.getId()));
        assertTrue(userBFriends.contains(userA.getId()));
    }

    @Test
    void mutualFriendRequest_ShouldAutoAccept_WhenBothUsersRequestEachOther() {
        // Step 1: User A sends friend request to User B
        CreateFriendRequestDTO requestAtoB = new CreateFriendRequestDTO(
            null, userA.getId(), userB.getId()
        );
        friendRequestService.saveFriendRequest(requestAtoB);

        // Step 2: User B sends friend request to User A (should auto-accept)
        CreateFriendRequestDTO requestBtoA = new CreateFriendRequestDTO(
            null, userB.getId(), userA.getId()
        );
        CreateFriendRequestDTO result = friendRequestService.saveFriendRequest(requestBtoA);

        // Step 3: Verify they are now friends
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        assertTrue(userService.isUserFriendOfUser(userB.getId(), userA.getId()));

        // Step 4: Verify no pending friend requests remain
        assertTrue(friendRequestRepository.findByReceiverId(userA.getId()).isEmpty());
        assertTrue(friendRequestRepository.findByReceiverId(userB.getId()).isEmpty());
    }

    @Test
    void mutualFriendCount_ShouldBeCorrect_WithMultipleFriendships() {
        // Create friendships: A-B, A-C, B-C
        userService.saveFriendToUser(userA.getId(), userB.getId());
        userService.saveFriendToUser(userA.getId(), userC.getId());
        userService.saveFriendToUser(userB.getId(), userC.getId());

        // A and B should have 1 mutual friend (C)
        int mutualCount = userService.getMutualFriendCount(userA.getId(), userB.getId());
        assertEquals(1, mutualCount);

        // Create additional users and friendships
        User userD = createAndSaveUser("userD", "userD@example.com");
        User userE = createAndSaveUser("userE", "userE@example.com");
        
        // Add more mutual friends: A-D, B-D, A-E, B-E
        userService.saveFriendToUser(userA.getId(), userD.getId());
        userService.saveFriendToUser(userB.getId(), userD.getId());
        userService.saveFriendToUser(userA.getId(), userE.getId());
        userService.saveFriendToUser(userB.getId(), userE.getId());

        // A and B should now have 3 mutual friends (C, D, E)
        mutualCount = userService.getMutualFriendCount(userA.getId(), userB.getId());
        assertEquals(3, mutualCount);
    }

    @Test
    void friendshipDeletion_ShouldWork_WhenFriendshipExists() {
        // Create friendship
        userService.saveFriendToUser(userA.getId(), userB.getId());
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userB.getId()));

        // Delete friendship
        friendshipRepository.deleteBidirectionally(userA.getId(), userB.getId());

        // Verify friendship no longer exists
        assertFalse(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        assertFalse(userService.isUserFriendOfUser(userB.getId(), userA.getId()));
    }

    @Test
    void canonicalOrdering_ShouldBeConsistent_RegardlessOfInputOrder() {
        // Create friendship in one order
        userService.saveFriendToUser(userA.getId(), userB.getId());

        // Try to create the same friendship in reverse order (should be no-op)
        userService.saveFriendToUser(userB.getId(), userA.getId());

        // Should only have one friendship record
        List<Friendship> allFriendships = friendshipRepository.findAll();
        long friendshipCount = allFriendships.stream()
            .filter(f -> (f.getUserA().getId().equals(userA.getId()) && f.getUserB().getId().equals(userB.getId())) ||
                        (f.getUserA().getId().equals(userB.getId()) && f.getUserB().getId().equals(userA.getId())))
            .count();
        
        assertEquals(1, friendshipCount);
    }

    @Test
    void fullFriendUserDTOs_ShouldContainCorrectFriends() {
        // Create network: A-B, A-C, B-C
        userService.saveFriendToUser(userA.getId(), userB.getId());
        userService.saveFriendToUser(userA.getId(), userC.getId());
        userService.saveFriendToUser(userB.getId(), userC.getId());

        // Get A's friends
        List<FullFriendUserDTO> userAFriends = userService.getFullFriendUsersByUserId(userA.getId());
        
        assertEquals(2, userAFriends.size()); // A has 2 friends: B and C
        
        // Find B in A's friend list
        FullFriendUserDTO friendB = userAFriends.stream()
            .filter(f -> f.getId().equals(userB.getId()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(friendB);
        assertEquals(userB.getUsername(), friendB.getUsername());
        
        // Verify mutual friend count using service method directly
        int mutualCount = userService.getMutualFriendCount(userA.getId(), userB.getId());
        assertEquals(1, mutualCount); // A and B have 1 mutual friend (C)
    }

    @Test
    void concurrentFriendshipCreation_ShouldHandleGracefully() {
        // This test simulates concurrent friendship creation
        // In a real scenario, this would be handled by database constraints
        
        // Try to create the same friendship multiple times
        assertDoesNotThrow(() -> {
            userService.saveFriendToUser(userA.getId(), userB.getId());
            userService.saveFriendToUser(userA.getId(), userB.getId()); // Should be no-op
            userService.saveFriendToUser(userB.getId(), userA.getId()); // Should be no-op
        });

        // Should still only have one friendship
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        
        List<UUID> userAFriends = userService.getFriendUserIdsByUserId(userA.getId());
        assertEquals(1, userAFriends.size());
    }

    @Test
    void userDeletion_ShouldCascadeDeleteFriendships() {
        // Create friendships
        userService.saveFriendToUser(userA.getId(), userB.getId());
        userService.saveFriendToUser(userA.getId(), userC.getId());
        
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userB.getId()));
        assertTrue(userService.isUserFriendOfUser(userA.getId(), userC.getId()));

        // Delete user A
        userRepository.delete(userA);
        entityManager.flush(); // Force the delete to execute
        entityManager.clear(); // Clear the persistence context

        // Friendships involving user A should be deleted
        assertFalse(friendshipRepository.existsBidirectionally(userA.getId(), userB.getId()));
        assertFalse(friendshipRepository.existsBidirectionally(userA.getId(), userC.getId()));
        
        // Other friendships should remain
        userService.saveFriendToUser(userB.getId(), userC.getId());
        assertTrue(userService.isUserFriendOfUser(userB.getId(), userC.getId()));
    }

    private User createAndSaveUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(username);
        user.setBio("Bio for " + username);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
