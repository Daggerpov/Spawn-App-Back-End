package com.danielagapov.spawn.Services.Email;

import jakarta.mail.MessagingException;

public interface IEmailService {

    /**
     * Base method used to send emails with given recipient (to), subject, and content.
     * Only public usage is by the test-email endpoint in AuthController
     */
    void sendEmail(String to, String subject, String content) throws MessagingException;

    /**
     * Builds and sends an email to a new user with a link to verify their account.
     * Builds the verification link from the given token.
     */
    void sendVerifyAccountEmail(String to, String token) throws MessagingException;

    /**
     * Builds and sends an email with a verification code to verify a user's email address.
     * @param to the email address to send the verification code to
     * @param verificationCode the 6-digit verification code
     * @param expiryTime the time when the code expires
     */
    void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime) throws MessagingException;
}
