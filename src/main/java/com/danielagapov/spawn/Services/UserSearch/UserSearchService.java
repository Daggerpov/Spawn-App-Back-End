package com.danielagapov.spawn.Services.UserSearch;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.User.AbstractUserDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.FullFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.FriendUser.RecommendedFriendUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FriendUserMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.User;
import com.danielagapov.spawn.Repositories.IUserRepository;
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
    private static final long recommendedFriendLimit = 3L;
    private final IFriendRequestService friendRequestService;
    private final IUserService userService;
    private final IUserRepository userRepository;
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
                // Get recommended mutual friends
                recommendedFriends = getRecommendedMutuals(requestingUserId)
                        .stream()
                        .filter(entry -> isQueryMatch(entry, searchQuery))
                        .collect(Collectors.toList());

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

    public List<RecommendedFriendUserDTO> getRecommendedMutuals(UUID userId) {
        // Fetch the requesting user's friends
        List<UUID> requestingUserFriendIds = userService.getFriendUserIdsByUserId(userId);

        Set<UUID> excludedUserIds = getExcludedUserIds(userId);

        // Collect friends of friends (excluding already existing friends, sent/received requests, and self)
        Map<UUID, Integer> mutualFriendCounts = getMutualFriendCounts(requestingUserFriendIds, excludedUserIds);

        // Map mutual friends to RecommendedFriendUserDTO
        return mutualFriendCounts.entrySet().stream()
                .map(entry -> {
                    UUID mutualFriendId = entry.getKey();
                    int mutualFriendCount = entry.getValue();
                    User user = userService.getUserEntityById(mutualFriendId);
                    return FriendUserMapper.toDTO(user, mutualFriendCount);
                })
                .sorted(Comparator.comparingInt(RecommendedFriendUserDTO::getMutualFriendCount).reversed())
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
                // Add to excluded list to prevent duplicates
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

    private boolean isQueryMatch(AbstractUserDTO recommendedFriend, String searchQuery) {
        final String lowercaseQuery = searchQuery.toLowerCase();
        return recommendedFriend.getFirstName().toLowerCase().contains(lowercaseQuery) ||
                recommendedFriend.getLastName().toLowerCase().contains(lowercaseQuery) ||
                recommendedFriend.getUsername().toLowerCase().contains(lowercaseQuery);
    }

    // Create a set of the requesting user's friends, users they've sent requests to, users they've received requests from, and self for quick lookup
    private Set<UUID> getExcludedUserIds(UUID userId) {
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

        return excludedUserIds;
    }

    @Override
    public List<BaseUserDTO> searchByQuery(String searchQuery) {
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
        // Return BaseUserDTOs
        return UserMapper.toDTOList(users);
    }


    /**
     * Computes the distances of each user (first name, last name, or username) to the query
     *
     * @param query the search query to compare names against
     * @param users list of database results
     * @return map of users => jaro-winkler distance
     */
    private Map<User, Double> computeJaroWinklerDistances(String query, List<User> users) {
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        return users.stream().collect(Collectors.toMap(user -> user, user ->
                Math.max(
                        jaroWinklerDistance.apply(query, user.getFirstName().toLowerCase()),
                        Math.max(
                                jaroWinklerDistance.apply(query, user.getLastName().toLowerCase()),
                                jaroWinklerDistance.apply(query, user.getUsername().toLowerCase())
                        )
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
