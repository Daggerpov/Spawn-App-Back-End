package com.danielagapov.spawn.shared.util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public final class PhoneNumberValidator {

    private static final String PHONE_REGEX = "^\\+?[1-9]\\d{1,14}$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    // Common country codes and their specific patterns
    private static final Map<String, String> COUNTRY_PATTERNS = Map.of(
            "+1", "^\\+1[2-9]\\d{2}[2-9]\\d{2}\\d{4}$", // US/Canada
            "+44", "^\\+44[1-9]\\d{8,9}$", // UK
            "+91", "^\\+91[6-9]\\d{9}$", // India
            "+86", "^\\+86[1][3-9]\\d{9}$", // China
            "+33", "^\\+33[1-9]\\d{8}$" // France
    );

    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        String cleanNumber = cleanPhoneNumber(phoneNumber);

        // Basic format validation
        if (cleanNumber == null || !PHONE_PATTERN.matcher(cleanNumber).matches()) {
            return false;
        }

        // Country-specific validation
        return validateByCountryCode(cleanNumber);
    }

    /**
     * Cleans a phone number without making assumptions about country codes.
     * This replaces the old approach that defaulted to +1.
     */
    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        System.out.println("🧹 BACKEND CLEANING PHONE: '" + phoneNumber + "'");

        // Check if it's obviously not a phone number first
        if (!PhoneNumberMatchingUtil.isReasonablePhoneNumber(phoneNumber)) {
            System.out.println("  REJECTED: Not a reasonable phone number format");
            return null;
        }

        // Use the new matching util for normalization
        String normalized = PhoneNumberMatchingUtil.normalizeForStorage(phoneNumber);
        System.out.println("  NORMALIZED: '" + normalized + "'");

        // If we got a clean result, return it
        if (normalized != null && !normalized.trim().isEmpty()) {
            System.out.println("  FINAL RESULT: '" + normalized + "'");
            return normalized;
        }

        System.out.println("  FINAL RESULT: null (invalid)");
        return null;
    }

    private static boolean validateByCountryCode(String phoneNumber) {
        // If it has a + prefix, try country-specific validation
        if (phoneNumber.startsWith("+")) {
            for (Map.Entry<String, String> entry : COUNTRY_PATTERNS.entrySet()) {
                if (phoneNumber.startsWith(entry.getKey())) {
                    return Pattern.compile(entry.getValue()).matcher(phoneNumber).matches();
                }
            }
        }

        // For numbers without country codes, use general validation
        // Don't assume any specific country
        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }

    public static String getCountryCode(String phoneNumber) {
        String cleaned = cleanPhoneNumber(phoneNumber);
        if (cleaned == null || !cleaned.startsWith("+")) return null;

        for (String countryCode : COUNTRY_PATTERNS.keySet()) {
            if (cleaned.startsWith(countryCode)) {
                return countryCode;
            }
        }

        return null;
    }
}
