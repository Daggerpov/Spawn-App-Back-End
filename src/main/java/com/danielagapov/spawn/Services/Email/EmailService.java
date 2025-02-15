package com.danielagapov.spawn.Services.Email;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class EmailService implements IEmailService {
    private final JavaMailSender mailSender;
    private final ILogger logger;


    @Override
    // TODO: look into @Async
    public void sendEmail(String to, String subject, String content) throws MessagingException {
        logger.log("Sending email to " + to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@spawn.com");
            helper.setText(content, true);
            mailSender.send(mailSender.createMimeMessage());
        } catch (MessagingException e) {
            logger.log("Failed to send email to " + to);
            throw e;
        }
    }
}
