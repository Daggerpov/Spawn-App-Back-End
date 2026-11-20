package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.activity.api.dto.UserIdActivityTimeDTO;
import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.ActivityUser;
import com.danielagapov.spawn.activity.internal.domain.ActivityUsersId;
import com.danielagapov.spawn.activity.internal.domain.Location;
import com.danielagapov.spawn.activity.internal.repositories.IActivityUserRepository;
import com.danielagapov.spawn.shared.util.ParticipationStatus;
import com.danielagapov.spawn.shared.util.UserStatus;
import com.danielagapov.spawn.user.internal.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RecentlySpawnedActivityQueriesTests {
    @Autowired private TestEntityManager entities;
    @Autowired private IActivityUserRepository repository;
    private final OffsetDateTime now = OffsetDateTime.of(2026, 11, 20, 18, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void joinedActivityIncludesHostWithoutParticipationRowAndExcludesInvitees() {
        User host = user("host");
        User guest = user("guest");
        User invitee = user("invitee");
        Activity activity = activity(host, now.minusDays(1));
        participation(activity, guest, ParticipationStatus.participating);
        participation(activity, invitee, ParticipationStatus.invited);
        entities.flush();

        var ids = repository.findPastActivityIdsForUser(guest.getId(), ParticipationStatus.participating, now, Limit.of(10));
        assertEquals(List.of(activity.getId()), ids);
        var people = repository.findOtherUserIdsByActivityIds(ids, guest.getId(), ParticipationStatus.participating);
        assertEquals(List.of(host.getId()), people.stream().map(UserIdActivityTimeDTO::getUserId).toList());
    }

    @Test
    void hostSeesGuestsOfOwnCompletedActivitiesWithoutParticipationRow() {
        User host = user("host");
        User guest = user("guest");
        Activity completed = activity(host, now.minusDays(1));
        participation(completed, guest, ParticipationStatus.participating);
        activity(host, now.plusDays(1));
        entities.flush();

        var ids = repository.findPastActivityIdsForUser(host.getId(), ParticipationStatus.participating, now, Limit.of(10));
        assertEquals(List.of(completed.getId()), ids);
        var people = repository.findOtherUserIdsByActivityIds(ids, host.getId(), ParticipationStatus.participating);
        assertEquals(List.of(guest.getId()), people.stream().map(UserIdActivityTimeDTO::getUserId).toList());
    }

    @Test
    void duplicateHostAndParticipantAppearOnceAtMostRecentSharedActivity() {
        User requester = user("requester");
        User other = user("other");
        Activity older = activity(other, now.minusDays(3));
        participation(older, requester, ParticipationStatus.participating);
        participation(older, other, ParticipationStatus.participating);
        Activity recent = activity(requester, now.minusDays(1));
        participation(recent, other, ParticipationStatus.participating);
        entities.flush();

        var ids = repository.findPastActivityIdsForUser(requester.getId(), ParticipationStatus.participating, now, Limit.of(10));
        assertEquals(List.of(recent.getId(), older.getId()), ids);
        var people = repository.findOtherUserIdsByActivityIds(ids, requester.getId(), ParticipationStatus.participating);
        assertEquals(1, people.size());
        assertEquals(other.getId(), people.get(0).getUserId());
        assertEquals(recent.getStartTime().toInstant(), people.get(0).getStartTime().toInstant());
        assertEquals(List.of(recent.getId()), repository.findPastActivityIdsForUser(
                requester.getId(), ParticipationStatus.participating, now, Limit.of(1)));
    }

    @Test
    void invitationAloneDoesNotCountAsSharedActivity() {
        User host = user("host");
        User invitee = user("invitee");
        participation(activity(host, now.minusDays(1)), invitee, ParticipationStatus.invited);
        entities.flush();
        assertTrue(repository.findPastActivityIdsForUser(
                invitee.getId(), ParticipationStatus.participating, now, Limit.of(10)).isEmpty());
    }

    private User user(String name) {
        User user = new User();
        user.setUsername(name);
        user.setName(name);
        user.setEmail(name + "@example.com");
        user.setStatus(UserStatus.ACTIVE);
        return entities.persist(user);
    }

    private Activity activity(User host, OffsetDateTime start) {
        Location location = new Location();
        location.setName("QA test location");
        entities.persist(location);
        Activity activity = new Activity();
        activity.setCreator(host);
        activity.setTitle("QA recent activity");
        activity.setLocation(location);
        activity.setStartTime(start);
        activity.setEndTime(start.plusHours(1));
        return entities.persist(activity);
    }

    private void participation(Activity activity, User user, ParticipationStatus status) {
        entities.persist(new ActivityUser(new ActivityUsersId(activity.getId(), user.getId()), activity, user, status));
    }
}
