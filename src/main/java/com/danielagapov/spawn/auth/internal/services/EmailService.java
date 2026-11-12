package com.danielagapov.spawn.auth.internal.services;

import com.danielagapov.spawn.shared.exceptions.Logger.ILogger;
import com.danielagapov.spawn.shared.util.EmailTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;


/**
 * Sends email through Resend's HTTPS API when RESEND_API_KEY is set, otherwise over SMTP.
 * Production runs on Railway, which blocks outbound SMTP on non-Pro plans, so prod must use Resend.
 */
@Service
public class EmailService implements IEmailService {
    private static final String BASE_URL;
    private static final String RESEND_API_KEY;
    private static final String RESEND_FROM;

    private static final String SMTP_FROM = "Spawn <spawnappmarketing@gmail.com>";
    private static final String REPLY_TO = "spawnappmarketing@gmail.com";
    private static final URI RESEND_URI = URI.create("https://api.resend.com/emails");

    static {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        BASE_URL = dotenv.get("BASE_URL");
        RESEND_API_KEY = dotenv.get("RESEND_API_KEY");
        RESEND_FROM = dotenv.get("RESEND_FROM", "Spawn <noreply@getspawn.com>");
    }

    // Uses the SMTP properties in application.properties; only used when Resend isn't configured
    private final JavaMailSender mailSender;
    private final ILogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmailService(JavaMailSender mailSender, ILogger logger) {
        this.mailSender = mailSender;
        this.logger = logger;
        logger.info("Email delivery via " + (usesResend() ? "Resend API" : "SMTP"));
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendEmail(String to, String subject, String content) {
        logger.info("Sending email asynchronously to " + to);
        try {
            deliver(to, subject, content);
            logger.info("Email sent successfully to " + to);
        } catch (Exception e) {
            // Logged, not re-thrown, since nothing is waiting on this async call
            logger.error("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void sendVerifyAccountEmail(String to, String token) {
        logger.info("Sending verification email asynchronously to " + to);
        try {
            deliver(to, "Verify Account", buildVerifyEmailBody(BASE_URL + token));
            logger.info("Verification email sent successfully to " + to);
        } catch (Exception e) {
            logger.error("Failed to send verification email to " + to + ": " + e.getMessage());
        }
    }

    @Override
    public void sendVerificationCodeEmail(String to, String verificationCode, String expiryTime) {
        logger.info("Sending verification code email to " + to);
        try {
            deliver(to, "Your Verification Code: " + verificationCode,
                    buildVerificationCodeEmailBody(verificationCode, expiryTime));
        } catch (Exception e) {
            logger.error("Failed to send verification code email to " + to + ": " + e.getMessage());
            throw new IllegalStateException("Failed to send verification code email", e);
        }
        logger.info("Verification code email sent successfully to " + to);
    }

    private static boolean usesResend() {
        return RESEND_API_KEY != null && !RESEND_API_KEY.isBlank();
    }

    private void deliver(String to, String subject, String html) throws Exception {
        if (usesResend()) {
            sendViaResend(to, subject, html);
        } else {
            sendViaSmtp(to, subject, html);
        }
    }

    private void sendViaResend(String to, String subject, String html) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "from", RESEND_FROM,
                "to", List.of(to),
                "reply_to", REPLY_TO,
                "subject", subject,
                "html", html));
        HttpRequest request = HttpRequest.newBuilder(RESEND_URI)
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + RESEND_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Resend returned HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private void sendViaSmtp(String to, String subject, String html) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper mimeHelper = new MimeMessageHelper(message, "utf-8");
        mimeHelper.setTo(to);
        mimeHelper.setSubject(subject);
        mimeHelper.setFrom(new InternetAddress(SMTP_FROM));
        mimeHelper.setText(html, true); // true enables HTML
        mailSender.send(message);
    }

    /**
     * Gets the "verify email" template and inserts the link
     */
    private String buildVerifyEmailBody(String link) {
        return EmailTemplates.getVerifyEmailBody().replace("[VERIFICATION_LINK]", link);
    }

    /**
     * Gets the "verification code" template and inserts the code and expiry time
     */
    private String buildVerificationCodeEmailBody(String verificationCode, String expiryTime) {
        return EmailTemplates.getEmailVerificationCodeBody()
                .replace("[VERIFICATION_CODE]", verificationCode)
                .replace("[EXPIRY_TIME]", expiryTime);
    }

}
