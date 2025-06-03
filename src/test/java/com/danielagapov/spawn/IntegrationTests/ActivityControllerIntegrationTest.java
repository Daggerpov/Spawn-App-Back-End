package com.danielagapov.spawn.IntegrationTests;

import com.danielagapov.spawn.DTOs.Activity.ActivityCreationDTO;
import com.danielagapov.spawn.DTOs.Activity.LocationDTO;
import com.danielagapov.spawn.DTOs.User.UserDTO;
import com.danielagapov.spawn.Enums.ActivityCategory;
import com.danielagapov.spawn.Services.User.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
public class ActivityControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private IUserService userService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // Create test user with correct constructor
        testUser = new UserDTO(
                UUID.randomUUID(),
                List.of(), // friendUserIds
                "testuser",
                "avatar.jpg",
                "Test User",
                "Test bio",
                List.of(), // friendTagIds
                "testuser@example.com"
        );
    }

    @Test
    void testCreateActivity() throws Exception {
        LocationDTO location = new LocationDTO(
                UUID.randomUUID(),
                "Test Location",
                49.2827,
                -123.1207
        );

        ActivityCreationDTO activityCreationDTO = new ActivityCreationDTO(
                UUID.randomUUID(),
                "Test Activity",
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(3),
                location,
                "Test activity note",
                "🎉",
                ActivityCategory.GENERAL,
                testUser.getId(),
                List.of(), // No invited friends for simplicity
                null // createdAt
        );

        mockMvc.perform(post("/api/v1/Activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityCreationDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Activity"))
                .andExpect(jsonPath("$.note").value("Test activity note"))
                .andExpect(jsonPath("$.creatorUserId").value(testUser.getId().toString()));
    }

    @Test
    void testGetFeedActivities() throws Exception {
        // First create an activity through the service
        LocationDTO location = new LocationDTO(
                UUID.randomUUID(),
                "Feed Test Location",
                49.2827,
                -123.1207
        );

        ActivityCreationDTO activityCreationDTO = new ActivityCreationDTO(
                UUID.randomUUID(),
                "Feed Test Activity",
                OffsetDateTime.now().plusHours(1),
                OffsetDateTime.now().plusHours(3),
                location,
                "Feed test activity note",
                "🍽️",
                ActivityCategory.FOOD_AND_DRINK,
                testUser.getId(),
                List.of(),
                null // createdAt
        );

        // Create activity first
        mockMvc.perform(post("/api/v1/Activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(activityCreationDTO)))
                .andExpect(status().isCreated());

        // Then test getting feed activities
        mockMvc.perform(get("/api/v1/Activities/feedActivities/{requestingUserId}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetFeedActivitiesWithInvalidUserId() throws Exception {
        mockMvc.perform(get("/api/v1/Activities/feedActivities/{requestingUserId}", UUID.randomUUID()))
                .andExpect(status().isOk()) // Should return empty array for non-existent user
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetFeedActivitiesWithNullUserId() throws Exception {
        mockMvc.perform(get("/api/v1/Activities/feedActivities/null"))
                .andExpect(status().isBadRequest());
    }
} 