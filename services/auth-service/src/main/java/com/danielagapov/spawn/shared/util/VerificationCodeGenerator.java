package com.danielagapov.spawn.shared.util;

import java.security.SecureRandom;

/**
 * Utility class for generating verification codes
 */
public final class VerificationCodeGenerator {
    
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DIGITS = "0123456789";
    
    /**
     * Generates a 6-digit verification code
     * @return a 6-digit string code
     */
    public static String generateVerificationCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        }
        return code.toString();
    }
} 