package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.activity.api.ActivityController;
import com.danielagapov.spawn.activity.api.dto.*;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.shared.util.EntityType;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.exceptions.ActivityFullException;
import com.danielagapov.spawn.shared.exceptions.Base.BaseNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Base.BasesNotFoundException;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.activity.api.IActivityService;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive unit tests for ActivityController
 * Tests all API endpoints for activity management, feed, participation, etc.
 */
@ExtendWith(MockitoExtension.class)
class ActivityControllerTests {

    @Mock
    private IActivityService activityService;

    @Mock
    private ILogger logger;

    @InjectMocks
    private ActivityController activityController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID activityId;
    private UUID userId;
    private UUID creatorId;
    private LocationDTO locationDTO;
    private ActivityDTO activityDTO;
    private FullFeedActivityDTO fullFeedActivityDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        mockMvc = MockMvcBuilders.standaloneSetup(activityController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
        
        activityId = UUID.randomUUID();
        userId = UUID.randomUUID();
        creatorId = UUID.randomUUID();
        
        locationDTO = new LocationDTO(UUID.randomUUID(), "Test Location", 40.7128, -74.0060);
        
        activityDTO = new ActivityDTO(
            activityId,
            "Test Activity",
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2),
            locationDTO,
            null,
            "Test note",
            "🎉",
            5,
            creatorId,
            List.of(),
            List.of(),
            List.of(),
            Instant.now(),
            false,
            "America/New_York"
        );
        
        // BaseUserDTO(UUID id, String username, String profilePictureUrlString, String name, String email, String bio)
        BaseUserDTO creator = new BaseUserDTO(creatorId, "testuser", "pic.jpg", "Test User", "test@example.com", "bio");
        
