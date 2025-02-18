package com.danielagapov.spawn.Services.Email;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Util.EmailTemplates;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
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
//            helper.setBcc(new String[]{"spawnappmarketing@gmail.com", "shanemander007@gmail.com", "evanxnawfal@gmail.com", "danielagapov1@gmail.com"});
            helper.setSubject(subject);
            helper.setFrom(new InternetAddress("Spawn <spawnappmarketing@gmail.com>"));
            //helper.setFrom("spawnappmarketing@gmail.com");
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.log("Failed to send email to " + to);
            throw e;
        }
    }

    @Override
    public void sendVerifyAccountEmail(String to, String token) throws MessagingException {
        logger.log("Sending verification email to " + to);
        final String link = "http://localhost:8080/api/v1/auth/verify-email?token=" + token;
        final String content = buildVerifyEmailBody(link);
        final String subject = "Verify Account";
        sendEmail(to, subject, content);
    }

    /**
     * Gets "verify email" template and inserts the link
     */
    private String buildVerifyEmailBody(String link) {
        String verifyEmailBody = EmailTemplates.getVerifyEmailBody();
        return verifyEmailBody.replace("[VERIFICATION_LINK]", link);
    }

}
