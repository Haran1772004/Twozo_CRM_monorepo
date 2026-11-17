package com.tlc.crm.common.iam.service;

import com.tlc.commons.util.RandomIdGenerator;

import java.security.SecureRandom;

/**
 * <p>
 * Generating random passwords that match a specified pattern
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public class PasswordGenerator {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "@#$%^&-+=()";

    /**
     * Generates a random password of the specified length, adhering to a strong password pattern.
     *
     * @param length The desired length of the generated password.
     * @return A randomly generated password containing a mix of uppercase and lowercase letters,
     * numbers, and special characters (@#$%^&-+=()).
     */
    public static String generatePassword(final int length) {
        final SecureRandom secureRandom = new SecureRandom();
        final StringBuilder password = new StringBuilder();

        password.append(UPPER.charAt(secureRandom.nextInt(UPPER.length())));
        password.append(LOWER.charAt(secureRandom.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(secureRandom.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(secureRandom.nextInt(SPECIAL_CHARS.length())));
        password.append(RandomIdGenerator.getInstance().generateUniqueASCII(length - 4));

        shufflePassword(password, secureRandom);

        return password.toString();
    }

    /**
     * Shuffles the characters of the provided password.
     *
     * @param password     The password represented as a {@link StringBuilder}.
     * @param secureRandom Represents the {@link SecureRandom}
     */
    private static void shufflePassword(final StringBuilder password, final SecureRandom secureRandom) {

        for (int currentIndex = password.length() - 1; currentIndex > 0; currentIndex--) {
            final int randomIndex = secureRandom.nextInt(currentIndex + 1);
            final char currentCharacter = password.charAt(currentIndex);

            password.setCharAt(currentIndex, password.charAt(randomIndex));
            password.setCharAt(randomIndex, currentCharacter);
        }
    }
}
