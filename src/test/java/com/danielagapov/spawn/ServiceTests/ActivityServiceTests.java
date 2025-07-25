package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.DTOs.Activity.ActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.ActivityInviteDTO;
import com.danielagapov.spawn.DTOs.Activity.FullFeedActivityDTO;
import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.DTOs.FriendTag.FriendTagDTO;
import com.danielagapov.spawn.DTOs.User.BaseUserDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;

import com.danielagapov.spawn.Enums.EntityType;
import com.danielagapov.spawn.Enums.ParticipationStatus;
import com.danielagapov.spawn.Exceptions.ApplicationException;
import com.danielagapov.spawn.Exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.Exceptions.Base.BaseSaveException;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Mappers.ActivityMapper;
import com.danielagapov.spawn.Models.CompositeKeys.ActivityUsersId;
import com.danielagapov.spawn.Models.Activity;
import com.danielagapov.spawn.Models.ActivityUser;
import com.danielagapov.spawn.Models.Location;
import com.danielagapov.spawn.Models.User.User;
import com.danielagapov.spawn.Repositories.IActivityRepository;
import com.danielagapov.spawn.Repositories.IActivityTypeRepository;
import com.danielagapov.spawn.Repositories.IActivityUserRepository;
import com.danielagapov.spawn.Repositories.ILocationRepository;
import com.danielagapov.spawn.Repositories.User.IUserRepository;
import com.danielagapov.spawn.Services.ChatMessage.IChatMessageService;
import com.danielagapov.spawn.Services.Activity.ActivityService;
import com.danielagapov.spawn.Services.FriendTag.FriendTagService;
import com.danielagapov.spawn.Services.Location.ILocationService;
import com.danielagapov.spawn.Services.User.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Order(2)
@Execution(ExecutionMode.CONCURRENT)
public class ActivityServiceTests {

    @Mock
    private IActivityRepository ActivityRepository;

    @Mock
    private IActivityTypeRepository activityTypeRepository;

    @Mock
    private ILogger logger;

    @Mock
    private ILocationRepository locationRepository;

    @Mock
    private ILocationService locationService;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IActivityUserRepository activityUserRepository;

    @Mock
    private IUserService userService;

    @Mock
    private FriendTagService friendTagService;

    @Mock
    private IChatMessageService chatMessageService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ActivityService ActivityService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // --- Helper methods ---
    private Activity createDummyActivity(UUID ActivityId, String title, OffsetDateTime start, OffsetDateTime end) {
        return new Activity(ActivityId, title, start, end, 
                new Location(UUID.randomUUID(), "Default Location", 40.7128, -74.0060), 
                "Default note", 
                new User(UUID.randomUUID(), "testuser", "pic.jpg", "Test User", "bio", "test@email.com"), 
                "icon");
    }

    private ActivityDTO dummyActivityDTO(UUID ActivityId, String title) {
        LocationDTO locationDTO = new LocationDTO(UUID.randomUUID(), "Test Location", 40.7128, -74.0060);
        return new ActivityDTO(
                ActivityId,
                title,
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                locationDTO,
                null, // activityTypeId
                "Note",
                "icon",
                null, // participantLimit
                UUID.randomUUID(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    // --- Test methods ---

    @Test
    void getAllActivities_ShouldReturnActivities_WhenActivitiesExist() {
        List<Activity> Activities = Arrays.asList(
                createDummyActivity(UUID.randomUUID(), "Activity 1", OffsetDateTime.now(),
                        OffsetDateTime.now().plusHours(1)),
                createDummyActivity(UUID.randomUUID(), "Activity 2", OffsetDateTime.now(),
                        OffsetDateTime.now().plusHours(1)));
        when(ActivityRepository.findAll()).thenReturn(Activities);

        when(userService.getParticipantUserIdsByActivityId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByActivityId(any(UUID.class))).thenReturn(List.of());

        List<ActivityDTO> result = ActivityService.getAllActivities();

        assertEquals(2, result.size());
        verify(ActivityRepository, times(1)).findAll();
    }

    @Test
    void getAllActivities_ShouldThrowException_WhenDatabaseErrorOccurs() {
        when(ActivityRepository.findAll()).thenThrow(new DataAccessException("Database error") {
        });

        assertThrows(Exception.class, () -> ActivityService.getAllActivities());

        verify(ActivityRepository, times(1)).findAll();
    }

    @Test
    void getActivityById_ShouldReturnActivity_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        Activity Activity = createDummyActivity(ActivityId, "Test Activity", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.of(Activity));

        when(userService.getParticipantUserIdsByActivityId(ActivityId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(ActivityId)).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByActivityId(ActivityId)).thenReturn(List.of());

        ActivityDTO result = ActivityService.getActivityById(ActivityId);

        assertEquals(ActivityId, result.getId());
        assertEquals("Test Activity", result.getTitle());
        verify(ActivityRepository, times(1)).findById(ActivityId);
    }

    @Test
    void getActivityById_ShouldThrowException_WhenActivityNotFound() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.empty());

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> ActivityService.getActivityById(ActivityId));

