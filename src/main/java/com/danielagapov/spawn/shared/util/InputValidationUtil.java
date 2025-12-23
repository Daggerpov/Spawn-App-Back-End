package com.danielagapov.spawn.shared.util;

import java.util.regex.Pattern;

/**
 * Utility class for input validation and sanitization to prevent injection attacks
 * and ensure data integrity
 */
public class InputValidationUtil {

    // Regex patterns for validation
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{6,14}$"); // E.164 format (7-15 digits)
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s'-]{1,100}$");
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s.,!?'-]{1,500}$");
    
    // Dangerous patterns to detect injection attempts
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|script|javascript|vbscript|onload|onerror).*"
    );
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script|</script|javascript:|vbscript:|onload=|onerror=|<iframe|</iframe).*"
    );
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\.[\\\\/]|[\\\\/]\\.\\.[\\\\/]|\\.\\.\\\\|\\.\\./).*"
    );

    /**
     * Validates username format and content
     */
    public static boolean isValidUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        
        // Reject usernames with leading/trailing spaces
        if (!username.equals(username.trim())) {
            return false;
        }
        
        return USERNAME_PATTERN.matcher(username).matches() && 
               !containsDangerousContent(username);
    }

    /**
     * Validates email format and content
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String cleanEmail = email.trim().toLowerCase();
        return EMAIL_PATTERN.matcher(cleanEmail).matches() && 
               !containsDangerousContent(cleanEmail);
    }

    /**
     * Validates phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String cleanPhone = phoneNumber.replaceAll("[\\s()-]", "");
        return PHONE_PATTERN.matcher(cleanPhone).matches();
    }

    /**
     * Validates name (first name, last name, display name)
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        String cleanName = name.trim();
        return NAME_PATTERN.matcher(cleanName).matches() && 
               !containsDangerousContent(cleanName);
    }

    /**
     * Validates general text input (descriptions, comments, etc.)
     */
    public static boolean isValidText(String text) {
        if (text == null) {
            return true; // null is acceptable for optional fields
        }
        
        if (text.trim().isEmpty()) {
            return true; // empty is acceptable
        }
        
        return SAFE_TEXT_PATTERN.matcher(text.trim()).matches() && 
               !containsDangerousContent(text);
    }

    /**
     * Sanitizes text input by removing dangerous characters
     */
    public static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        
        // Remove HTML tags
        String sanitized = text.replaceAll("<[^>]*>", "");
        
        // Remove script-related content
        sanitized = sanitized.replaceAll("(?i)(javascript:|vbscript:|data:)", "");
        
        // Remove SQL injection attempts
        sanitized = sanitized.replaceAll("(?i)(union|select|insert|update|delete|drop|create|alter|exec)", "");
        
        // Remove path traversal attempts
        sanitized = sanitized.replaceAll("(\\.\\./|\\.\\\\)", "");
        
        // Trim whitespace
        sanitized = sanitized.trim();
        
        return sanitized;
    }

    /**
     * Sanitizes username by removing invalid characters
     */
    public static String sanitizeUsername(String username) {
        if (username == null) {
            return null;
        }
        
        // Keep only allowed characters
        String sanitized = username.replaceAll("[^a-zA-Z0-9._-]", "");
        
        // Ensure length constraints
        if (sanitized.length() > 30) {
            sanitized = sanitized.substring(0, 30);
        }
        
        return sanitized.trim();
    }

    /**
     * Sanitizes email by converting to lowercase and trimming
     */
    public static String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }
        
        return email.trim().toLowerCase();
    }

    /**
     * Validates password strength
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial && !containsDangerousContent(password);
    }

    /**
     * Checks if content contains potentially dangerous patterns
     */
    private static boolean containsDangerousContent(String content) {
        if (content == null) {
            return false;
        }
        
        String lowerContent = content.toLowerCase();
        
        return SQL_INJECTION_PATTERN.matcher(lowerContent).matches() ||
               XSS_PATTERN.matcher(lowerContent).matches() ||
               PATH_TRAVERSAL_PATTERN.matcher(lowerContent).matches();
    }

    /**
     * Validates UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return false;
        }
        
        Pattern uuidPattern = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );
        
        return uuidPattern.matcher(uuid.trim()).matches();
    }

    /**
     * Validates that a string is within allowed length limits
     */
    public static boolean isValidLength(String text, int minLength, int maxLength) {
        if (text == null) {
            return minLength == 0; // null is only valid if minimum length is 0
        }
        
        int length = text.trim().length();
        return length >= minLength && length <= maxLength;
    }
}
