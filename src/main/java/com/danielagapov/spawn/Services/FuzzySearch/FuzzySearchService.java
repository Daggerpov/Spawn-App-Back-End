package com.danielagapov.spawn.Services.FuzzySearch;

import com.danielagapov.spawn.Config.FuzzySearchConfig;
import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced fuzzy search service using Jaro-Winkler algorithm.
 * 
 * This service implements the research findings that Jaro-Winkler is optimal for:
 * - Human names and usernames
 * - Handling transpositions and typos
 * - Fast performance with good accuracy
 * - Excellent handling of common prefixes
 * 
 * Key Features:
 * - Configurable similarity thresholds
 * - Weighted scoring for names vs usernames
 * - Prefix matching optimization
 * - Performance monitoring and analytics
 * - Thread-safe operations
 * 
 * @param <T> The type of objects being searched (e.g., User, DTO)
 */
@Service
@AllArgsConstructor
public class FuzzySearchService<T> {
    
    private final FuzzySearchConfig config;
    private final ILogger logger;
    private final JaroWinklerDistance jaroWinklerDistance;
    
    // Cache for frequently used distance calculations
    private final Map<String, Double> distanceCache = new ConcurrentHashMap<>();
    
    /**
     * Search result container with similarity score and metadata.
     */
    @Data
    public static class SearchResult<T> {
        private final T item;
        private final double similarityScore;
        private final String matchedField;
        private final boolean isPrefixMatch;
        private final long processingTimeMs;
        private final double rawScore; // For ranking purposes when scores are capped
        
        public SearchResult(T item, double similarityScore, String matchedField, boolean isPrefixMatch) {
            this.item = item;
            this.similarityScore = similarityScore;
            this.matchedField = matchedField;
            this.isPrefixMatch = isPrefixMatch;
            this.processingTimeMs = System.currentTimeMillis();
            this.rawScore = similarityScore; // Default to same as similarity score
        }
        
        public SearchResult(T item, double similarityScore, String matchedField, boolean isPrefixMatch, double rawScore) {
            this.item = item;
            this.similarityScore = similarityScore;
            this.matchedField = matchedField;
            this.isPrefixMatch = isPrefixMatch;
            this.processingTimeMs = System.currentTimeMillis();
            this.rawScore = rawScore;
        }
    }
    
    /**
     * Search analytics data for performance monitoring.
     */
    @Data
    public static class SearchAnalytics {
        private final String query;
        private final int totalItems;
        private final int matchedItems;
        private final int filteredItems;
        private final long processingTimeMs;
        private final double averageSimilarity;
        private final double maxSimilarity;
        private final double minSimilarity;
    }
    
    /**
     * Functional interface for extracting searchable text from objects.
     */
    @FunctionalInterface
    public interface TextExtractor<T> {
        String extract(T item);
    }
    
    /**
     * Performs fuzzy search on a collection of items using Jaro-Winkler algorithm.
     * 
     * @param query The search query string
     * @param items Collection of items to search through
     * @param nameExtractor Function to extract name text from items
     * @param usernameExtractor Function to extract username text from items
     * @return List of search results ordered by similarity score (highest first)
     */
    public List<SearchResult<T>> search(String query, Collection<T> items, 
                                       TextExtractor<T> nameExtractor, 
                                       TextExtractor<T> usernameExtractor) {
        
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        long startTime = System.currentTimeMillis();
        String normalizedQuery = normalizeQuery(query);
        
        // Skip fuzzy search for very short queries if configured
        if (normalizedQuery.length() < config.getMinQueryLength()) {
            return performExactPrefixSearch(normalizedQuery, items, nameExtractor, usernameExtractor);
        }
        
        List<SearchResult<T>> results = new ArrayList<>();
        
        // Compute similarity scores for all items
        for (T item : items) {
            SearchResult<T> result = computeSimilarity(normalizedQuery, item, nameExtractor, usernameExtractor);
            if (result != null) {
                results.add(result);
            }
        }
        
        // Filter results based on similarity threshold
        List<SearchResult<T>> filteredResults = results.stream()
                .filter(result -> result.getSimilarityScore() >= config.getSimilarityThreshold())
                .collect(Collectors.toList());
        
        // If no results meet the threshold, fall back to top results
        if (filteredResults.isEmpty() && !results.isEmpty()) {
            filteredResults = results.stream()
                    .sorted(Comparator.comparingDouble(SearchResult<T>::getRawScore).reversed())
                    .limit(config.getMaxFinalResults())
                    .collect(Collectors.toList());
        }
        
        // Sort by raw score (highest first) for proper ranking when weights are involved, then limit results
        List<SearchResult<T>> finalResults = filteredResults.stream()
                .sorted(Comparator.comparingDouble(SearchResult<T>::getRawScore).reversed())
                .limit(config.getMaxFinalResults())
                .collect(Collectors.toList());
        
        // Log analytics if enabled
        if (config.isEnableAnalytics()) {
            logSearchAnalytics(query, items.size(), results.size(), finalResults.size(), 
                             System.currentTimeMillis() - startTime, finalResults);
        }
        
        return finalResults;
    }
    
