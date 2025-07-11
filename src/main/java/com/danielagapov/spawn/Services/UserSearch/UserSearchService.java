package com.danielagapov.spawn.Services.UserSearch;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.SearchResultUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Enums.UserRelationshipType;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendUserMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.FriendRequest.IFriendRequestService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Services.Analytics.SearchAnalyticsService;
import com.danielagapov.spawn.Services.FuzzySearch.FuzzySearchService;
import com.danielagapov.spawn.Util.SearchedUserResult;
import lombok.AllArgsConstructor;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class UserSearchService implements IUserSearchService {
    /*
     * RECOMMENDED FRIENDS ALGORITHM CONFIGURATION
     * 
     * This limit controls how many recommended friends we return in total.
     * The algorithm ensures that the top 3 are always the most recommended 
     * (based on composite scoring) so that the FriendsTabView can show the 
     * most relevant suggestions before the user clicks "show all".
     * 
     * Value breakdown:
     * - 15 total recommendations provides a good balance between variety and performance
     * - Top 3 are guaranteed to be the highest scored for immediate display
     * - Remaining 12 provide additional options when user requests to see all
     */
    private static final long recommendedFriendLimit = 15L;
    private final IFriendRequestService friendRequestService;
    private final IUserService userService;
    private final IUserRepository userRepository;
    private final IBlockedUserService blockedUserService;
    private final IActivityUserRepository activityUserRepository;
    private final FuzzySearchService<User> fuzzySearchService;
    private final SearchAnalyticsService searchAnalyticsService;
    private final ILogger logger;


    @Override
    public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
        try {
            List<SearchResultUserDTO> allUsers = new ArrayList<>();

            // Get incoming friend requests
            List<FetchFriendRequestDTO> incomingFriendRequests = friendRequestService.getIncomingFetchFriendRequestsByUserId(requestingUserId);
            
            // Filter using fuzzy search if query is provided
            if (!searchQuery.isEmpty()) {
                incomingFriendRequests = fuzzyFilterFriendRequests(incomingFriendRequests, searchQuery, true);
            }
            
            for (FetchFriendRequestDTO request : incomingFriendRequests) {
                allUsers.add(new SearchResultUserDTO(
                    request.getSenderUser(),
                    UserRelationshipType.INCOMING_FRIEND_REQUEST,
                    null,
                    request.getId()
                ));
            }

            // Get outgoing friend requests
            List<CreateFriendRequestDTO> outgoingFriendRequests = friendRequestService.getSentFriendRequestsByUserId(requestingUserId);
            
            for (CreateFriendRequestDTO request : outgoingFriendRequests) {
                User receiverUser = userService.getUserEntityById(request.getReceiverUserId());
                BaseUserDTO receiverUserDTO = new BaseUserDTO(
                    receiverUser.getId(),
                    receiverUser.getName(),
                    receiverUser.getEmail(),
                    receiverUser.getUsername(),
                    receiverUser.getBio(),
                    receiverUser.getProfilePictureUrlString()
                );
                
                // Use fuzzy search for filtering if query is provided
                if (searchQuery.isEmpty() || fuzzyMatchUser(receiverUserDTO, searchQuery)) {
                    allUsers.add(new SearchResultUserDTO(
                        receiverUserDTO,
                        UserRelationshipType.OUTGOING_FRIEND_REQUEST,
                        null,
                        request.getId()
                    ));
                }
            }

            List<RecommendedFriendUserDTO> recommendedFriends;
            List<FullFriendUserDTO> friends;

            // If searchQuery is empty, return all recommended friends and friends
            if (searchQuery.isEmpty()) {
                recommendedFriends = userService.getLimitedRecommendedFriendsForUserId(requestingUserId);
                friends = userService.getFullFriendUsersByUserId(requestingUserId);
            } else {
                // First search with fuzzy search query
                List<User> users = searchUsersByQuery(searchQuery);
                Set<UUID> seen = users.stream().map(User::getId).collect(Collectors.toSet());
                recommendedFriends = users.stream().map(user -> FriendUserMapper.toDTO(user, 0)).collect(Collectors.toList());

                if (recommendedFriends.size() < recommendedFriendLimit) {
                    // Get recommended mutual friends and filter with fuzzy search
                    List<RecommendedFriendUserDTO> mutualFriends = getRecommendedMutuals(requestingUserId);
                    List<RecommendedFriendUserDTO> filteredMutuals = fuzzyFilterRecommendedFriends(mutualFriends, searchQuery)
                            .stream()
                            .filter(entry -> !seen.contains(entry.getId()))
                            .collect(Collectors.toList());
                    
                    recommendedFriends.addAll(filteredMutuals);
                }

                // If not enough mutual friends, supplement with random recommendations using fuzzy search
                if (recommendedFriends.size() < recommendedFriendLimit) {
                    List<RecommendedFriendUserDTO> randomRecommendations = getRandomRecommendations(requestingUserId);
                    List<RecommendedFriendUserDTO> filteredRandom = fuzzyFilterRecommendedFriends(randomRecommendations, searchQuery)
                            .stream()
                            .limit(recommendedFriendLimit - recommendedFriends.size())
                            .collect(Collectors.toList());

                    recommendedFriends.addAll(filteredRandom);
                }

                // Get friends who match the search query using fuzzy search
                List<FullFriendUserDTO> allFriends = userService.getFullFriendUsersByUserId(requestingUserId);
                friends = fuzzyFilterFriends(allFriends, searchQuery);
            }

            // Add recommended friends to the unified list
            for (RecommendedFriendUserDTO recommended : recommendedFriends) {
                BaseUserDTO baseUser = new BaseUserDTO(
                    recommended.getId(),
                    recommended.getName(),
                    recommended.getEmail(),
                    recommended.getUsername(),
                    recommended.getBio(),
                    recommended.getProfilePicture()
                );
                allUsers.add(new SearchResultUserDTO(
                    baseUser,
                    UserRelationshipType.RECOMMENDED_FRIEND,
                    recommended.getMutualFriendCount(),
                    null
                ));
            }

            // Add friends to the unified list
            for (FullFriendUserDTO friend : friends) {
                BaseUserDTO baseUser = new BaseUserDTO(
                    friend.getId(),
                    friend.getName(),
                    friend.getEmail(),
                    friend.getUsername(),
                    friend.getBio(),
                    friend.getProfilePicture()
                );
                allUsers.add(new SearchResultUserDTO(
                    baseUser,
                    UserRelationshipType.FRIEND,
                    null,
                    null
                ));
            }

            return new SearchedUserResult(allUsers);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
        try {
            // This method is cached at the UserService level via @Cacheable("recommendedFriends")
            // Cache is automatically invalidated when friend relationships change
            // First get mutuals-based recommendations
            List<RecommendedFriendUserDTO> recommendedFriends = getRecommendedMutuals(userId);

            // If we already have enough mutual-based recommendations, limit and return them
            if (recommendedFriends.size() >= recommendedFriendLimit) {
                return recommendedFriends.stream()
                        .limit(recommendedFriendLimit)
                        .collect(Collectors.toList());
            }

            // Otherwise, supplement with random recommendations
            List<RecommendedFriendUserDTO> randomRecommendations = getRandomRecommendations(userId);

            // Add random recommendations, avoiding duplicates
            Set<UUID> existingIds = recommendedFriends.stream()
                    .map(RecommendedFriendUserDTO::getId)
                    .collect(Collectors.toSet());

            for (RecommendedFriendUserDTO randomFriend : randomRecommendations) {
                // Skip if we've reached the limit
                if (recommendedFriends.size() >= recommendedFriendLimit) {
                    break;
                }

                // Skip if this user is already in our recommendations
                if (!existingIds.contains(randomFriend.getId())) {
                    recommendedFriends.add(randomFriend);
                    existingIds.add(randomFriend.getId());
                }
            }

            return recommendedFriends;
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /*
     * ENHANCED RECOMMENDATION ALGORITHM
     * 
     * This method implements a sophisticated friend recommendation system that considers 
     * multiple factors to provide the most relevant suggestions. The algorithm combines
     * mutual friends analysis with shared activity participation to create a comprehensive
     * recommendation score.
     * 
     * SCORING METHODOLOGY:
     * 
     * 1. MUTUAL FRIENDS FACTOR (Weight: 3x)
     *    - Calculates how many friends the requesting user shares with potential recommendations
     *    - Higher mutual friend count indicates existing social connections
     *    - Weighted more heavily as it's a strong indicator of social compatibility
     * 
     * 2. SHARED ACTIVITIES FACTOR (Weight: 2x)
     *    - Calculates how many activities the requesting user has participated in with potential friends
     *    - Users who have "spawned" together in activities likely share interests and compatibility
     *    - Weighted significantly as it represents actual interaction history
     * 
     * 3. COMPOSITE SCORE FORMULA:
     *    Final Score = (mutualFriends * 3) + (sharedActivities * 2)
     * 
     * RATIONALE FOR WEIGHTS:
     * - Mutual friends (3x): Strong social proof, indicates existing network connections
     * - Shared activities (2x): Demonstrates actual interaction and shared interests
     * - This weighting ensures that social connections are prioritized while still valuing
     *   direct interaction history
     * 
     * SORTING BEHAVIOR:
     * - Results are sorted by composite score in descending order
     * - This ensures the first 3 results are always the most recommended
     * - Supports the FriendsTabView requirement to show top recommendations before "show all"
     * 
     * @param userId The ID of the user requesting friend recommendations
     * @return List of recommended friends sorted by composite score (highest first)
     */
    public List<RecommendedFriendUserDTO> getRecommendedMutuals(UUID userId) {
        // Fetch the requesting user's friends
        List<UUID> requestingUserFriendIds = userService.getFriendUserIdsByUserId(userId);

        Set<UUID> excludedUserIds = getExcludedUserIds(userId);

        // Collect friends of friends (excluding already existing friends, sent/received requests, and self)
        Map<UUID, Integer> mutualFriendCounts = getMutualFriendCounts(requestingUserFriendIds, excludedUserIds);

        // Map mutual friends to RecommendedFriendUserDTO with enhanced scoring
        return mutualFriendCounts.entrySet().stream()
                .map(entry -> {
                    UUID potentialFriendId = entry.getKey();
                    int mutualFriendCount = entry.getValue();
                    
                    // Calculate shared activities count for this potential friend
                    int sharedActivitiesCount = getSharedActivitiesCount(userId, potentialFriendId);
                    
                    User user = userService.getUserEntityById(potentialFriendId);
                    return FriendUserMapper.toDTO(user, mutualFriendCount, sharedActivitiesCount);
                })
                .sorted((friend1, friend2) -> {
                    // Calculate composite scores for sorting
                    // Formula: (mutualFriends * 3) + (sharedActivities * 2)
                    int score1 = (friend1.getMutualFriendCount() * 3) + (friend1.getSharedActivitiesCount() * 2);
                    int score2 = (friend2.getMutualFriendCount() * 3) + (friend2.getSharedActivitiesCount() * 2);
                    
                    // Sort in descending order (highest scores first)
                    return Integer.compare(score2, score1);
                })
                .collect(Collectors.toList());
    }

    private List<RecommendedFriendUserDTO> getRandomRecommendations(UUID userId) {
        List<RecommendedFriendUserDTO> recommendedFriends = new ArrayList<>();
        List<UserDTO> allUsers = userService.getAllUsers();
        Set<UUID> excludedUserIds = getExcludedUserIds(userId);

        for (UserDTO potentialFriend : allUsers) {
            if (recommendedFriends.size() >= recommendedFriendLimit) break;
            UUID potentialFriendId = potentialFriend.getId();

            // Check if the potential friend is already excluded
            if (!excludedUserIds.contains(potentialFriendId)) {
                recommendedFriends.add(FriendUserMapper.toDTO(userService.getUserEntityById(potentialFriendId), 0));
                // Add to excluded list to prActivity duplicates
                excludedUserIds.add(potentialFriendId);
            }
        }
        return recommendedFriends;
    }

    private Map<UUID, Integer> getMutualFriendCounts(List<UUID> requestingUserFriendIds, Set<UUID> excludedUserIds) {
        Map<UUID, Integer> mutualFriendCounts = new HashMap<>();
        for (UUID friendId : requestingUserFriendIds) {
            List<UUID> friendOfFriendIds = userService.getFriendUserIdsByUserId(friendId);

            for (UUID friendOfFriendId : friendOfFriendIds) {
                if (!excludedUserIds.contains(friendOfFriendId)) {
                    mutualFriendCounts.merge(friendOfFriendId, 1, Integer::sum);
                }
            }
        }
        return mutualFriendCounts;
    }

    /*
     * SHARED ACTIVITIES CALCULATION METHOD
     * 
     * This method calculates how many activities two users have participated in together.
     * It's used as a key metric in the recommendation algorithm because users who have
     * "spawned" (participated in activities) together are likely to be good friend recommendations.
     * 
     * Algorithm:
     * 1. Get all activities where the requesting user has participated (status = PARTICIPATING)
     * 2. For each activity, check if the potential friend also participated
     * 3. Count the number of shared activities
     * 
     * Why this matters:
     * - Users who frequently attend activities together likely have shared interests
     * - Past participation indicates compatibility and mutual enjoyment
     * - This metric complements mutual friends to provide more accurate recommendations
     * 
     * @param requestingUserId The ID of the user requesting recommendations
     * @param potentialFriendId The ID of the potential friend to check shared activities with
     * @return The number of activities both users have participated in together
     */
    private int getSharedActivitiesCount(UUID requestingUserId, UUID potentialFriendId) {
        try {
            // Get all activities where the requesting user has participated
            List<ActivityUser> requestingUserActivities = activityUserRepository
                    .findByUser_IdAndStatus(requestingUserId, ParticipationStatus.participating);
            
            // If the requesting user hasn't participated in any activities, return 0
            if (requestingUserActivities.isEmpty()) {
                return 0;
            }
            
            // Extract activity IDs from the requesting user's participated activities
            Set<UUID> requestingUserActivityIds = requestingUserActivities.stream()
                    .map(au -> au.getActivity().getId())
                    .collect(Collectors.toSet());
            
            // Get all activities where the potential friend has participated
            List<ActivityUser> potentialFriendActivities = activityUserRepository
                    .findByUser_IdAndStatus(potentialFriendId, ParticipationStatus.participating);
            
            // Count how many activities overlap between the two users
            long sharedActivitiesCount = potentialFriendActivities.stream()
                    .map(au -> au.getActivity().getId())
                    .filter(requestingUserActivityIds::contains)
                    .count();
            
            return (int) sharedActivitiesCount;
        } catch (Exception e) {
            logger.error("Error calculating shared activities between users " + requestingUserId + " and " + potentialFriendId + ": " + e.getMessage());
            return 0;
        }
    }

    /**
     * Helper method to filter a list of FullFriendUserDTO objects using fuzzy search
     */
    private List<FullFriendUserDTO> fuzzyFilterFriends(List<FullFriendUserDTO> friends, String searchQuery) {
        if (searchQuery.isEmpty()) return friends;
        
        // Convert to User objects for fuzzy search
        List<User> userObjects = friends.stream()
                .map(friend -> {
                    User user = new User();
                    user.setId(friend.getId());
                    user.setName(friend.getName());
                    user.setUsername(friend.getUsername());
                    return user;
                })
                .collect(Collectors.toList());
        
        // Use fuzzy search service
        List<FuzzySearchService.SearchResult<User>> searchResults = fuzzySearchService.search(
                searchQuery,
                userObjects,
                User::getName,
                User::getUsername
        );
        
        // Convert back to FullFriendUserDTO, maintaining the original objects
        Set<UUID> matchedUserIds = searchResults.stream()
                .map(result -> result.getItem().getId())
                .collect(Collectors.toSet());
        
        return friends.stream()
                .filter(friend -> matchedUserIds.contains(friend.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Helper method to filter a list of RecommendedFriendUserDTO objects using fuzzy search
     */
    private List<RecommendedFriendUserDTO> fuzzyFilterRecommendedFriends(List<RecommendedFriendUserDTO> recommendedFriends, String searchQuery) {
        if (searchQuery.isEmpty()) return recommendedFriends;
        
        // Convert to User objects for fuzzy search
        List<User> userObjects = recommendedFriends.stream()
                .map(recommended -> {
                    User user = new User();
                    user.setId(recommended.getId());
                    user.setName(recommended.getName());
                    user.setUsername(recommended.getUsername());
                    return user;
                })
                .collect(Collectors.toList());
        
        // Use fuzzy search service
        List<FuzzySearchService.SearchResult<User>> searchResults = fuzzySearchService.search(
                searchQuery,
                userObjects,
                User::getName,
                User::getUsername
        );
        
        // Convert back to RecommendedFriendUserDTO, maintaining the original objects
        Set<UUID> matchedUserIds = searchResults.stream()
                .map(result -> result.getItem().getId())
                .collect(Collectors.toSet());
        
        return recommendedFriends.stream()
                .filter(recommended -> matchedUserIds.contains(recommended.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Helper method to filter a list of FetchFriendRequestDTO objects using fuzzy search
     */
    private List<FetchFriendRequestDTO> fuzzyFilterFriendRequests(List<FetchFriendRequestDTO> friendRequests, String searchQuery, boolean isSenderUser) {
        if (searchQuery.isEmpty()) return friendRequests;
        
        // Convert to User objects for fuzzy search
        List<User> userObjects = friendRequests.stream()
                .map(request -> {
                    BaseUserDTO userDTO = request.getSenderUser();
                    User user = new User();
                    user.setId(userDTO.getId());
                    user.setName(userDTO.getName());
                    user.setUsername(userDTO.getUsername());
                    return user;
                })
                .collect(Collectors.toList());
        
        // Use fuzzy search service
        List<FuzzySearchService.SearchResult<User>> searchResults = fuzzySearchService.search(
                searchQuery,
                userObjects,
                User::getName,
                User::getUsername
        );
        
        // Convert back to FetchFriendRequestDTO, maintaining the original objects
        Set<UUID> matchedUserIds = searchResults.stream()
                .map(result -> result.getItem().getId())
                .collect(Collectors.toSet());
        
        return friendRequests.stream()
                .filter(request -> matchedUserIds.contains(request.getSenderUser().getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Helper method to check if a single user matches the search query using fuzzy search
     */
    private boolean fuzzyMatchUser(BaseUserDTO user, String searchQuery) {
        if (searchQuery.isEmpty()) return true;
        
        // Convert to User object for fuzzy search
        User userObject = new User();
        userObject.setId(user.getId());
        userObject.setName(user.getName());
        userObject.setUsername(user.getUsername());
        
        // Use fuzzy search service
        List<FuzzySearchService.SearchResult<User>> searchResults = fuzzySearchService.search(
                searchQuery,
                List.of(userObject),
                User::getName,
                User::getUsername
        );
        
        return !searchResults.isEmpty();
    }

    private boolean isQueryMatch(AbstractUserDTO recommendedFriend, String searchQuery) {
        // This method is now deprecated in favor of fuzzy search
        // Keeping it for backward compatibility, but it should not be used for new implementations
        final String lowercaseQuery = searchQuery.toLowerCase();
        boolean nameMatch = false;
        if (recommendedFriend.getName() != null) {
            String[] nameParts = recommendedFriend.getName().toLowerCase().split(" ");
            for (String part : nameParts) {
                if (part.contains(lowercaseQuery)) {
                    nameMatch = true;
                    break;
                }
            }
        }
        boolean usernameMatch = recommendedFriend.getUsername() != null && recommendedFriend.getUsername().toLowerCase().contains(lowercaseQuery);
        return nameMatch || usernameMatch;
    }

    // Create a set of the requesting user's friends, users they've sent requests to, users they've received requests from, and self for quick lookup
    public Set<UUID> getExcludedUserIds(UUID userId) {
        // Fetch the requesting user's friends
        List<UUID> requestingUserFriendIds = userService.getFriendUserIdsByUserId(userId);

        // Fetch users who have already received a friend request from the user
        List<UUID> sentFriendRequestReceiverUserIds = friendRequestService.getSentFriendRequestsByUserId(userId)
                .stream()
                .map(CreateFriendRequestDTO::getReceiverUserId)
                .toList();

        // Map mutual friends to RecommendedFriendUserDTO
        List<UUID> receivedFriendRequestSenderUserIds = friendRequestService.getIncomingCreateFriendRequestsByUserId(userId)
                .stream()
                .map(CreateFriendRequestDTO::getSenderUserId)
                .toList();

        // Create a set of the requesting user's friends, users they've sent requests to, users they've received requests from, and self for quick lookup
        Set<UUID> excludedUserIds = new HashSet<>(requestingUserFriendIds);
        excludedUserIds.addAll(sentFriendRequestReceiverUserIds);
        excludedUserIds.addAll(receivedFriendRequestSenderUserIds);
        excludedUserIds.add(userId); // Exclude self
        excludedUserIds.addAll(blockedUserService.getBlockedUserIds(userId));

        return excludedUserIds;
    }

    @Override
    public List<BaseUserDTO> searchByQuery(String searchQuery, UUID requestingUserId) {
        List<User> users = searchUsersByQuery(searchQuery);
        
        // Filter out the requesting user and other excluded users if requestingUserId is provided
        if (requestingUserId != null) {
            Set<UUID> excludedUserIds = getExcludedUserIds(requestingUserId);
            users = users.stream()
                    .filter(user -> !excludedUserIds.contains(user.getId()))
                    .collect(Collectors.toList());
        }
        
        // Return BaseUserDTOs
        return UserMapper.toDTOList(users);
    }

    private List<User> searchUsersByQuery(String searchQuery) {
        // If query is empty do nothing
        if (searchQuery.isBlank()) return Collections.emptyList();

        long startTime = System.currentTimeMillis();

        // First get users that start with the same prefix as query (database optimization)
        String prefix = searchQuery.toLowerCase().substring(0, 1);
        List<User> users = userRepository.findUsersWithPrefix(prefix, Limit.of(100));

        // If no results were returned, then return early with empty list
        if (users.isEmpty()) return Collections.emptyList();

        // Use the enhanced fuzzy search service
        List<FuzzySearchService.SearchResult<User>> searchResults = fuzzySearchService.search(
                searchQuery,
                users,
                User::getName,        // Name extractor
                User::getUsername     // Username extractor
        );

        // Record analytics data
        long processingTime = System.currentTimeMillis() - startTime;
        searchAnalyticsService.recordSearchResults(searchQuery, processingTime, users.size(), searchResults);

        // Extract just the User objects from the search results
        return searchResults.stream()
                .map(FuzzySearchService.SearchResult::getItem)
                .collect(Collectors.toList());
    }


    /**
     * Legacy method for backward compatibility - now uses enhanced FuzzySearchService
     * @deprecated Use FuzzySearchService directly for better performance and configurability
     */
    @Deprecated
    private Map<User, Double> computeJaroWinklerDistances(String query, List<User> users) {
        // This method is kept for backward compatibility but is no longer used
        // All fuzzy search functionality has been moved to FuzzySearchService
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        return users.stream().collect(Collectors.toMap(user -> user, user ->
                Math.max(
                        jaroWinklerDistance.apply(query, user.getName().toLowerCase()),
                        jaroWinklerDistance.apply(query, user.getUsername().toLowerCase())
                )
        ));
    }
}
