# Authentication Flows and Validation

## Overview

The Spawn application supports multiple authentication methods, each with different validation requirements, particularly around password handling.

## Authentication Methods

### 1. Email/Password Authentication

**Registration Endpoint:** `POST /api/v1/auth/register`

**DTO:** `AuthUserDTO`

**Fields:**
- `username` - Required, validated with `@ValidUsername` (no spaces, 3-30 chars)
- `email` - Required, validated with `@Email`
- `name` - Optional, validated with `@ValidName` (allows spaces)
- `bio` - Optional, max 500 characters
- `password` - **REQUIRED**, validated with `@NotBlank` and `@Size(min=8)`

**Flow:**
1. User provides email, username, and password
2. Backend validates all fields including password
3. Password is hashed with bcrypt
4. User is created with status `EMAIL_VERIFIED` (after verification)
5. User can log in with email/username and password

**Example Request:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "name": "John Doe",
  "password": "SecurePass123!"
}
```

### 2. OAuth Authentication (Google, Apple)

**Registration Endpoint:** `POST /api/v1/auth/register/oauth`

**DTO:** `OAuthRegistrationDTO`

**Fields:**
- `email` - Required, validated with `@Email` and `@NotBlank`
- `idToken` - Required, validated with `@NotBlank`
- `provider` - Required (GOOGLE or APPLE)
- `name` - Optional, validated with `@ValidName` (allows spaces)
- `profilePictureUrl` - Optional
- **NO PASSWORD FIELD** ❌

**Flow:**
1. User authenticates with Google/Apple
2. Mobile app receives ID token from provider
3. Backend validates ID token with provider
4. User is created with status `EMAIL_VERIFIED` immediately
5. Username and phone number set to null (provided during onboarding)
6. **No password is stored** - authentication is via OAuth provider
7. User can sign in with OAuth in future sessions

**Example Request:**
```json
{
  "email": "john@gmail.com",
  "idToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "provider": "GOOGLE",
  "name": "John Doe",
  "profilePictureUrl": "https://lh3.googleusercontent.com/..."
}
```

**Sign-In Endpoint:** `GET /api/v1/auth/sign-in`

**Parameters:**
- `idToken` - OAuth ID token from provider
- `provider` - GOOGLE or APPLE
- `email` - Optional, for additional verification

### 3. Legacy OAuth Flow (Deprecated)

**Endpoint:** `POST /api/v1/auth/make-user`

**DTO:** `UserCreationDTO`

**Fields:**
- Inherits from `AbstractUserDTO` (username, email, name, bio)
- `profilePictureData` - Raw image bytes
- **NO PASSWORD FIELD** ❌

**Note:** This endpoint is deprecated and kept for backward compatibility.

## Validation Rules by Authentication Method

| Field | Email/Password Auth | OAuth Auth | Legacy OAuth |
|-------|-------------------|-----------|--------------|
| **Password** | ✅ Required (`@NotBlank`) | ❌ Not present | ❌ Not present |
| **Username** | ⚠️ Optional (set during onboarding) | ⚠️ Optional (set during onboarding) | ⚠️ Optional |
| **Email** | ✅ Required (`@Email`) | ✅ Required (`@Email`, `@NotBlank`) | ✅ Required (`@Email`) |
| **Name** | ⚠️ Optional (`@ValidName`) | ⚠️ Optional (`@ValidName`) | ⚠️ Optional (`@ValidName`) |
| **ID Token** | ❌ Not present | ✅ Required (`@NotBlank`) | ✅ Required |
| **Provider** | ❌ Not present | ✅ Required (`@NotNull`) | ✅ Required |

## User Status Flow

### Email/Password Users
1. `PENDING` → User registered, email not verified
2. `EMAIL_VERIFIED` → User verified email
3. `USERNAME_AND_PHONE_NUMBER` → User provided username and phone
4. `NAME_AND_PHOTO` → User provided name and photo
5. `CONTACT_IMPORT` → User imported contacts
6. `ACTIVE` → User is fully onboarded

### OAuth Users
1. `EMAIL_VERIFIED` → User authenticated with OAuth (starts here, skips PENDING)
2. `USERNAME_AND_PHONE_NUMBER` → User provided username and phone
3. `NAME_AND_PHOTO` → User provided name and photo (may already have from OAuth)
4. `CONTACT_IMPORT` → User imported contacts
5. `ACTIVE` → User is fully onboarded

## Password Handling

### Email/Password Auth
- Password is validated on registration
- Minimum 8 characters (enforced by `@Size(min=8)`)
- Recommended: uppercase, lowercase, digit, special char (checked by `InputValidationUtil.isStrongPassword()`)
- Password is hashed with bcrypt before storage
- Never stored in plain text
- User can change password via `/api/v1/auth/change-password`

### OAuth Auth
- **No password is created or stored**
- Authentication is always via OAuth provider
- If OAuth user tries to log in with email/password, it will fail
- OAuth users cannot use password-based login
- OAuth users cannot change password (they don't have one)

## Security Considerations

### OAuth Token Validation
- ID tokens are verified with the OAuth provider (Google/Apple)
- External user IDs are mapped to internal user IDs
- Prevents token replay and spoofing attacks
- Tokens are validated on every OAuth sign-in

### Password Security (Email/Password Auth Only)
- Passwords hashed with bcrypt (work factor 10)
- Password validation prevents weak passwords
- No password complexity requirements enforced at validation level (but recommended)
- Rate limiting on login attempts (handled by auth service)

### Mixed Authentication
- Users cannot have both OAuth and email/password authentication
- If email exists with OAuth, cannot create email/password account
- If email exists with password, cannot create OAuth account
- Prevents account confusion and security issues

## API Responses

### Successful Registration (Any Method)
```json
{
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "username": null,  // Set during onboarding
    "name": "John Doe",
    "profilePicture": "https://...",
    "hasCompletedOnboarding": false
  },
  "status": "EMAIL_VERIFIED"
}
```

### Validation Errors
```json
{
  "message": "Username must be 3-30 characters and contain only letters, numbers, dots, underscores, and hyphens (no spaces)"
}
```

or

```json
{
  "message": "Password is required"
}
```

## Testing Different Flows

### Test Email/Password Registration
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "email": "test@example.com",
    "name": "Test User",
    "password": "SecurePass123!"
  }'
```

