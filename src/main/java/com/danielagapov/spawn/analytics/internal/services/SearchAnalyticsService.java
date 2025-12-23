package com.danielagapov.spawn.analytics.internal.services;

import com.danielagapov.spawn.shared.config.FuzzySearchConfig;
import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.user.internal.services.FuzzySearchService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for monitoring and analyzing fuzzy search performance.
 * Provides insights into search patterns, performance metrics, and effectiveness.
 */
@Service
@AllArgsConstructor
public class SearchAnalyticsService {

    private final FuzzySearchConfig config;
    private final ILogger logger;

    // Analytics data storage
    private final Map<String, SearchMetrics> queryMetrics = new ConcurrentHashMap<>();
    private final AtomicInteger totalSearches = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final Map<String, AtomicInteger> popularQueries = new ConcurrentHashMap<>();
    private final List<SearchEvent> recentSearches = Collections.synchronizedList(new ArrayList<>());

    /**
     * Individual search event for detailed tracking.
     */
    @Data
    public static class SearchEvent {
        private final String query;
        private final LocalDateTime timestamp;
        private final long processingTimeMs;
        private final int totalItems;
        private final int matchedItems;
        private final double averageSimilarity;
        private final double bestSimilarity;
        private final String bestMatchField;
        private final boolean hadResults;
    }

    /**
     * Aggregated metrics for a specific query pattern.
     */
    @Data
    public static class SearchMetrics {
        private final String query;
        private int searchCount;
        private long totalProcessingTime;
        private int totalItems;
        private int totalMatches;
        private double bestSimilarity;
        private double averageSimilarity;
        private LocalDateTime firstSearched;
        private LocalDateTime lastSearched;
        private int successfulSearches;
        private int emptyResults;

        public SearchMetrics(String query) {
            this.query = query;
            this.searchCount = 0;
            this.totalProcessingTime = 0L;
            this.totalItems = 0;
            this.totalMatches = 0;
            this.bestSimilarity = 0.0;
            this.averageSimilarity = 0.0;
            this.firstSearched = LocalDateTime.now();
            this.lastSearched = LocalDateTime.now();
            this.successfulSearches = 0;
            this.emptyResults = 0;
        }

        public void updateMetrics(SearchEvent event) {
            this.searchCount++;
            this.totalProcessingTime += event.getProcessingTimeMs();
            this.totalItems += event.getTotalItems();
            this.totalMatches += event.getMatchedItems();
            this.bestSimilarity = Math.max(this.bestSimilarity, event.getBestSimilarity());
            this.averageSimilarity = (this.averageSimilarity * (this.searchCount - 1) + event.getAverageSimilarity()) / this.searchCount;
            this.lastSearched = event.getTimestamp();
            
            if (event.isHadResults()) {
                this.successfulSearches++;
            } else {
                this.emptyResults++;
            }
        }

        public double getAverageProcessingTime() {
            return searchCount > 0 ? (double) totalProcessingTime / searchCount : 0.0;
        }

        public double getSuccessRate() {
            return searchCount > 0 ? (double) successfulSearches / searchCount : 0.0;
        }

        public double getAverageMatches() {
            return searchCount > 0 ? (double) totalMatches / searchCount : 0.0;
        }
    }

    /**
     * Records a search event for analytics.
     */
    public void recordSearchEvent(String query, long processingTimeMs, int totalItems, 
                                 int matchedItems, double averageSimilarity, 
                                 double bestSimilarity, String bestMatchField, boolean hadResults) {
        
        if (!config.isEnableAnalytics()) {
            return;
        }

        SearchEvent event = new SearchEvent(
                query, LocalDateTime.now(), processingTimeMs, totalItems, 
                matchedItems, averageSimilarity, bestSimilarity, bestMatchField, hadResults
        );

        // Update global metrics
        totalSearches.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);

        // Update query-specific metrics
        queryMetrics.computeIfAbsent(query, SearchMetrics::new).updateMetrics(event);

        // Update popular queries
        popularQueries.computeIfAbsent(query, k -> new AtomicInteger(0)).incrementAndGet();

        // Store recent search (limit to last 100)
        synchronized (recentSearches) {
            recentSearches.add(event);
            if (recentSearches.size() > 100) {
                recentSearches.remove(0);
            }
        }

