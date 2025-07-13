package com.danielagapov.spawn.Util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * Utility class to generate random two-word combinations for share codes.
 * Uses curated lists of adjectives and nouns to create memorable, human-readable codes.
 */
@Component
public class ShareCodeGenerator {
    
    private static final SecureRandom random = new SecureRandom();
    
    // Positive adjectives that are short and memorable
    private static final List<String> ADJECTIVES = List.of(
        "happy", "bright", "swift", "clever", "gentle", "bold", "calm", "eager",
        "fresh", "grand", "lively", "merry", "noble", "proud", "quiet", "smart",
        "strong", "warm", "wise", "young", "active", "brave", "clean", "clear",
        "crisp", "deep", "fair", "fast", "fine", "free", "good", "great",
        "kind", "light", "lucky", "nice", "open", "pure", "quick", "rich",
        "safe", "sharp", "shiny", "smooth", "solid", "sweet", "thick", "tight",
        "true", "wild", "zesty", "agile", "alive", "amber", "awake", "azure",
        "basic", "blond", "brave", "broad", "brown", "busy", "coral", "curly",
        "cute", "dense", "dizzy", "early", "exact", "fancy", "giant", "green",
        "huge", "ideal", "ivory", "jazzy", "level", "loyal", "major", "minor",
        "mixed", "modal", "vital", "vivid", "whole", "witty", "zonal"
    );
    
    // Common nouns that are easy to remember and type
    private static final List<String> NOUNS = List.of(
        "bear", "bird", "boat", "book", "cake", "car", "cat", "city", "cloud", "coin",
        "deer", "dog", "door", "duck", "eagle", "earth", "face", "fire", "fish", "flower",
        "fox", "frog", "game", "gift", "goat", "gold", "grass", "hand", "hawk", "hill",
        "horse", "house", "island", "key", "king", "lake", "lamp", "leaf", "light", "lion",
        "moon", "mouse", "night", "ocean", "owl", "park", "path", "piano", "plant", "pond",
        "queen", "rain", "river", "rock", "room", "rose", "sail", "sea", "ship", "sky",
        "snow", "song", "star", "sun", "swan", "tiger", "tree", "wave", "whale", "wind",
        "wolf", "wood", "apple", "arrow", "beach", "bench", "brick", "bridge", "brush", "castle",
        "chair", "cheese", "chest", "coral", "crown", "dance", "dream", "field", "flame", "fruit",
        "ghost", "glass", "globe", "grape", "heart", "honey", "jewel", "juice", "magic", "maple",
        "music", "pearl", "phase", "plank", "plate", "plaza", "prism", "quest", "ridge", "shell",
        "smile", "space", "spark", "spear", "steam", "stone", "storm", "tower", "trail", "trumpet",
        "valley", "violin", "water", "wheat", "wheel", "world"
    );
    
    /**
     * Generate a random two-word combination separated by a hyphen
     * @return A share code like "happy-dolphin"
     */
    public String generateShareCode() {
        String adjective = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
        return adjective + "-" + noun;
    }
    
    /**
     * Generate a share code with retry logic to avoid duplicates
     * @param maxRetries Maximum number of retry attempts
     * @param existsChecker Function to check if a code already exists
     * @return A unique share code
     */
    public String generateUniqueShareCode(int maxRetries, java.util.function.Function<String, Boolean> existsChecker) {
        for (int i = 0; i < maxRetries; i++) {
            String code = generateShareCode();
            if (!existsChecker.apply(code)) {
                return code;
            }
        }
        
        // If we couldn't generate a unique code after retries, append a number
        String baseCode = generateShareCode();
        int counter = 1;
        String finalCode = baseCode + "-" + counter;
        
        while (existsChecker.apply(finalCode)) {
            counter++;
            finalCode = baseCode + "-" + counter;
        }
        
        return finalCode;
    }
} 