package com.danielagapov.spawn.shared.util;


import software.amazon.awssdk.utils.IoUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class is used for retrieving HTML email templates from resources/templates
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

    // Load from the classpath so templates resolve inside the packaged JAR
    private static String readHTMLFile(String resourcePath) {
        try (InputStream inputStream = EmailTemplates.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Classpath resource not found: " + resourcePath);
            }
            return IoUtils.toUtf8String(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
