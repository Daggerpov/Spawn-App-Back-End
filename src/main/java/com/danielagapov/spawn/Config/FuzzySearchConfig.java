package com.danielagapov.spawn.Config;

import lombok.Data;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for fuzzy search parameters.
 * This class provides configurable settings for the Jaro-Winkler based fuzzy search system.
 * 
 * Based on research findings, Jaro-Winkler is optimal for:
 * - Human names and usernames
 * - Handling transpositions and typos
 * - Fast performance with good accuracy
 * 
 * Configuration properties can be set in application.properties with prefix "fuzzy-search"
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "fuzzy-search")
public class FuzzySearchConfig {
    
    /**
     * Similarity threshold for Jaro-Winkler distance.
     * Values range from 0.0 (no similarity) to 1.0 (perfect match).
     * 
     * Default: 0.6 (60% similarity threshold)
     * - Values above 0.6 generally indicate good matches for names/usernames
     * - Lower values (0.4-0.5) may include more false positives
     * - Higher values (0.7-0.8) are more strict but may miss valid matches
     */
    private double similarityThreshold = 0.6;
    
    /**
     * Minimum similarity threshold for very strict matching.
     * Used for high-confidence matches in critical scenarios.
     * 
     * Default: 0.8 (80% similarity threshold)
     */
    private double strictSimilarityThreshold = 0.8;
    
    /**
     * Maximum number of database results to fetch before applying fuzzy matching.
     * This limits the initial query size for performance optimization.
     * 
     * Default: 100
     */
    private int maxDatabaseResults = 100;
    
    /**
     * Maximum number of final results to return after fuzzy matching and ranking.
     * This limits the response size for optimal user experience.
     * 
     * Default: 10
     */
    private int maxFinalResults = 10;
    
    /**
     * Weight for name matching in the overall similarity score.
     * Higher values prioritize name matches over username matches.
     * 
     * Default: 1.0 (equal weight)
     */
    private double nameWeight = 1.0;
    
    /**
     * Weight for username matching in the overall similarity score.
     * Higher values prioritize username matches over name matches.
     * 
     * Default: 1.0 (equal weight)
     */
    private double usernameWeight = 1.0;
    
    /**
     * Prefix matching boost factor.
     * Multiplier applied to similarity scores when query matches the beginning of a name/username.
     * This leverages Jaro-Winkler's strength with common prefixes.
     * 
     * Default: 1.2 (20% boost for prefix matches)
     */
    private double prefixBoost = 1.2;
    
    /**
     * Minimum query length to perform fuzzy search.
     * Queries shorter than this will use exact prefix matching only.
     * 
     * Default: 2
     */
    private int minQueryLength = 2;
    
    /**
     * Enable case-insensitive matching.
     * When true, all comparisons are done in lowercase.
     * 
     * Default: true
     */
    private boolean caseInsensitive = true;
    
    /**
     * Enable prefix optimization.
     * When true, database queries are optimized to fetch users with matching prefixes first.
     * 
     * Default: true
     */
    private boolean enablePrefixOptimization = true;
    
    /**
     * Enable analytics and performance monitoring.
     * When true, search performance metrics are collected and logged.
     * 
     * Default: false
     */
    private boolean enableAnalytics = false;
    
    /**
     * Creates a JaroWinklerDistance bean for dependency injection.
     * This bean is configured as a singleton and can be injected into services.
     * 
     * @return JaroWinklerDistance instance for fuzzy matching
     */
    @Bean
    public JaroWinklerDistance jaroWinklerDistance() {
        return new JaroWinklerDistance();
    }
} 