package com.danielagapov.spawn.Services.SMS;

public interface ISMSVerificationService {

    void sendSMSVerification(String phoneNumber);

    boolean checkSMSVerification(String phoneNumber, String verificationCode);
}