        // FullFeedActivityDTO constructor:
        // (UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, LocationDTO location,
        //  UUID activityTypeId, String note, String icon, Integer participantLimit, BaseUserDTO creatorUser,
        //  List<BaseUserDTO> participantUsers, List<BaseUserDTO> invitedUsers, List<FullActivityChatMessageDTO> chatMessages,
        //  ParticipationStatus participationStatus, boolean isSelfOwned, Instant createdAt, boolean isExpired, String clientTimezone)
        fullFeedActivityDTO = new FullFeedActivityDTO(
            activityId,
            "Test Activity",
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2),
            locationDTO,
            null,  // activityTypeId
            "Test note",
            "🎉",
            5,
            creator,
            List.of(),  // participantUsers
            List.of(),  // invitedUsers
            List.of(),  // chatMessages
            ParticipationStatus.participating,  // participationStatus
            false,  // isSelfOwned
            Instant.now(),
            false,  // isExpired
            "America/New_York"
        );
    }

    // MARK: - GET Profile Activities Tests

    @Test
    void getProfileActivities_ShouldReturnActivities_WhenValidRequest() throws Exception {
        UUID profileUserId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        
        // Create a proper ProfileActivityDTO using the full constructor
        BaseUserDTO creator = new BaseUserDTO(creatorId, "testuser", "pic.jpg", "Test User", "test@example.com", "bio");
        ProfileActivityDTO profileActivity = new ProfileActivityDTO(
            activityId,
            "Test Activity",
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2),
            locationDTO,
            "Test note",
            "🎉",
            5,
            creator,
            List.of(),  // participantUsers
            List.of(),  // invitedUsers
            List.of(),  // chatMessageIds
            Instant.now(),
            false,  // isExpired
            "America/New_York",
            false  // isPastActivity
        );
        List<ProfileActivityDTO> activities = List.of(profileActivity);
        
        when(activityService.getProfileActivities(profileUserId, requestingUserId)).thenReturn(activities);

        mockMvc.perform(get("/api/v1/activities/profile/{profileUserId}", profileUserId)
                .param("requestingUserId", requestingUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activityId.toString()));

        verify(activityService, times(1)).getProfileActivities(profileUserId, requestingUserId);
    }

    @Test
    void getProfileActivities_ShouldReturnBadRequest_WhenNullParameters() throws Exception {
        UUID profileUserId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/activities/profile/{profileUserId}", profileUserId))
                .andExpect(status().isBadRequest());

        verify(activityService, never()).getProfileActivities(any(), any());
    }

    @Test
    void getProfileActivities_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        UUID profileUserId = UUID.randomUUID();
        UUID requestingUserId = UUID.randomUUID();
        
        when(activityService.getProfileActivities(profileUserId, requestingUserId))
                .thenThrow(new BaseNotFoundException(EntityType.User, profileUserId));

        mockMvc.perform(get("/api/v1/activities/profile/{profileUserId}", profileUserId)
                .param("requestingUserId", requestingUserId.toString()))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found"));
    }

    // MARK: - POST Create Activity Tests

    @Test
    void createActivity_ShouldReturnCreated_WhenValidActivity() throws Exception {
        when(activityService.createActivityWithSuggestions(any(ActivityDTO.class)))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(post("/api/v1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Test Activity"));

        verify(activityService, times(1)).createActivityWithSuggestions(any(ActivityDTO.class));
    }

    @Test
    void createActivity_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        when(activityService.createActivityWithSuggestions(any(ActivityDTO.class)))
                .thenThrow(new IllegalArgumentException("Invalid activity data"));

        mockMvc.perform(post("/api/v1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isBadRequest());

        verify(logger, times(1)).error(contains("Invalid request for activity creation"));
    }

    @Test
    void createActivity_ShouldReturnNotFound_WhenEntityNotFound() throws Exception {
        when(activityService.createActivityWithSuggestions(any(ActivityDTO.class)))
                .thenThrow(new BaseNotFoundException(EntityType.User, creatorId));

        mockMvc.perform(post("/api/v1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Entity not found during activity creation"));
    }

    // MARK: - PUT Replace Activity Tests

    @Test
    void replaceActivity_ShouldReturnOk_WhenActivityReplaced() throws Exception {
        when(activityService.replaceActivity(any(ActivityDTO.class), eq(activityId)))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(put("/api/v1/activities/{id}", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()));

        verify(activityService, times(1)).replaceActivity(any(ActivityDTO.class), eq(activityId));
    }

    @Test
    void replaceActivity_ShouldReturnNotFound_WhenInvalidId() throws Exception {
        // Note: Passing null to a path variable in MockMvc results in a malformed URL,
        // which Spring interprets as a 404 or 405 (not a 400 from our controller logic).
        // Testing with an invalid UUID format instead.
        mockMvc.perform(put("/api/v1/activities/invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replaceActivity_ShouldReturnNotFound_WhenActivityNotFound() throws Exception {
        when(activityService.replaceActivity(any(ActivityDTO.class), eq(activityId)))
                .thenThrow(new BaseNotFoundException(EntityType.Activity, activityId));

        mockMvc.perform(put("/api/v1/activities/{id}", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(activityDTO)))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Activity not found for replacement"));
    }

    // MARK: - PATCH Partial Update Activity Tests

    @Test
    void partialUpdateActivity_ShouldReturnOk_WhenValidUpdate() throws Exception {
        ActivityPartialUpdateDTO updates = new ActivityPartialUpdateDTO();
        updates.setTitle("Updated Title");
        
        when(activityService.partialUpdateActivity(any(ActivityPartialUpdateDTO.class), eq(activityId)))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(patch("/api/v1/activities/{id}/partial", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());

        verify(activityService, times(1)).partialUpdateActivity(any(ActivityPartialUpdateDTO.class), eq(activityId));
    }

    @Test
    void partialUpdateActivity_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        ActivityPartialUpdateDTO updates = new ActivityPartialUpdateDTO();
        updates.setStartTime(OffsetDateTime.now().minusDays(1).toString()); // Past date
        
        when(activityService.partialUpdateActivity(any(ActivityPartialUpdateDTO.class), eq(activityId)))
                .thenThrow(new IllegalArgumentException("Activity start time cannot be in the past"));

        mockMvc.perform(patch("/api/v1/activities/{id}/partial", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isBadRequest());

        verify(logger, times(1)).error(contains("Invalid update data"));
    }

    // MARK: - DELETE Activity Tests

    @Test
    void deleteActivity_ShouldReturnNoContent_WhenSuccessful() throws Exception {
        when(activityService.deleteActivityById(activityId)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/activities/{id}", activityId))
                .andExpect(status().isNoContent());

        verify(activityService, times(1)).deleteActivityById(activityId);
    }

    @Test
    void deleteActivity_ShouldReturnNotFound_WhenActivityNotFound() throws Exception {
        when(activityService.deleteActivityById(activityId))
                .thenThrow(new BaseNotFoundException(EntityType.Activity, activityId));

        mockMvc.perform(delete("/api/v1/activities/{id}", activityId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("Activity not found for deletion"));
    }

    @Test
    void deleteActivity_ShouldReturnInternalServerError_WhenDeletionFails() throws Exception {
        when(activityService.deleteActivityById(activityId)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/activities/{id}", activityId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Failed to delete activity"));
    }

    // MARK: - PUT Toggle Participation Tests

    @Test
    void toggleParticipation_ShouldReturnOk_WhenSuccessful() throws Exception {
        when(activityService.toggleParticipation(activityId, userId))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(put("/api/v1/activities/{activityId}/toggle-status/{userId}", activityId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()));

        verify(activityService, times(1)).toggleParticipation(activityId, userId);
    }

    @Test
    void toggleParticipation_ShouldReturnBadRequest_WhenInvalidUuid() throws Exception {
        // Note: Passing null to a path variable in MockMvc results in a malformed URL,
        // which Spring interprets as a 404 (not a 400 from our controller logic).
        // Testing with an invalid UUID format instead.
        mockMvc.perform(put("/api/v1/activities/{activityId}/toggle-status/invalid-uuid", activityId))
                .andExpect(status().isBadRequest());

        verify(activityService, never()).toggleParticipation(any(), any());
    }

    @Test
    void toggleParticipation_ShouldReturnBadRequest_WhenActivityFull() throws Exception {
        // ActivityFullException(UUID activityId, Integer participantLimit)
        when(activityService.toggleParticipation(activityId, userId))
                .thenThrow(new ActivityFullException(activityId, 5));

        mockMvc.perform(put("/api/v1/activities/{activityId}/toggle-status/{userId}", activityId, userId))
                .andExpect(status().isBadRequest());

        verify(logger, times(1)).error(contains("Activity is full"));
    }

    @Test
    void toggleParticipation_ShouldReturnNotFound_WhenUserNotInvited() throws Exception {
        when(activityService.toggleParticipation(activityId, userId))
                .thenThrow(new BaseNotFoundException(EntityType.ActivityUser, activityId));

        mockMvc.perform(put("/api/v1/activities/{activityId}/toggle-status/{userId}", activityId, userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not invited to activity"));
    }

    // MARK: - GET Feed Activities Tests

    @Test
    void getFeedActivities_ShouldReturnActivities_WhenValidRequest() throws Exception {
        List<FullFeedActivityDTO> feedActivities = List.of(fullFeedActivityDTO);
        when(activityService.getFeedActivities(userId)).thenReturn(feedActivities);

        mockMvc.perform(get("/api/v1/activities/feed-activities/{requestingUserId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(activityId.toString()));

        verify(activityService, times(1)).getFeedActivities(userId);
    }

    @Test
    void getFeedActivities_ShouldReturnEmptyList_WhenNoActivitiesFound() throws Exception {
        when(activityService.getFeedActivities(userId))
                .thenThrow(new BasesNotFoundException(EntityType.Activity));

        mockMvc.perform(get("/api/v1/activities/feed-activities/{requestingUserId}", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(activityService, times(1)).getFeedActivities(userId);
    }

    @Test
    void getFeedActivities_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        when(activityService.getFeedActivities(userId))
                .thenThrow(new BaseNotFoundException(EntityType.User, userId));

        mockMvc.perform(get("/api/v1/activities/feed-activities/{requestingUserId}", userId))
                .andExpect(status().isNotFound());

        verify(logger, times(1)).error(contains("User not found for feed activities"));
    }

    // MARK: - GET Full Activity By ID Tests

    @Test
    void getFullActivityById_ShouldReturnActivity_WhenValidRequest() throws Exception {
        when(activityService.getFullActivityById(activityId, userId))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(get("/api/v1/activities/{id}", activityId)
                .param("requestingUserId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()))
                .andExpect(jsonPath("$.title").value("Test Activity"));

        verify(activityService, times(1)).getFullActivityById(activityId, userId);
    }

    @Test
    void getFullActivityById_ShouldReturnActivityInvite_WhenExternalInvite() throws Exception {
        // ActivityInviteDTO constructor:
        // (UUID id, String title, OffsetDateTime startTime, OffsetDateTime endTime, UUID locationId,
        //  UUID activityTypeId, String note, String icon, Integer participantLimit, UUID creatorUserId,
        //  List<UUID> participantUserIds, List<UUID> invitedUserIds, Instant createdAt, boolean isExpired, String clientTimezone)
        ActivityInviteDTO inviteDTO = new ActivityInviteDTO(
            activityId,
            "Test Activity",
            OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2),
            locationDTO.getId(),
            null,  // activityTypeId
            "Test note",
            "🎉",
            5,
            creatorId,
            List.of(),  // participantUserIds
            List.of(),  // invitedUserIds
            Instant.now(),
            false,  // isExpired
            "America/New_York"
        );
        
        when(activityService.getActivityInviteById(activityId)).thenReturn(inviteDTO);

        mockMvc.perform(get("/api/v1/activities/{id}", activityId)
                .param("isActivityExternalInvite", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()));

        verify(activityService, times(1)).getActivityInviteById(activityId);
        verify(activityService, never()).getFullActivityById(any(), any());
    }

    @Test
    void getFullActivityById_ShouldAutoJoin_WhenAutoJoinTrue() throws Exception {
        when(activityService.autoJoinUserToActivity(activityId, userId))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(get("/api/v1/activities/{id}", activityId)
                .param("requestingUserId", userId.toString())
                .param("autoJoin", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId.toString()));

        verify(activityService, times(1)).autoJoinUserToActivity(activityId, userId);
        verify(activityService, never()).getFullActivityById(any(), any());
    }

    @Test
    void getFullActivityById_ShouldReturnEmptyList_WhenActivityNotFound() throws Exception {
        when(activityService.getFullActivityById(activityId, userId))
                .thenThrow(new BaseNotFoundException(EntityType.Activity, activityId));

        mockMvc.perform(get("/api/v1/activities/{id}", activityId)
                .param("requestingUserId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));

        verify(activityService, times(1)).getFullActivityById(activityId, userId);
    }

    // MARK: - GET Chat Messages Tests

    @Test
    void getChatMessagesForActivity_ShouldReturnMessages_WhenValidRequest() throws Exception {
        List<FullActivityChatMessageDTO> messages = List.of();
        when(activityService.getChatMessagesByActivityId(activityId)).thenReturn(messages);

        mockMvc.perform(get("/api/v1/activities/{activityId}/chats", activityId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

        verify(activityService, times(1)).getChatMessagesByActivityId(activityId);
    }

    @Test
    void getChatMessagesForActivity_ShouldReturnInternalServerError_WhenServiceFails() throws Exception {
        when(activityService.getChatMessagesByActivityId(activityId))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/v1/activities/{activityId}/chats", activityId))
                .andExpect(status().isInternalServerError());

        verify(logger, times(1)).error(contains("Error getting chat messages"));
    }

    // MARK: - Direct Controller Method Tests

    @Test
    void createActivity_DirectCall_ShouldReturnCreated_WhenSuccessful() {
        when(activityService.createActivityWithSuggestions(any(ActivityDTO.class)))
                .thenReturn(fullFeedActivityDTO);

        ResponseEntity<ActivityCreationResponseDTO> response = activityController.createActivity(activityDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getActivity());
        assertEquals(activityId, response.getBody().getActivity().getId());
        verify(activityService, times(1)).createActivityWithSuggestions(any(ActivityDTO.class));
    }

    @Test
    void toggleParticipation_DirectCall_ShouldReturnOk_WhenSuccessful() {
        when(activityService.toggleParticipation(activityId, userId))
                .thenReturn(fullFeedActivityDTO);

        ResponseEntity<?> response = activityController.toggleParticipation(activityId, userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(activityService, times(1)).toggleParticipation(activityId, userId);
    }

    @Test
    void getFeedActivities_DirectCall_ShouldReturnOk_WhenSuccessful() {
        List<FullFeedActivityDTO> feedActivities = List.of(fullFeedActivityDTO);
        when(activityService.getFeedActivities(userId)).thenReturn(feedActivities);

        ResponseEntity<?> response = activityController.getFeedActivities(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(activityService, times(1)).getFeedActivities(userId);
    }

    // MARK: - Edge Case Tests

    @Test
    void createActivity_ShouldHandleLargeParticipantList_WhenManyInvites() throws Exception {
        List<UUID> largeInviteList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            largeInviteList.add(UUID.randomUUID());
        }
        
        ActivityDTO largeActivity = new ActivityDTO(
            null, "Large Activity", OffsetDateTime.now().plusDays(1),
            OffsetDateTime.now().plusDays(1).plusHours(2), locationDTO, null,
            "Large note", "🎉", 100, creatorId, List.of(), largeInviteList, List.of(),
            Instant.now(), false, "America/New_York"
        );
        
        when(activityService.createActivityWithSuggestions(any(ActivityDTO.class)))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(post("/api/v1/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largeActivity)))
                .andExpect(status().isCreated());

        verify(activityService, times(1)).createActivityWithSuggestions(any(ActivityDTO.class));
    }

    @Test
    void partialUpdateActivity_ShouldHandleMultipleFieldUpdates_WhenComplexUpdate() throws Exception {
        ActivityPartialUpdateDTO updates = new ActivityPartialUpdateDTO();
        updates.setTitle("New Title");
        updates.setNote("New Note");
        updates.setParticipantLimit(10);
        
        when(activityService.partialUpdateActivity(any(ActivityPartialUpdateDTO.class), eq(activityId)))
                .thenReturn(fullFeedActivityDTO);

        mockMvc.perform(patch("/api/v1/activities/{id}/partial", activityId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());

        verify(activityService, times(1)).partialUpdateActivity(any(ActivityPartialUpdateDTO.class), eq(activityId));
    }

    @Test
    void getFullActivityById_ShouldReturnBadRequest_WhenMissingRequestingUserId() throws Exception {
        mockMvc.perform(get("/api/v1/activities/{id}", activityId))
                .andExpect(status().isBadRequest());

        verify(activityService, never()).getFullActivityById(any(), any());
    }

    @Test
    void getActivitiesCreatedByUserId_ShouldReturnActivities_WhenUserHasActivities() throws Exception {
        List<FullFeedActivityDTO> activities = List.of(fullFeedActivityDTO);
        when(activityService.getActivitiesByOwnerId(creatorId)).thenReturn(List.of(activityDTO));
        when(activityService.convertActivitiesToFullFeedSelfOwnedActivities(any(), eq(creatorId)))
                .thenReturn(activities);

        mockMvc.perform(get("/api/v1/activities/user/{creatorUserId}", creatorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(activityService, times(1)).getActivitiesByOwnerId(creatorId);
    }
}
