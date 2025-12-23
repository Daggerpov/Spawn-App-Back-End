package com.danielagapov.spawn.RepositoryTests;

import com.danielagapov.spawn.activity.internal.domain.Activity;
import com.danielagapov.spawn.activity.internal.domain.Location;
import com.danielagapov.spawn.user.internal.domain.User;
import com.danielagapov.spawn.activity.internal.repositories.IActivityRepository;
import com.danielagapov.spawn.activity.internal.repositories.ILocationRepository;
import com.danielagapov.spawn.user.internal.repositories.IUserRepository;
import com.danielagapov.spawn.shared.util.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ActivityRepository
 * Tests database operations for Activity entities
 */
@DataJpaTest
@ActiveProfiles("test")
class ActivityRepositoryTests {

    @Autowired
    private IActivityRepository activityRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ILocationRepository locationRepository;

    private User testUser;
    private Location testLocation;
    private Activity testActivity;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        activityRepository.deleteAll();
        userRepository.deleteAll();
        locationRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setProfilePictureUrlString("pic.jpg");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser = userRepository.save(testUser);

        // Create test location
        testLocation = new Location();
        testLocation.setName("Test Location");
        testLocation.setLatitude(40.7128);
        testLocation.setLongitude(-74.0060);
        testLocation = locationRepository.save(testLocation);

