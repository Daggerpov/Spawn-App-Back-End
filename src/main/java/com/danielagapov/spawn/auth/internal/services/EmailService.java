package com.danielagapov.spawn.auth.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EmailTemplates;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class EmailService implements IEmailService {
    private static final String BASE_URL;

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        BASE_URL = dotenv.get("BASE_URL");

    }

    // Dependency to send emails which uses the properties specified in application.properties
    private final JavaMailSender mailSender;
    private final ILogger logger;


    @Override
    @Async("emailTaskExecutor")
    public void sendEmail(String to, String subject, String content) {
        logger.info("Sending email asynchronously to " + to);
        try {
            sendMimeEmail(to, subject, content);
            logger.info("Email sent successfully to " + to);
        } catch (MessagingException e) {
            logger.error("Failed to send email to " + to + ": " + e.getMessage());
            // Exception is logged but not re-thrown since this is an async method
        } catch (Exception e) {
            logger.error("Unexpected error sending email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendVerifyAccountEmail(String to, String token) {
        logger.info("Sending verification email asynchronously to " + to);
        try {
            final String link = BASE_URL + token;
            final String content = buildVerifyEmailBody(link);
            final String subject = "Verify Account";
            
            sendMimeEmail(to, subject, content);
            logger.info("Verification email sent successfully to " + to);
        } catch (MessagingException e) {
            logger.error("Failed to send verification email to " + to + ": " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending verification email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime) {
        logger.info("Sending verification code email asynchronously to " + to);
        try {
            final String content = buildVerificationCodeEmailBody(verificationCode, expiryTime);
            final String subject = "Your Verification Code: " + verificationCode;
            
            sendMimeEmail(to, subject, content);
            logger.info("Verification code email sent successfully to " + to);
        } catch (MessagingException e) {
            logger.error("Failed to send verification code email to " + to + ": " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending verification code email to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Creates and sends a MIME email message with the provided details.
     * MIME (Multipurpose Internet Mail Extensions) is an internet standard for email message format.
     * 
     * @param to The recipient email address
     * @param subject The email subject line
     * @param content The HTML content of the email
     * @throws MessagingException if there's an error creating or sending the email
     */
    private void sendMimeEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper mimeHelper = new MimeMessageHelper(message, "utf-8");
        mimeHelper.setTo(to);
        mimeHelper.setSubject(subject);
        mimeHelper.setFrom(new InternetAddress("Spawn <spawnappmarketing@gmail.com>"));
        mimeHelper.setText(content, true); // true enables HTML
        mailSender.send(message);
    }

    /**
     * Gets the "verify email" template and inserts the link
     */
    private String buildVerifyEmailBody(String link) {
        String verifyEmailBody = EmailTemplates.getVerifyEmailBody();
        return verifyEmailBody.replace("[VERIFICATION_LINK]", link);
    }

    /**
     * Gets the "verification code" template and inserts the code and expiry time
     */
    private String buildVerificationCodeEmailBody(String verificationCode, String expiryTime) {
        String verificationCodeBody = EmailTemplates.getEmailVerificationCodeBody();
        return verificationCodeBody
                .replace("[VERIFICATION_CODE]", verificationCode)
                .replace("[EXPIRY_TIME]", expiryTime);
    }

}

