package com.tlc.commons.crypto;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * A verifier that verifies the hashed data
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class HashVerifier {

    private final static Logger LOGGER = LoggerFactory.getLogger(HashVerifier.class);

    /**
     * Verifies the hash of given input against the hash string
     *
     * @param input      Input that has to be tested
     * @param hashString Original hash
     * @return If the input's hash matches the hashString, it will return true
     */
    public static boolean verifyHash(String input, String hashString) {
        try {
            final String[] hashParts = hashString.split(":");
            final int iterations = HashGenerator.ITERATIONS;
            final byte[] salt = convertHexStringToByteArray(hashParts[0]);
            final byte[] hash = convertHexStringToByteArray(hashParts[1]);
            final PBEKeySpec spec = new PBEKeySpec(input.toCharArray(),
                    salt, iterations, hash.length * 8);
            final byte[] passwordHash = SecretKeyFactory.getInstance(HashGenerator.HASH_GENERATOR_ALGORITHM).generateSecret(spec).getEncoded();
            int difference = hash.length ^ passwordHash.length;

            for (int index = 0; index < hash.length && index < passwordHash.length; index++) {
                difference |= hash[index] ^ passwordHash[index];
            }

            return difference == 0;
        } catch (final Exception exception) {
            LOGGER.error("Hash validation failed, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.HASH_VALIDATION_FAILED);
        }
    }

    private static byte[] convertHexStringToByteArray(final String hex) {
        final byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = 2 * i;

            bytes[i] = (byte) Integer.parseInt(hex.substring(index, index + 2), 16);
        }
        return bytes;
    }
}