        // Create test activity
        testActivity = new Activity();
        testActivity.setTitle("Test Activity");
        testActivity.setStartTime(OffsetDateTime.now().plusDays(1));
        testActivity.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
        testActivity.setLocation(testLocation);
        testActivity.setNote("Test note");
        testActivity.setCreator(testUser);
        testActivity.setIcon("🎉");
    }

    // MARK: - Basic CRUD Tests

    @Test
    void save_ShouldPersistActivity_WhenValidActivity() {
        Activity saved = activityRepository.save(testActivity);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("Test Activity", saved.getTitle());
        assertEquals(testUser.getId(), saved.getCreator().getId());
        assertEquals(testLocation.getId(), saved.getLocation().getId());
    }

    @Test
    void findById_ShouldReturnActivity_WhenActivityExists() {
        Activity saved = activityRepository.save(testActivity);

        Optional<Activity> found = activityRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Test Activity", found.get().getTitle());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenActivityDoesNotExist() {
        Optional<Activity> found = activityRepository.findById(UUID.randomUUID());

        assertFalse(found.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllActivities_WhenActivitiesExist() {
        activityRepository.save(testActivity);

        Activity secondActivity = new Activity();
        secondActivity.setTitle("Second Activity");
        secondActivity.setStartTime(OffsetDateTime.now().plusDays(2));
        secondActivity.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(2));
        secondActivity.setLocation(testLocation);
        secondActivity.setCreator(testUser);
        secondActivity.setIcon("🍽️");
        activityRepository.save(secondActivity);

        List<Activity> all = activityRepository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void deleteById_ShouldRemoveActivity_WhenActivityExists() {
        Activity saved = activityRepository.save(testActivity);
        UUID activityId = saved.getId();

        activityRepository.deleteById(activityId);

        Optional<Activity> found = activityRepository.findById(activityId);
        assertFalse(found.isPresent());
    }

    @Test
    void existsById_ShouldReturnTrue_WhenActivityExists() {
        Activity saved = activityRepository.save(testActivity);

        boolean exists = activityRepository.existsById(saved.getId());

        assertTrue(exists);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenActivityDoesNotExist() {
        boolean exists = activityRepository.existsById(UUID.randomUUID());

        assertFalse(exists);
    }

    // MARK: - Custom Query Tests

    @Test
    void findByCreatorId_ShouldReturnActivities_WhenUserHasActivities() {
        activityRepository.save(testActivity);

        Activity secondActivity = new Activity();
        secondActivity.setTitle("Second Activity");
        secondActivity.setStartTime(OffsetDateTime.now().plusDays(2));
        secondActivity.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(2));
        secondActivity.setLocation(testLocation);
        secondActivity.setCreator(testUser);
        secondActivity.setIcon("⚽");
        activityRepository.save(secondActivity);

        List<Activity> userActivities = activityRepository.findByCreatorId(testUser.getId());

        assertEquals(2, userActivities.size());
        assertTrue(userActivities.stream().allMatch(a -> a.getCreator().getId().equals(testUser.getId())));
    }

    @Test
    void findByCreatorId_ShouldReturnEmpty_WhenUserHasNoActivities() {
        User anotherUser = new User();
        anotherUser.setUsername("anotheruser");
        anotherUser.setEmail("another@example.com");
        anotherUser.setName("Another User");
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser = userRepository.save(anotherUser);

        List<Activity> userActivities = activityRepository.findByCreatorId(anotherUser.getId());

        assertTrue(userActivities.isEmpty());
    }

    // MARK: - Update Tests

    @Test
    void update_ShouldModifyActivity_WhenActivityExists() {
        Activity saved = activityRepository.save(testActivity);

        saved.setTitle("Updated Title");
        saved.setNote("Updated note");
        Activity updated = activityRepository.save(saved);

        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated note", updated.getNote());
        assertEquals(saved.getId(), updated.getId());
    }

    @Test
    void update_ShouldModifyStartTime_WhenTimeChanged() {
        Activity saved = activityRepository.save(testActivity);
        OffsetDateTime newStartTime = OffsetDateTime.now().plusDays(3);

        saved.setStartTime(newStartTime);
        Activity updated = activityRepository.save(saved);

        assertEquals(newStartTime, updated.getStartTime());
    }

    @Test
    void update_ShouldModifyParticipantLimit_WhenLimitChanged() {
        testActivity.setParticipantLimit(10);
        Activity saved = activityRepository.save(testActivity);

        saved.setParticipantLimit(20);
        Activity updated = activityRepository.save(saved);

        assertEquals(20, updated.getParticipantLimit());
    }

    // MARK: - Relationship Tests

    @Test
    void save_ShouldMaintainCreatorRelationship_WhenActivitySaved() {
        Activity saved = activityRepository.save(testActivity);

        Activity found = activityRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getCreator());
        assertEquals(testUser.getId(), found.getCreator().getId());
        assertEquals(testUser.getUsername(), found.getCreator().getUsername());
    }

    @Test
    void save_ShouldMaintainLocationRelationship_WhenActivitySaved() {
        Activity saved = activityRepository.save(testActivity);

        Activity found = activityRepository.findById(saved.getId()).orElseThrow();

        assertNotNull(found.getLocation());
        assertEquals(testLocation.getId(), found.getLocation().getId());
        assertEquals(testLocation.getName(), found.getLocation().getName());
    }

    @Test
    void delete_ShouldNotDeleteCreator_WhenActivityDeleted() {
        Activity saved = activityRepository.save(testActivity);
        UUID creatorId = testUser.getId();

        activityRepository.deleteById(saved.getId());

        Optional<User> creator = userRepository.findById(creatorId);
        assertTrue(creator.isPresent());
    }

    @Test
    void delete_ShouldNotDeleteLocation_WhenActivityDeleted() {
        Activity saved = activityRepository.save(testActivity);
        UUID locationId = testLocation.getId();

        activityRepository.deleteById(saved.getId());

        Optional<Location> location = locationRepository.findById(locationId);
        assertTrue(location.isPresent());
    }

    // MARK: - Edge Case Tests

    @Test
    void save_ShouldHandleNullNote_WhenNoteNotProvided() {
        testActivity.setNote(null);

        Activity saved = activityRepository.save(testActivity);

        assertNotNull(saved);
        assertNull(saved.getNote());
    }

    @Test
    void save_ShouldHandleNullParticipantLimit_WhenLimitNotSet() {
        testActivity.setParticipantLimit(null);

        Activity saved = activityRepository.save(testActivity);

        assertNotNull(saved);
        assertNull(saved.getParticipantLimit());
    }

    @Test
    void save_ShouldHandleLongTitle_WhenTitleIsVeryLong() {
        String longTitle = "A".repeat(255);
        testActivity.setTitle(longTitle);

        Activity saved = activityRepository.save(testActivity);

        assertEquals(longTitle, saved.getTitle());
    }

    @Test
    void findAll_ShouldReturnEmpty_WhenNoActivities() {
        List<Activity> all = activityRepository.findAll();

        assertTrue(all.isEmpty());
    }

    @Test
    void save_ShouldHandleMultipleActivitiesSameTime_WhenTimesOverlap() {
        OffsetDateTime sameTime = OffsetDateTime.now().plusDays(1);
        
        testActivity.setStartTime(sameTime);
        testActivity.setEndTime(sameTime.plusHours(2));
        Activity first = activityRepository.save(testActivity);

        Activity second = new Activity();
        second.setTitle("Overlapping Activity");
        second.setStartTime(sameTime);
        second.setEndTime(sameTime.plusHours(2));
        second.setLocation(testLocation);
        second.setCreator(testUser);
        second.setIcon("🎊");
        Activity secondSaved = activityRepository.save(second);

        assertNotNull(first.getId());
        assertNotNull(secondSaved.getId());
        assertNotEquals(first.getId(), secondSaved.getId());
    }

    @Test
    void findByCreatorId_ShouldReturnActivitiesInOrder_WhenMultipleActivities() {
        // Create activities with different times
        Activity first = new Activity();
        first.setTitle("First Activity");
        first.setStartTime(OffsetDateTime.now().plusDays(1));
        first.setEndTime(OffsetDateTime.now().plusDays(1).plusHours(2));
        first.setLocation(testLocation);
        first.setCreator(testUser);
        first.setIcon("1️⃣");
        activityRepository.save(first);

        Activity second = new Activity();
        second.setTitle("Second Activity");
        second.setStartTime(OffsetDateTime.now().plusDays(2));
        second.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(2));
        second.setLocation(testLocation);
        second.setCreator(testUser);
        second.setIcon("2️⃣");
        activityRepository.save(second);

        Activity third = new Activity();
        third.setTitle("Third Activity");
        third.setStartTime(OffsetDateTime.now().plusDays(3));
        third.setEndTime(OffsetDateTime.now().plusDays(3).plusHours(2));
        third.setLocation(testLocation);
        third.setCreator(testUser);
        third.setIcon("3️⃣");
        activityRepository.save(third);

        List<Activity> activities = activityRepository.findByCreatorId(testUser.getId());

        assertEquals(3, activities.size());
    }

    @Test
    void save_ShouldGenerateUniqueIds_WhenMultipleActivities() {
        Activity first = activityRepository.save(testActivity);

        Activity second = new Activity();
        second.setTitle("Second Activity");
        second.setStartTime(OffsetDateTime.now().plusDays(2));
        second.setEndTime(OffsetDateTime.now().plusDays(2).plusHours(2));
        second.setLocation(testLocation);
        second.setCreator(testUser);
        second.setIcon("🎊");
        Activity secondSaved = activityRepository.save(second);

        assertNotEquals(first.getId(), secondSaved.getId());
    }
}

