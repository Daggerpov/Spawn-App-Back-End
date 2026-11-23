package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.activity.internal.domain.ActivityType;
import com.danielagapov.spawn.activity.internal.repositories.IActivityTypeRepository;
import com.danielagapov.spawn.auth.internal.domain.EmailVerification;
import com.danielagapov.spawn.auth.internal.domain.UserIdExternalIdMap;
import com.danielagapov.spawn.auth.internal.repositories.IEmailVerificationRepository;
import com.danielagapov.spawn.auth.internal.repositories.IUserIdExternalIdMapRepository;
import com.danielagapov.spawn.auth.internal.services.IAuthService;
import com.danielagapov.spawn.media.internal.services.IS3Service;
import com.danielagapov.spawn.notification.internal.domain.NotificationPreferences;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.AuthenticatedUserProvider;
import com.danielagapov.spawn.shared.util.OAuthProvider;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.social.internal.services.IBlockedUserService;
import com.danielagapov.spawn.social.internal.services.IFriendRequestService;
import com.danielagapov.spawn.user.api.UserController;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.user.internal.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Execution(ExecutionMode.SAME_THREAD)
@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserService.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserDeletionIntegrationTests {
    @Autowired TestEntityManager entities;
    @Autowired IUserRepository users;
    @Autowired IActivityTypeRepository types;
    @Autowired IEmailVerificationRepository verifications;
    @Autowired IUserIdExternalIdMapRepository oauthMappings;
    @Autowired IUserService service;
    @MockBean IS3Service storage;
    @MockBean ILogger logger;
    @MockBean IUserSearchQueryService search;
    @MockBean IUserFriendshipQueryService queries;
    @MockBean IFriendRequestService requests;
    @MockBean CacheManager caches;

    @BeforeEach
    void configureDependencies() {
        when(queries.getFriendUserIdsByUserId(any())).thenReturn(List.of());
        when(caches.getCache(anyString())).thenAnswer(call -> new ConcurrentMapCache(call.getArgument(0)));
    }

    @Test
    void deletesAccountWithLinkedEmailVerification() {
        User user = user("deleting");
        verification(user, "deleting-code");
        User survivor = user("survivor");
        verification(survivor, "survivor-code");
        commitFixture();
        assertDeleted(user);
        assertFalse(verifications.existsByEmail(user.getEmail()));
        assertTrue(verifications.existsByEmail(survivor.getEmail()));
    }

    @Test
    void removesOnlyDeletedAccountFromAnotherUsersType() {
        User owner = user("owner");
        User member = user("member");
        User survivor = user("survivor");
        ActivityType type = type(owner, member, survivor);
        commitFixture();
        assertDeleted(member);
        var remaining = types.findActivityTypesByCreatorId(owner.getId());
        assertEquals(List.of(type.getId()), remaining.stream().map(ActivityType::getId).toList());
        assertEquals(List.of(survivor.getId()), remaining.get(0).getAssociatedFriends().stream().map(User::getId).toList());
        assertTrue(users.existsById(owner.getId()));
        assertTrue(users.existsById(survivor.getId()));
    }

    @Test
    void deletesOwnedTypesWithoutDeletingTheirMembers() {
        User owner = user("owner");
        User member = user("member");
        type(owner, member);
        commitFixture();
        assertDeleted(owner);
        assertEquals(0L, types.countByCreatorId(owner.getId()));
        assertTrue(users.existsById(member.getId()));
    }

    @Test
    void deletesCombinedReferencesAndExistingCascades() {
        User user = user("deleting");
        User survivor = user("survivor");
        type(user, survivor);
        type(survivor, user);
        verification(user, "qa-code");
        entities.persist(new UserIdExternalIdMap("local-oauth-id", user, OAuthProvider.google));
        NotificationPreferences preferences = new NotificationPreferences();
        preferences.setUser(user);
        entities.persist(preferences);
        commitFixture();
        assertDeleted(user);
        assertFalse(oauthMappings.existsById("local-oauth-id"));
        assertFalse(verifications.existsByEmail(user.getEmail()));
        assertTrue(users.existsById(survivor.getId()));
        assertTrue(types.findActivityTypesByCreatorId(survivor.getId()).get(0).getAssociatedFriends().isEmpty());
    }

    @Test
    void failureRollsBackMembershipAndVerificationCleanup() {
        User user = user("deleting");
        user.setProfilePictureUrlString("https://example.com/qa-picture");
        User survivor = user("survivor");
        type(survivor, user);
        verification(user, "qa-code");
        commitFixture();
        doThrow(new IllegalStateException("Local storage failure")).when(storage).deleteObjectByURL(anyString());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller(Optional.of(user.getId())).deleteUser(user.getId()).getStatusCode());
        assertTrue(users.existsById(user.getId()));
        assertTrue(verifications.existsByEmail(user.getEmail()));
        assertEquals(List.of(user.getId()), types.findActivityTypesByCreatorId(survivor.getId()).get(0).getAssociatedFriends().stream().map(User::getId).toList());
    }

    @Test
    void rejectsUnauthenticatedAndOtherUsersWithoutRemovingData() {
        User user = user("deleting");
        User survivor = user("survivor");
        type(survivor, user);
        verification(user, "qa-code");
        commitFixture();
        assertEquals(HttpStatus.UNAUTHORIZED, controller(Optional.empty()).deleteUser(user.getId()).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, controller(Optional.of(survivor.getId())).deleteUser(user.getId()).getStatusCode());
        assertTrue(users.existsById(user.getId()));
        assertTrue(verifications.existsByEmail(user.getEmail()));
        assertEquals(1, types.findActivityTypesByCreatorId(survivor.getId()).get(0).getAssociatedFriends().size());
        verifyNoInteractions(storage);
    }

    @Test
    void deletesAccountWithoutReferences() {
        User user = user("deleting");
        commitFixture();
        assertDeleted(user);
    }

    private void assertDeleted(User user) {
        assertEquals(HttpStatus.NO_CONTENT, controller(Optional.of(user.getId())).deleteUser(user.getId()).getStatusCode());
        assertFalse(users.existsById(user.getId()));
    }

    private UserController controller(Optional<UUID> userId) {
        AuthenticatedUserProvider caller = mock(AuthenticatedUserProvider.class);
        when(caller.getCurrentUserId()).thenReturn(userId);
        return new UserController(service, storage, logger, mock(IAuthService.class), mock(IBlockedUserService.class),
                mock(IRecentlySpawnedService.class), requests, caller);
    }

    private ActivityType type(User owner, User... members) {
        ActivityType type = new ActivityType(owner, "QA type", "⭐");
        type.setOrderNum(0);
        type.setAssociatedFriends(new ArrayList<>(List.of(members)));
        return entities.persist(type);
    }

    private void verification(User user, String code) {
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setEmail(user.getEmail());
        verification.setVerificationCode(code);
        entities.persist(verification);
    }

    private void commitFixture() {
        entities.flush();
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    private User user(String label) {
        User user = new User();
        user.setUsername(label);
        user.setName(label);
        user.setEmail(label + "@example.com");
        user.setStatus(UserStatus.ACTIVE);
        return entities.persist(user);
    }
}
