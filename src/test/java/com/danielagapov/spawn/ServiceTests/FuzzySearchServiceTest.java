package com.danielagapov.spawn.ServiceTests;

import com.danielagapov.spawn.Config.FuzzySearchConfig;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Services.FuzzySearch.FuzzySearchService;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FuzzySearchService using Jaro-Winkler algorithm.
 * Tests verify the research findings that Jaro-Winkler is optimal for:
 * - Human names and usernames
 * - Handling transpositions and typos
 * - Fast performance with good accuracy
 * - Excellent handling of common prefixes
 */
@ExtendWith(MockitoExtension.class)
public class FuzzySearchServiceTest {

    @Mock
    private ILogger logger;

    private FuzzySearchConfig config;
    private FuzzySearchService<TestUser> fuzzySearchService;

    // Test data class
    static class TestUser {
        private final String name;
        private final String username;

        public TestUser(String name, String username) {
            this.name = name;
            this.username = username;
        }

        public String getName() { return name; }
        public String getUsername() { return username; }
    }

    @BeforeEach
    void setUp() {
        config = new FuzzySearchConfig();
        JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();
        fuzzySearchService = new FuzzySearchService<>(config, logger, jaroWinklerDistance);
    }

    @Test
    void testJaroWinklerWithExactMatch() {
        // Test exact matches should return perfect score
        List<TestUser> users = Arrays.asList(
                new TestUser("John Doe", "johndoe"),
                new TestUser("Jane Smith", "janesmith")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "John Doe", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        assertEquals(1.0, results.get(0).getSimilarityScore(), 0.01);
        assertEquals("John Doe", results.get(0).getItem().getName());
        assertEquals("name", results.get(0).getMatchedField());
    }

    @Test
    void testJaroWinklerWithTypos() {
        // Test the research finding: Jaro-Winkler handles typos well
        List<TestUser> users = Arrays.asList(
                new TestUser("Daniel Agapov", "daggerpov"),
                new TestUser("Michael Johnson", "mjohnson")
        );

        // Test with typo in name
        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "Danel Agapov", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getSimilarityScore() > 0.8); // Should be high similarity
        assertEquals("Daniel Agapov", results.get(0).getItem().getName());
    }

