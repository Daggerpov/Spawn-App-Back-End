package com.danielagapov.spawn.Services.FriendRequest;

import com.danielagapov.spawn.DTOs.FriendRequest.CreateFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchFriendRequestDTO;
import com.danielagapov.spawn.DTOs.FriendRequest.FetchSentFriendRequestDTO;
import com.danielagapov.spawn.Events.FriendRequestAcceptedNotificationEvent;
import com.danielagapov.spawn.Events.FriendRequestNotificationEvent;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.FetchFriendRequestMapper;
import com.danielagapov.spawn.Mappers.FriendRequestMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.FriendRequest;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IFriendRequestsRepository;
import com.danielagapov.spawn.Services.BlockedUser.IBlockedUserService;
import com.danielagapov.spawn.Services.User.IUserService;
import com.danielagapov.spawn.Util.LoggingUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public final class FriendRequestService implements IFriendRequestService {
    private final IFriendRequestsRepository repository;
    private final IUserService userService;
    private final IBlockedUserService blockedUserService;
    private final ILogger logger;
    private final ApplicationEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    public FriendRequestService(
            IFriendRequestsRepository repository,
            IUserService userService,
            IBlockedUserService blockedUserService, ILogger logger,
            ApplicationEventPublisher eventPublisher, CacheManager cacheManager) {
        this.repository = repository;
        this.userService = userService;
        this.blockedUserService = blockedUserService;
        this.logger = logger;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "incomingFetchFriendRequests", key = "#friendRequestDTO.receiverUserId"),
            @CacheEvict(value = "sentFetchFriendRequests", key = "#friendRequestDTO.senderUserId")
    })
    public CreateFriendRequestDTO saveFriendRequest(CreateFriendRequestDTO friendRequestDTO) {
        try {
            // Extract sender and receiver IDs from the FriendRequestDTO
            UUID senderId = friendRequestDTO.getSenderUserId();
            UUID receiverId = friendRequestDTO.getReceiverUserId();

            User sender = userService.getUserEntityById(senderId);
            User receiver = userService.getUserEntityById(receiverId);

            logger.info("Creating friend request from sender: " + LoggingUtils.formatUserInfo(sender) +
                    " to receiver: " + LoggingUtils.formatUserInfo(receiver));

            // Check if a friend request already exists between these users
            List<FriendRequest> existingRequests = repository.findBySenderIdAndReceiverId(senderId, receiverId);
            if (!existingRequests.isEmpty()) {
                FriendRequest existingRequest = existingRequests.get(0);
                logger.info("Friend request already exists between sender: " + LoggingUtils.formatUserInfo(sender) +
                        " and receiver: " + LoggingUtils.formatUserInfo(receiver) + ". Returning existing request.");
                return FriendRequestMapper.toDTO(existingRequest);
            }

            // Check if a friend request already exists in the reverse direction (receiver -> sender)
            List<FriendRequest> reverseRequests = repository.findBySenderIdAndReceiverId(receiverId, senderId);
            if (!reverseRequests.isEmpty()) {
                FriendRequest reverseRequest = reverseRequests.get(0);
                logger.info("Friend request already exists in reverse direction from receiver: " + LoggingUtils.formatUserInfo(receiver) +
                        " to sender: " + LoggingUtils.formatUserInfo(sender) + ". Auto-accepting the existing request.");
                
                // Auto-accept the existing friend request (this will create the friendship and send notification to the original sender)
                acceptFriendRequest(reverseRequest.getId());
                
                // Send an additional notification to the current sender (who tried to send the request)
                // to let them know their attempted request resulted in becoming friends
                eventPublisher.publishEvent(
                    new FriendRequestAcceptedNotificationEvent(sender, receiverId)
                );
                
                // Evict friend request caches for both users since mutual requests were resolved
                if (cacheManager.getCache("incomingFetchFriendRequests") != null) {
                    cacheManager.getCache("incomingFetchFriendRequests").evict(senderId);
                }
                if (cacheManager.getCache("sentFetchFriendRequests") != null) {
                    cacheManager.getCache("sentFetchFriendRequests").evict(senderId);
                }
                
                // Return the DTO for the reverse request that was accepted
                return FriendRequestMapper.toDTO(reverseRequest);
            }

            // Map the DTO to entity
            FriendRequest friendRequest = FriendRequestMapper.toEntity(friendRequestDTO, sender, receiver);

            // Save the friend request
            repository.save(friendRequest);
            logger.info("Friend request saved successfully");

            // Evict recommended friends cache for both users since their recommendation status has changed
            if (cacheManager.getCache("recommendedFriends") != null) {
                cacheManager.getCache("recommendedFriends").evict(senderId);
                cacheManager.getCache("recommendedFriends").evict(receiverId);
            }

            // Publish friend request notification Activity
            eventPublisher.publishEvent(new FriendRequestNotificationEvent(sender, receiverId));

            // Return the saved friend request DTO
            return FriendRequestMapper.toDTO(friendRequest);
        } catch (DataAccessException e) {
            logger.error("Failed to save friend request from user " + LoggingUtils.formatUserIdInfo(friendRequestDTO.getSenderUserId()) +
                    " to user " + LoggingUtils.formatUserIdInfo(friendRequestDTO.getReceiverUserId()) + ": " + e.getMessage());
            throw new BaseSaveException("Failed to save friend request: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating friend request from user " + LoggingUtils.formatUserIdInfo(friendRequestDTO.getSenderUserId()) +
                    " to user " + LoggingUtils.formatUserIdInfo(friendRequestDTO.getReceiverUserId()) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "incomingFetchFriendRequests", key = "#id")
    public List<FetchFriendRequestDTO> getIncomingFetchFriendRequestsByUserId(UUID id) {
        try {
            User user = userService.getUserEntityById(id);
            logger.info("Getting incoming fetch friend requests for user: " + LoggingUtils.formatUserInfo(user));

            List<FriendRequest> friendRequests = getIncomingFriendRequestsByUserId(id);
            
            // Debug logging for friend request IDs
            logger.info("Debug: Raw friend requests for user " + LoggingUtils.formatUserInfo(user) + ":");
            for (FriendRequest fr : friendRequests) {
                logger.info("  - Friend request: ID=" + fr.getId() + ", Sender=" + LoggingUtils.formatUserInfo(fr.getSender()));
            }
            
            // Filter out any friend requests with null IDs to prevent JSON decoding errors
            List<FriendRequest> validFriendRequests = friendRequests.stream()
                    .filter(fr -> {
                        if (fr.getId() == null) {
                            logger.error("Critical: Friend request with null ID found for user: " + LoggingUtils.formatUserInfo(user) + 
                                    ". Sender: " + (fr.getSender() != null ? LoggingUtils.formatUserInfo(fr.getSender()) : "null") +
                                    ", Receiver: " + (fr.getReceiver() != null ? LoggingUtils.formatUserInfo(fr.getReceiver()) : "null") +
                                    ". This indicates a data integrity issue that should be investigated.");
                            return false;
                        }
                        return true;
                    })
                    .toList();
            
            // Log if any invalid friend requests were found
            int invalidCount = friendRequests.size() - validFriendRequests.size();
            if (invalidCount > 0) {
                logger.error("CRITICAL DATA INTEGRITY ISSUE: Found " + invalidCount + " friend requests with null IDs for user: " + 
                        LoggingUtils.formatUserInfo(user) + ". These will be excluded from the response. " +
                        "Database cleanup migration V13__Clean_Null_ID_Friend_Requests.sql should be run immediately.");
            }
            
            // Note: Blocked user filtering is now handled at the controller level

            List<FetchFriendRequestDTO> result = validFriendRequests.stream()
                    .map(fr -> {
                        FetchFriendRequestDTO dto = FetchFriendRequestMapper.toDTO(fr,
                        userService.getMutualFriendCount(id, fr.getSender().getId()));
                        logger.info("Debug: Created DTO with ID=" + dto.getId() + " from friend request ID=" + fr.getId() + 
                                  ". DTO senderUser ID=" + (dto.getSenderUser() != null ? dto.getSenderUser().getId() : "null"));
                        
                        // Additional validation to ensure we're returning the correct DTO structure
                        if (dto.getSenderUser() == null) {
                            logger.error("CRITICAL: FetchFriendRequestDTO has null senderUser for friend request ID=" + fr.getId() + 
                                       ". This will cause JSON decoding errors on the client.");
                        }
                        
                        return dto;
                    })
                    .toList();

            logger.info("Found " + result.size() + " incoming fetch friend requests for user: " + LoggingUtils.formatUserInfo(user));
            return result;
        } catch (Exception e) {
            logger.error("Error retrieving incoming fetch friend requests for user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<CreateFriendRequestDTO> getIncomingCreateFriendRequestsByUserId(UUID id) {
        try {
            List<FriendRequest> friendRequests = getIncomingFriendRequestsByUserId(id);
            List<CreateFriendRequestDTO> result = FriendRequestMapper.toDTOList(friendRequests);

            return result;
        } catch (Exception e) {
            logger.error("Error retrieving incoming create friend requests for user: " + LoggingUtils.formatUserIdInfo(id) + ": " + e.getMessage());
            throw e;
        }
    }

    public List<FriendRequest> getIncomingFriendRequestsByUserId(UUID id) {
        try {
            List<FriendRequest> requests = repository.findByReceiverId(id);

            return requests;
        } catch (DataAccessException e) {
            logger.error("Database access error while retrieving incoming friend requests for user: " + LoggingUtils.formatUserIdInfo(id));
            throw e; // Only throw for actual database access issues
        } catch (Exception e) {
            logger.error("Error retrieving incoming friend requests for user: " + LoggingUtils.formatUserIdInfo(id));
            throw e;
        }
    }

    @Override
    public void acceptFriendRequest(UUID id) {
        try {
            // Check if friend request exists first
            FriendRequest fr = repository.findById(id).orElse(null);
            if (fr == null) {
                logger.info("Friend request with ID: " + id + " not found - may have been already processed or auto-accepted");
                return; // Gracefully handle already processed requests
            }
            
            User sender = fr.getSender();
            User receiver = fr.getReceiver();

            // Check if users are already friends to avoid duplicate processing
            if (userService.isUserFriendOfUser(sender.getId(), receiver.getId())) {
                logger.info("Users are already friends, deleting stale friend request with ID: " + id);
                deleteFriendRequest(id);
                return;
            }

            logger.info("Accepting friend request with ID: " + id + " from sender: " +
                    LoggingUtils.formatUserInfo(sender) + " to receiver: " + LoggingUtils.formatUserInfo(receiver));

            userService.saveFriendToUser(sender.getId(), receiver.getId());

            // Publish friend request accepted notification Activity
            eventPublisher.publishEvent(
                    new FriendRequestAcceptedNotificationEvent(receiver, sender.getId())
            );

            deleteFriendRequest(id);

            // Evict friend request caches for both users
            if (cacheManager.getCache("incomingFetchFriendRequests") != null) {
                cacheManager.getCache("incomingFetchFriendRequests").evict(receiver.getId());
            }
            if (cacheManager.getCache("sentFetchFriendRequests") != null) {
                cacheManager.getCache("sentFetchFriendRequests").evict(sender.getId());
            }


            if (cacheManager.getCache("friendsByUserId") != null) {
                cacheManager.getCache("friendsByUserId").evict(sender.getId());
                cacheManager.getCache("friendsByUserId").evict(receiver.getId());
            }

            // Evict recommended friends cache for both users since they're now friends
            if (cacheManager.getCache("recommendedFriends") != null) {
                cacheManager.getCache("recommendedFriends").evict(sender.getId());
                cacheManager.getCache("recommendedFriends").evict(receiver.getId());
            }

            logger.info("Friend request accepted and deleted successfully");
        } catch (Exception e) {
            logger.warn("Error accepting friend request with ID: " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteFriendRequest(UUID id) {
        try {
            FriendRequest fr = repository.findById(id).orElse(null);
            if (fr != null) {
                User sender = fr.getSender();
                User receiver = fr.getReceiver();
                logger.info("Deleting friend request with ID: " + id + " from sender: " +
                        LoggingUtils.formatUserInfo(sender) + " to receiver: " + LoggingUtils.formatUserInfo(receiver));
                
                // Evict friend request caches for both users
                if (cacheManager.getCache("incomingFetchFriendRequests") != null) {
                    cacheManager.getCache("incomingFetchFriendRequests").evict(receiver.getId());
                }
                if (cacheManager.getCache("sentFetchFriendRequests") != null) {
                    cacheManager.getCache("sentFetchFriendRequests").evict(sender.getId());
                }
                
                // Evict recommended friends cache for both users since their recommendation status has changed
                if (cacheManager.getCache("recommendedFriends") != null) {
                    cacheManager.getCache("recommendedFriends").evict(sender.getId());
                    cacheManager.getCache("recommendedFriends").evict(receiver.getId());
                }
                
                repository.deleteById(id);
                logger.info("Friend request deleted successfully");
            } else {
                logger.info("Friend request with ID: " + id + " was already deleted or does not exist");
            }
        } catch (DataAccessException e) {
            logger.error("Error deleting friend request with ID: " + id + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error deleting friend request with ID: " + id + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void deleteFriendRequestBetweenUsersIfExists(UUID senderId, UUID receiverId) {
        try {
            User sender = userService.getUserEntityById(senderId);
            User receiver = userService.getUserEntityById(receiverId);

            logger.info("Deleting friend requests between sender: " + LoggingUtils.formatUserInfo(sender) +
                    " and receiver: " + LoggingUtils.formatUserInfo(receiver));

            List<FriendRequest> requests = repository.findBySenderIdAndReceiverId(senderId, receiverId);
            for (FriendRequest fr : requests) {
                repository.delete(fr);
            }

            // Evict recommended friends cache for both users since their recommendation status has changed
            if (cacheManager.getCache("recommendedFriends") != null) {
                cacheManager.getCache("recommendedFriends").evict(senderId);
                cacheManager.getCache("recommendedFriends").evict(receiverId);
            }

            logger.info("Deleted " + requests.size() + " friend requests between users");
        } catch (Exception e) {
            logger.error("Error deleting friend request from user " + LoggingUtils.formatUserIdInfo(senderId) +
                    " to user " + LoggingUtils.formatUserIdInfo(receiverId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FetchFriendRequestDTO> convertFriendRequestsToFetchFriendRequests(List<CreateFriendRequestDTO> friendRequests) {
        try {
            logger.info("Converting " + friendRequests.size() + " friend requests to fetch friend requests");

            List<FetchFriendRequestDTO> fullFriendRequests = new ArrayList<>();
            for (CreateFriendRequestDTO friendRequest : friendRequests) {
                UUID senderId = friendRequest.getSenderUserId();
                UUID receiverId = friendRequest.getReceiverUserId();
                int mutualFriendCount = userService.getMutualFriendCount(receiverId, senderId);

                User sender = userService.getUserEntityById(senderId);
                fullFriendRequests.add(new FetchFriendRequestDTO(
                        friendRequest.getId(),
                        UserMapper.toDTO(sender),
                        mutualFriendCount
                ));
            }

            logger.info("Converted " + fullFriendRequests.size() + " friend requests successfully");
            return fullFriendRequests;
        } catch (Exception e) {
            logger.error("Error converting friend requests to fetch friend requests: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<CreateFriendRequestDTO> getSentFriendRequestsByUserId(UUID userId) {
        try {
            User user = userService.getUserEntityById(userId);

            List<FriendRequest> friendRequests = repository.findBySenderId(userId);
            List<CreateFriendRequestDTO> dtos = FriendRequestMapper.toDTOList(friendRequests);

            return dtos;
        } catch (Exception e) {
            logger.error("Error retrieving sent friend requests for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "sentFetchFriendRequests", key = "#userId")
    public List<FetchSentFriendRequestDTO> getSentFetchFriendRequestsByUserId(UUID userId) {
        try {
            User user = userService.getUserEntityById(userId);
            logger.info("Getting sent fetch friend requests for user: " + LoggingUtils.formatUserInfo(user));

            List<FriendRequest> friendRequests = repository.findBySenderId(userId);
            
            // Debug logging for friend request IDs
            logger.info("Debug: Raw sent friend requests for user " + LoggingUtils.formatUserInfo(user) + ":");
            for (FriendRequest fr : friendRequests) {
                logger.info("  - Friend request: ID=" + fr.getId() + ", Receiver=" + LoggingUtils.formatUserInfo(fr.getReceiver()));
            }
            
            // Filter out any friend requests with null IDs to prevent JSON decoding errors
            List<FriendRequest> validFriendRequests = friendRequests.stream()
                    .filter(fr -> {
                        if (fr.getId() == null) {
                            logger.error("Critical: Friend request with null ID found for user: " + LoggingUtils.formatUserInfo(user) + 
                                    ". Sender: " + (fr.getSender() != null ? LoggingUtils.formatUserInfo(fr.getSender()) : "null") +
                                    ", Receiver: " + (fr.getReceiver() != null ? LoggingUtils.formatUserInfo(fr.getReceiver()) : "null") +
                                    ". This indicates a data integrity issue that should be investigated.");
                            return false;
                        }
                        return true;
                    })
                    .toList();
            
            // Log if any invalid friend requests were found
            int invalidCount = friendRequests.size() - validFriendRequests.size();
            if (invalidCount > 0) {
                logger.error("CRITICAL DATA INTEGRITY ISSUE: Found " + invalidCount + " friend requests with null IDs for user: " + 
                        LoggingUtils.formatUserInfo(user) + ". These will be excluded from the response. " +
                        "Database cleanup migration V13__Clean_Null_ID_Friend_Requests.sql should be run immediately.");
            }
            
            // Note: Blocked user filtering is now handled at the controller level

            List<FetchSentFriendRequestDTO> result = validFriendRequests.stream()
                    .map(fr -> {
                        FetchSentFriendRequestDTO dto = FetchFriendRequestMapper.toSentDTO(fr);
                        logger.info("Debug: Created sent DTO with ID=" + dto.getId() + " from friend request ID=" + fr.getId());
                        return dto;
                    })
                    .toList();

            logger.info("Found " + result.size() + " sent fetch friend requests for user: " + LoggingUtils.formatUserInfo(user));
            return result;
        } catch (Exception e) {
            logger.error("Error retrieving sent fetch friend requests for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestFriendRequestTimestamp(UUID userId) {
        try {
            logger.info("Getting latest friend request timestamp for user: " + LoggingUtils.formatUserIdInfo(userId));
            
            // Get the latest incoming friend request
            java.util.Optional<FriendRequest> latestIncoming = repository.findTopByReceiverIdOrderByCreatedAtDesc(userId);
            
            // Get the latest sent friend request
            java.util.Optional<FriendRequest> latestSent = repository.findTopBySenderIdOrderByCreatedAtDesc(userId);
            
            Instant latestTimestamp = null;
            
            // Compare and find the most recent timestamp
            if (latestIncoming.isPresent() && latestSent.isPresent()) {
                latestTimestamp = latestIncoming.get().getCreatedAt().isAfter(latestSent.get().getCreatedAt()) 
                    ? latestIncoming.get().getCreatedAt() 
                    : latestSent.get().getCreatedAt();
            } else if (latestIncoming.isPresent()) {
                latestTimestamp = latestIncoming.get().getCreatedAt();
            } else if (latestSent.isPresent()) {
                latestTimestamp = latestSent.get().getCreatedAt();
            }
            
            logger.info("Latest friend request timestamp for user " + LoggingUtils.formatUserIdInfo(userId) + ": " + latestTimestamp);
            return latestTimestamp;
        } catch (Exception e) {
            logger.error("Error retrieving latest friend request timestamp for user: " + LoggingUtils.formatUserIdInfo(userId) + ": " + e.getMessage());
            throw e;
        }
    }


}
