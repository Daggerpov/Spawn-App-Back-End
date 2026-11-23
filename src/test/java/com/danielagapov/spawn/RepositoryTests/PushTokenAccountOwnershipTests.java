package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.notification.api.NotificationController;
import com.danielagapov.spawn.notification.api.dto.DeviceTokenDTO;
import com.danielagapov.spawn.notification.internal.domain.DeviceToken;
import com.danielagapov.spawn.notification.internal.repositories.IDeviceTokenRepository;
import com.danielagapov.spawn.notification.internal.services.*;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.events.*;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.chat.api.dto.ChatMessageDTO;
import com.danielagapov.spawn.notification.internal.domain.NotificationPreferences;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import com.danielagapov.spawn.shared.util.*;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.services.IUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql=false)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@Import({NotificationService.class, NotificationController.class, AuthenticatedUserProvider.class})
class PushTokenAccountOwnershipTests {
    @Autowired TestEntityManager entities;
    @Autowired IDeviceTokenRepository tokens;
    @Autowired NotificationService notifications;
    @Autowired NotificationController controller;
    @MockBean IUserService users;
    @MockBean ILogger logger;
    @MockBean FCMService fcm;

    @AfterEach void clearCaller() { SecurityContextHolder.clearContext(); }

    @Test void accountSwitchMovesExistingTokenAndPreservesOtherDevices() throws Exception {
        User oldUser=user("old"), currentUser=user("current");
        DeviceToken shared=token("local-shared-token",oldUser);
        token("local-old-only-token",oldUser);
        entities.flush();
        signIn(currentUser);
        assertEquals(HttpStatus.OK,controller.registerDeviceToken(dto(shared.getToken(),currentUser)).getStatusCode());
        entities.flush();entities.clear();
        assertEquals(currentUser.getId(),tokens.findByToken(shared.getToken()).get(0).getUser().getId());
        assertEquals(1,tokens.findByUserId(oldUser.getId()).size());
        assertEquals(1,tokens.findByUserId(currentUser.getId()).size());
        clearInvocations(fcm);
        notifications.sendNotificationToUser(currentUser.getId(),"Local QA","account switch",Map.of("type","new_comment"));
        ArgumentCaptor<NotificationVO> delivered=ArgumentCaptor.forClass(NotificationVO.class);
        verify(fcm).sendMessageToToken(delivered.capture());
        assertEquals(shared.getToken(),delivered.getValue().getDeviceToken());
    }

    @Test void oldAccountCannotUnregisterTokenAfterItMovesToNewAccount() throws Exception {
        User oldUser=user("old"), currentUser=user("current");
        DeviceToken shared=token("local-shared-token",oldUser);
        entities.flush();
        signIn(currentUser);
        controller.registerDeviceToken(dto(shared.getToken(),currentUser));
        entities.flush();
        signIn(oldUser);
        controller.unregisterDeviceToken(dto(shared.getToken(),oldUser));
        entities.flush();entities.clear();
        assertEquals(List.of(currentUser.getId()),tokens.findByToken(shared.getToken()).stream().map(t->t.getUser().getId()).toList());
    }

    @Test void rejectsRegistrationForAnotherAccount() {
        User caller=user("caller"), other=user("other");
        signIn(caller);
        assertEquals(HttpStatus.FORBIDDEN,controller.registerDeviceToken(new DeviceTokenDTO("local-token",DeviceType.IOS,other.getId())).getStatusCode());
        assertTrue(tokens.findByToken("local-token").isEmpty());
    }

