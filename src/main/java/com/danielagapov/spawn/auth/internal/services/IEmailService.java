package com.danielagapov.spawn.auth.internal.services;

public interface IEmailService {

    /**
     * Base method used to send emails with given recipient (to), subject, and content.
     * Only public usage is by the test-email endpoint in AuthController.
     * This method executes asynchronously and handles exceptions internally.
     */
    void sendEmail(String to, String subject, String content);

    /**
     * Builds and sends an email to a new user with a link to verify their account.
     * Builds the verification link from the given token.
     * This method executes asynchronously and handles exceptions internally.
     */
    void sendVerifyAccountEmail(String to, String token);

    /**
     * Builds and sends an email with a verification code to verify a user's email address.
     * This method executes asynchronously and handles exceptions internally.
     * @param to the email address to send the verification code to
     * @param verificationCode the 6-digit verification code
     * @param expiryTime the time when the code expires
     */
    void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime);

    /**
     * Synchronous variant for verification code emails. Used during registration so that
     * SMTP failures propagate to the API response instead of failing silently.
     * @param to the email address to send the verification code to
     * @param verificationCode the 6-digit verification code
     * @param expiryTime the time when the code expires
     * @throws jakarta.mail.MessagingException if there's an error sending the email
     */
    void sendVerificationCodeEmailSync(String to, String verificationCode, String expiryTime) throws jakarta.mail.MessagingException;
}
