package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.SearchResultUserDTO;
import com.danielagapov.spawn.Enums.UserRelationshipType;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.Analytics.SearchAnalyticsService;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.FuzzySearch.FuzzySearchService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.UserSearch.UserSearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSearchServiceTests {

    @Mock
    private IUserRepository userRepository; // Mock repository

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Mock
    private IFriendRequestService friendRequestService;

    @Mock
    private IUserService userService;

    @Mock
    private ILogger logger;

    @Mock
    private IBlockedUserService blockedUserService;

    @Mock
    private FuzzySearchService<User> fuzzySearchService;

    @Mock
    private SearchAnalyticsService searchAnalyticsService;

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
        List<BaseUserDTO> result = userSearchService.searchByQuery("", null);
        assertTrue(result.isEmpty(), "Expected an empty list for a blank query.");
    }

    @Test
    void testSearchByQuery_ReturnsEmptyList_WhenNoUsersMatchQuery() {
        // Arrange
        String searchQuery = "nonexistent";
        when(userRepository.findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class))).thenReturn(List.of());

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchByQuery_FiltersAndRanksUsersCorrectly() {
        // Arrange
        String searchQuery = "alice";
        List<User> mockUsers = List.of(user1, user2); // user1="Alice Johnson", user2="Alicia Jameson" 
        when(userRepository.findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class))).thenReturn(mockUsers);

        // Mock fuzzy search to return user1 and user2 as matching results
        when(fuzzySearchService.search(eq(searchQuery), eq(mockUsers), any(), any()))
                .thenReturn(List.of(
                        new FuzzySearchService.SearchResult<>(user1, 0.9, "name", false),
                        new FuzzySearchService.SearchResult<>(user2, 0.8, "name", false)
                ));

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertFalse(result.isEmpty(), "Expected non-empty results.");
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getName().equals("Alice Johnson")));
        assertTrue(result.stream().anyMatch(u -> u.getName().equals("Alicia Jameson")));
    }

    @Test
    void testSearchByQuery_IncludesUsersIfFilteringRemovesTooManyResults() {
        // Arrange
        String searchQuery = "bob";
        List<User> mockUsers = List.of(user3); // user3 = "Bob Smith"
        when(userRepository.findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class))).thenReturn(mockUsers);

        // Mock fuzzy search to return user3 as a matching result
        when(fuzzySearchService.search(eq(searchQuery), eq(mockUsers), any(), any()))
                .thenReturn(List.of(
                        new FuzzySearchService.SearchResult<>(user3, 0.85, "name", false)
                ));

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertEquals(1, result.size(), "Expected Bob to be included even if filtering removes too many users.");
        assertEquals("Bob Smith", result.get(0).getName());
    }

    @Test
    void testSearchByQuery_SortsUsersByJaroWinklerDistance() {
        // Arrange
        String searchQuery = "alice";
        List<User> mockUsers = List.of(user1, user2); // user1="Alice Johnson", user2="Alicia Jameson"
        when(userRepository.findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class))).thenReturn(mockUsers);

        // Mock fuzzy search to return users sorted by similarity (user1 has higher score than user2)
        when(fuzzySearchService.search(eq(searchQuery), eq(mockUsers), any(), any()))
                .thenReturn(List.of(
                        new FuzzySearchService.SearchResult<>(user1, 0.95, "name", false),
                        new FuzzySearchService.SearchResult<>(user2, 0.80, "name", false)
                ));

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertFalse(result.isEmpty(), "Expected non-empty result list.");
        assertEquals(2, result.size());
        // The first result should be the one with higher similarity score
        assertEquals("Alice Johnson", result.get(0).getName());
        assertEquals("Alicia Jameson", result.get(1).getName());
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

        // Mock fuzzy search service to return Alice Smith as a match for "Alice"
        User userObject = new User();
        userObject.setId(friend1.getId());
        userObject.setName(friend1.getName());
        userObject.setUsername(friend1.getUsername());
        FuzzySearchService.SearchResult<User> searchResult = new FuzzySearchService.SearchResult<>(userObject, 0.95, "name", false);
        when(fuzzySearchService.search(eq("Alice"), any(), any(), any())).thenReturn(List.of(searchResult));

        // Use spy to isolate the test from internal implementations
        UserSearchService spyUserSearchService = spy(userSearchService);
        when(spyUserSearchService.getRecommendedMutuals(userId)).thenReturn(List.of(friend1, friend2, friend3));
        when(userService.getFullFriendUsersByUserId(userId)).thenReturn(List.of());

        // Act
        SearchedUserResult result = spyUserSearchService.getRecommendedFriendsBySearch(userId, "Alice");

        // Assert
        assertEquals(1, result.getUsers().size()); // Only Alice should be returned
        
        // Verify the user details and relationship type
        var searchResultUser = result.getUsers().get(0);
        assertEquals(friend1.getId(), searchResultUser.getUser().getId());
        assertEquals(friend1.getName(), searchResultUser.getUser().getName());
        assertEquals(UserRelationshipType.RECOMMENDED_FRIEND, searchResultUser.getRelationshipType());
        assertEquals(friend1.getMutualFriendCount(), searchResultUser.getMutualFriendCount());
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
        assertEquals(2, result.getUsers().size()); // Both friends should be returned
        
        // Verify both users are present with correct relationship type
        var recommendedFriends = result.getUsers().stream()
                .filter(u -> u.getRelationshipType() == UserRelationshipType.RECOMMENDED_FRIEND)
                .toList();
        assertEquals(2, recommendedFriends.size());
        
        // Check that both friend IDs are present
        var userIds = recommendedFriends.stream()
                .map(u -> u.getUser().getId())
                .toList();
        assertTrue(userIds.contains(friend1.getId()));
        assertTrue(userIds.contains(friend2.getId()));
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
        assertEquals(0, result.getUsers().size()); // No recommendations should match
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

        // Mock fuzzy search service to return both user3Full and user5Full as matches for "person"
        User userObject3 = new User();
        userObject3.setId(user3Full.getId());
        userObject3.setName(user3Full.getName());
        userObject3.setUsername(user3Full.getUsername());
        FuzzySearchService.SearchResult<User> searchResult3 = new FuzzySearchService.SearchResult<>(userObject3, 0.95, "username", false);
        
        User userObject5 = new User();
        userObject5.setId(user5Full.getId());
        userObject5.setName(user5Full.getName());
        userObject5.setUsername(user5Full.getUsername());
        FuzzySearchService.SearchResult<User> searchResult5 = new FuzzySearchService.SearchResult<>(userObject5, 0.85, "name", false);
        
        when(fuzzySearchService.search(eq("person"), any(), any(), any())).thenReturn(List.of(searchResult3, searchResult5));

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserSearchService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(userService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of(user5Full));

        SearchedUserResult res = spyUserSearchService.getRecommendedFriendsBySearch(user1Id, "person");
        
        // Verify that we get the expected users in the results
        assertEquals(2, res.getUsers().size());
        
        // Find the recommended friend and the actual friend in the results
        boolean foundRecommendedFriend = res.getUsers().stream()
                .anyMatch(u -> u.getUser().getId().equals(user3Full.getId()) && 
                              u.getRelationshipType() == UserRelationshipType.RECOMMENDED_FRIEND);
        boolean foundFriend = res.getUsers().stream()
                .anyMatch(u -> u.getUser().getId().equals(user5Full.getId()) && 
                              u.getRelationshipType() == UserRelationshipType.FRIEND);
        
        assertTrue(foundRecommendedFriend, "Should contain the recommended friend");
        assertTrue(foundFriend, "Should contain the actual friend");
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

        // Mock fuzzy search service to return user3Full as a match for "person"
        User userObject = new User();
        userObject.setId(user3Full.getId());
        userObject.setName(user3Full.getName());
        userObject.setUsername(user3Full.getUsername());
        FuzzySearchService.SearchResult<User> searchResult = new FuzzySearchService.SearchResult<>(userObject, 0.95, "username", false);
        when(fuzzySearchService.search(eq("person"), any(), any(), any())).thenReturn(List.of(searchResult));

        when(friendRequestService.getIncomingFetchFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getIncomingCreateFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(friendRequestService.getSentFriendRequestsByUserId(user1Id)).thenReturn(List.of());
        when(spyUserSearchService.getRecommendedMutuals(user1Id)).thenReturn(List.of(user2Full, user3Full, user4Full));
        when(userService.getFullFriendUsersByUserId(user1Id)).thenReturn(List.of());

        SearchedUserResult res = spyUserSearchService.getRecommendedFriendsBySearch(user1Id, "person");
        
        // Verify that we get the expected user in the results
        assertEquals(1, res.getUsers().size());
        
        // Find the recommended friend in the results
        boolean foundRecommendedFriend = res.getUsers().stream()
                .anyMatch(u -> u.getUser().getId().equals(user3Full.getId()) && 
                              u.getRelationshipType() == UserRelationshipType.RECOMMENDED_FRIEND);
        
        assertTrue(foundRecommendedFriend, "Should contain the recommended friend");
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
        assertEquals(1, result.getUsers().size());
        
        // Verify the user details and relationship type
        var searchResultUser = result.getUsers().get(0);
        assertEquals(friend.getId(), searchResultUser.getUser().getId());
        assertEquals(friend.getName(), searchResultUser.getUser().getName());
        assertEquals(UserRelationshipType.RECOMMENDED_FRIEND, searchResultUser.getRelationshipType());
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
        UUID friendRequestId = UUID.randomUUID();
        when(friendRequest.getSenderUser()).thenReturn(requesterInfo);
        when(friendRequest.getId()).thenReturn(friendRequestId);

        // Mock fuzzy search service to return requesterInfo as a match for "search"
        User userObject = new User();
        userObject.setId(requesterInfo.getId());
        userObject.setName(requesterInfo.getName());
        userObject.setUsername(requesterInfo.getUsername());
        FuzzySearchService.SearchResult<User> searchResult = new FuzzySearchService.SearchResult<>(userObject, 0.95, "name", false);
        when(fuzzySearchService.search(eq("search"), any(), any(), any())).thenReturn(List.of(searchResult));

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
        assertEquals(1, result.getUsers().size());
        
        // Verify the friend request is in the results with correct relationship type
        var searchResultUser = result.getUsers().get(0);
        assertEquals(requesterInfo.getId(), searchResultUser.getUser().getId());
        assertEquals(UserRelationshipType.INCOMING_FRIEND_REQUEST, searchResultUser.getRelationshipType());
        assertEquals(friendRequestId, searchResultUser.getFriendRequestId());
    }

    @Test
    void testSearchByQuery_PartialMatchFunctionality() {
        // Arrange
        String searchQuery = "ali";
        List<User> mockUsers = List.of(user1, user2); // user1="Alice Johnson", user2="Alicia Jameson"
        when(userRepository.findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class))).thenReturn(mockUsers);

        // Mock fuzzy search to return both users as matching results
        when(fuzzySearchService.search(eq(searchQuery), eq(mockUsers), any(), any()))
                .thenReturn(List.of(
                        new FuzzySearchService.SearchResult<>(user1, 0.85, "name", false),
                        new FuzzySearchService.SearchResult<>(user2, 0.90, "name", false)
                ));

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getName().equals("Alice Johnson")));
        assertTrue(result.stream().anyMatch(u -> u.getName().equals("Alicia Jameson")));
        
        // Verify that the partial match query was called with the correct parameters
        verify(userRepository).findUsersWithPartialMatch(eq(searchQuery.toLowerCase()), any(Limit.class));
    }

    @Test
    void testSearchByQuery_HandlesEmptyQuery() {
        // Arrange
        String searchQuery = "";

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
        
        // Verify that the repository was not called for empty query
        verify(userRepository, never()).findUsersWithPartialMatch(any(), any());
    }

    @Test
    void testSearchByQuery_HandlesBlanksAndWhitespace() {
        // Arrange
        String searchQuery = "   ";

        // Act
        List<BaseUserDTO> result = userSearchService.searchByQuery(searchQuery, UUID.randomUUID());

        // Assert
        assertTrue(result.isEmpty());
        
        // Verify that the repository was not called for blank query
        verify(userRepository, never()).findUsersWithPartialMatch(any(), any());
    }
}

