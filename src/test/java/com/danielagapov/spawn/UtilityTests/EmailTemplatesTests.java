package com.danielagapov.spawn.UtilityTests;

import com.danielagapov.spawn.shared.util.EmailTemplates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmailTemplatesTests {

    @Test
    void verifyEmailBody_loadsFromClasspath() {
        String body = EmailTemplates.getVerifyEmailBody();
        assertNotNull(body);
        assertFalse(body.isBlank());
    }

    @Test
    void emailVerificationCodeBody_loadsFromClasspath() {
        String body = EmailTemplates.getEmailVerificationCodeBody();
        assertNotNull(body);
        assertFalse(body.isBlank());
    }
}
