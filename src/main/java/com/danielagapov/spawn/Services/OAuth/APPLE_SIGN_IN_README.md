# Apple Sign In Implementation

This documentation explains how Apple Sign In is implemented in the Spawn App backend.

## Overview

The backend now supports Apple Sign In using OAuth with JWT token verification. The implementation replaces the previous Auth0-based authentication system and provides a secure way to verify Apple ID tokens.

## Configuration

The following configuration is needed for Apple Sign In to work properly:

1. Set the Apple Client ID in your environment variables:
   ```
   APPLE_CLIENT_ID=your.apple.service.id.here
   ```

2. The application.properties file contains the configuration for Apple Sign In:
   ```
   apple.client.id=${APPLE_CLIENT_ID}
   ```

## Implementation Details

The implementation uses the following components:

1. **JWT Libraries**: We use Auth0's JWT and JWK libraries to verify Apple ID tokens.
   - `com.auth0:java-jwt`: For JWT token parsing and verification
   - `com.auth0:jwks-rsa`: For JWK key retrieval and handling

2. **Apple JWK Provider**: We fetch Apple's public keys from their JWKS endpoint to verify token signatures.
   - The JWK Provider caches keys to minimize external requests
   - Keys are fetched from: https://appleid.apple.com/auth/keys

3. **Token Verification Process**:
   - Parse the JWT without verification to extract the Key ID (kid)
   - Retrieve the corresponding public key from Apple's JWKS endpoint
   - Verify the token's signature, issuer, audience, and expiration
   - Extract the subject (user ID) from the verified token

## API Endpoints

The API supports Apple Sign In through the following endpoints:

1. `/api/v1/auth/sign-in`: Checks if a user exists and returns user data if found.
   - Parameters:
     - `idToken`: The Apple ID token
     - `provider`: Set to `apple`
     - `email`: User's email address

2. `/api/v1/auth/make-user`: Creates a new user account using Apple ID token.
   - Parameters:
     - `idToken`: The Apple ID token
     - `provider`: Set to `apple`
     - Request body: UserCreationDTO with user details

## Security Considerations

The implementation includes several security measures:

1. Token signature verification using Apple's public keys
2. Verification of the token's issuer (must be Apple)
3. Verification of the audience claim (must match your app's client ID)
4. Rate limiting for JWK key requests
5. Caching of JWK keys to reduce external requests

## Mobile Integration

The mobile app needs to:

1. Call Sign In with Apple on the device
2. Send the following to the backend:
   - The ID token from Apple
   - The provider parameter with value "apple"
   - The user's email address 