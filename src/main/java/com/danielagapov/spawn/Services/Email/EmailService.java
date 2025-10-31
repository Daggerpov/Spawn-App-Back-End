package com.danielagapov.spawn.Services.Email;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.danielagapov.spawn.Util.EmailTemplates;
import io.github.cdimascio.dotenv.Dotenv;
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
    private static final String BASE_URL;

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        BASE_URL = dotenv.get("BASE_URL");

    }

    // Dependency to send emails which uses the properties specified in application.properties
    private final JavaMailSender mailSender;
    private final ILogger logger;


    @Override
    // TODO: look into @Async
    public void sendEmail(String to, String subject, String content) throws MessagingException {
        logger.info("Sending email to " + to);
        try {
            // MIME is an internet standard for the format of email messages
            MimeMessage message = mailSender.createMimeMessage();
            // Helper used to populate the MIME message
            MimeMessageHelper mimeHelper = new MimeMessageHelper(message, "utf-8");
            // Add recipient, subject, sender, and content to email
            mimeHelper.setTo(to);
            mimeHelper.setSubject(subject);
            mimeHelper.setFrom(new InternetAddress("Spawn <spawnappmarketing@gmail.com>"));
            mimeHelper.setText(content, true);
            // Send email
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error("Failed to send email to " + to);
            throw e;
        }
    }

    @Override
    public void sendVerifyAccountEmail(String to, String token) throws MessagingException {
        logger.info("Sending verification email to " + to);
        final String link = BASE_URL + token;
        final String content = buildVerifyEmailBody(link);
        final String subject = "Verify Account";
        sendEmail(to, subject, content);
    }

    @Override
    public void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime) throws MessagingException {
        logger.info("Sending verification code email to " + to);
        final String content = buildVerificationCodeEmailBody(verificationCode, expiryTime);
        final String subject = "Your Verification Code: " + verificationCode;
        sendEmail(to, subject, content);
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