    @Test
    void testJaroWinklerWithTranspositions() {
        // Test the research finding: Jaro-Winkler handles transpositions well
        List<TestUser> users = Arrays.asList(
                new TestUser("Martha", "martha"),
                new TestUser("John", "john")
        );

        // Test with transposition (martha vs marhta)
        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "marhta", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getSimilarityScore() > 0.9); // Should be very high similarity
        assertEquals("Martha", results.get(0).getItem().getName());
    }

    @Test
    void testJaroWinklerWithPrefixBoost() {
        // Test the research finding: Jaro-Winkler gives extra weight to common prefixes
        config.setPrefixBoost(1.2);
        
        List<TestUser> users = Arrays.asList(
                new TestUser("Dixon", "dixon"),
                new TestUser("Dickson", "dickson")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "di", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        // Both should match with prefix boost
        assertTrue(results.stream().anyMatch(r -> r.getItem().getName().equals("Dixon")));
        assertTrue(results.stream().anyMatch(r -> r.getItem().getName().equals("Dickson")));
        assertTrue(results.stream().allMatch(r -> r.isPrefixMatch()));
    }

    @Test
    void testUsernameVsNameWeighting() {
        // Test configurable weighting for usernames vs names
        config.setUsernameWeight(2.0);
        config.setNameWeight(1.0);

        List<TestUser> users = Arrays.asList(
                new TestUser("Alex", "alexander"),
                new TestUser("Alexander", "alex")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "alex", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        // User with matching username should score higher due to weight
        assertEquals("Alexander", results.get(0).getItem().getName());
        assertEquals("username", results.get(0).getMatchedField());
    }

    @Test
    void testSimilarityThreshold() {
        // Test similarity threshold filtering
        config.setSimilarityThreshold(0.8);

        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane"),
                new TestUser("Bob", "bob")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "Johnny", users, TestUser::getName, TestUser::getUsername
        );

        // Only results above threshold should be returned
        assertTrue(results.stream().allMatch(r -> r.getSimilarityScore() >= 0.8));
    }

    @Test
    void testCaseInsensitiveSearch() {
        // Test case-insensitive matching
        config.setCaseInsensitive(true);

        List<TestUser> users = Arrays.asList(
                new TestUser("John Doe", "JohnDoe"),
                new TestUser("Jane Smith", "janesmith")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "JOHN", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getItem().getName().equals("John Doe")));
    }

    @Test
    void testMinQueryLength() {
        // Test minimum query length handling
        config.setMinQueryLength(3);

        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane")
        );

        // Short query should use prefix search
        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "jo", users, TestUser::getName, TestUser::getUsername
        );

        if (!results.isEmpty()) {
            assertTrue(results.stream().allMatch(r -> r.isPrefixMatch()));
        }
    }

    @Test
    void testMaxFinalResults() {
        // Test result limit
        config.setMaxFinalResults(2);

        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Johnny", "johnny"),
                new TestUser("Jonathan", "jonathan"),
                new TestUser("Jones", "jones")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "john", users, TestUser::getName, TestUser::getUsername
        );

        assertTrue(results.size() <= 2);
    }

    @Test
    void testEmptyQuery() {
        // Test empty query handling
        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "", users, TestUser::getName, TestUser::getUsername
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void testNullQuery() {
        // Test null query handling
        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                null, users, TestUser::getName, TestUser::getUsername
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void testEmptyUserList() {
        // Test empty user list
        List<TestUser> users = Collections.emptyList();

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "john", users, TestUser::getName, TestUser::getUsername
        );

        assertTrue(results.isEmpty());
    }

    @Test
    void testNullValues() {
        // Test handling of null names and usernames
        List<TestUser> users = Arrays.asList(
                new TestUser(null, "john"),
                new TestUser("Jane", null),
                new TestUser("Bob", "bob")
        );

        List<FuzzySearchService.SearchResult<TestUser>> results = fuzzySearchService.search(
                "john", users, TestUser::getName, TestUser::getUsername
        );

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> r.getItem().getUsername() != null && r.getItem().getUsername().equals("john")));
    }

    @Test
    void testAnalyticsEnabled() {
        // Test analytics logging
        config.setEnableAnalytics(true);

        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane")
        );

        fuzzySearchService.search(
                "john", users, TestUser::getName, TestUser::getUsername
        );

        // Verify analytics were logged
        verify(logger, atLeastOnce()).info(contains("FuzzySearch Analytics"));
    }

    @Test
    void testCacheSize() {
        // Test cache functionality
        List<TestUser> users = Arrays.asList(
                new TestUser("John", "john"),
                new TestUser("Jane", "jane")
        );

        // Perform multiple searches to populate cache
        for (int i = 0; i < 5; i++) {
            fuzzySearchService.search(
                    "john", users, TestUser::getName, TestUser::getUsername
            );
        }

        assertTrue(fuzzySearchService.getCacheSize() > 0);

        // Clear cache
        fuzzySearchService.clearCache();
        assertEquals(0, fuzzySearchService.getCacheSize());
    }

    @Test
    void testResearchFindingsValidation() {
        // Test the key research findings about Jaro-Winkler
        List<TestUser> users = Arrays.asList(
                new TestUser("Martha", "martha"),
                new TestUser("Dixon", "dixon"),
                new TestUser("Daniel", "daniel")
        );

        // Test 1: Transposition handling (martha vs marhta)
        List<FuzzySearchService.SearchResult<TestUser>> results1 = fuzzySearchService.search(
                "marhta", users, TestUser::getName, TestUser::getUsername
        );
        assertFalse(results1.isEmpty());
        assertTrue(results1.get(0).getSimilarityScore() > 0.9);

        // Test 2: Prefix weighting (dixon vs dickson)
        List<TestUser> users2 = Arrays.asList(
                new TestUser("Dixon", "dixon"),
                new TestUser("Dickson", "dickson")
        );
        List<FuzzySearchService.SearchResult<TestUser>> results2 = fuzzySearchService.search(
                "di", users2, TestUser::getName, TestUser::getUsername
        );
        assertFalse(results2.isEmpty());
        assertTrue(results2.stream().allMatch(r -> r.isPrefixMatch()));

        // Test 3: Human name optimization
        List<FuzzySearchService.SearchResult<TestUser>> results3 = fuzzySearchService.search(
                "danel", users, TestUser::getName, TestUser::getUsername
        );
        assertFalse(results3.isEmpty());
        assertTrue(results3.get(0).getSimilarityScore() > 0.7);
    }
} 