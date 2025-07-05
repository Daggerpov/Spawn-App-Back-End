package com.danielagapov.spawn.Services.UserSearch;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ParticipationStatus;
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
    private final ILogger logger;


    @Override
    public SearchedUserResult getRecommendedFriendsBySearch(UUID requestingUserId, String searchQuery) {
        try {
            List<FetchFriendRequestDTO> incomingFriendRequests = friendRequestService.getIncomingFetchFriendRequestsByUserId(requestingUserId)
                    .stream()
                    .filter(fr -> isQueryMatch(fr.getSenderUser(), searchQuery))
                    .toList();

            List<RecommendedFriendUserDTO> recommendedFriends;
            List<FullFriendUserDTO> friends;

            // If searchQuery is empty, return all recommended friends
            if (searchQuery.isEmpty()) {
                recommendedFriends = getLimitedRecommendedFriendsForUserId(requestingUserId);
                friends = userService.getFullFriendUsersByUserId(requestingUserId);
            } else {
                // First search with query
                List<User> users = searchUsersByQuery(searchQuery);
                Set<UUID> seen = users.stream().map(User::getId).collect(Collectors.toSet());
                recommendedFriends = users.stream().map(user -> FriendUserMapper.toDTO(user, 0)).collect(Collectors.toList());

                if (recommendedFriends.size() < recommendedFriendLimit) {
                    // Get recommended mutual friends
                    recommendedFriends.addAll(
                            getRecommendedMutuals(requestingUserId)
                                    .stream()
                                    .filter(entry -> isQueryMatch(entry, searchQuery) && !seen.contains(entry.getId()))
                                    .toList()
                    );
                }

                // If not enough mutual friends, supplement with random recommendations
                if (recommendedFriends.size() < recommendedFriendLimit) {
                    List<RecommendedFriendUserDTO> randomRecommendations = getRandomRecommendations(requestingUserId)
                            .stream()
                            .filter(entry -> isQueryMatch(entry, searchQuery))
                            .limit(recommendedFriendLimit - recommendedFriends.size())
                            .toList();

                    recommendedFriends.addAll(randomRecommendations);
                }

                // Get friends who match the search query
                friends = userService.getFullFriendUsersByUserId(requestingUserId)
                        .stream()
                        .filter(user -> isQueryMatch(user, searchQuery))
                        .collect(Collectors.toList());
            }

            return new SearchedUserResult(incomingFriendRequests, recommendedFriends, friends);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public List<RecommendedFriendUserDTO> getLimitedRecommendedFriendsForUserId(UUID userId) {
        try {
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

    private boolean isQueryMatch(AbstractUserDTO recommendedFriend, String searchQuery) {
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
    public List<BaseUserDTO> searchByQuery(String searchQuery) {
        List<User> users = searchUsersByQuery(searchQuery);
        // Return BaseUserDTOs
        return UserMapper.toDTOList(users);
    }

    private List<User> searchUsersByQuery(String searchQuery) {
        final int searchLimit = 100; // Max number of results returned by database query
        final int resultLimit = 10; // Max number of results to return
        // If query is empty do nothing
        if (searchQuery.isBlank()) return Collections.emptyList();

        // First get users that start with the same prefix as query
        String prefix = searchQuery.toLowerCase().substring(0, 1);
        List<User> users = userRepository.findUsersWithPrefix(prefix, Limit.of(searchLimit));

        // If no results were returned, then return early with empty list
        if (users.isEmpty()) return Collections.emptyList();

        Map<User, Double> userDistances = computeJaroWinklerDistances(searchQuery.toLowerCase(), users);
        // Filter users that are not similar to query
        List<User> filteredUsers = filterNonSimilarUsers(users, userDistances);
        if (filteredUsers.isEmpty()) {
            filteredUsers = users;
        }
        // Rank users by their similarity to query and collect top `resultLimit` results
        users = rankUsers(filteredUsers, userDistances).stream().limit(resultLimit).toList();
        return users;
    }


    /**
     * Computes the distances of each user (name or username) to the query
     *
     * @param query the search query to compare names against
     * @param users list of database results
     * @return map of users => jaro-winkler distance
     */
    private Map<User, Double> computeJaroWinklerDistances(String query, List<User> users) {
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        return users.stream().collect(Collectors.toMap(user -> user, user ->
                Math.max(
                        jaroWinklerDistance.apply(query, user.getName().toLowerCase()),
                        jaroWinklerDistance.apply(query, user.getUsername().toLowerCase())
                )
        ));
    }

    /**
     * Filter for users that are "similar" to the query, where "similar" is defined as having a
     * Jaro-Winkler distance score greater than or equal to `threshold`
     *
     * @param users list of database results
     * @return filtered list of users with Jaro-Winkler distance score >= `tolerance`
     */
    private List<User> filterNonSimilarUsers(List<User> users, Map<User, Double> userDistMap) {
        final double threshold = 0.6;
        return users.stream()
                .filter(user -> userDistMap.get(user) >= threshold)
                .collect(Collectors.toList());
    }

    /**
     * Sorts users by their Jaro-Winkler distance in ascending order.
     * Ascending order means the returned list will have the most similar results first, the least similar results last
     *
     * @param users list of database results
     * @return sorted list of users by Jaro-Winkler distance
     */
    private List<User> rankUsers(List<User> users, Map<User, Double> userDistMap) {
        return users.stream()
                .sorted(Comparator.comparingDouble(userDistMap::get).reversed())
                .collect(Collectors.toList());
    }
}
