package com.danielagapov.spawn.shared.validation;

import com.danielagapov.spawn.shared.util.InputValidationUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for input validation utilities to ensure usernames, names, emails, etc.
 * follow standard conventions
 */
public class ValidationTest {

    @Test
    public void testValidUsername() {
        // Valid usernames
        assertTrue(InputValidationUtil.isValidUsername("john_doe"));
        assertTrue(InputValidationUtil.isValidUsername("user123"));
        assertTrue(InputValidationUtil.isValidUsername("test.user"));
        assertTrue(InputValidationUtil.isValidUsername("my-username"));
        assertTrue(InputValidationUtil.isValidUsername("abc"));
        
        // Invalid usernames - with spaces
        assertFalse(InputValidationUtil.isValidUsername("john doe"));
        assertFalse(InputValidationUtil.isValidUsername("user name"));
        assertFalse(InputValidationUtil.isValidUsername(" username"));
        assertFalse(InputValidationUtil.isValidUsername("username "));
        
        // Invalid usernames - too short
        assertFalse(InputValidationUtil.isValidUsername("ab"));
        assertFalse(InputValidationUtil.isValidUsername("a"));
        
        // Invalid usernames - too long
        assertFalse(InputValidationUtil.isValidUsername("a".repeat(31)));
        
        // Invalid usernames - special characters
        assertFalse(InputValidationUtil.isValidUsername("user@name"));
        assertFalse(InputValidationUtil.isValidUsername("user#name"));
        assertFalse(InputValidationUtil.isValidUsername("user$name"));
        assertFalse(InputValidationUtil.isValidUsername("user%name"));
        
        // Invalid usernames - null or empty
        assertFalse(InputValidationUtil.isValidUsername(null));
        assertFalse(InputValidationUtil.isValidUsername(""));
        assertFalse(InputValidationUtil.isValidUsername("   "));
    }

    @Test
    public void testValidName() {
        // Valid names
        assertTrue(InputValidationUtil.isValidName("John Doe"));
        assertTrue(InputValidationUtil.isValidName("Mary Jane"));
        assertTrue(InputValidationUtil.isValidName("O'Brien"));
        assertTrue(InputValidationUtil.isValidName("Jean-Pierre"));
        assertTrue(InputValidationUtil.isValidName("Anne-Marie"));
        assertTrue(InputValidationUtil.isValidName("John"));
        
        // Valid names with multiple spaces
        assertTrue(InputValidationUtil.isValidName("John Michael Doe"));
        
        // Invalid names - numbers
        assertFalse(InputValidationUtil.isValidName("John123"));
        assertFalse(InputValidationUtil.isValidName("123John"));
        
        // Invalid names - special characters
        assertFalse(InputValidationUtil.isValidName("John@Doe"));
        assertFalse(InputValidationUtil.isValidName("John#Doe"));
        assertFalse(InputValidationUtil.isValidName("John$Doe"));
        
        // Invalid names - too long
        assertFalse(InputValidationUtil.isValidName("a".repeat(101)));
        
        // Invalid names - null or empty
        assertFalse(InputValidationUtil.isValidName(null));
        assertFalse(InputValidationUtil.isValidName(""));
        assertFalse(InputValidationUtil.isValidName("   "));
    }

    @Test
    public void testValidEmail() {
        // Valid emails
        assertTrue(InputValidationUtil.isValidEmail("test@example.com"));
        assertTrue(InputValidationUtil.isValidEmail("user.name@example.com"));
        assertTrue(InputValidationUtil.isValidEmail("user+tag@example.co.uk"));
        assertTrue(InputValidationUtil.isValidEmail("test_user@example.com"));
        
        // Invalid emails - no @
        assertFalse(InputValidationUtil.isValidEmail("testexample.com"));
        
        // Invalid emails - no domain
        assertFalse(InputValidationUtil.isValidEmail("test@"));
        
        // Invalid emails - no local part
        assertFalse(InputValidationUtil.isValidEmail("@example.com"));
        
        // Invalid emails - spaces
        assertFalse(InputValidationUtil.isValidEmail("test user@example.com"));
        assertFalse(InputValidationUtil.isValidEmail("test@exam ple.com"));
        
        // Invalid emails - null or empty
        assertFalse(InputValidationUtil.isValidEmail(null));
        assertFalse(InputValidationUtil.isValidEmail(""));
        assertFalse(InputValidationUtil.isValidEmail("   "));
    }

