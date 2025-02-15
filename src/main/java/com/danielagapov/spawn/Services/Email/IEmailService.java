package com.danielagapov.spawn.Services.Email;

import jakarta.mail.MessagingException;

public interface IEmailService {

    void sendEmail(String to, String subject, String content) throws MessagingException;
}
