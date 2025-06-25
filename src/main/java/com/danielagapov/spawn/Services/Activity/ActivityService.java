package com.danielagapov.spawn.Services.Activity;

import com.danielagapov.spawn.DTOs.Activity.*;
import com.danielagapov.spawn.DTOs.ChatMessage.FullActivityChatMessageDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Events.ActivityInviteNotificationEvent;
import com.danielagapov.spawn.Events.ActivityParticipationNotificationEvent;
import com.danielagapov.spawn.Events.ActivityUpdateNotificationEvent;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityMapper;
import com.danielagapov.spawn.Mappers.LocationMapper;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.CompositeKeys.ActivityUsersId;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.FriendTag.IFriendTagService;
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
    private final ILocationRepository locationRepository;
    private final IActivityUserRepository activityUserRepository;
    private final IUserRepository userRepository;
    private final IFriendTagService friendTagService;
    private final IUserService userService;
    private final IChatMessageService chatMessageService;
    private final ILogger logger;
    private final ILocationService locationService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    @Lazy // avoid circular dependency problems with ChatMessageService
    public ActivityService(IActivityRepository repository, ILocationRepository locationRepository,
                        IActivityUserRepository activityUserRepository, IUserRepository userRepository,
                        IFriendTagService friendTagService, IUserService userService, IChatMessageService chatMessageService,
                        ILogger logger, ILocationService locationService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.activityUserRepository = activityUserRepository;
        this.userRepository = userRepository;
        this.friendTagService = friendTagService;
        this.userService = userService;
        this.chatMessageService = chatMessageService;
        this.logger = logger;
        this.locationService = locationService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public List<FullFeedActivityDTO> getAllFullActivities() {
        ArrayList<FullFeedActivityDTO> fullActivities = new ArrayList<>();
        for (ActivityDTO e : getAllActivities()) {
            fullActivities.add(getFullActivityByActivity(e, null, new HashSet<>()));
        }
        return fullActivities;
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

        return ActivityMapper.toDTO(Activity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
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
        
        // Get all attendees (both participating and invited users)
        List<ActivityUser> allActivityUsers = activityUserRepository.findByActivity_Id(id);
        List<BaseUserDTO> attendees = allActivityUsers.stream()
                .map(activityUser -> userService.getBaseUserById(activityUser.getUser().getId()))
                .collect(Collectors.toList());
        
        int totalAttendees = attendees.size() + 1; // +1 for the creator
        
        return new ActivityInviteDTO(
                activity.getId(),
                activity.getTitle(),
                activity.getStartTime(),
                activity.getEndTime(),
                activity.getNote(),
                activity.getIcon(),
                activity.getCategory(),
                activity.getCreatedAt(),
                locationName,
                creatorName,
                creatorUsername,
                attendees,
                totalAttendees
        );
    }

    @Override
    @Cacheable(value = "ActivitiesByFriendTagId", key = "#tagId")
    public List<ActivityDTO> getActivitiesByFriendTagId(UUID tagId) {
        try {
            // Step 1: Retrieve the FriendTagDTO and its associated friend user IDs
            FriendTagDTO friendTag = friendTagService.getFriendTagById(tagId);
            List<UUID> friendIds = friendTag.getFriendUserIds();

            // Step 2: Retrieve Activities created by any of the friends
            // Step 3: Filter Activities based on whether their owner is in the list of friend
            // IDs
            List<Activity> filteredActivities = repository.findByCreatorIdIn(friendIds);

            // Step 3: Map filtered Activities to detailed DTOs
            return filteredActivities.stream()
                    .map(Activity -> ActivityMapper.toDTO(
                            Activity,
                            Activity.getCreator().getId(),
                            userService.getParticipantUserIdsByActivityId(Activity.getId()),
                            userService.getInvitedUserIdsByActivityId(Activity.getId()),
                            chatMessageService.getChatMessageIdsByActivityId(Activity.getId())))
                    .toList();
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new RuntimeException("Error retrieving Activities by friend tag ID", e);
        } catch (BaseNotFoundException e) {
            logger.error(e.getMessage());
            throw e; // Rethrow if it's a custom not-found exception
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            @CacheEvict(value = "filteredFeedActivities", allEntries = true)
    })
    public AbstractActivityDTO saveActivity(AbstractActivityDTO Activity) {
        try {
            Activity ActivityEntity;

            if (Activity instanceof FullFeedActivityDTO fullFeedActivityDTO) {
                ActivityEntity = ActivityMapper.convertFullFeedActivityDTOToActivityEntity(fullFeedActivityDTO);
            } else if (Activity instanceof ActivityDTO ActivityDTO) {
                Location location = locationRepository.findById(ActivityDTO.getLocationId()).orElse(null);

                // Map ActivityDTO to Activity entity with the resolved Location
                ActivityEntity = ActivityMapper.toEntity(ActivityDTO, location,
                        userService.getUserEntityById(ActivityDTO.getCreatorUserId()));
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
                    chatMessageService.getChatMessageIdsByActivityId(ActivityEntity.getId()) // chatMessageIds
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
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            @CacheEvict(value = "filteredFeedActivities", allEntries = true)
    })
    public AbstractActivityDTO createActivity(ActivityCreationDTO ActivityCreationDTO) {
        try {
            Location location = locationService.save(LocationMapper.toEntity(ActivityCreationDTO.getLocation()));

            User creator = userRepository.findById(ActivityCreationDTO.getCreatorUserId())
                    .orElseThrow(() -> new BaseNotFoundException(EntityType.User, ActivityCreationDTO.getCreatorUserId()));

            Activity Activity = ActivityMapper.fromCreationDTO(ActivityCreationDTO, location, creator);

            Activity = repository.save(Activity);

            for (UUID userId: ActivityCreationDTO.getInvitedFriendUserIds()) {
                User invitedUser = userRepository.findById(userId)
                        .orElseThrow(() -> new BaseNotFoundException(EntityType.User, userId));
                ActivityUsersId compositeId = new ActivityUsersId(Activity.getId(), userId);
                ActivityUser ActivityUser = new ActivityUser();
                ActivityUser.setId(compositeId);
                ActivityUser.setActivity(Activity);
                ActivityUser.setUser(invitedUser);
                ActivityUser.setStatus(ParticipationStatus.invited);
                activityUserRepository.save(ActivityUser);
            }

            // Create and publish Activity invite notification directly
            eventPublisher.publishEvent(
                new ActivityInviteNotificationEvent(Activity.getCreator(), Activity, new HashSet<>(ActivityCreationDTO.getInvitedFriendUserIds()))
            );

            return ActivityMapper.toDTO(Activity, creator.getId(), null, new ArrayList<>(ActivityCreationDTO.getInvitedFriendUserIds()), null);
        } catch (Exception e) {
            logger.error("Error creating Activity: " + e.getMessage());
            throw new ApplicationException("Failed to create Activity", e);
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
                        chatMessageService.getChatMessageIdsByActivityId(Activity.getId())))
                .toList();
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            @CacheEvict(value = "filteredFeedActivities", allEntries = true)
    })
    public ActivityDTO replaceActivity(ActivityDTO newActivity, UUID id) {
        return repository.findById(id).map(Activity -> {
            // Update basic Activity details
            Activity.setTitle(newActivity.getTitle());
            Activity.setNote(newActivity.getNote());
            Activity.setEndTime(newActivity.getEndTime());
            Activity.setStartTime(newActivity.getStartTime());

            // Fetch the location entity by locationId from DTO
            Activity.setLocation(locationService.getLocationEntityById(newActivity.getLocationId()));

            // Save updated Activity
            repository.save(Activity);

            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(Activity.getCreator(), Activity, activityUserRepository)
            );
            return constructDTOFromEntity(Activity);
        }).orElseGet(() -> {
            // Map and save new Activity, fetch location and creator
            Location location = locationService.getLocationEntityById(newActivity.getLocationId());
            User creator = userService.getUserEntityById(newActivity.getCreatorUserId());

            // Convert DTO to entity
            Activity ActivityEntity = ActivityMapper.toEntity(newActivity, location, creator);
            ActivityEntity = repository.save(ActivityEntity);
            
            eventPublisher.publishEvent(
                new ActivityUpdateNotificationEvent(ActivityEntity.getCreator(), ActivityEntity, activityUserRepository)
            );
            return constructDTOFromEntity(ActivityEntity);
        });
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

        return ActivityMapper.toDTO(ActivityEntity, creatorUserId, participantUserIds, invitedUserIds, chatMessageIds);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "ActivityById", key = "#result.id"),
            @CacheEvict(value = "fullActivityById", allEntries = true),
            @CacheEvict(value = "ActivitiesByOwnerId", key = "#result.creatorUserId"),
            @CacheEvict(value = "feedActivities", allEntries = true),
            @CacheEvict(value = "filteredFeedActivities", allEntries = true)
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
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivityById", key = "#ActivityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedActivities", key = "#userId"),
            @CacheEvict(value = "filteredFeedActivities", key = "#userId")
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
            @CacheEvict(value = "ActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivitiesInvitedTo", key = "#userId"),
            @CacheEvict(value = "fullActivityById", key = "#ActivityId.toString() + ':' + #userId.toString()"),
            @CacheEvict(value = "feedActivities", key = "#userId"),
            @CacheEvict(value = "filteredFeedActivities", key = "#userId")
    })
    public FullFeedActivityDTO toggleParticipation(UUID ActivityId, UUID userId) {
        ActivityUser ActivityUser = activityUserRepository.findByActivity_IdAndUser_Id(ActivityId, userId).orElseThrow(() -> new BaseNotFoundException(EntityType.ActivityUser));

        if (ActivityUser.getStatus() == ParticipationStatus.participating) {
            ActivityUser.setStatus(ParticipationStatus.invited);
        } else if (ActivityUser.getStatus().equals(ParticipationStatus.invited)) {
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
    @Cacheable(value = "ActivitiesInvitedToByFriendTagId", key = "#friendTagId.toString() + ':' + #requestingUserId")
    public List<ActivityDTO> getActivitiesInvitedToByFriendTagId(UUID friendTagId, UUID requestingUserId) {
        try {
            List<Activity> Activities = repository.getActivitiesInvitedToWithFriendTagId(friendTagId, requestingUserId);
            return getActivityDTOs(Activities);
        } catch (DataAccessException e) {
            logger.error(e.getMessage());
            throw new BaseNotFoundException(EntityType.FriendTag, friendTagId);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
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
     * An Activity is considered expired if its end time is set and is before the current time.
     *
     * @param Activities the list of Activities to filter
     * @return the modified list
     */
    private List<FullFeedActivityDTO> removeExpiredActivities(List<FullFeedActivityDTO> Activities) {
        OffsetDateTime now = OffsetDateTime.now();

        if (Activities == null) {
            return Collections.emptyList();
        }

        return Activities.stream()
                .filter(Objects::nonNull)
                .filter(Activity -> Activity.getEndTime() == null || !Activity.getEndTime().isBefore(now))
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
    @Cacheable(value = "filteredFeedActivities", key = "#friendTagFilterId")
    public List<FullFeedActivityDTO> getFilteredFeedActivitiesByFriendTagId(UUID friendTagFilterId) {
        try {
            UUID requestingUserId = friendTagService.getFriendTagById(friendTagFilterId).getOwnerUserId();
            List<FullFeedActivityDTO> ActivitiesCreated = convertActivitiesToFullFeedSelfOwnedActivities(getActivitiesByOwnerId(requestingUserId), requestingUserId);
            List<FullFeedActivityDTO> ActivitiesByFriendTagFilter = convertActivitiesToFullFeedActivities(getActivitiesInvitedToByFriendTagId(friendTagFilterId, requestingUserId), requestingUserId);

            // Remove expired Activities and sort
            return makeFeed(ActivitiesCreated, ActivitiesByFriendTagFilter);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    @Override
    public FullFeedActivityDTO getFullActivityByActivity(ActivityDTO Activity, UUID requestingUserId, Set<UUID> visitedActivities) {
        try {
            if (visitedActivities.contains(Activity.getId())) {
                return null;
            }
            visitedActivities.add(Activity.getId());

            // Safely fetch location and creator
            LocationDTO location = Activity.getLocationId() != null
                    ? locationService.getLocationById(Activity.getLocationId())
                    : null;

            UserDTO creator = userService.getUserById(Activity.getCreatorUserId());

            return new FullFeedActivityDTO(
                    Activity.getId(),
                    Activity.getTitle(),
                    Activity.getStartTime(),
                    Activity.getEndTime(),
                    location,
                    Activity.getNote(),
                    Activity.getIcon(),
                    Activity.getCategory(),
                    creator,
                    userService.getParticipantsByActivityId(Activity.getId()),
                    userService.getInvitedByActivityId(Activity.getId()),
                    chatMessageService.getFullChatMessagesByActivityId(Activity.getId()),
                    requestingUserId != null ? getFriendTagColorHexCodeForRequestingUser(Activity, requestingUserId) : null,
                    requestingUserId != null ? getParticipationStatus(Activity.getId(), requestingUserId) : null,
                    Activity.getCreatorUserId().equals(requestingUserId),
                    Activity.getCreatedAt()
            );
        } catch (BaseNotFoundException e) {
            return null;
        }
    }

    @Override
    public String getFriendTagColorHexCodeForRequestingUser(ActivityDTO ActivityDTO, UUID requestingUserId) {
        // get Activity creator from ActivityDTO

        // use creator to get the friend tag that relates the requesting user to see
        // which friend tag they've placed them in
        return Optional.ofNullable(friendTagService.getPertainingFriendTagBetweenUsers(requestingUserId, ActivityDTO.getCreatorUserId()))
                .flatMap(optional -> optional)  // This will flatten the Optional<Optional<FriendTagDTO>> to Optional<FriendTagDTO>
                .map(FriendTagDTO::getColorHexCode)
                .orElse("#8693FF"); // Default color if no tag exists or if result is null
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
            fullFeedActivity.setActivityFriendTagColorHexCodeForRequestingUser("#8693FF");

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
            OffsetDateTime now = OffsetDateTime.now();
            List<Activity> pastActivities = repository.getPastActivitiesWhereUserInvited(inviterUserId, requestingUserId, now);
            List<ActivityDTO> pastActivityDTOs = getActivityDTOs(pastActivities);
            
            // Convert to FullFeedActivityDTOs then to ProfileActivityDTOs and mark them as past Activities
            List<FullFeedActivityDTO> fullFeedActivities = convertActivitiesToFullFeedActivities(pastActivityDTOs, requestingUserId);
            List<ProfileActivityDTO> result = new ArrayList<>();
            
            // Convert each FullFeedActivityDTO to ProfileActivityDTO with isPastActivity set to true
            for (FullFeedActivityDTO fullFeedActivity : fullFeedActivities) {
                result.add(ProfileActivityDTO.fromFullFeedActivityDTO(fullFeedActivity, true));
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
            
            // If there are upcoming Activities, return them as ProfileActivityDTOs with isPastActivity = false
            if (!nonExpiredActivities.isEmpty()) {
                sortActivitiesByStartTime(nonExpiredActivities);
                for (FullFeedActivityDTO Activity : nonExpiredActivities) {
                    result.add(ProfileActivityDTO.fromFullFeedActivityDTO(Activity, false));
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
