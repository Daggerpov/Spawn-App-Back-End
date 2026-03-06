package com.danielagapov.spawn.shared.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * This class is used for retrieving HTML email templates from resources/templates.
 * Auth-service version: uses standard Java IO instead of AWS SDK IoUtils.
 */
public class EmailTemplates {
    private static String VERIFY_EMAIL_BODY = null;
    private static String EMAIL_VERIFICATION_CODE_BODY = null;

    public static String getVerifyEmailBody() {
        if (VERIFY_EMAIL_BODY == null) {
            VERIFY_EMAIL_BODY = readHTMLFile("templates/verifyEmailBody.html");
        }
        return VERIFY_EMAIL_BODY;
    }

    public static String getEmailVerificationCodeBody() {
        if (EMAIL_VERIFICATION_CODE_BODY == null) {
            EMAIL_VERIFICATION_CODE_BODY = readHTMLFile("templates/emailVerificationCode.html");
        }
        return EMAIL_VERIFICATION_CODE_BODY;
    }

    private static String readHTMLFile(String resourcePath) {
        try (InputStream inputStream = EmailTemplates.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new RuntimeException("Template not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template: " + resourcePath, e);
        }
    }
}