    @Test void rejectsUnauthenticatedRegistration() {
        User user=user("caller");
        SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.UNAUTHORIZED,controller.registerDeviceToken(new DeviceTokenDTO("local-token",DeviceType.IOS,user.getId())).getStatusCode());
        assertTrue(tokens.findByToken("local-token").isEmpty());
    }

    @Test void sameAccountRegistrationRemainsIdempotent() {
        User user=user("current");DeviceToken token=token("local-token",user);entities.flush();
        signIn(user);
        controller.registerDeviceToken(dto(token.getToken(),user));
        controller.registerDeviceToken(dto(token.getToken(),user));
        entities.flush();entities.clear();
        assertEquals(List.of(token.getId()),tokens.findByToken(token.getToken()).stream().map(DeviceToken::getId).toList());
    }

    @ParameterizedTest
    @EnumSource(value=NotificationType.class,names={"FRIEND_REQUEST","Activity_INVITE","Activity_UPDATE","NEW_COMMENT"})
    void eventTargetsTheCurrentAccountAfterSwitch(NotificationType type) throws Exception {
        User oldUser=user("old"), currentUser=user("current");
        token("local-shared-token",oldUser);entities.flush();signIn(currentUser);
        controller.registerDeviceToken(dto("local-shared-token",currentUser));
        entities.flush();entities.clear();clearInvocations(fcm);
        notifications.handleNotificationEvent(event(currentUser,type));
        ArgumentCaptor<NotificationVO> delivered=ArgumentCaptor.forClass(NotificationVO.class);
        verify(fcm).sendMessageToToken(delivered.capture());
        assertEquals("local-shared-token",delivered.getValue().getDeviceToken());
        assertEquals(event(currentUser,type).getData().get("type"),delivered.getValue().getData().get("type"));
        clearInvocations(fcm);
        notifications.handleNotificationEvent(event(oldUser,type));
        verifyNoInteractions(fcm);
    }

    @ParameterizedTest
    @EnumSource(value=NotificationType.class,names={"FRIEND_REQUEST","Activity_INVITE","Activity_UPDATE","NEW_COMMENT"})
    void respectsDisabledPreferencesAfterSwitch(NotificationType type) throws Exception {
        User oldUser=user("old"), currentUser=user("current");
        token("local-shared-token",oldUser);entities.flush();signIn(currentUser);
        controller.registerDeviceToken(dto("local-shared-token",currentUser));
        NotificationPreferences preferences=new NotificationPreferences();preferences.setUser(currentUser);
        preferences.setFriendRequestsEnabled(false);preferences.setActivityInvitesEnabled(false);
        preferences.setActivityUpdatesEnabled(false);preferences.setChatMessagesEnabled(false);
        entities.persist(preferences);entities.flush();entities.clear();clearInvocations(fcm);
        notifications.handleNotificationEvent(event(currentUser,type));
        verifyNoInteractions(fcm);
    }

    @Test void unregisterRemovesOnlyCurrentUsersSelectedToken() {
        User current=user("current"), other=user("other");
        token("local-current-one",current);token("local-current-two",current);token("local-other",other);entities.flush();signIn(current);
        assertEquals(HttpStatus.OK,controller.unregisterDeviceToken(dto("local-current-one",current)).getStatusCode());
        entities.flush();entities.clear();
        assertTrue(tokens.findByToken("local-current-one").isEmpty());
        assertEquals(1,tokens.findByUserId(current.getId()).size());
        assertEquals(1,tokens.findByUserId(other.getId()).size());
    }

    @Test void rejectsUnregisterForAnotherAccount() {
        User caller=user("caller"), other=user("other");token("local-other",other);entities.flush();signIn(caller);
        assertEquals(HttpStatus.FORBIDDEN,controller.unregisterDeviceToken(dto("local-other",other)).getStatusCode());
        assertEquals(1,tokens.findByUserId(other.getId()).size());
    }

    @Test void rejectsUnauthenticatedUnregister() {
        User user=user("current");token("local-token",user);entities.flush();SecurityContextHolder.clearContext();
        assertEquals(HttpStatus.UNAUTHORIZED,controller.unregisterDeviceToken(dto("local-token",user)).getStatusCode());
        assertEquals(1,tokens.findByUserId(user.getId()).size());
    }

    @Test void rejectsMalformedRegistrationAndUnregister() {
        User user=user("current");signIn(user);
        assertEquals(HttpStatus.BAD_REQUEST,controller.registerDeviceToken(new DeviceTokenDTO(" ",DeviceType.IOS,user.getId())).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,controller.unregisterDeviceToken(new DeviceTokenDTO(null,DeviceType.IOS,user.getId())).getStatusCode());
        assertTrue(tokens.findByUserId(user.getId()).isEmpty());
    }

    private NotificationEvent event(User user,NotificationType type) {
        User sender=new User();sender.setId(UUID.fromString("A0000000-0000-4000-8000-000000000001"));sender.setUsername("local_sender");
        Activity activity=new Activity();activity.setId(UUID.fromString("A0000000-0000-4000-8000-000000000002"));activity.setTitle("Local QA activity");
        return switch(type) {
            case FRIEND_REQUEST -> new FriendRequestNotificationEvent(sender,user.getId());
            case Activity_INVITE -> new ActivityInviteNotificationEvent(sender,activity,Set.of(user.getId()));
            case Activity_UPDATE -> new ActivityUpdateNotificationEvent(sender.getId(),sender.getUsername(),activity.getId(),activity.getTitle(),List.of(user.getId()));
            case NEW_COMMENT -> new NewCommentNotificationEvent(sender.getId(),sender.getUsername(),activity.getId(),activity.getTitle(),sender.getId(),
                    new ChatMessageDTO(UUID.fromString("A0000000-0000-4000-8000-000000000003"),"Local QA comment",java.time.Instant.EPOCH,sender.getId(),activity.getId(),List.of()),List.of(user.getId()));
            default -> throw new IllegalArgumentException("Unsupported test event");
        };
    }

    private DeviceTokenDTO dto(String token,User user) {return new DeviceTokenDTO(token,DeviceType.IOS,user.getId());}
    private User user(String label) {
        User user=new User();user.setUsername(label);user.setName(label);user.setEmail(label+"@example.com");user.setStatus(UserStatus.ACTIVE);
        entities.persist(user);when(users.getUserEntityById(user.getId())).thenReturn(user);return user;
    }
    private DeviceToken token(String value,User user) {
        DeviceToken token=new DeviceToken();token.setToken(value);token.setUser(user);token.setDeviceType(DeviceType.IOS);return entities.persist(token);
    }
    private void signIn(User user) {
        var principal=org.springframework.security.core.userdetails.User.withUsername(user.getUsername()).password("unused-local-fixture").authorities("USER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
}