        // Log if detailed analytics is enabled
        if (config.isEnableAnalytics()) {
            logger.info(String.format("Search Analytics - Query: '%s', Processing: %dms, Items: %d, Matches: %d, Avg Similarity: %.3f, Best Similarity: %.3f",
                    query, processingTimeMs, totalItems, matchedItems, averageSimilarity, bestSimilarity));
        }
    }

    /**
     * Records a search event from FuzzySearchService results.
     */
    public <T> void recordSearchResults(String query, long processingTimeMs, int totalItems, 
                                       List<FuzzySearchService.SearchResult<T>> results) {
        
        if (!config.isEnableAnalytics() || results == null) {
            return;
        }

        double averageSimilarity = results.stream()
                .mapToDouble(FuzzySearchService.SearchResult::getSimilarityScore)
                .average()
                .orElse(0.0);

        double bestSimilarity = results.stream()
                .mapToDouble(FuzzySearchService.SearchResult::getSimilarityScore)
                .max()
                .orElse(0.0);

        String bestMatchField = results.stream()
                .max(Comparator.comparingDouble(FuzzySearchService.SearchResult::getSimilarityScore))
                .map(FuzzySearchService.SearchResult::getMatchedField)
                .orElse("none");

        recordSearchEvent(query, processingTimeMs, totalItems, results.size(), 
                         averageSimilarity, bestSimilarity, bestMatchField, !results.isEmpty());
    }

    /**
     * Gets analytics summary for all searches.
     */
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalSearches", totalSearches.get());
        summary.put("averageProcessingTime", totalSearches.get() > 0 ? 
                   (double) totalProcessingTime.get() / totalSearches.get() : 0.0);
        summary.put("totalProcessingTime", totalProcessingTime.get());
        
        // Top queries
        List<Map.Entry<String, AtomicInteger>> topQueries = popularQueries.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                        (a, b) -> b.get() - a.get()))
                .limit(10)
                .toList();
        
        summary.put("topQueries", topQueries.stream()
                .collect(HashMap::new, 
                        (map, entry) -> map.put(entry.getKey(), entry.getValue().get()),
                        HashMap::putAll));
        
        // Performance metrics
        double avgSuccessRate = queryMetrics.values().stream()
                .mapToDouble(SearchMetrics::getSuccessRate)
                .average()
                .orElse(0.0);
        
        summary.put("averageSuccessRate", avgSuccessRate);
        summary.put("uniqueQueries", queryMetrics.size());
        summary.put("recentSearchCount", recentSearches.size());
        
        return summary;
    }

    /**
     * Gets detailed metrics for a specific query.
     */
    public Optional<SearchMetrics> getQueryMetrics(String query) {
        return Optional.ofNullable(queryMetrics.get(query));
    }

    /**
     * Gets the most popular queries.
     */
    public List<String> getMostPopularQueries(int limit) {
        return popularQueries.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(
                        (a, b) -> b.get() - a.get()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Gets recent search events.
     */
    public List<SearchEvent> getRecentSearches(int limit) {
        synchronized (recentSearches) {
            return recentSearches.stream()
                    .sorted(Comparator.comparing(SearchEvent::getTimestamp).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    /**
     * Gets performance statistics for the last N searches.
     */
    public Map<String, Object> getPerformanceStats(int lastN) {
        List<SearchEvent> recent = getRecentSearches(lastN);
        
        if (recent.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, Object> stats = new HashMap<>();
        
        double avgProcessingTime = recent.stream()
                .mapToDouble(SearchEvent::getProcessingTimeMs)
                .average()
                .orElse(0.0);
        
        double avgSimilarity = recent.stream()
                .mapToDouble(SearchEvent::getAverageSimilarity)
                .average()
                .orElse(0.0);
        
        double successRate = (double) recent.stream()
                .mapToInt(event -> event.isHadResults() ? 1 : 0)
                .sum() / recent.size();
        
        stats.put("averageProcessingTime", avgProcessingTime);
        stats.put("averageSimilarity", avgSimilarity);
        stats.put("successRate", successRate);
        stats.put("totalSearches", recent.size());
        
        return stats;
    }

    /**
     * Clears all analytics data.
     */
    public void clearAnalytics() {
        queryMetrics.clear();
        totalSearches.set(0);
        totalProcessingTime.set(0);
        popularQueries.clear();
        recentSearches.clear();
        
        logger.info("Search analytics data cleared");
    }

    /**
     * Logs a comprehensive analytics report.
     */
    public void logAnalyticsReport() {
        if (!config.isEnableAnalytics()) {
            return;
        }

        Map<String, Object> summary = getAnalyticsSummary();
        
        logger.info("=== FUZZY SEARCH ANALYTICS REPORT ===");
        logger.info(String.format("Total Searches: %s", summary.get("totalSearches")));
        logger.info(String.format("Average Processing Time: %.2fms", summary.get("averageProcessingTime")));
        logger.info(String.format("Average Success Rate: %.2f%%", (Double) summary.get("averageSuccessRate") * 100));
        logger.info(String.format("Unique Queries: %s", summary.get("uniqueQueries")));
        
        logger.info("Top 5 Queries:");
        getMostPopularQueries(5).forEach(query -> {
            SearchMetrics metrics = queryMetrics.get(query);
            if (metrics != null) {
                logger.info(String.format("  '%s': %d searches, %.1fms avg, %.1f%% success rate",
                        query, metrics.getSearchCount(), metrics.getAverageProcessingTime(),
                        metrics.getSuccessRate() * 100));
            }
        });
        
        logger.info("=======================================");
    }
} 