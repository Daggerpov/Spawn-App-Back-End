package com.danielagapov.spawn.activity.internal.services;

import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.activity.api.dto.*;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.events.ActivityInviteNotificationEvent;
import com.danielagapov.spawn.shared.events.ActivityParticipationNotificationEvent;
import com.danielagapov.spawn.shared.events.ActivityUpdateNotificationEvent;
import com.danielagapov.spawn.shared.exceptions.ActivityFullException;
import com.danielagapov.spawn.shared.exceptions.ApplicationException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.ActivityMapper;
import com.danielagapov.spawn.shared.util.LocationMapper;
import com.danielagapov.spawn.shared.util.UserMapper;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.activity.internal.domain.ActivityUsersId;
import com.danielagapov.spawn.activity.internal.domain.Location;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.activity.internal.repositories.IActivityTypeRepository;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.activity.internal.repositories.ILocationRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.services.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the Activity module's public API.
 * 
 * This service provides full access to activity management operations
 * for both internal use (controllers) and external modules (User, Chat, Analytics, etc.)
 * without exposing internal repositories.
 * 
 * Part of Phase 3: Shared Data Resolution in Spring Modulith refactoring.
 */
@Service
public class ActivityService implements IActivityService {
    private final IActivityRepository repository;
    private final IActivityTypeRepository activityTypeRepository;
    private final ILocationRepository locationRepository;
    private final IActivityUserRepository activityUserRepository;
    private final IUserRepository userRepository;
    private final IUserService userService;
    private final IChatQueryService chatQueryService;
    private final ILogger logger;
    private final ILocationService locationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ActivityExpirationService expirationService;
    private final IActivityTypeService activityTypeService;

    @Autowired
    public ActivityService(IActivityRepository repository, IActivityTypeRepository activityTypeRepository,
                        ILocationRepository locationRepository, IActivityUserRepository activityUserRepository, 
                        IUserRepository userRepository, IUserService userService, 
                        IChatQueryService chatQueryService, ILogger logger, ILocationService locationService, 
                        ApplicationEventPublisher eventPublisher, ActivityExpirationService expirationService,
                        IActivityTypeService activityTypeService) {
        this.repository = repository;
        this.activityTypeRepository = activityTypeRepository;
        this.locationRepository = locationRepository;
        this.activityUserRepository = activityUserRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.chatQueryService = chatQueryService;
        this.logger = logger;
        this.locationService = locationService;
        this.eventPublisher = eventPublisher;
        this.expirationService = expirationService;
        this.activityTypeService = activityTypeService;
    }
    
    // ==================== Participant Queries (Public API) ====================
    
