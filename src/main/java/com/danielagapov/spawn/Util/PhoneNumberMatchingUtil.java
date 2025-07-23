package com.danielagapov.spawn.Util;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for phone number matching and comparison that doesn't make assumptions
 * about country codes. This replaces the unreliable approach of forcing +1 on all numbers.
 */
@Component
public class PhoneNumberMatchingUtil {

    private static final Pattern DIGITS_ONLY_PATTERN = Pattern.compile("[^0-9]");
    private static final Pattern PHONE_VALIDATION_PATTERN = Pattern.compile("^\\+?[1-9]\\d{7,14}$");
    
    /**
     * Normalizes a phone number for storage without making country code assumptions.
     * Only cleans formatting but preserves the actual number structure.
     */
    public static String normalizeForStorage(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        
        // If it's empty after cleaning, return null
        if (cleaned.isEmpty()) {
            return null;
        }
        
        // Validate basic format (must have + and reasonable length)
        if (cleaned.startsWith("+") && PHONE_VALIDATION_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }
        
        // If no + prefix and looks like a reasonable number, keep as-is for now
        // Let the user or admin decide the country code later
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(cleaned).replaceAll("");
        if (digitsOnly.length() >= 7 && digitsOnly.length() <= 15) {
            return cleaned.startsWith("+") ? cleaned : digitsOnly;
        }
        
        return null; // Invalid format
    }
    
    /**
     * Generates multiple possible formats for a phone number to enable flexible matching.
     * This allows us to match phone numbers even when they're stored in different formats.
     */
    public static Set<String> generateMatchingVariants(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> variants = new HashSet<>();
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(phoneNumber).replaceAll("");
        
        if (digitsOnly.length() < 7) {
            return Collections.emptySet(); // Too short to be valid
        }
        
        // Add the original (cleaned)
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        if (!cleaned.isEmpty()) {
            variants.add(cleaned);
        }
        
        // Add digits-only version
        variants.add(digitsOnly);
        
        // If it already has a country code, add without it
        if (cleaned.startsWith("+")) {
            variants.add(digitsOnly);
        }
        
        // For common formats, add likely variants
        if (digitsOnly.length() == 10) {
            // Could be US/Canada without country code
            variants.add("+1" + digitsOnly);
            variants.add("1" + digitsOnly);
        } else if (digitsOnly.length() == 11 && digitsOnly.startsWith("1")) {
            // Could be US/Canada with 1 prefix
            variants.add("+" + digitsOnly);
            variants.add(digitsOnly.substring(1)); // Remove the leading 1
            variants.add("+1" + digitsOnly.substring(1));
        }
        
        // Add common international prefixes for the same base number
        if (!digitsOnly.startsWith("1") && digitsOnly.length() >= 9 && digitsOnly.length() <= 11) {
            // Could be other countries - add some common patterns but don't assume
            variants.add("+" + digitsOnly);
        }
        
        return variants;
    }
    
    /**
     * Checks if two phone numbers could be the same, considering various formatting differences.
     */
    public static boolean couldMatch(String phoneNumber1, String phoneNumber2) {
        if (phoneNumber1 == null || phoneNumber2 == null) {
            return false;
        }
        
        Set<String> variants1 = generateMatchingVariants(phoneNumber1);
        Set<String> variants2 = generateMatchingVariants(phoneNumber2);
        
        // Check if any variants overlap
        for (String variant1 : variants1) {
            if (variants2.contains(variant1)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates if a phone number has a reasonable format without making country assumptions.
     */
    public static boolean isReasonablePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        String digitsOnly = DIGITS_ONLY_PATTERN.matcher(cleaned).replaceAll("");
        
        // Check for obviously invalid patterns
        if (phoneNumber.contains("@") || // Email
            phoneNumber.contains("-") && phoneNumber.length() > 20 || // UUID-like
            phoneNumber.matches(".*[a-zA-Z].*") && !phoneNumber.contains("@")) { // Letters but not email
            return false;
        }
        
        // Check digit count
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }
    
    /**
     * Extracts all possible search variants for database queries.
     * Used when searching for users by phone numbers.
     */
    public static List<String> getSearchVariants(List<String> phoneNumbers) {
        Set<String> allVariants = new HashSet<>();
        
        for (String phoneNumber : phoneNumbers) {
            allVariants.addAll(generateMatchingVariants(phoneNumber));
        }
        
        return new ArrayList<>(allVariants);
    }
} 