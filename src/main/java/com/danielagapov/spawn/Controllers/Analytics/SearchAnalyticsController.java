package com.danielagapov.spawn.Controllers.Analytics;

import com.danielagapov.spawn.Services.Analytics.SearchAnalyticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for accessing fuzzy search analytics data.
 * Provides endpoints for administrators to monitor search performance and patterns.
 */
@RestController
@RequestMapping("/api/v1/analytics/search")
@AllArgsConstructor
public class SearchAnalyticsController {

    private final SearchAnalyticsService searchAnalyticsService;

    /**
     * Gets overall search analytics summary.
     * 
     * @return Analytics summary including total searches, performance metrics, etc.
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getAnalyticsSummary() {
        try {
            Map<String, Object> summary = searchAnalyticsService.getAnalyticsSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets metrics for a specific query.
     * 
     * @param query The search query to get metrics for
     * @return Detailed metrics for the specified query
     */
    @GetMapping("/query/{query}")
    public ResponseEntity<SearchAnalyticsService.SearchMetrics> getQueryMetrics(@PathVariable String query) {
        try {
            Optional<SearchAnalyticsService.SearchMetrics> metrics = searchAnalyticsService.getQueryMetrics(query);
            return metrics.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets the most popular search queries.
     * 
     * @param limit Number of top queries to return (default: 10)
     * @return List of most popular search queries
     */
    @GetMapping("/popular")
    public ResponseEntity<List<String>> getMostPopularQueries(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<String> popularQueries = searchAnalyticsService.getMostPopularQueries(limit);
            return ResponseEntity.ok(popularQueries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets recent search events.
     * 
     * @param limit Number of recent searches to return (default: 20)
     * @return List of recent search events
     */
    @GetMapping("/recent")
    public ResponseEntity<List<SearchAnalyticsService.SearchEvent>> getRecentSearches(@RequestParam(defaultValue = "20") int limit) {
        try {
            List<SearchAnalyticsService.SearchEvent> recentSearches = searchAnalyticsService.getRecentSearches(limit);
            return ResponseEntity.ok(recentSearches);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Gets performance statistics for recent searches.
     * 
     * @param lastN Number of recent searches to analyze (default: 100)
     * @return Performance statistics for the specified number of recent searches
     */
    @GetMapping("/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceStats(@RequestParam(defaultValue = "100") int lastN) {
        try {
            Map<String, Object> stats = searchAnalyticsService.getPerformanceStats(lastN);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Logs a comprehensive analytics report.
     * 
     * @return Success message
     */
    @PostMapping("/report")
    public ResponseEntity<String> generateAnalyticsReport() {
        try {
            searchAnalyticsService.logAnalyticsReport();
            return ResponseEntity.ok("Analytics report generated and logged successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to generate analytics report");
        }
    }

    /**
     * Clears all analytics data.
     * 
     * @return Success message
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearAnalytics() {
        try {
            searchAnalyticsService.clearAnalytics();
            return ResponseEntity.ok("Analytics data cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Failed to clear analytics data");
        }
    }
} 