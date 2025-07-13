package com.danielagapov.spawn.Util;


import software.amazon.awssdk.utils.IoUtils;

import java.io.FileInputStream;
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
            VERIFY_EMAIL_BODY = readHTMLFile("src/main/resources/templates/verifyEmailBody.html");
        }
        return VERIFY_EMAIL_BODY;
    }

    public static String getEmailVerificationCodeBody() {
        if (EMAIL_VERIFICATION_CODE_BODY == null) {
            EMAIL_VERIFICATION_CODE_BODY = readHTMLFile("src/main/resources/templates/emailVerificationCode.html");
        }
        return EMAIL_VERIFICATION_CODE_BODY;
    }

    private static String readHTMLFile(String fileName) {
        final InputStream inputStream;
        String content;
        try {
            inputStream = new FileInputStream(fileName);
            content = IoUtils.toUtf8String(inputStream);
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return content;
    }
}