    @Test
    public void testValidPhoneNumber() {
        // Valid phone numbers - E.164 format
        assertTrue(InputValidationUtil.isValidPhoneNumber("+14155552671"));
        assertTrue(InputValidationUtil.isValidPhoneNumber("+442071838750"));
        assertTrue(InputValidationUtil.isValidPhoneNumber("+61412345678"));
        assertTrue(InputValidationUtil.isValidPhoneNumber("14155552671"));
        
        // Valid phone numbers with formatting (should be cleaned)
        assertTrue(InputValidationUtil.isValidPhoneNumber("+1 (415) 555-2671"));
        assertTrue(InputValidationUtil.isValidPhoneNumber("+1-415-555-2671"));
        
        // Invalid phone numbers - too short
        assertFalse(InputValidationUtil.isValidPhoneNumber("+1"));
        assertFalse(InputValidationUtil.isValidPhoneNumber("123"));
        
        // Invalid phone numbers - too long
        assertFalse(InputValidationUtil.isValidPhoneNumber("+1" + "2".repeat(20)));
        
        // Invalid phone numbers - letters
        assertFalse(InputValidationUtil.isValidPhoneNumber("+1415ABC2671"));
        
        // Invalid phone numbers - null or empty
        assertFalse(InputValidationUtil.isValidPhoneNumber(null));
        assertFalse(InputValidationUtil.isValidPhoneNumber(""));
        assertFalse(InputValidationUtil.isValidPhoneNumber("   "));
    }

    @Test
    public void testSanitizeUsername() {
        // Test removing invalid characters
        assertEquals("johndoe", InputValidationUtil.sanitizeUsername("john doe"));
        assertEquals("john_doe", InputValidationUtil.sanitizeUsername("john_doe"));
        assertEquals("johndoe", InputValidationUtil.sanitizeUsername("john@doe"));
        assertEquals("johndoe123", InputValidationUtil.sanitizeUsername("john#doe$123"));
        
        // Test length constraint
        assertEquals("a".repeat(30), InputValidationUtil.sanitizeUsername("a".repeat(50)));
        
        // Test null handling
        assertNull(InputValidationUtil.sanitizeUsername(null));
    }

    @Test
    public void testSanitizeEmail() {
        // Test trimming and lowercasing
        assertEquals("test@example.com", InputValidationUtil.sanitizeEmail("Test@Example.com"));
        assertEquals("test@example.com", InputValidationUtil.sanitizeEmail("  test@example.com  "));
        assertEquals("test@example.com", InputValidationUtil.sanitizeEmail("TEST@EXAMPLE.COM"));
        
        // Test null handling
        assertNull(InputValidationUtil.sanitizeEmail(null));
    }

    @Test
    public void testPasswordStrength() {
        // Valid strong passwords
        assertTrue(InputValidationUtil.isStrongPassword("Password123!"));
        assertTrue(InputValidationUtil.isStrongPassword("MyP@ssw0rd"));
        assertTrue(InputValidationUtil.isStrongPassword("Str0ng!Pass"));
        
        // Invalid passwords - too short
        assertFalse(InputValidationUtil.isStrongPassword("Pass1!"));
        assertFalse(InputValidationUtil.isStrongPassword("Abc123!"));
        
        // Invalid passwords - missing uppercase
        assertFalse(InputValidationUtil.isStrongPassword("password123!"));
        
        // Invalid passwords - missing lowercase
        assertFalse(InputValidationUtil.isStrongPassword("PASSWORD123!"));
        
        // Invalid passwords - missing digit
        assertFalse(InputValidationUtil.isStrongPassword("Password!"));
        
        // Invalid passwords - missing special character
        assertFalse(InputValidationUtil.isStrongPassword("Password123"));
        
        // Invalid passwords - null
        assertFalse(InputValidationUtil.isStrongPassword(null));
    }
}

