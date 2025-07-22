package com.danielagapov.spawn.Util;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

@Component
public class PhoneNumberValidator {

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
        if (!PHONE_PATTERN.matcher(cleanNumber).matches()) {
            return false;
        }

        // Country-specific validation
        return validateByCountryCode(cleanNumber);
    }

    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        System.out.println("🧹 BACKEND CLEANING PHONE: '" + phoneNumber + "'");

        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^+\\d]", "");
        System.out.println("  Step 1 - remove non-digits except +: '" + cleaned + "'");

        // If no country code, assume it's a local number (add your default country code)
        if (!cleaned.startsWith("+")) {
            cleaned = "+1" + cleaned; // Default to US, change as needed
            System.out.println("  Step 2 - add +1 prefix: '" + cleaned + "'");
        } else {
            System.out.println("  Step 2 - already has + prefix: '" + cleaned + "'");
        }

        System.out.println("  FINAL RESULT: '" + cleaned + "'");
        return cleaned;
    }

    private static boolean validateByCountryCode(String phoneNumber) {
        for (Map.Entry<String, String> entry : COUNTRY_PATTERNS.entrySet()) {
            if (phoneNumber.startsWith(entry.getKey())) {
                return Pattern.compile(entry.getValue()).matcher(phoneNumber).matches();
            }
        }

        // If no specific pattern found, use general validation
        return phoneNumber.length() >= 10 && phoneNumber.length() <= 15;
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
