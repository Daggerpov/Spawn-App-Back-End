# Apple ID Token Implementation Guide

## Overview
This document outlines the steps needed to complete the implementation of Apple ID token verification.

## Dependencies to Add
Add the following dependencies to your `pom.xml`:

```xml
<!-- JWT and JWK handling for Apple Sign In -->
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>jwks-rsa</artifactId>
    <version>0.22.1</version>
</dependency>
```

## Configuration
Add the Apple client ID to your application properties:

```properties
# For local development
apple.client.id=YOUR_APPLE_SERVICE_ID_HERE

# In production, use environment variables
# apple.client.id=${APPLE_CLIENT_ID}
```

## Complete the Implementation

The current implementation in `OAuthService.java` includes placeholders for Apple ID token verification. Once the dependencies are added, replace the placeholder methods with the actual implementation:

1. Update the `verifyAppleIdToken` method to properly decode and verify the Apple ID token
2. Implement the `verifyAppleTokenSignature` method to validate the token's signature using Apple's public keys

## Security Considerations

- Always verify the token's expiration date
- Validate the token's issuer is "https://appleid.apple.com"
- Verify the audience claim matches your app's client ID
- Check for a valid signature using Apple's public keys
- Extract the subject claim (user ID) only after verification

## Mobile App Changes

Update your mobile app to:

1. Send the Apple ID token instead of just the external user ID
2. Include the provider parameter with value "apple"
3. Continue sending the email for additional verification

## Testing

Test both authentication flows:
1. Sign-in with an existing user
2. Creating a new user

For both Google and Apple authentication providers. 