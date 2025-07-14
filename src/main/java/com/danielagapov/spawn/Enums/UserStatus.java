package com.danielagapov.spawn.Enums;

/**
 * Each status represents the most recently completed step in the registration process.
 * For example, if a user has status EMAIL_VERIFIED, they have completed the email verification step but not the USERNAME_AND_PHONE_NUMBER step.
 */
public enum UserStatus {
    EMAIL_VERIFIED,
    USERNAME_AND_PHONE_NUMBER,
    NAME_AND_PHOTO,
    ACTIVE
}