    /**
     * Performs exact prefix search for very short queries.
     */
    private List<SearchResult<T>> performExactPrefixSearch(String query, Collection<T> items,
                                                          TextExtractor<T> nameExtractor, 
                                                          TextExtractor<T> usernameExtractor) {
        
        return items.stream()
                .map(item -> {
                    String name = normalizeText(nameExtractor.extract(item));
                    String username = normalizeText(usernameExtractor.extract(item));
                    
                    boolean nameMatch = name != null && name.startsWith(query);
                    boolean usernameMatch = username != null && username.startsWith(query);
                    
                    if (nameMatch) {
                        return new SearchResult<>(item, 1.0, "name", true);
                    } else if (usernameMatch) {
                        return new SearchResult<>(item, 1.0, "username", true);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(SearchResult<T>::getRawScore).reversed())
                .limit(config.getMaxFinalResults())
                .collect(Collectors.toList());
    }
    
    /**
     * Computes similarity score for a single item against the query.
     */
    private SearchResult<T> computeSimilarity(String query, T item, 
                                            TextExtractor<T> nameExtractor, 
                                            TextExtractor<T> usernameExtractor) {
        
        String name = normalizeText(nameExtractor.extract(item));
        String username = normalizeText(usernameExtractor.extract(item));
        
        double nameScore = 0.0;
        double usernameScore = 0.0;
        String matchedField = null;
        boolean isPrefixMatch = false;
        
        // Calculate name similarity
        if (name != null && !name.isEmpty()) {
            nameScore = calculateJaroWinklerSimilarity(query, name);
            
            // Apply prefix boost if applicable
            if (name.startsWith(query)) {
                nameScore *= config.getPrefixBoost();
                isPrefixMatch = true;
            }
            
            // Apply name weight
            nameScore *= config.getNameWeight();
        }
        
        // Calculate username similarity
        if (username != null && !username.isEmpty()) {
            usernameScore = calculateJaroWinklerSimilarity(query, username);
            
            // Apply prefix boost if applicable
            if (username.startsWith(query)) {
                usernameScore *= config.getPrefixBoost();
                isPrefixMatch = true;
            }
            
            // Apply username weight
            usernameScore *= config.getUsernameWeight();
        }
        
        // Determine best match
        double rawScore;
        double finalScore;
        if (nameScore >= usernameScore) {
            rawScore = nameScore;
            matchedField = "name";
        } else {
            rawScore = usernameScore;
            matchedField = "username";
        }
        
        // Normalize displayed score to ensure it doesn't exceed 1.0 after weight application
        // But keep the raw score for ranking purposes
        finalScore = Math.min(rawScore, 1.0);
        
        return new SearchResult<>(item, finalScore, matchedField, isPrefixMatch, rawScore);
    }
    
    /**
     * Calculates Jaro-Winkler similarity with caching for performance.
     */
    private double calculateJaroWinklerSimilarity(String query, String target) {
        // Create cache key
        String cacheKey = query + "|" + target;
        
        // Check cache first
        if (distanceCache.containsKey(cacheKey)) {
            return distanceCache.get(cacheKey);
        }
        
        // Calculate distance and convert to similarity
        // JaroWinklerDistance.apply() returns distance (0.0 = identical, 1.0 = completely different)
        // We need similarity (1.0 = identical, 0.0 = completely different)
        double distance = jaroWinklerDistance.apply(query, target);
        double similarity = 1.0 - distance;
        
        // Cache the result
        distanceCache.put(cacheKey, similarity);
        
        // Prevent cache from growing too large
        if (distanceCache.size() > 10000) {
            distanceCache.clear();
        }
        
        return similarity;
    }
    
    /**
     * Normalizes query string for consistent matching.
     */
    private String normalizeQuery(String query) {
        if (query == null) return "";
        
        String normalized = query.trim();
        if (config.isCaseInsensitive()) {
            normalized = normalized.toLowerCase();
        }
        
        return normalized;
    }
    
    /**
     * Normalizes text for consistent matching.
     */
    private String normalizeText(String text) {
        if (text == null) return null;
        
        String normalized = text.trim();
        if (config.isCaseInsensitive()) {
            normalized = normalized.toLowerCase();
        }
        
        return normalized;
    }
    
    /**
     * Logs search analytics for performance monitoring.
     */
    private void logSearchAnalytics(String query, int totalItems, int matchedItems, 
                                   int filteredItems, long processingTime, 
                                   List<SearchResult<T>> results) {
        
        double avgSimilarity = results.stream()
                .mapToDouble(SearchResult::getSimilarityScore)
                .average()
                .orElse(0.0);
        
        double maxSimilarity = results.stream()
                .mapToDouble(SearchResult::getSimilarityScore)
                .max()
                .orElse(0.0);
        
        double minSimilarity = results.stream()
                .mapToDouble(SearchResult::getSimilarityScore)
                .min()
                .orElse(0.0);
        
        SearchAnalytics analytics = new SearchAnalytics(
                query, totalItems, matchedItems, filteredItems, processingTime,
                avgSimilarity, maxSimilarity, minSimilarity
        );
        
        logger.info("FuzzySearch Analytics: " + analytics.toString());
    }
    
    /**
     * Clears the internal distance cache.
     * Useful for memory management in long-running applications.
     */
    public void clearCache() {
        distanceCache.clear();
    }
    
    /**
     * Gets current cache size for monitoring.
     */
    public int getCacheSize() {
        return distanceCache.size();
    }
} 