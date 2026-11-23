package com.danielagapov.spawn.ControllerTests;

import com.danielagapov.spawn.activity.api.dto.FullFeedActivityDTO;
import com.danielagapov.spawn.activity.internal.services.ActivityService;
import com.danielagapov.spawn.analytics.api.ShareLinkController;
import com.danielagapov.spawn.analytics.internal.domain.ShareLink;
import com.danielagapov.spawn.analytics.internal.services.ShareLinkService;
import com.danielagapov.spawn.chat.api.dto.FullActivityChatMessageDTO;
import com.danielagapov.spawn.shared.util.ShareLinkType;
import com.danielagapov.spawn.user.api.dto.BaseUserDTO;
import com.danielagapov.spawn.user.api.dto.UserDTO;
import com.danielagapov.spawn.user.internal.services.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The share-code lookups are public, so they must not hand out emails, invite lists or chats. */
class ShareLinkControllerTests {

    private ShareLinkService shareLinkService;
    private ActivityService activityService;
    private UserService userService;
    private ShareLinkController controller;

    @BeforeEach
    void setUp() {
        shareLinkService = mock(ShareLinkService.class);
        activityService = mock(ActivityService.class);
        userService = mock(UserService.class);
        controller = new ShareLinkController(shareLinkService, activityService, userService,
                new ObjectMapper().findAndRegisterModules());
    }

    private static ShareLink link(UUID targetId, ShareLinkType type) {
        ShareLink link = new ShareLink();
        link.setShareCode("gentle-rose");
        link.setTargetId(targetId);
        link.setType(type);
        return link;
    }

    private static BaseUserDTO user(String username) {
        return new BaseUserDTO(UUID.randomUUID(), username, username + "@example.com", username, "", null);
    }

    @Test
    void activityLookupLeavesOutEmailsInvitesAndChat() {
        UUID activityId = UUID.randomUUID();
        BaseUserDTO creator = user("creator");
        FullFeedActivityDTO activity = new FullFeedActivityDTO(activityId, "Pickup soccer", null, null, null, null,
                null, "⚽", null, creator, List.of(user("participant")), List.of(user("invitee")),
                List.of(new FullActivityChatMessageDTO(UUID.randomUUID(), "private", Instant.now(), creator, activityId, List.of())),
                null, false, Instant.now(), false, null);
        when(shareLinkService.resolveShareCode("gentle-rose")).thenReturn(Optional.of(link(activityId, ShareLinkType.ACTIVITY)));
        when(activityService.getFullActivityById(eq(activityId), any())).thenReturn(activity);

        ResponseEntity<JsonNode> response = controller.resolveActivityShareCode("gentle-rose");

        JsonNode body = response.getBody();
        assertNotNull(body);
        assertEquals(activityId.toString(), body.get("id").asText());
        assertEquals("creator", body.get("creatorUser").get("username").asText());
        assertFalse(body.get("creatorUser").has("email"));
        assertFalse(body.get("participantUsers").get(0).has("email"));
        assertFalse(body.has("invitedUsers"));
        assertFalse(body.has("chatMessages"));
        // The cached DTO itself is untouched
        assertEquals("creator@example.com", activity.getCreatorUser().getEmail());
        assertEquals(1, activity.getChatMessages().size());
    }

    @Test
    void profileLookupLeavesOutEmail() {
        UUID userId = UUID.randomUUID();
        UserDTO user = new UserDTO(userId, List.of(), "rose", null, "Rose", "", "rose@example.com", true);
        when(shareLinkService.resolveShareCode("gentle-rose")).thenReturn(Optional.of(link(userId, ShareLinkType.PROFILE)));
        when(userService.getUserById(userId)).thenReturn(user);

        BaseUserDTO body = controller.resolveProfileShareCode("gentle-rose").getBody();

        assertNotNull(body);
        assertEquals(userId, body.getId());
        assertEquals("rose", body.getUsername());
        assertNull(body.getEmail());
    }
}