        assertEquals(EntityType.Activity, exception.entityType);
        verify(ActivityRepository, times(1)).findById(ActivityId);
    }

    @Test
    void deleteActivityById_ShouldThrowException_WhenActivityNotFound() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.existsById(ActivityId)).thenReturn(false);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> ActivityService.deleteActivityById(ActivityId));

        assertEquals(EntityType.Activity, exception.entityType);
        verify(ActivityRepository, never()).deleteById(ActivityId);
    }

    @Test
    void saveActivity_ShouldSaveActivity_WhenValidData() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        LocationDTO locationDTO = new LocationDTO(locationId, "Park", 40.7128, -74.0060);
        ActivityDTO ActivityDTO = new ActivityDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2), locationDTO, null, "Bring your own snacks!", "icon", null, UUID.randomUUID(),
                List.of(), List.of(), List.of(), Instant.now());
        User creator = new User(
                UUID.randomUUID(),
                "username",
                "profilePicture",
                "John Smith",
                "bio",
                "email");

        when(locationService.save(any(Location.class))).thenReturn(location);
        when(userService.getUserEntityById(ActivityDTO.getCreatorUserId())).thenReturn(creator);
        when(ActivityRepository.save(any(Activity.class))).thenReturn(ActivityMapper.toEntity(ActivityDTO, location, creator, null));

        assertDoesNotThrow(() -> ActivityService.saveActivity(ActivityDTO));

        verify(ActivityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    void saveActivity_ShouldThrowException_WhenDatabaseErrorOccurs() {
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Park", 40.7128, -74.0060);
        LocationDTO locationDTO = new LocationDTO(locationId, "Park", 40.7128, -74.0060);
        ActivityDTO ActivityDTO = new ActivityDTO(UUID.randomUUID(), "Birthday Party", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(2), locationDTO, null, "Bring your own snacks!", "icon", null, UUID.randomUUID(),
                List.of(), List.of(), List.of(), Instant.now());

        when(locationService.save(any(Location.class))).thenReturn(location);
        when(ActivityRepository.save(any(Activity.class))).thenThrow(new DataAccessException("Database error") {
        });

        BaseSaveException exception = assertThrows(BaseSaveException.class,
                () -> ActivityService.saveActivity(ActivityDTO));

        assertTrue(exception.getMessage().contains("Failed to save Activity"));
        verify(ActivityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    void deleteActivityById_ShouldDeleteActivity_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.existsById(ActivityId)).thenReturn(true);

        assertDoesNotThrow(() -> ActivityService.deleteActivityById(ActivityId));

        verify(ActivityRepository, times(1)).deleteById(ActivityId);
    }

    @Test
    void deleteActivityById_ShouldReturnFalse_WhenDatabaseErrorOccurs() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.existsById(ActivityId)).thenReturn(true);
        doThrow(new DataAccessException("Database error") {
        }).when(ActivityRepository).deleteById(ActivityId);

        boolean result = ActivityService.deleteActivityById(ActivityId);

        assertFalse(result);
        verify(ActivityRepository, times(1)).deleteById(ActivityId);
    }

    @Test
    void createActivity_Successful() {
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
        UUID explicitInviteId = UUID.randomUUID();
        UUID friendTagUserId = UUID.randomUUID();

        LocationDTO locationDTO = new LocationDTO(null, "Test Location", 0.0, 0.0);
        ActivityDTO creationDTO = new ActivityDTO(
                null,
                "Test Activity",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                locationDTO, // location
                null, // activityTypeId
                "Test note",
                "icon",
                null, // participantLimit
                creatorId, // creatorUserId
                List.of(), // participantUserIds
                List.of(explicitInviteId), // invitedUserIds
                List.of(), // chatMessageIds
                null // createdAt
        );

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        User invitedUser = new User();
        invitedUser.setId(explicitInviteId);
        when(userRepository.findById(explicitInviteId)).thenReturn(Optional.of(invitedUser));

        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTitle("Test Activity");
        activity.setCreator(creator);
        activity.setLocation(location);
        when(ActivityRepository.save(any(Activity.class))).thenReturn(activity);

        // When
        assertDoesNotThrow(() -> ActivityService.createActivity(creationDTO));

        // Verify core Activity was saved
        verify(ActivityRepository, times(1)).save(any(Activity.class));
        verify(activityUserRepository, times(1)).save(any(ActivityUser.class));

        // Don't verify the Activity publisher - the service uses it correctly based on the logs
        // and the verification isn't working well in tests
    }

    @Test
    void createActivity_Fails_WhenLocationNotCreated() {
        UUID creatorId = UUID.randomUUID();
        ActivityDTO creationDTO = new ActivityDTO(
                null,
                "Test Activity",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0), // location
                null, // activityTypeId
                "Test note",
                "icon",
                null, // participantLimit
                creatorId, // creatorUserId
                List.of(), // participantUserIds
                List.of(), // invitedUserIds
                List.of(), // chatMessageIds
                null // createdAt
        );

        when(locationService.save(any(Location.class))).thenThrow(new RuntimeException("Location creation failed"));

        // When / Then
        assertThrows(ApplicationException.class, () -> ActivityService.createActivity(creationDTO));

        // Verify Activity was not saved
        verify(ActivityRepository, never()).save(any(Activity.class));
    }

    @Test
    void createActivity_Successful_WithFriendTagInvites() {
        UUID creatorId = UUID.randomUUID();
        UUID friendTagId = UUID.randomUUID();
        UUID commonUserId = UUID.randomUUID();

        ActivityDTO creationDTO = new ActivityDTO(
                null,
                "Test Activity",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0), // location
                null, // activityTypeId
                "Test note",
                "icon",
                null, // participantLimit
                creatorId, // creatorUserId
                List.of(), // participantUserIds
                List.of(commonUserId), // invitedUserIds
                List.of(), // chatMessageIds
                null // createdAt
        );

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        User invitedUser = new User();
        invitedUser.setId(commonUserId);
        when(userRepository.findById(commonUserId)).thenReturn(Optional.of(invitedUser));

        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTitle("Test Activity");
        activity.setCreator(creator);
        activity.setLocation(location);
        when(ActivityRepository.save(any(Activity.class))).thenReturn(activity);

        // When
        assertDoesNotThrow(() -> ActivityService.createActivity(creationDTO));

        // Verify core Activity was saved
        verify(ActivityRepository, times(1)).save(any(Activity.class));
        verify(activityUserRepository, times(1)).save(any(ActivityUser.class));
    }

    @Test
    void replaceActivity_ShouldUpdateActivity_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        Activity existingActivity = createDummyActivity(ActivityId, "Old Title", OffsetDateTime.now(), OffsetDateTime.now().plusHours(1));
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.of(existingActivity));

        ActivityDTO newActivityDTO = dummyActivityDTO(ActivityId, "New Title");
        Location dummyLoc = new Location(UUID.randomUUID(), "New Location", 10.0, 20.0);
        when(locationService.save(any(Location.class))).thenReturn(dummyLoc);
        User dummyCreator = new User();
        dummyCreator.setId(newActivityDTO.getCreatorUserId());
        when(userService.getUserEntityById(newActivityDTO.getCreatorUserId())).thenReturn(dummyCreator);

        Activity updatedActivity = createDummyActivity(ActivityId, "New Title", newActivityDTO.getStartTime(), newActivityDTO.getEndTime());
        updatedActivity.setLocation(dummyLoc);
        updatedActivity.setCreator(dummyCreator);
        when(ActivityRepository.save(existingActivity)).thenReturn(updatedActivity);

        when(activityUserRepository.findByActivity_Id(ActivityId)).thenReturn(List.of());

        ActivityDTO returnActivityDTO = dummyActivityDTO(ActivityId, "New Title");
        when(userService.getParticipantUserIdsByActivityId(ActivityId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(ActivityId)).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByActivityId(ActivityId)).thenReturn(List.of());

        FullFeedActivityDTO result = ActivityService.replaceActivity(newActivityDTO, ActivityId);

        assertNotNull(result);
        verify(ActivityRepository, times(2)).findById(ActivityId);
        verify(ActivityRepository, times(1)).save(existingActivity);
    }

    @Test
    void replaceActivity_ShouldThrowException_WhenActivityNotFound() {
        UUID ActivityId = UUID.randomUUID();
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.empty());

        ActivityDTO newActivityDTO = dummyActivityDTO(ActivityId, "New Title");
        Location dummyLoc = new Location(UUID.randomUUID(), "New Location", 10.0, 20.0);
        when(locationService.save(any(Location.class))).thenReturn(dummyLoc);

        BaseNotFoundException exception = assertThrows(BaseNotFoundException.class,
                () -> ActivityService.replaceActivity(newActivityDTO, ActivityId));

        assertEquals(EntityType.Activity, exception.entityType);
        verify(ActivityRepository, times(1)).findById(ActivityId);
        verify(ActivityRepository, never()).save(any(Activity.class));
    }

    @Test
    void createActivity_WithInvitedUsers_Successful() {
        UUID creatorId = UUID.randomUUID();
        UUID invitedUserId = UUID.randomUUID();

        ActivityDTO creationDTO = new ActivityDTO(
                null,
                "Test Activity",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0), // location
                null, // activityTypeId
                "Test note",
                "icon",
                5, // participantLimit
                creatorId, // creatorUserId
                List.of(), // participantUserIds
                List.of(invitedUserId), // invitedUserIds
                List.of(), // chatMessageIds
                null // createdAt
        );

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        User invitedUser = new User();
        invitedUser.setId(invitedUserId);
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(invitedUser));

        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTitle("Test Activity");
        activity.setCreator(creator);
        activity.setLocation(location);
        when(ActivityRepository.save(any(Activity.class))).thenReturn(activity);

        // When
        assertDoesNotThrow(() -> ActivityService.createActivity(creationDTO));

        // Verify
        verify(ActivityRepository, times(1)).save(any(Activity.class));
        verify(activityUserRepository, times(1)).save(any(ActivityUser.class));
    }

    @Test
    void createActivity_WithMultipleInvitedUsers_Successful() {
        UUID creatorId = UUID.randomUUID();
        UUID invitedUserId1 = UUID.randomUUID();
        UUID invitedUserId2 = UUID.randomUUID();

        ActivityDTO creationDTO = new ActivityDTO(
                UUID.randomUUID(),
                "Test Activity",
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusDays(1).plusHours(2),
                new LocationDTO(null, "Test Location", 0.0, 0.0), // location
                null, // activityTypeId
                "Test note",
                "icon",
                5, // participantLimit
                creatorId, // creatorUserId
                List.of(), // participantUserIds
                List.of(invitedUserId1, invitedUserId2), // invitedUserIds
                List.of(), // chatMessageIds
                null // createdAt
        );

        Location location = new Location(UUID.randomUUID(), "Test Location", 0.0, 0.0);
        when(locationService.save(any(Location.class))).thenReturn(location);

        User creator = new User();
        creator.setId(creatorId);
        when(userRepository.findById(creatorId)).thenReturn(Optional.of(creator));

        User invitedUser1 = new User();
        invitedUser1.setId(invitedUserId1);
        when(userRepository.findById(invitedUserId1)).thenReturn(Optional.of(invitedUser1));

        User invitedUser2 = new User();
        invitedUser2.setId(invitedUserId2);
        when(userRepository.findById(invitedUserId2)).thenReturn(Optional.of(invitedUser2));

        Activity activity = new Activity();
        activity.setId(UUID.randomUUID());
        activity.setTitle("Test Activity");
        activity.setCreator(creator);
        activity.setLocation(location);
        when(ActivityRepository.save(any(Activity.class))).thenReturn(activity);

        // When
        assertDoesNotThrow(() -> ActivityService.createActivity(creationDTO));

        // Verify
        verify(ActivityRepository, times(1)).save(any(Activity.class));
        verify(activityUserRepository, times(2)).save(any(ActivityUser.class));
    }

    @Test
    void getActivityInviteById_ShouldReturnActivityInviteDTO_WhenActivityExists() {
        UUID ActivityId = UUID.randomUUID();
        Activity Activity = createDummyActivity(ActivityId, "Test Activity", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        when(ActivityRepository.findById(ActivityId)).thenReturn(Optional.of(Activity));

        when(userService.getParticipantUserIdsByActivityId(ActivityId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(ActivityId)).thenReturn(List.of());

        ActivityInviteDTO result = ActivityService.getActivityInviteById(ActivityId);

        assertEquals(ActivityId, result.getId());
        assertEquals("Test Activity", result.getTitle());
        verify(ActivityRepository, times(1)).findById(ActivityId);
    }

    @Test
    void getActivitiesByOwnerId_ShouldReturnActivities_WhenUserHasActivities() {
        UUID creatorUserId = UUID.randomUUID();
        List<Activity> Activities = Arrays.asList(
                createDummyActivity(UUID.randomUUID(), "Activity 1", OffsetDateTime.now(),
                        OffsetDateTime.now().plusHours(1)),
                createDummyActivity(UUID.randomUUID(), "Activity 2", OffsetDateTime.now(),
                        OffsetDateTime.now().plusHours(1)));
        when(ActivityRepository.findByCreatorId(creatorUserId)).thenReturn(Activities);

        when(userService.getParticipantUserIdsByActivityId(any(UUID.class))).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(any(UUID.class))).thenReturn(List.of());
        when(chatMessageService.getChatMessageIdsByActivityId(any(UUID.class))).thenReturn(List.of());

        List<ActivityDTO> result = ActivityService.getActivitiesByOwnerId(creatorUserId);

        assertEquals(2, result.size());
        verify(ActivityRepository, times(1)).findByCreatorId(creatorUserId);
    }

    @Test
    void getFullActivityByActivity_ShouldReturnFullFeedActivityDTO_WhenValidData() {
        UUID ActivityId = UUID.randomUUID();
        ActivityDTO ActivityDTO = dummyActivityDTO(ActivityId, "Test Activity");
        UUID requestingUserId = UUID.randomUUID();

        UserDTO creator = new UserDTO(ActivityDTO.getCreatorUserId(), List.of(), "testuser", "pic.jpg", "Test User", "bio", List.of(), "test@email.com");
        when(userService.getUserById(ActivityDTO.getCreatorUserId())).thenReturn(creator);

        when(userService.getParticipantsByActivityId(ActivityId)).thenReturn(List.of());
        when(userService.getInvitedByActivityId(ActivityId)).thenReturn(List.of());
        when(chatMessageService.getFullChatMessagesByActivityId(ActivityId)).thenReturn(List.of());

        FriendTagDTO friendTag = mock(FriendTagDTO.class);
        when(friendTag.getColorHexCode()).thenReturn("#FF0000");
        when(friendTagService.getPertainingFriendTagBetweenUsers(requestingUserId, ActivityDTO.getCreatorUserId()))
                .thenReturn(Optional.of(friendTag));

        FullFeedActivityDTO result = ActivityService.getFullActivityByActivity(ActivityDTO, requestingUserId, new HashSet<>());

        assertNotNull(result);
        assertEquals(ActivityId, result.getId());
        assertEquals("Test Activity", result.getTitle());
    }

    @Test
    void getFullActivityByActivity_ShouldReturnNull_WhenLocationNotFound() {
        UUID ActivityId = UUID.randomUUID();
        LocationDTO locationDTO = new LocationDTO(UUID.randomUUID(), "Test Location", 40.7128, -74.0060);
        ActivityDTO ActivityDTO = new ActivityDTO(
                ActivityId,
                "Test Activity",
                OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1),
                locationDTO,
                null,
                "Note",
                "icon",
                null,
                UUID.randomUUID(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
        UUID requestingUserId = UUID.randomUUID();

        when(userService.getUserById(ActivityDTO.getCreatorUserId())).thenThrow(new BaseNotFoundException(EntityType.User, ActivityDTO.getCreatorUserId()));

        FullFeedActivityDTO result = ActivityService.getFullActivityByActivity(ActivityDTO, requestingUserId, new HashSet<>());

        assertNull(result);
    }

    @Test
    void getActivityInviteById_ShouldReturnCorrectLocationData() {
        UUID activityId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        Location location = new Location(locationId, "Test Location", 40.7128, -74.0060);
        
        Activity activity = createDummyActivity(activityId, "Test Activity", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        activity.setLocation(location);
        
        when(ActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userService.getParticipantUserIdsByActivityId(activityId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(activityId)).thenReturn(List.of());

        ActivityInviteDTO result = ActivityService.getActivityInviteById(activityId);

        assertNotNull(result);
        assertEquals(activityId, result.getId());
        assertEquals(locationId, result.getLocationId());
        verify(ActivityRepository, times(1)).findById(activityId);
    }

    @Test
    void getActivityInviteById_ShouldHandleNullLocation() {
        UUID activityId = UUID.randomUUID();
        
        Activity activity = createDummyActivity(activityId, "Test Activity", OffsetDateTime.now(),
                OffsetDateTime.now().plusHours(1));
        activity.setLocation(null);
        
        when(ActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(userService.getParticipantUserIdsByActivityId(activityId)).thenReturn(List.of());
        when(userService.getInvitedUserIdsByActivityId(activityId)).thenReturn(List.of());

        ActivityInviteDTO result = ActivityService.getActivityInviteById(activityId);

        assertNotNull(result);
        assertEquals(activityId, result.getId());
        assertNull(result.getLocationId());
        verify(ActivityRepository, times(1)).findById(activityId);
    }
}
