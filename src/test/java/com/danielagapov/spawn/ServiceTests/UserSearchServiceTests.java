package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserSearch.UserSearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSearchServiceTests {

    @Mock
    private IUserRepository userRepository; // Mock repository

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    @Mock
    private IBlockedUserService blockedUserService;

    @InjectMocks
    private UserSearchService userSearchService; // Injected service to test

    // Create at least 5 users for testing
    private User user1, user2, user3, user4, user5;

    @BeforeEach
    void setUp() {
        // Assuming User has a constructor: User(UUID id, String username, String profilePicture, String name, String bio, String email)
        user1 = new User(UUID.randomUUID(), "alicej", null, "Alice Johnson", "Bio of Alice", "alice@example.com");
        user2 = new User(UUID.randomUUID(), "alicia123", null, "Alicia Jameson", "Bio of Alicia", "alicia@example.com");
        user3 = new User(UUID.randomUUID(), "bob99", null, "Bob Smith", "Bio of Bob", "bob@example.com");
        user4 = new User(UUID.randomUUID(), "albert007", null, "Albert Jones", "Bio of Albert", "albert@example.com");
        user5 = new User(UUID.randomUUID(), "alexw", null, "Alex Williams", "Bio of Alex", "alex@example.com");
    }

    @Test
    void testSearchByQuery_ReturnsEmptyList_WhenQueryIsBlank() {
        List<BaseUserDTO> result = userSearchService.searchByQuery("");
        assertTrue(result.isEmpty(), "Expected an empty list for a blank query.");
    }

    @Test
    void testSearchByQuery_ReturnsEmptyList_WhenNoUsersMatchPrefix() {
        // Simulate no users found for given prefix
        when(userRepository.findUsersWithPrefix(anyString(), any()))
                .thenReturn(Collections.emptyList());

        List<BaseUserDTO> result = userSearchService.searchByQuery("Xyz");
        assertTrue(result.isEmpty(), "Expected an empty list when no users match the prefix.");
    }

    @Test
    void testSearchByQuery_FiltersAndRanksUsersCorrectly() {
        // Simulate database returning 5 users when prefix is "a"
        when(userRepository.findUsersWithPrefix(eq("a"), any()))
                .thenReturn(Arrays.asList(user1, user2, user3, user4, user5));

        List<BaseUserDTO> result = userSearchService.searchByQuery("Alice");

        // Assert that results are not empty
        assertFalse(result.isEmpty(), "Expected non-empty results.");

        // Check that at least one user is returned; adjust expected size if your filtering logic is known
        // For example, if filtering is strict, you might expect 3 matches.
        // Here we simply ensure the top result is the closest match.
        BaseUserDTO topResult = result.get(0);
        assertEquals("Alice Johnson", topResult.getName(), "The most similar user should be ranked first.");
    }

    @Test
    void testSearchByQuery_IncludesUsersIfFilteringRemovesTooManyResults() {
        // Simulate only one user returned for prefix "b"
        when(userRepository.findUsersWithPrefix(eq("b"), any()))
                .thenReturn(List.of(user3));

        List<BaseUserDTO> result = userSearchService.searchByQuery("Bobby");
        assertEquals(1, result.size(), "Expected Bob to be included even if filtering removes too many users.");
    }

    @Test
    void testSearchByQuery_SortsUsersByJaroWinklerDistance() {
        // Simulate database returning users with names starting with "a"
        when(userRepository.findUsersWithPrefix(eq("a"), any()))
                .thenReturn(Arrays.asList(user5, user4, user2, user1));

        List<BaseUserDTO> result = userSearchService.searchByQuery("Alicia");

        // Expect that the user with the closest match ("Alicia") appears first.
        // Adjust assertions based on how your ranking works. For this test, we assume user2 ("Alicia") is best.
        assertFalse(result.isEmpty(), "Expected non-empty result list.");
        assertEquals("Alicia Jameson", result.get(0).getName(), "Alicia should be ranked highest based on similarity.");
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnFilteredRecommendations_WhenSearchQueryIsProvided() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "alice", "profile.jpg", "Alice Smith", "Bio", "alice@example.com", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "bob", "profile.jpg", "Bob Johnson", "Bio", "bob@example.com", 1);
        RecommendedFriendUserDTO friend3 = new RecommendedFriendUserDTO(UUID.randomUUID(), "charlie", "profile.jpg", "Charlie Brown", "Bio", "charlie@example.com", 1);

        // Mock all required friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserSearchService spyUserSearchService = spy(userSearchService);
        when(spyUserSearchService.getRecommendedMutuals(userId)).thenReturn(List.of(friend1, friend2, friend3));
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(List.of());

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "Alice");

        // Assert
        assertEquals(1, result.getSecond().size()); // Only Alice should be returned
        assertEquals(friend1, result.getSecond().get(0));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnAllRecommendations_WhenSearchQueryIsEmpty() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "alice", "profile.jpg", "Alice Smith", "Bio", "alice@example.com", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "bob", "profile.jpg", "Bob Johnson", "Bio", "bob@example.com", 1);

        // Mock the friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserSearchService spyUserSearchService = spy(userSearchService);
        // Mock the UserService method instead of the local method
        when(userService.getLimitedRecommendedFriendsForUserId(userId)).thenReturn(List.of(friend1, friend2));
        doReturn(List.of()).when(userService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "");

        // Assert
        assertEquals(2, result.getSecond().size()); // Both friends should be returned
        assertTrue(result.getSecond().contains(friend1));
        assertTrue(result.getSecond().contains(friend2));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldReturnEmpty_WhenNoRecommendationsMatch() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend1 = new RecommendedFriendUserDTO(UUID.randomUUID(), "alice", "profile.jpg", "Alice Smith", "Bio", "alice@example.com", 1);
        RecommendedFriendUserDTO friend2 = new RecommendedFriendUserDTO(UUID.randomUUID(), "bob", "profile.jpg", "Bob Johnson", "Bio", "bob@example.com", 1);

        // Mock the friend request service methods
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        // Use spy to isolate the test from internal implementations
        UserSearchService spyUserSearchService = spy(userSearchService);
        doReturn(List.of(friend1, friend2)).when(spyUserSearchService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(userService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "Charlie");

        // Assert
        assertEquals(0, result.getSecond().size()); // No recommendations should match
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldWorkWithQueryFullRecommendationsAndFriends() {
        UserSearchService spyUserSearchService = spy(userSearchService);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();
        UUID user4Id = UUID.randomUUID();
        UUID user5Id = UUID.randomUUID();
        RecommendedFriendUserDTO user2Full = new RecommendedFriendUserDTO(user2Id, "jane_doe", "profile.jpg", "Jane Doe", "A bio", "jane.doe@example.com", 1);
        RecommendedFriendUserDTO user3Full = new RecommendedFriendUserDTO(user3Id, "person", "profile.jpg", "Lorem Ipsum", "A bio", "email@e.com", 1);
        RecommendedFriendUserDTO user4Full = new RecommendedFriendUserDTO(user4Id, "LaurenIbson", "profile.jpg", "Lauren Ibson", "A bio", "lauren_ibson@e.ca", 1);

        UUID ftId = UUID.randomUUID();
        // Very incomplete relationship but it should suffice for a test.
        FriendTagDTO ft = new FriendTagDTO(ftId, "Everyone", "#ffffff", user1Id, List.of(), true);
        FullFriendUserDTO user5Full = new FullFriendUserDTO(user5Id, "thatPerson", "profile.jpg", "That Person", "A bio", "thatPerson@email.com", List.of(ft));

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserSearchService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(userService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of(user5Full));

        SearchedUserResult res = spyUserSearchService.getRecommendedFriendsBySearch(user1Id, "person");
        SearchedUserResult expected = new SearchedUserResult(List.of(), List.of(user3Full), List.of(user5Full));
        assertEquals(expected, res);
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldWorkWithQueryFullRecommendations() {
        UserSearchService spyUserSearchService = spy(userSearchService);
        UUID user1Id = UUID.randomUUID();
        UUID user2Id = UUID.randomUUID();
        UUID user3Id = UUID.randomUUID();
        UUID user4Id = UUID.randomUUID();
        RecommendedFriendUserDTO user2Full = new RecommendedFriendUserDTO(user2Id, "jane_doe", "profile.jpg", "Jane Doe", "A bio", "jane.doe@example.com", 1);
        RecommendedFriendUserDTO user3Full = new RecommendedFriendUserDTO(user3Id, "person", "profile.jpg", "Lorem Ipsum", "A bio", "email@e.com", 1);
        RecommendedFriendUserDTO user4Full = new RecommendedFriendUserDTO(user4Id, "LaurenIbson", "profile.jpg", "Lauren Ibson", "A bio", "lauren_ibson@e.ca", 1);

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserSearchService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(userService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of());

        SearchedUserResult res = spyUserSearchService.getRecommendedFriendsBySearch(user1Id, "person");
        assertEquals(new SearchedUserResult(List.of(), List.of(user3Full), List.of()), res);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialFirstName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userSearchService, "isQueryMatch", user, "Jo");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialLastName() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userSearchService, "isQueryMatch", user, "oe");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldMatchPartialUsername() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userSearchService, "isQueryMatch", user, "hnd");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldBeCaseInsensitive() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userSearchService, "isQueryMatch", user, "JOHN");
        assertTrue(result);
    }

    @Test
    void isQueryMatch_ShouldReturnFalseWhenNoMatch() {
        // Arrange
        UUID userId = UUID.randomUUID();
        AbstractUserDTO user = new BaseUserDTO(userId, "John Doe", "john@example.com", "johndoe", "Bio", "profile.jpg");

        // Act & Assert - Using reflection to access private method
        boolean result = (boolean) ReflectionTestUtils.invokeMethod(userSearchService, "isQueryMatch", user, "xyz");
        assertFalse(result);
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldHandleEmptySearchQuery() {
        // Arrange
        UUID userId = UUID.randomUUID();
        RecommendedFriendUserDTO friend = new RecommendedFriendUserDTO(UUID.randomUUID(), "alice", "profile.jpg", "Alice Smith", "Bio", "alice@example.com", 1);

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserSearchService spyUserSearchService = spy(userSearchService);
        // Mock the UserService method instead of the local method
        when(userService.getLimitedRecommendedFriendsForUserId(userId)).thenReturn(List.of(friend));
        doReturn(List.of()).when(userService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "");

        // Assert
        assertEquals(1, result.getSecond().size());
        assertEquals(friend, result.getSecond().get(0));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldHandleSpecialCharactersInQuery() {
        // Arrange
        UUID userId = UUID.randomUUID();

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserSearchService spyUserSearchService = spy(userSearchService);
        doReturn(List.of()).when(spyUserSearchService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(userService).getFullFriendUsersByUserId(userId);

        // Act & Assert - Should not throw exceptions for unusual search terms
        assertDoesNotThrow(() -> spyUserSearchService.getRecommendedFriendsBySearch(userId, "%^&*"));
    }

    @Test
    void getRecommendedFriendsBySearch_ShouldFilterIncomingFriendRequests() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        BaseUserDTO requesterInfo = new BaseUserDTO(requesterId, "David Search", "dsearch@example.com", "davidsearch", "Bio", "profile.jpg");
        FetchFriendRequestDTO friendRequest = mock(FetchFriendRequestDTO.class);
        when(friendRequest.getSenderUser()).thenReturn(requesterInfo);

        // Mock services
        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(userId)).thenReturn(List.of(friendRequest));
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(userId)).thenReturn(List.of());

        UserSearchService spyUserSearchService = spy(userSearchService);
        doReturn(List.of()).when(spyUserSearchService).getRecommendedMutuals(userId);
        doReturn(List.of()).when(userService).getFullFriendUsersByUserId(userId);

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "search");

        // Assert
        assertEquals(1, result.getFirst().size());
        assertEquals(friendRequest, result.getFirst().get(0));
    }
}

