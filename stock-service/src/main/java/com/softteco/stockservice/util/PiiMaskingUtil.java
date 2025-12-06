package com.softteco.stockservice.util;

/**
 * Utility class for masking personally identifiable information (PII) in logs.
 * Helps maintain GDPR and privacy compliance.
 */
public final class PiiMaskingUtil {

    private PiiMaskingUtil() {
        // Prevent instantiation
    }

    /**
     * Masks an email address by showing only the first 3 characters before the @
     * symbol.
     * Example: demo@example.com -> dem***@example.com
     *
     * @param email the email address to mask
     * @return masked email address, or "***" if email is null/empty
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }

        if (!email.contains("@")) {
            return "***";
        }

        return email.replaceAll("(^[^@]{1,3})[^@]*", "$1***");
    }

    /**
     * Masks a credit card number by showing only the last 4 digits.
     * Example: 1234567890123456 -> ************3456
     *
     * @param cardNumber the card number to mask
     * @return masked card number
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "*".repeat(cardNumber.length() - 4) + lastFour;
    }

    /**
     * Masks a phone number by showing only the last 4 digits.
     * Example: +1234567890 -> ******7890
     *
     * @param phoneNumber the phone number to mask
     * @return masked phone number
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }

        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "*".repeat(phoneNumber.length() - 4) + lastFour;
    }
}
