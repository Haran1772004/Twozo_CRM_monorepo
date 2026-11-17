package com.tlc.commons.crypto;

import java.security.SecureRandom;

/**
 * A generator that generates random data
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class RandomDataGenerator {

    private final SecureRandom secureRandom;

    private RandomDataGenerator() {
        secureRandom = new SecureRandom();
    }

    private static class Instance {
        private static final RandomDataGenerator INSTANCE = new RandomDataGenerator();
    }

    public static RandomDataGenerator getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Generates random bytes of given size
     *
     * @param size Size of output array
     * @return An array filled with random bytes
     */
    public byte[] generateRandomBytes(final int size) {
        final byte[] randomBytes = new byte[size];

        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * Generates a random number within the given range
     *
     * @param maxSize Value boundary
     * @return Random number
     */
    public int generateRandomNumber(int maxSize) {
        return secureRandom.nextInt(maxSize);
    }
}