### Test OAuth Registration (Google)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/oauth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@gmail.com",
    "idToken": "<google-id-token>",
    "provider": "GOOGLE",
    "name": "Test User"
  }'
```

### Test Invalid Password (Email Auth)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "email": "test@example.com",
    "password": ""
  }'
# Expected: 400 Bad Request - "Password is required"
```

### Test Missing Password (OAuth - Should Succeed)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register/oauth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@gmail.com",
    "idToken": "<google-id-token>",
    "provider": "GOOGLE",
    "name": "Test User"
  }'
# Expected: 200 OK - OAuth users don't have passwords
```

## Frontend Considerations

### Mobile App Validation

**Email/Password Registration Form:**
- Show password field with strength indicator
- Validate password is at least 8 characters
- Recommend strong password (uppercase, lowercase, digit, special char)
- Show error if password is empty

**OAuth Registration Flow:**
- **Do not show password field**
- Use native Google/Apple sign-in buttons
- Pass ID token to backend
- No password storage on device

### Handling Different User Types

```swift
// Check if user has OAuth provider
if user.isOAuthUser {
    // Hide password change option
    // Show "Connected with Google/Apple"
} else {
    // Show password change option
    // Allow password-based login
}
```

## Conclusion

The validation system correctly handles different authentication methods:
- **Email/Password users:** Password is required and validated
- **OAuth users:** No password field, validated via OAuth provider
- Each flow has appropriate validation for its specific requirements
- Password validation only applies where passwords are actually used

