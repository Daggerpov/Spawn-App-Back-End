# Input Validation Implementation

## Overview

This document describes the comprehensive input validation system implemented to ensure data integrity and security across the Spawn application. The validation system follows standard conventions and prevents common security vulnerabilities.

## Validation Rules

### Username Validation

**Rules:**
- Length: 3-30 characters
- Allowed characters: letters (a-z, A-Z), numbers (0-9), dots (.), underscores (_), hyphens (-)
- **No spaces allowed**
- No special characters (@, #, $, %, etc.)
- No SQL injection or XSS patterns

**Examples:**
- ✅ Valid: `john_doe`, `user123`, `test.user`, `my-username`
- ❌ Invalid: `john doe` (space), `user@name` (special char), `ab` (too short)

### Name Validation (First Name, Last Name, Display Name)

**Rules:**
- Length: 1-100 characters
- Allowed characters: letters (a-z, A-Z), spaces, hyphens (-), apostrophes (')
- **Spaces are allowed** (for multi-part names)
- No numbers or special characters
- No SQL injection or XSS patterns

**Examples:**
- ✅ Valid: `John Doe`, `Mary-Jane`, `O'Brien`, `Jean Pierre`
- ❌ Invalid: `John123` (numbers), `John@Doe` (special char)

### Email Validation

**Rules:**
- Must follow standard email format: `local@domain.tld`
- Case-insensitive (automatically converted to lowercase)
- No spaces allowed
- No SQL injection or XSS patterns

**Examples:**
- ✅ Valid: `test@example.com`, `user.name@example.com`, `user+tag@example.co.uk`
- ❌ Invalid: `testexample.com` (no @), `test user@example.com` (space)

### Phone Number Validation

**Rules:**
- E.164 format: `+[country code][number]`
- 1-15 digits (after country code)
- Optional + prefix
- Spaces, parentheses, and hyphens are stripped during validation

**Examples:**
- ✅ Valid: `+14155552671`, `+442071838750`, `+1 (415) 555-2671`
- ❌ Invalid: `+1` (too short), `+1415ABC2671` (letters)

### Bio/Description Validation

**Rules:**
- Maximum length: 500 characters
- Allows letters, numbers, spaces, and common punctuation (.,!?'-)
- No SQL injection or XSS patterns

### Password Validation

**Rules:**
- Minimum length: 8 characters
- Must contain at least one uppercase letter
- Must contain at least one lowercase letter
- Must contain at least one digit
- Must contain at least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)
- No SQL injection or XSS patterns

## Implementation Layers

### 1. Custom Validation Annotations (Jakarta Bean Validation)

Located in `src/main/java/com/danielagapov/spawn/shared/validation/`:

- `@ValidUsername` - Validates username format
- `@ValidName` - Validates name format
- `@ValidPhoneNumber` - Validates phone number format
- Plus standard Jakarta annotations: `@Email`, `@NotBlank`, `@Size`, `@NotNull`

**Usage in DTOs:**
```java
public class UserUpdateDTO {
    @ValidUsername(optional = true)
    private String username;
    
    @ValidName(optional = true)
    private String name;
    
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
}
```

### 2. Controller-Level Validation

Controllers use `@Valid` annotation to trigger automatic validation:

```java
@PostMapping("register")
public ResponseEntity<UserDTO> register(@Valid @RequestBody AuthUserDTO authUserDTO) {
    // Validation happens automatically before this method is called
}
```

**Controllers with validation:**
- `AuthController` - Registration, login, user details update
- `UserController` - User profile updates

### 3. Service-Level Validation (Fallback)

Services perform additional validation as a defense-in-depth measure:

```java
public UserDTO registerUser(AuthUserDTO authUserDTO) {
    // Validate input fields
    if (!InputValidationUtil.isValidUsername(authUserDTO.getUsername())) {
        throw new IllegalArgumentException("Username must be 3-30 characters...");
    }
    // ... rest of the method
}
```

**Services with validation:**
- `AuthService.registerUser()` - Validates username, email, name
- `AuthService.updateUserDetails()` - Validates username, phone number
- `UserService.updateUser()` - Validates username, name, bio

### 4. Utility-Level Validation

The `InputValidationUtil` class provides reusable validation methods:

**Validation methods:**
- `isValidUsername(String)` - Returns true if username is valid
- `isValidName(String)` - Returns true if name is valid
- `isValidEmail(String)` - Returns true if email is valid
- `isValidPhoneNumber(String)` - Returns true if phone number is valid
- `isStrongPassword(String)` - Returns true if password meets strength requirements
- `isValidText(String)` - Returns true if general text is safe

**Sanitization methods:**
- `sanitizeUsername(String)` - Removes invalid characters from username
- `sanitizeEmail(String)` - Trims and lowercases email
- `sanitizeText(String)` - Removes dangerous content from text

## Security Features

### SQL Injection Prevention

The validation system detects and rejects inputs containing SQL injection patterns:
- Keywords: `union`, `select`, `insert`, `update`, `delete`, `drop`, `create`, `alter`, `exec`
- These patterns are blocked in all validated fields

### XSS Prevention

The validation system detects and rejects inputs containing XSS patterns:
- HTML tags: `<script>`, `<iframe>`
- JavaScript protocols: `javascript:`, `vbscript:`
- Event handlers: `onload=`, `onerror=`

### Path Traversal Prevention

The validation system detects and rejects inputs containing path traversal patterns:
- Patterns: `../`, `..\\`, `/../`, `\\..\\`

## Testing

Comprehensive tests are provided in `ValidationTest.java`:

```bash
# Run validation tests
./mvnw test -Dtest=ValidationTest
```

**Test coverage:**
- Username validation (valid and invalid cases)
- Name validation (with spaces, hyphens, apostrophes)
- Email validation
- Phone number validation
- Password strength validation
- Sanitization methods

## Error Handling

When validation fails, the system returns appropriate HTTP status codes:

- `400 Bad Request` - Invalid input format
- `409 Conflict` - Username or email already exists

**Error response format:**
```json
{
  "message": "Username must be 3-30 characters and contain only letters, numbers, dots, underscores, and hyphens (no spaces)"
}
```

## Migration Notes

### Existing Data

If you have existing data that doesn't meet the new validation rules:

1. **Usernames with spaces**: These will need to be updated. Use the `sanitizeUsername()` method to remove spaces.
2. **Names with numbers**: These will need to be cleaned up manually.
3. **Invalid emails**: These should be corrected or removed.

### Database Constraints

The validation rules complement existing database constraints:
- `username` - UNIQUE, nullable
- `email` - UNIQUE, nullable
- `phoneNumber` - UNIQUE, nullable
- `name` - nullable

## Best Practices

1. **Always validate at multiple layers**: DTO annotations, controller @Valid, service-level checks
2. **Use sanitization when appropriate**: Clean user input before storage
3. **Provide clear error messages**: Help users understand what went wrong
4. **Log validation failures**: Monitor for potential security issues
5. **Keep validation rules consistent**: Between frontend and backend

## Future Enhancements

Potential improvements to consider:

1. **Configurable validation rules**: Allow admins to adjust username/name requirements
2. **Internationalization**: Support for non-Latin characters in names
3. **Custom error messages**: Localized validation messages
4. **Rate limiting**: Prevent brute-force validation attacks
5. **Validation metrics**: Track validation failure rates

## References

- Jakarta Bean Validation: https://jakarta.ee/specifications/bean-validation/
- E.164 Phone Number Format: https://en.wikipedia.org/wiki/E.164
- OWASP Input Validation: https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html

