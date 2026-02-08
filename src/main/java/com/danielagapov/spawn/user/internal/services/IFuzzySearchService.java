package com.danielagapov.spawn.user.internal.services;

import java.util.Collection;
import java.util.List;

/**
 * Interface for fuzzy search service to enable mocking in tests.
 *
 * @param <T> The type of objects being searched (e.g., User, DTO)
 */
public interface IFuzzySearchService<T> {
    
    /**
     * Performs fuzzy search on a collection of items using Jaro-Winkler algorithm.
     * 
     * @param query The search query string
     * @param items Collection of items to search through
     * @param nameExtractor Function to extract name text from items
     * @param usernameExtractor Function to extract username text from items
     * @return List of search results ordered by similarity score (highest first)
     */
    List<FuzzySearchService.SearchResult<T>> search(String query, Collection<T> items, 
                                                    FuzzySearchService.TextExtractor<T> nameExtractor, 
                                                    FuzzySearchService.TextExtractor<T> usernameExtractor);
    
    /**
     * Clears the internal distance cache.
     */
    void clearCache();
    
    /**
     * Gets current cache size for monitoring.
     */
    int getCacheSize();
}
