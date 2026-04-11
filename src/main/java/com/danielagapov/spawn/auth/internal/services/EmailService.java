package com.danielagapov.spawn.auth.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EmailTemplates;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
public class EmailService implements IEmailService {
    private static final String BASE_URL;

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        BASE_URL = dotenv.get("BASE_URL");
    }

    private final JavaMailSender mailSender;
    private final ILogger logger;
    private final Resend resend;
    private final String fromEmail;

    public EmailService(
            JavaMailSender mailSender,
            ILogger logger,
            @Value("${resend.api.key:}") String resendApiKey,
            @Value("${resend.from.email:Spawn <noreply@getspawn.com>}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.logger = logger;
        this.fromEmail = fromEmail;
        this.resend = (resendApiKey != null && !resendApiKey.isBlank())
                ? new Resend(resendApiKey)
                : null;
    }

    private boolean isResendAvailable() {
        return resend != null;
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendEmail(String to, String subject, String content) {
        logger.info("Sending email asynchronously to " + to);
        try {
            sendViaResendOrSmtp(to, subject, content);
            logger.info("Email sent successfully to " + to);
        } catch (Exception e) {
            logger.error("Failed to send email to " + to + ": " + e.getMessage());
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

            sendViaResendOrSmtp(to, subject, content);
            logger.info("Verification email sent successfully to " + to);
        } catch (Exception e) {
            logger.error("Failed to send verification email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime) {
        logger.info("Sending verification code email asynchronously to " + to);
        try {
            final String content = buildVerificationCodeEmailBody(verificationCode, expiryTime);
            final String subject = "Your Verification Code: " + verificationCode;

            sendViaResendOrSmtp(to, subject, content);
            logger.info("Verification code email sent successfully to " + to);
        } catch (Exception e) {
            logger.error("Failed to send verification code email to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Sends an email via Resend HTTP API if configured, otherwise falls back to SMTP.
     */
    private void sendViaResendOrSmtp(String to, String subject, String content) throws MessagingException, ResendException {
        if (isResendAvailable()) {
            sendViaResend(to, subject, content);
        } else {
            logger.info("Resend API key not configured, falling back to SMTP");
            sendMimeEmail(to, subject, content);
        }
    }

    private void sendViaResend(String to, String subject, String content) throws ResendException {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(content)
                .build();

        CreateEmailResponse response = resend.emails().send(params);
        logger.info("Email sent via Resend (id: " + response.getId() + ") to " + to);
    }

    private void sendMimeEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper mimeHelper = new MimeMessageHelper(message, "utf-8");
        mimeHelper.setTo(to);
        mimeHelper.setSubject(subject);
        mimeHelper.setFrom(new InternetAddress("Spawn <spawnappmarketing@gmail.com>"));
        mimeHelper.setText(content, true);
        mailSender.send(message);
    }

    private String buildVerifyEmailBody(String link) {
        String verifyEmailBody = EmailTemplates.getVerifyEmailBody();
        return verifyEmailBody.replace("[VERIFICATION_LINK]", link);
    }

    private String buildVerificationCodeEmailBody(String verificationCode, String expiryTime) {
        String verificationCodeBody = EmailTemplates.getEmailVerificationCodeBody();
        return verificationCodeBody
                .replace("[VERIFICATION_CODE]", verificationCode)
                .replace("[EXPIRY_TIME]", expiryTime);
    }
}