    @Override
    public List<UUID> getParticipantUserIdsByActivityIdAndStatus(UUID activityId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndStatus(activityId, status)
                .stream()
                .map(au -> au.getUser().getId())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UUID> getActivityIdsByUserIdAndStatus(UUID userId, ParticipationStatus status) {
        return activityUserRepository.findByUser_IdAndStatus(userId, status)
                .stream()
                .map(au -> au.getActivity().getId())
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean isUserParticipantWithStatus(UUID activityId, UUID userId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndUser_Id(activityId, userId)
                .map(au -> au.getStatus() == status)
                .orElse(false);
    }
    
    @Override
    public int getParticipantCountByStatus(UUID activityId, ParticipationStatus status) {
        return activityUserRepository.findByActivity_IdAndStatus(activityId, status).size();
    }
    
    // ==================== Activity History Queries ====================
    
    @Override
    public List<UUID> getPastActivityIdsForUser(UUID userId, ParticipationStatus status, OffsetDateTime now, Limit limit) {
        return activityUserRepository.findPastActivityIdsForUser(userId, status, now, limit);
    }
    
    @Override
    public List<UserIdActivityTimeDTO> getOtherUserIdsByActivityIds(List<UUID> activityIds, UUID excludeUserId, ParticipationStatus status) {
        return activityUserRepository.findOtherUserIdsByActivityIds(activityIds, excludeUserId, status);
    }
    
    // ==================== Shared Activities Queries ====================
    
    @Override
    public int getSharedActivitiesCount(UUID userId1, UUID userId2, ParticipationStatus status) {
        // Get all activities where user1 has participated
        List<ActivityUser> user1Activities = activityUserRepository.findByUser_IdAndStatus(userId1, status);
        
        if (user1Activities.isEmpty()) {
            return 0;
        }
        
        // Extract activity IDs from user1's participated activities
        Set<UUID> user1ActivityIds = user1Activities.stream()
                .map(au -> au.getActivity().getId())
                .collect(Collectors.toSet());
        
        // Get all activities where user2 has participated
        List<ActivityUser> user2Activities = activityUserRepository.findByUser_IdAndStatus(userId2, status);
        
        // Count how many activities overlap between the two users
        return (int) user2Activities.stream()
                .map(au -> au.getActivity().getId())
                .filter(user1ActivityIds::contains)
                .count();
    }
    
    // ==================== Activity Creator Queries ====================
    
    @Override
    public UUID getActivityCreatorId(UUID activityId) {
        return repository.findById(activityId)
                .map(activity -> activity.getCreator().getId())
                .orElse(null);
    }
    
    @Override
    public List<UUID> getActivityIdsCreatedByUser(UUID userId) {
        return repository.findByCreatorId(userId)
                .stream()
                .map(Activity::getId)
                .collect(Collectors.toList());
    }
    
    // ==================== Activity Info Queries (for external modules) ====================
    
    @Override
    public String getActivityTitle(UUID activityId) {
        return repository.findById(activityId)
                .map(Activity::getTitle)
                .orElse(null);
    }
    
    @Override
    public boolean activityExists(UUID activityId) {
        return repository.existsById(activityId);
    }

    // ==================== Activity CRUD Operations ====================

    @Override
    public List<FullFeedActivityDTO> getAllFullActivities() {
        try {
            List<Activity> activities = repository.findAll();
            return getOptimizedFullActivities(activities, null);
        } catch (Exception e) {
            logger.error("Error fetching all full activities: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Optimized method to convert a list of activities to FullFeedActivityDTOs
     * using batch queries to prevent N+1 query problems.
     */
    private List<FullFeedActivityDTO> getOptimizedFullActivities(List<Activity> activities, UUID requestingUserId) {
        if (activities.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<UUID> activityIds = activities.stream()
                .map(Activity::getId)
                .collect(Collectors.toList());
        
        // Batch fetch all related data
        Map<UUID, List<UUID>> participantsByActivity = getBatchParticipantIds(activityIds);
        Map<UUID, List<UUID>> invitedByActivity = getBatchInvitedIds(activityIds);
        Map<UUID, List<UUID>> chatMessagesByActivity = getBatchChatMessageIds(activityIds);
        
        // Convert to DTOs efficiently
        List<FullFeedActivityDTO> result = new ArrayList<>();
        Set<UUID> visitedActivities = new HashSet<>();
        
        for (Activity activity : activities) {
            try {
                ActivityDTO activityDTO = ActivityMapper.toDTO(
                    activity,
                    activity.getCreator().getId(),
                    participantsByActivity.getOrDefault(activity.getId(), List.of()),
                    invitedByActivity.getOrDefault(activity.getId(), List.of()),
                    chatMessagesByActivity.getOrDefault(activity.getId(), List.of()),
                    expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt(), activity.getClientTimezone())
                );
                
                FullFeedActivityDTO fullActivity = getFullActivityByActivity(activityDTO, requestingUserId, visitedActivities);
                if (fullActivity != null) {
                    result.add(fullActivity);
                }
            } catch (Exception e) {
                logger.warn("Skipping activity " + activity.getId() + " due to error: " + e.getMessage());
                // Continue with other activities instead of failing completely
            }
        }
        
        return result;
    }
    
    /**
     * Batch fetch participant user IDs for multiple activities.
     */
    private Map<UUID, List<UUID>> getBatchParticipantIds(List<UUID> activityIds) {
        List<Object[]> results = activityUserRepository.findUserIdsByActivityIdsAndStatus(
            activityIds, ParticipationStatus.participating);
        
        return results.stream()
                .collect(Collectors.groupingBy(
                    row -> (UUID) row[0], // activity ID
                    Collectors.mapping(
                        row -> (UUID) row[1], // user ID
                        Collectors.toList()
                    )
                ));
    }
    
    /**
     * Batch fetch invited user IDs for multiple activities.
     */
    private Map<UUID, List<UUID>> getBatchInvitedIds(List<UUID> activityIds) {
        List<Object[]> results = activityUserRepository.findUserIdsByActivityIdsAndStatus(
            activityIds, ParticipationStatus.invited);
        
        return results.stream()
                .collect(Collectors.groupingBy(
                    row -> (UUID) row[0], // activity ID
                    Collectors.mapping(
                        row -> (UUID) row[1], // user ID
                        Collectors.toList()
                    )
                ));
    }
    
    /**
     * Batch fetch chat message IDs for multiple activities.
     * Uses event-driven query to Chat module to avoid circular dependency.
     */
    private Map<UUID, List<UUID>> getBatchChatMessageIds(List<UUID> activityIds) {
        return chatQueryService.getChatMessageIdsByActivityIds(activityIds);
    }

    @Override
    public List<ActivityDTO> getAllActivities() {
        try {
            List<Activity> Activities = repository.findAll();
            return getActivityDTOs(Activities);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BasesNotFoundException(EntityType.Activity);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Cacheable(value = "ActivityById", key = "#id")
    public ActivityDTO getActivityById(UUID id) {
        Activity Activity = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, id));

        UUID creatorUserId = Activity.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByActivityId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByActivityId(id);
        List<UUID> chatMessageIds = chatQueryService.getChatMessageIdsByActivityId(id);

        return ActivityMapper.toDTO(Activity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds, 
                expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt(), Activity.getClientTimezone()));
    }

    @Override
    @Cacheable(value = "fullActivityById", key = "#id.toString() + ':' + #requestingUserId.toString()")
    public FullFeedActivityDTO getFullActivityById(UUID id, UUID requestingUserId) {
        return getFullActivityByActivity(getActivityById(id), requestingUserId, new HashSet<>());
    }

    @Override
    @Cacheable(value = "ActivityInviteById", key = "#id")
    public ActivityInviteDTO getActivityInviteById(UUID id) {
        Activity activity = repository.findById(id)
                .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, id));

        // Get creator information
        User creator = activity.getCreator();
        String creatorName = creator.getName();
        String creatorUsername = "@" + creator.getUsername();
        
        // Get location name
        String locationName = activity.getLocation() != null ? activity.getLocation().getName() : null;
        
        // Get participating and invited user IDs
        List<UUID> participatingUserIds = userService.getParticipantUserIdsByActivityId(id);
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByActivityId(id);
        
        return new ActivityInviteDTO(
                activity.getId(),
                activity.getTitle(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getLocation() != null ? activity.getLocation().getId() : null,
                activity.getActivityType() != null ? activity.getActivityType().getId() : null,
                activity.getNote(),
                activity.getIcon(),
                activity.getParticipantLimit(),
                activity.getCreator().getId(),
                participatingUserIds,
                invitedUserIds,
                activity.getCreatedAt(),
                expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt(), activity.getClientTimezone()),
                activity.getClientTimezone()
        );
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "ActivityInviteById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUser.id"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            
            @CacheEvict(value = "userStatsById", key = "#result.creatorUser.id")
    })
    public AbstractActivityDTO saveActivity(AbstractActivityDTO Activity) {
        try {
            Activity ActivityEntity;

            if (Activity instanceof FullFeedActivityDTO fullFeedActivityDTO) {
                ActivityEntity = ActivityMapper.convertFullFeedActivityDTOToActivityEntity(fullFeedActivityDTO);
            } else if (Activity instanceof ActivityDTO ActivityDTO) {
                Location location = null;
                if (ActivityDTO.getLocation() != null) {
                    location = locationService.save(LocationMapper.toEntity(ActivityDTO.getLocation()));
                }
                ActivityType activityType = ActivityDTO.getActivityTypeId() != null 
                    ? activityTypeRepository.findById(ActivityDTO.getActivityTypeId()).orElse(null) 
                    : null;

                // Map ActivityDTO to Activity entity with the resolved Location and ActivityType
                ActivityEntity = ActivityMapper.toEntity(ActivityDTO, location,
                        userService.getUserEntityById(ActivityDTO.getCreatorUserId()), activityType);
            } else {
                throw new IllegalArgumentException("Unsupported Activity type");
            }

            // Save the Activity entity
            ActivityEntity = repository.save(ActivityEntity);

            // Map saved Activity entity back to ActivityDTO with all necessary fields
            // creatorUserId
            // participantUserIds
            // invitedUserIds
            // chatMessageIds
            return ActivityMapper.toDTO(
                    ActivityEntity,
                    ActivityEntity.getCreator().getId(), // creatorUserId
                    userService.getParticipantUserIdsByActivityId(ActivityEntity.getId()), // participantUserIds
                    userService.getInvitedUserIdsByActivityId(ActivityEntity.getId()), // invitedUserIds
                    chatQueryService.getChatMessageIdsByActivityId(ActivityEntity.getId()), // chatMessageIds
                    expirationService.isActivityExpired(ActivityEntity.getStartTime(), ActivityEntity.getEndTime(), ActivityEntity.getCreatedAt(), ActivityEntity.getClientTimezone()) // isExpired
            );
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseSaveException("Failed to save Activity: " + e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "ActivityInviteById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUser.id"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            
            @CacheEvict(value = "userStatsById", key = "#result.creatorUser.id")
    })
    public AbstractActivityDTO createActivity(ActivityDTO activityDTO) {
        try {
            // Handle location - save the provided LocationDTO
            Location location = null;
            if (activityDTO.getLocation() != null) {
                location = locationService.save(LocationMapper.toEntity(activityDTO.getLocation()));
            } else {
                throw new IllegalArgumentException("Location must be provided");
            }

            User creator = userRepository.findById(activityDTO.getCreatorUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, activityDTO.getCreatorUserId()));

            ActivityType activityType = activityDTO.getActivityTypeId() != null
                    ? activityTypeRepository.findById(activityDTO.getActivityTypeId()).orElse(null)
                    : null;

            // Validate that start time and end time are not in the past
            OffsetDateTime now = OffsetDateTime.now();
            
            if (activityDTO.getStartTime() != null && activityDTO.getStartTime().isBefore(now)) {
                throw new IllegalArgumentException("Activity start time cannot be in the past");
            }
            
            if (activityDTO.getEndTime() != null && activityDTO.getEndTime().isBefore(now)) {
                throw new IllegalArgumentException("Activity end time cannot be in the past");
            }

            // Create Activity entity from ActivityDTO
            Activity activity = new Activity();
            activity.setTitle(activityDTO.getTitle());
            activity.setStartTime(activityDTO.getStartTime());
            activity.setEndTime(activityDTO.getEndTime());
            activity.setNote(activityDTO.getNote());
            activity.setIcon(activityDTO.getIcon());
            activity.setParticipantLimit(activityDTO.getParticipantLimit());
            activity.setLocation(location);
            activity.setCreator(creator);
            activity.setActivityType(activityType);
            activity.setClientTimezone(activityDTO.getClientTimezone());

            activity = repository.save(activity);

            // Check if this is the user's first activity and mark onboarding as completed
            if (!creator.getHasCompletedOnboarding()) {
                creator.markOnboardingCompleted();
                userRepository.save(creator);
            }

            // Handle invited friends
            List<UUID> invitedIds = activityDTO.getInvitedUserIds();
                
            if (invitedIds != null) {
                for (UUID userId: invitedIds) {
                    User invitedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
                    ActivityUsersId compositeId = new ActivityUsersId(activity.getId(), userId);
                    ActivityUser activityUser = new ActivityUser();
                    activityUser.setId(compositeId);
                    activityUser.setActivity(activity);
                    activityUser.setUser(invitedUser);
                    activityUser.setStatus(ParticipationStatus.invited);
                    activityUserRepository.save(activityUser);
                }

                // Create and publish Activity invite notification directly
                eventPublisher.publishEvent(
                    new ActivityInviteNotificationEvent(activity.getCreator(), activity, new HashSet<>(invitedIds))
                );
            }

            // Return a FullFeedActivityDTO instead of ActivityDTO to include full location information
            LocationDTO locationDTO = LocationMapper.toDTO(location);
            BaseUserDTO creatorUserDTO = UserMapper.toDTO(creator);
            List<BaseUserDTO> invitedUserDTOs = new ArrayList<>();
            
            if (invitedIds != null) {
                invitedUserDTOs = invitedIds.stream()
                        .map(userId -> {
                            try {
                                return UserMapper.toDTO(userRepository.findById(userId)
                                        .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId)));
                            } catch (Exception e) {
                                logger.warn("Could not load invited user: " + userId);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            return new FullFeedActivityDTO(
                    activity.getId(),
                    activity.getTitle(),
                    activity.getStartTime(),
                    activity.getEndTime(),
                    locationDTO,
                    activity.getActivityType() != null ? activity.getActivityType().getId() : null,
                    activity.getNote(),
                    activity.getIcon(),
                    activity.getParticipantLimit(),
                    creatorUserDTO,
                    new ArrayList<>(), // participantUsers - empty for new activity
                    invitedUserDTOs,
                    new ArrayList<>(), // chatMessages - empty for new activity
                    null, // participationStatus - not applicable for creator
                    true, // isSelfOwned - true since this is the creator
                    activity.getCreatedAt(),
                    expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt(), activity.getClientTimezone()),
                    activity.getClientTimezone()
            );
        } catch (Exception e) {
            logger.error("Error creating Activity: " + e.getMessage());
            throw new ApplicationException("Failed to create Activity", e);
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "ActivityInviteById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUser.id"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            
            @CacheEvict(value = "userStatsById", key = "#result.creatorUser.id")
    })
    public FullFeedActivityDTO createActivityWithSuggestions(ActivityDTO activityDTO) {
        try {
            // Create the activity using the existing method
            AbstractActivityDTO createdActivity = createActivity(activityDTO);
            
            // Cast to FullFeedActivityDTO (which is what createActivity returns)
            FullFeedActivityDTO fullActivity = (FullFeedActivityDTO) createdActivity;
            
            // Return the activity directly (friend suggestions feature was removed)
            return fullActivity;
            
        } catch (Exception e) {
            logger.error("Error creating Activity with suggestions: " + e.getMessage());
            throw new ApplicationException("Failed to create Activity with suggestions", e);
        }
    }

    @Override
    @Cacheable(value = "ActivitiesByOwnerId", key = "#creatorUserId")
    public List<ActivityDTO> getActivitiesByOwnerId(UUID creatorUserId) {
        List<Activity> Activities = repository.findByCreatorId(creatorUserId);
        return getActivityDTOs(Activities);
    }

    private List<ActivityDTO> getActivityDTOs(List<Activity> Activities) {
        return Activities.stream()
                .map(Activity -> ActivityMapper.toDTO(
                        Activity,
                        Activity.getCreator().getId(),
                        userService.getParticipantUserIdsByActivityId(Activity.getId()),
                        userService.getInvitedUserIdsByActivityId(Activity.getId()),
                        chatQueryService.getChatMessageIdsByActivityId(Activity.getId()),
                        expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt(), Activity.getClientTimezone())))
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "ActivityInviteById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUser.id"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            
    })
    public FullFeedActivityDTO replaceActivity(ActivityDTO newActivity, UUID id) {
        return repository.findById(id).map(activity -> {
            // Update basic activity details
            activity.setTitle(newActivity.getTitle());
            activity.setNote(newActivity.getNote());
            activity.setEndTime(newActivity.getEndTime());
            activity.setStartTime(newActivity.getStartTime());
            activity.setIcon(newActivity.getIcon());
            activity.setParticipantLimit(newActivity.getParticipantLimit());

            // Handle location
            if (newActivity.getLocation() != null) {
                Location location = locationService.save(LocationMapper.toEntity(newActivity.getLocation()));
                activity.setLocation(location);
            }

            // Update activity type if provided
            if (newActivity.getActivityTypeId() != null) {
                ActivityType activityType = activityTypeRepository.findById(newActivity.getActivityTypeId()).orElse(null);
                activity.setActivityType(activityType);
            }

            // Save updated activity
            Activity savedActivity = repository.save(activity);

            // Handle invited friends updates if provided
            List<UUID> invitedIds = newActivity.getInvitedUserIds();
                
            if (invitedIds != null) {
                // Remove existing invitations
                List<ActivityUser> existingActivityUsers = activityUserRepository.findByActivity_Id(id);
                activityUserRepository.deleteAll(existingActivityUsers);
                
                // Add new invitations
                for (UUID userId : invitedIds) {
                    User invitedUser = userRepository.findById(userId)
                            .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
                    ActivityUsersId compositeId = new ActivityUsersId(id, userId);
                    ActivityUser activityUser = new ActivityUser();
                    activityUser.setId(compositeId);
                    activityUser.setActivity(savedActivity);
                    activityUser.setUser(invitedUser);
                    activityUser.setStatus(ParticipationStatus.invited);
                    activityUserRepository.save(activityUser);
                }
            }

            // Get participant IDs for the notification event
            List<UUID> participantIds = getParticipatingUserIdsByActivityId(savedActivity.getId());
            
            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(
                    savedActivity.getCreator().getId(),
                    savedActivity.getCreator().getUsername(),
                    savedActivity.getId(),
                    savedActivity.getTitle(),
                    participantIds)
            );
            return getFullActivityById(savedActivity.getId(), newActivity.getCreatorUserId());
        }).orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, id));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUser.id"),
            @CacheEvict(value = "feedActivities", allEntries = true),
    })
    public FullFeedActivityDTO partialUpdateActivity(ActivityPartialUpdateDTO updates, UUID id) {
        return repository.findById(id).map(activity -> {
            // Only update the fields that are provided in the updates DTO
            if (updates.getTitle() != null) {
                activity.setTitle(updates.getTitle());
            }
            
            if (updates.getIcon() != null) {
                activity.setIcon(updates.getIcon());
            }
            
            if (updates.getNote() != null) {
                activity.setNote(updates.getNote());
            }
            
            if (updates.getParticipantLimit() != null) {
                activity.setParticipantLimit(updates.getParticipantLimit());
            }
            
            if (updates.getStartTime() != null) {
                try {
                    OffsetDateTime startTime = OffsetDateTime.parse(updates.getStartTime());
                    // Validate that start time is not in the past
                    OffsetDateTime now = OffsetDateTime.now();
                    if (startTime.isBefore(now)) {
                        throw new IllegalArgumentException("Activity start time cannot be in the past");
                    }
                    activity.setStartTime(startTime);
                } catch (IllegalArgumentException e) {
                    // Re-throw validation exceptions
                    throw e;
                } catch (Exception e) {
                    logger.warn("Invalid startTime format in partial update: " + updates.getStartTime());
                }
            }
            
            if (updates.getEndTime() != null) {
                try {
                    OffsetDateTime endTime = OffsetDateTime.parse(updates.getEndTime());
                    // Validate that end time is not in the past
                    OffsetDateTime now = OffsetDateTime.now();
                    if (endTime.isBefore(now)) {
                        throw new IllegalArgumentException("Activity end time cannot be in the past");
                    }
                    activity.setEndTime(endTime);
                } catch (IllegalArgumentException e) {
                    // Re-throw validation exceptions
                    throw e;
                } catch (Exception e) {
                    logger.warn("Invalid endTime format in partial update: " + updates.getEndTime());
                }
            }

            // Update the lastUpdated timestamp
            activity.setLastUpdated(Instant.now());

            // Save updated activity
            Activity savedActivity = repository.save(activity);

            // Get participant IDs for the notification event
            List<UUID> participantIds = getParticipatingUserIdsByActivityId(savedActivity.getId());
            
            // Publish update event
            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(
                    savedActivity.getCreator().getId(),
                    savedActivity.getCreator().getUsername(),
                    savedActivity.getId(),
                    savedActivity.getTitle(),
                    participantIds)
            );

            // Get the creator's user ID for the full activity fetch
            UUID creatorUserId = savedActivity.getCreator().getId();
            return getFullActivityById(savedActivity.getId(), creatorUserId);
        }).orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, id));
    }

    private List<UUID> getParticipatingUserIdsByActivityId(UUID ActivityId) {
        try {
            List<ActivityUser> ActivityUsers = activityUserRepository.findActivitiesByActivity_IdAndStatus(ActivityId, ParticipationStatus.participating);
            return ActivityUsers.stream().map((ActivityUser -> ActivityUser.getUser().getId())).collect(Collectors.toList());
        } catch (DataAccessException e) {
            logger.error("Error finding Activities by Activity id: " + e.getMessage());
            throw e;
        }
    }

    private ActivityDTO constructDTOFromEntity(Activity ActivityEntity) {
        // Fetch related data for DTO
        UUID creatorUserId = ActivityEntity.getCreator().getId();
        List<UUID> participantUserIds = userService.getParticipantUserIdsByActivityId(ActivityEntity.getId());
        List<UUID> invitedUserIds = userService.getInvitedUserIdsByActivityId(ActivityEntity.getId());
        List<UUID> chatMessageIds = chatQueryService.getChatMessageIdsByActivityId(ActivityEntity.getId());

        return ActivityMapper.toDTO(ActivityEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds,
                expirationService.isActivityExpired(ActivityEntity.getStartTime(), ActivityEntity.getEndTime(), ActivityEntity.getCreatedAt(), ActivityEntity.getClientTimezone()));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#id"),
            @CacheEvict(value = "ActivityInviteById", key = "#id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", allEntries = true),
            @CacheEvict(value = "feedActivities", allEntries = true)
    })
    public boolean deleteActivityById(UUID id) {
        if (!repository.existsById(id)) {
            throw new BaseNotFoundException(EntityType.Activity, id);
        }

        try {
            repository.deleteById(id);
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public List<UserDTO> getParticipatingUsersByActivityId(UUID ActivityId) {
        try {
            List<ActivityUser> ActivityUsers = activityUserRepository.findByActivity_IdAndStatus(ActivityId, ParticipationStatus.participating);
            return ActivityUsers.stream()
                    .map(ActivityUser -> userService.getUserById(ActivityUser.getUser().getId()))
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.Activity, ActivityId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public ParticipationStatus getParticipationStatus(UUID ActivityId, UUID userId) {
        ActivityUsersId compositeId = new ActivityUsersId(ActivityId, userId);
        return activityUserRepository.findById(compositeId)
                .map(ActivityUser::getStatus)
                .orElse(ParticipationStatus.notInvited);
    }


    // return type boolean represents whether the user was already invited or not
    // if false -> invites them
    // if true -> return 400 in Controller to indicate that the user has already
    // been invited, or it is a bad request.
    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityInviteById", key = "#ActivityId"),
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesParticipatingIn", key = "#userId"),
            @CacheEvict(value = "fullActivityById", key = "#ActivityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedActivities", key = "#userId"),
            
    })
    public boolean inviteUser(UUID ActivityId, UUID userId) {
        ActivityUsersId compositeId = new ActivityUsersId(ActivityId, userId);
        Optional<ActivityUser> existingActivityUser = activityUserRepository.findById(compositeId);

        if (existingActivityUser.isPresent()) {
            // User is already invited
            return existingActivityUser.get().getStatus().equals(ParticipationStatus.invited);
        } else {
            // Create a new invitation.
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
            Activity Activity = repository.findById(ActivityId)
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, ActivityId));

            ActivityUser newActivityUser = new ActivityUser();
            newActivityUser.setId(compositeId);
            newActivityUser.setActivity(Activity);
            newActivityUser.setUser(user);
            newActivityUser.setStatus(ParticipationStatus.invited);

            activityUserRepository.save(newActivityUser);
            return false;
        }
    }


    // returns the updated Activity, with modified participants and invited users
    // invited/participating
    // if true -> change status
    // if false -> return 400 in controller to indicate that the user is not
    // invited/participating
    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityInviteById", key = "#ActivityId"),
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesParticipatingIn", key = "#userId"),
            @CacheEvict(value = "fullActivityById", key = "#ActivityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedActivities", key = "#userId"),
            
            @CacheEvict(value = "userStatsById", key = "#userId")
    })
    public FullFeedActivityDTO toggleParticipation(UUID ActivityId, UUID userId) {
        ActivityUser ActivityUser = activityUserRepository.findByActivity_IdAndUser_Id(ActivityId, userId).orElseThrow(() -> new BaseNotFoundException(EntityType.ActivityUser));

        if (ActivityUser.getStatus() == ParticipationStatus.participating) {
            ActivityUser.setStatus(ParticipationStatus.invited);
        } else if (ActivityUser.getStatus().equals(ParticipationStatus.invited)) {
            // Check if activity has a participant limit and if it's already full
            final Activity activity = ActivityUser.getActivity();
            if (activity.getParticipantLimit() != null) {
                // Count current participants
                long currentParticipants = activityUserRepository.findByActivity_IdAndStatus(ActivityId, ParticipationStatus.participating).size();
                if (currentParticipants >= activity.getParticipantLimit()) {
                    throw new ActivityFullException(ActivityId, activity.getParticipantLimit());
                }
            }
            ActivityUser.setStatus(ParticipationStatus.participating);
        }
        
        final Activity Activity = ActivityUser.getActivity();
        final User user = ActivityUser.getUser();
        final ParticipationStatus status = ActivityUser.getStatus();
        
        if (status == ParticipationStatus.participating) { // Status changed from invited to participating
            eventPublisher.publishEvent(
                ActivityParticipationNotificationEvent.forJoining(user, Activity)
            );
        } else if (status == ParticipationStatus.invited) { // Status changed from participating to invited
            eventPublisher.publishEvent(
                ActivityParticipationNotificationEvent.forLeaving(user, Activity)
            );
        }
        
        activityUserRepository.save(ActivityUser);
        return getFullActivityById(ActivityId, userId);
    }

    @Override
    @Cacheable(value = "ActivitiesInvitedTo", key = "#id")
    public List<ActivityDTO> getActivitiesInvitedTo(UUID id) {
        List<ActivityUser> ActivityUsers = activityUserRepository.findByUser_IdAndStatus(id, ParticipationStatus.invited);
        return getActivityDTOs(ActivityUsers.stream()
                .map(ActivityUser::getActivity)
                .toList());
    }

    @Override
    @Cacheable(value = "fullActivitiesInvitedTo", key = "#id")
    public List<FullFeedActivityDTO> getFullActivitiesInvitedTo(UUID id) {
        List<ActivityUser> ActivityUsers = activityUserRepository.findByUser_IdAndStatus(id, ParticipationStatus.invited);
        return convertActivitiesToFullFeedActivities(
                getActivityDTOs(ActivityUsers.stream()
                        .map(ActivityUser::getActivity)
                        .toList()),
                id);
    }

    @Override
    @Cacheable(value = "fullActivitiesParticipatingIn", key = "#id")
    public List<FullFeedActivityDTO> getFullActivitiesParticipatingIn(UUID id) {
        List<ActivityUser> ActivityUsers = activityUserRepository.findByUser_IdAndStatus(id, ParticipationStatus.participating);
        return convertActivitiesToFullFeedActivities(
                getActivityDTOs(ActivityUsers.stream()
                        .map(ActivityUser::getActivity)
                        .toList()),
                id);
    }

    /**
     * @param requestingUserId this is the user whose feed is being loaded
     * @return This method returns the feed Activities for a user, with their created ones
     * first in the `universalAccentColor`, followed by Activities they're invited to and participating in
     */
    @Override
    @Cacheable(value = "feedActivities", key = "#requestingUserId")
    public List<FullFeedActivityDTO> getFeedActivities(UUID requestingUserId) {
        try {
            // Retrieve Activities created by the user.
            List<FullFeedActivityDTO> ActivitiesCreated = convertActivitiesToFullFeedSelfOwnedActivities(
                    getActivitiesByOwnerId(requestingUserId),
                    requestingUserId
            );

            List<FullFeedActivityDTO> ActivitiesInvitedTo = getFullActivitiesInvitedTo(requestingUserId);
            List<FullFeedActivityDTO> ActivitiesParticipatingIn = getFullActivitiesParticipatingIn(requestingUserId);

            return makeFeed(ActivitiesCreated, ActivitiesInvitedTo, ActivitiesParticipatingIn);
        } catch (Exception e) {
            logger.error("Error fetching feed Activities for user: " + requestingUserId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Helper function to remove expired Activities, sort by time, and combine the Activities created by a user,
     * the Activities they are invited to, and the Activities they are participating in
     */
    private List<FullFeedActivityDTO> makeFeed(List<FullFeedActivityDTO> ActivitiesCreated, List<FullFeedActivityDTO> ActivitiesInvitedTo, List<FullFeedActivityDTO> ActivitiesParticipatingIn) {
        // Remove expired Activities
        ActivitiesCreated = removeExpiredActivities(ActivitiesCreated);
        ActivitiesInvitedTo = removeExpiredActivities(ActivitiesInvitedTo);
        ActivitiesParticipatingIn = removeExpiredActivities(ActivitiesParticipatingIn);

        // Sort Activities
        sortActivitiesByStartTime(ActivitiesCreated);
        sortActivitiesByStartTime(ActivitiesInvitedTo);
        sortActivitiesByStartTime(ActivitiesParticipatingIn);

        // Combine the three lists into one.
        List<FullFeedActivityDTO> combinedActivities = new ArrayList<>(ActivitiesCreated);
        combinedActivities.addAll(ActivitiesInvitedTo);
        combinedActivities.addAll(ActivitiesParticipatingIn);
        return combinedActivities;
    }

    /**
     * Removes expired Activities from the provided list, and returns it modified.
     * Uses the centralized ActivityExpirationService for consistent expiration logic.
     *
     * @param Activities the list of Activities to filter
     * @return the modified list
     */
    private List<FullFeedActivityDTO> removeExpiredActivities(List<FullFeedActivityDTO> Activities) {
        if (Activities == null) {
            return Collections.emptyList();
        }

        return Activities.stream()
                .filter(Objects::nonNull)
                .filter(activity -> !expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt(), activity.getClientTimezone()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Sorts a list of Activities by their start time, keeping null values at the end.
     *
     * @param Activities the list of Activities to sort
     */
    private void sortActivitiesByStartTime(List<FullFeedActivityDTO> Activities) {
        Activities.sort(Comparator.comparing(FullFeedActivityDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    @Override
    public FullFeedActivityDTO getFullActivityByActivity(ActivityDTO Activity, UUID requestingUserId, Set<UUID> visitedActivities) {
        try {
            if (visitedActivities.contains(Activity.getId())) {
                return null;
            }
            visitedActivities.add(Activity.getId());

            // Safely fetch location
            LocationDTO location = Activity.getLocation();

            // Fetch creator - if this fails, we can't show the activity
            UserDTO creator;
            try {
                creator = userService.getUserById(Activity.getCreatorUserId());
            } catch (BaseNotFoundException e) {
                logger.warn("Cannot display activity " + Activity.getId() + " - creator not found: " + e.getMessage());
                return null;
            }

            // Fetch participants - if this fails, show empty list instead of dropping activity
            List<BaseUserDTO> participants;
            try {
                participants = userService.getParticipantsByActivityId(Activity.getId());
            } catch (Exception e) {
                logger.warn("Error fetching participants for activity " + Activity.getId() + ": " + e.getMessage());
                participants = new ArrayList<>();
            }

            // Fetch invited users - if this fails, show empty list instead of dropping activity
            List<BaseUserDTO> invitedUsers;
            try {
                invitedUsers = userService.getInvitedByActivityId(Activity.getId());
            } catch (Exception e) {
                logger.warn("Error fetching invited users for activity " + Activity.getId() + ": " + e.getMessage());
                invitedUsers = new ArrayList<>();
            }

            // Fetch chat messages - if this fails, show empty list instead of dropping activity
            List<FullActivityChatMessageDTO> chatMessages;
            try {
                chatMessages = chatQueryService.getFullChatMessagesByActivityId(Activity.getId());
            } catch (Exception e) {
                logger.warn("Error fetching chat messages for activity " + Activity.getId() + ": " + e.getMessage());
                chatMessages = new ArrayList<>();
            }

            // Fetch participation status - if this fails, show notInvited instead of dropping activity
            ParticipationStatus participationStatus = null;
            if (requestingUserId != null) {
                try {
                    participationStatus = getParticipationStatus(Activity.getId(), requestingUserId);
                } catch (Exception e) {
                    logger.warn("Error fetching participation status for activity " + Activity.getId() + ": " + e.getMessage());
                    participationStatus = ParticipationStatus.notInvited;
                }
            }

            return new FullFeedActivityDTO(
                    Activity.getId(),
                    Activity.getTitle(),
                    Activity.getStartTime(),
                    Activity.getEndTime(),
                    location,
                    Activity.getActivityTypeId(),
                    Activity.getNote(),
                    Activity.getIcon(),
                    Activity.getParticipantLimit(),
                    creator,
                    participants,
                    invitedUsers,
                    chatMessages,
                    participationStatus,
                    Activity.getCreatorUserId().equals(requestingUserId),
                    Activity.getCreatedAt(),
                    expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt(), Activity.getClientTimezone()),
                    Activity.getClientTimezone()
            );
        } catch (Exception e) {
            logger.error("Unexpected error converting activity " + Activity.getId() + " to FullFeedActivityDTO: " + e.getMessage());
            return null;
        }
    }

    @Override
    public List<FullFeedActivityDTO> convertActivitiesToFullFeedActivities(List<ActivityDTO> Activities, UUID requestingUserId) {
        ArrayList<FullFeedActivityDTO> fullActivities = new ArrayList<>();

        for (ActivityDTO ActivityDTO : Activities) {
            fullActivities.add(getFullActivityByActivity(ActivityDTO, requestingUserId, new HashSet<>()));
        }

        return fullActivities;
    }

    @Override
    public List<FullFeedActivityDTO> convertActivitiesToFullFeedSelfOwnedActivities(List<ActivityDTO> Activities, UUID requestingUserId) {
        ArrayList<FullFeedActivityDTO> fullActivities = new ArrayList<>();

        for (ActivityDTO ActivityDTO : Activities) {
            FullFeedActivityDTO fullFeedActivity = getFullActivityByActivity(ActivityDTO, requestingUserId, new HashSet<>());

            if (fullFeedActivity == null) {
                continue;
            }

            // Apply universal accent color
            // No tag color in new friend-based system

            fullActivities.add(fullFeedActivity);
        }

        return fullActivities;
    }

    @Override
    public Instant getLatestCreatedActivityTimestamp(UUID userId) {
        try {
            return repository.findTopByCreatorIdOrderByLastUpdatedDesc(userId)
                    .map(Activity::getLastUpdated)
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest created Activity timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestInvitedActivityTimestamp(UUID userId) {
        try {
            return activityUserRepository.findTopByUserIdAndStatusOrderByActivityLastUpdatedDesc(userId, ParticipationStatus.invited)
                    .map(ActivityUser -> ActivityUser.getActivity().getLastUpdated())
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest invited Activity timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    @Override
    public Instant getLatestUpdatedActivityTimestamp(UUID userId) {
        try {
            return activityUserRepository.findTopByUserIdAndStatusOrderByActivityLastUpdatedDesc(userId, ParticipationStatus.participating)
                    .map(ActivityUser -> ActivityUser.getActivity().getLastUpdated())
                    .orElse(null);
        } catch (DataAccessException e) {
            logger.error("Error fetching latest updated Activity timestamp for user: " + userId + " - " + e.getMessage());
            throw e;
        }
    }

    @Override
    public List<FullActivityChatMessageDTO> getChatMessagesByActivityId(UUID activityId) {
        return chatQueryService.getFullChatMessagesByActivityId(activityId);
    }
    
    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityInviteById", key = "#activityId"),
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesParticipatingIn", key = "#userId"),
            @CacheEvict(value = "fullActivityById", key = "#activityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedActivities", key = "#userId"),
            @CacheEvict(value = "userStatsById", key = "#userId")
    })
    public FullFeedActivityDTO autoJoinUserToActivity(UUID activityId, UUID userId) {
        try {
            // Check if user is already participating or invited
            ActivityUsersId compositeId = new ActivityUsersId(activityId, userId);
            Optional<ActivityUser> existingActivityUser = activityUserRepository.findById(compositeId);
            
            if (existingActivityUser.isPresent()) {
                // User is already invited or participating
                ActivityUser activityUser = existingActivityUser.get();
                
                // If they're just invited, automatically set them to participating
                if (activityUser.getStatus() == ParticipationStatus.invited) {
                    // Check if activity has a participant limit and if it's already full
                    final Activity activity = activityUser.getActivity();
                    if (activity.getParticipantLimit() != null) {
                        // Count current participants
                        long currentParticipants = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.participating).size();
                        if (currentParticipants >= activity.getParticipantLimit()) {
                            throw new ActivityFullException(activityId, activity.getParticipantLimit());
                        }
                    }
                    
                    activityUser.setStatus(ParticipationStatus.participating);
                    
                    // Publish participation event
                    final User user = activityUser.getUser();
                    eventPublisher.publishEvent(
                        ActivityParticipationNotificationEvent.forJoining(user, activity)
                    );
                    
                    activityUserRepository.save(activityUser);
                    logger.info("User " + userId + " auto-joined activity " + activityId + " (was previously invited)");
                }
                // If they're already participating, do nothing
            } else {
                // User is not invited, so invite them and set them to participating
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
                Activity activity = repository.findById(activityId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.Activity, activityId));

                // Check if activity has a participant limit and if it's already full
                if (activity.getParticipantLimit() != null) {
                    // Count current participants
                    long currentParticipants = activityUserRepository.findByActivity_IdAndStatus(activityId, ParticipationStatus.participating).size();
                    if (currentParticipants >= activity.getParticipantLimit()) {
                        throw new ActivityFullException(activityId, activity.getParticipantLimit());
                    }
                }

                ActivityUser newActivityUser = new ActivityUser();
                newActivityUser.setId(compositeId);
                newActivityUser.setActivity(activity);
                newActivityUser.setUser(user);
                newActivityUser.setStatus(ParticipationStatus.participating); // Direct to participating

                // Publish participation event
                eventPublisher.publishEvent(
                    ActivityParticipationNotificationEvent.forJoining(user, activity)
                );

                activityUserRepository.save(newActivityUser);
                logger.info("User " + userId + " auto-joined activity " + activityId + " (was not previously invited)");
            }
            
            // Return the updated activity
            return getFullActivityById(activityId, userId);
            
        } catch (BaseNotFoundException e) {
            logger.error("Error auto-joining user to activity: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error auto-joining user " + userId + " to activity " + activityId + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Gets past Activities where the specified user invited the requesting user
     * 
     * @param inviterUserId The user ID of the person who invited the requesting user
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of past Activities where inviterUserId invited requestingUserId
     */
    @Override
    public List<ProfileActivityDTO> getPastActivitiesWhereUserInvited(UUID inviterUserId, UUID requestingUserId) {
        try {
            // Use UTC for consistent timezone comparison across server and client timezones
            OffsetDateTime now = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            List<Activity> pastActivities = repository.getPastActivitiesWhereUserInvited(inviterUserId, requestingUserId, now);
            List<ActivityDTO> pastActivityDTOs = getActivityDTOs(pastActivities);
            
            // Convert to FullFeedActivityDTOs then to ProfileActivityDTOs and mark them as past Activities
            List<FullFeedActivityDTO> fullFeedActivities = convertActivitiesToFullFeedActivities(pastActivityDTOs, requestingUserId);
            List<ProfileActivityDTO> result = new ArrayList<>();
            
            for (FullFeedActivityDTO fullFeedActivity : fullFeedActivities) {
                result.add(ProfileActivityDTO.fromFullFeedActivityDTO(fullFeedActivity, true)); // true = isPastActivity
            }
            
            return result;
        } catch (Exception e) {
            logger.error("Error fetching past Activities where user " + inviterUserId + " invited user " + requestingUserId + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Gets feed Activities for a profile. If the profile user has no upcoming Activities, returns past Activities
     * that the profile user invited the requesting user to, with a flag indicating they are past Activities.
     *
     * @param profileUserId The user ID of the profile being viewed
     * @param requestingUserId The user ID of the user viewing the profile
     * @return List of Activities with a flag indicating if they are past Activities
     */
    @Override
    public List<ProfileActivityDTO> getProfileActivities(UUID profileUserId, UUID requestingUserId) {
        try {
            // Get upcoming Activities created by the profile user
            List<ActivityDTO> upcomingActivities = getActivitiesByOwnerId(profileUserId);
            List<FullFeedActivityDTO> upcomingFullActivities = convertActivitiesToFullFeedSelfOwnedActivities(upcomingActivities, requestingUserId);
            
            // Remove expired Activities
            List<FullFeedActivityDTO> nonExpiredActivities = removeExpiredActivities(upcomingFullActivities);
            
            // Convert to ProfileActivityDTO
            List<ProfileActivityDTO> result = new ArrayList<>();
            
            // If there are upcoming Activities, return them as ProfileActivityDTOs
            if (!nonExpiredActivities.isEmpty()) {
                sortActivitiesByStartTime(nonExpiredActivities);
                for (FullFeedActivityDTO Activity : nonExpiredActivities) {
                    result.add(ProfileActivityDTO.fromFullFeedActivityDTO(Activity, false)); // false = not past activity
                }
                return result;
            }
            
            // If no upcoming Activities, get past Activities where the profile user invited the requesting user
            return getPastActivitiesWhereUserInvited(profileUserId, requestingUserId);
        } catch (Exception e) {
            logger.error("Error fetching profile Activities for user " + profileUserId + 
                         " requested by " + requestingUserId + ": " + e.getMessage());
            throw e;
        }
    }

}
