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
}
