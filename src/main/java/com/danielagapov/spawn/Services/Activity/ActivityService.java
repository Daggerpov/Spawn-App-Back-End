package com.danielagapov.spawn.Services.Activity;

import com.danielagapov.spawn.DTOs.Activity.*;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Events.ActivityInviteNotificationEvent;
import com.danielagapov.spawn.Events.ActivityParticipationNotificationEvent;
import com.danielagapov.spawn.Events.ActivityUpdateNotificationEvent;
import com.danielagapov.spawn.Exceptions.ActivityFullException;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Mappers.UserMapper;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityType;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.CompositeKeys.ActivityUsersId;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Activity.ActivityExpirationService;
import com.danielagapov.spawn.Services.ActivityType.IActivityTypeService;
import com.danielagapov.spawn.Services.Location.ILocationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityService implements IActivityService {
    private final IActivityRepository repository;
    private final IActivityTypeRepository activityTypeRepository;
    private final ILocationRepository locationRepository;
    private final IActivityUserRepository activityUserRepository;
    private final IUserRepository userRepository;
    private final IUserService userService;
    private final IChatMessageService chatMessageService;
    private final ILogger logger;
    private final ILocationService locationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ActivityExpirationService expirationService;
    private final IActivityTypeService activityTypeService;

    @Autowired
    @Lazy // avoid circular dependency problems with ChatMessageService
    public ActivityService(IActivityRepository repository, IActivityTypeRepository activityTypeRepository,
                        ILocationRepository locationRepository, IActivityUserRepository activityUserRepository, 
                        IUserRepository userRepository, IUserService userService, 
                        IChatMessageService chatMessageService, ILogger logger, ILocationService locationService, 
                        ApplicationEventPublisher eventPublisher, ActivityExpirationService expirationService,
                        IActivityTypeService activityTypeService) {
        this.repository = repository;
        this.activityTypeRepository = activityTypeRepository;
        this.locationRepository = locationRepository;
        this.activityUserRepository = activityUserRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
        this.locationService = locationService;
        this.eventPublisher = eventPublisher;
        this.expirationService = expirationService;
        this.activityTypeService = activityTypeService;
    }

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
                    expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt())
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
     */
    private Map<UUID, List<UUID>> getBatchChatMessageIds(List<UUID> activityIds) {
        List<Object[]> results = chatMessageService.getChatMessageIdsByActivityIds(activityIds);
        
        return results.stream()
                .collect(Collectors.groupingBy(
                    row -> (UUID) row[0], // activity ID
                    Collectors.mapping(
                        row -> (UUID) row[1], // message ID
                        Collectors.toList()
                    )
                ));
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
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByActivityId(id);

        return ActivityMapper.toDTO(Activity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds, 
                expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt()));
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
                false // isExpired - assuming false for now, could be calculated based on endTime
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
                    chatMessageService.getChatMessageIdsByActivityId(ActivityEntity.getId()), // chatMessageIds
                    expirationService.isActivityExpired(ActivityEntity.getStartTime(), ActivityEntity.getEndTime(), ActivityEntity.getCreatedAt()) // isExpired
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

            activity = repository.save(activity);

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
                    expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt())
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
                        chatMessageService.getChatMessageIdsByActivityId(Activity.getId()),
                        expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt())))
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

            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(savedActivity.getCreator(), savedActivity, activityUserRepository)
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
                    activity.setStartTime(startTime);
                } catch (Exception e) {
                    logger.warn("Invalid startTime format in partial update: " + updates.getStartTime());
                }
            }
            
            if (updates.getEndTime() != null) {
                try {
                    OffsetDateTime endTime = OffsetDateTime.parse(updates.getEndTime());
                    activity.setEndTime(endTime);
                } catch (Exception e) {
                    logger.warn("Invalid endTime format in partial update: " + updates.getEndTime());
                }
            }

            // Update the lastUpdated timestamp
            activity.setLastUpdated(Instant.now());

            // Save updated activity
            Activity savedActivity = repository.save(activity);

            // Publish update event
            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(savedActivity.getCreator(), savedActivity, activityUserRepository)
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
        List<UUID> chatMessageIds = chatMessageService.getChatMessageIdsByActivityId(ActivityEntity.getId());

        return ActivityMapper.toDTO(ActivityEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds,
                expirationService.isActivityExpired(ActivityEntity.getStartTime(), ActivityEntity.getEndTime(), ActivityEntity.getCreatedAt()));
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

    /**
     * @param requestingUserId this is the user whose feed is being loaded
     * @return This method returns the feed Activities for a user, with their created ones
     * first in the `universalAccentColor`, followed by Activities they're invited to
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

            return makeFeed(ActivitiesCreated, ActivitiesInvitedTo);
        } catch (Exception e) {
            logger.error("Error fetching feed Activities for user: " + requestingUserId + " - " + e.getMessage());
            throw e;
        }
    }

    /**
     * Helper function to remove expired Activities, sort by time, and combine the Activities created by a user,
     * and the Activities they are invited to
     */
    private List<FullFeedActivityDTO> makeFeed(List<FullFeedActivityDTO> ActivitiesCreated, List<FullFeedActivityDTO> ActivitiesInvitedTo) {
        // Remove expired Activities
        ActivitiesCreated = removeExpiredActivities(ActivitiesCreated);
        ActivitiesInvitedTo = removeExpiredActivities(ActivitiesInvitedTo);

        // Sort Activities
        sortActivitiesByStartTime(ActivitiesCreated);
        sortActivitiesByStartTime(ActivitiesInvitedTo);

        // Combine the two lists into one.
        List<FullFeedActivityDTO> combinedActivities = new ArrayList<>(ActivitiesCreated);
        combinedActivities.addAll(ActivitiesInvitedTo);
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
                .filter(activity -> !expirationService.isActivityExpired(activity.getStartTime(), activity.getEndTime(), activity.getCreatedAt()))
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

            // Safely fetch location and creator
            LocationDTO location = Activity.getLocation();

            UserDTO creator = userService.getUserById(Activity.getCreatorUserId());

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
                    userService.getParticipantsByActivityId(Activity.getId()),
                    userService.getInvitedByActivityId(Activity.getId()),
                    chatMessageService.getFullChatMessagesByActivityId(Activity.getId()),
                    requestingUserId != null ? getParticipationStatus(Activity.getId(), requestingUserId) : null,
                    Activity.getCreatorUserId().equals(requestingUserId),
                    Activity.getCreatedAt(),
                    expirationService.isActivityExpired(Activity.getStartTime(), Activity.getEndTime(), Activity.getCreatedAt())
            );
        } catch (BaseNotFoundException e) {
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
        return chatMessageService.getFullChatMessagesByActivityId(activityId);
    }
    
    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityInviteById", key = "#activityId"),
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
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
            
            // Convert each FullFeedActivityDTO to ProfileActivityDTO
            for (FullFeedActivityDTO fullFeedActivity : fullFeedActivities) {
                result.add(ProfileActivityDTO.fromFullFeedActivityDTO(fullFeedActivity));
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
                    result.add(ProfileActivityDTO.fromFullFeedActivityDTO(Activity));
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
