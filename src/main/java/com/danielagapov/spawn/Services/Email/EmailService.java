package com.danielagapov.spawn.Services.Email;

import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;


    @Override
    public void sendEmail(String to, String email) {

    }
}
