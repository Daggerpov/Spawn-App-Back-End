package com.danielagapov.spawn.Services.SMS;

import com.danielagapov.spawn.Exceptions.Logger.ILogger;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TwilioService implements ISMSVerificationService {
    @Value("${twilio.account-sid}")
    private static String twilioAccountSid;

    @Value("${twilio.auth-token}")
    private static String twilioAuthToken;

    @Value("${twilio.service_sid}")
    private static String twilioServiceSid;

    private final ILogger logger;

    public TwilioService(ILogger logger) {
        this.logger = logger;
    }

    @PostConstruct
    public void init() {
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }

    public void sendSMSVerification(String toPhoneNumber) {
        // Expiry is 10 mins
        logger.info("Sending SMS verification");
        Verification verification = Verification.creator(twilioServiceSid, toPhoneNumber, "sms").create();
    }

    public boolean checkSMSVerification(String phoneNumber, String verificationCode) {
        logger.info("Checking SMS verification");
        VerificationCheck verificationCheck = VerificationCheck.creator(twilioServiceSid)
                .setTo(phoneNumber)
                .setCode(verificationCode)
                .create();
        // TODO: block users for too many attempts
        return verificationCheck.getStatus().equals("approved");
    }

}
