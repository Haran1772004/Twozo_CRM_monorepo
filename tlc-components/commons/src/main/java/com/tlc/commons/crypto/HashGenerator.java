package com.tlc.commons.crypto;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * A helper class that can be used to generate a hash of given data
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class HashGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashGenerator.class);

    static final String HASH_GENERATOR_ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final String SECURE_RANDOM_GENERATOR_ALGORITHM = "SHA1PRNG";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final int HASH_LENGTH = 256;
    public static final int ITERATIONS = 100;
    private final SecretKeyFactory secretKeyFactory;
    private final SecureRandom secureRandom;

    private static class Instance {
        private static final HashGenerator INSTANCE = new HashGenerator();
    }

    /**
     * <p>
     * Gets the {@link HashGenerator} instance
     * </p>
     *
     * @return The {@link HashGenerator}
     */
    public static HashGenerator getInstance() {
        return HashGenerator.Instance.INSTANCE;
    }

    private HashGenerator() {
        try {
            secretKeyFactory = SecretKeyFactory.getInstance(HASH_GENERATOR_ALGORITHM);
            secureRandom = SecureRandom.getInstance(SECURE_RANDOM_GENERATOR_ALGORITHM, "SUN");
        } catch (Exception exception) {
            LOGGER.error("Hash generator init action failed, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.HASH_GENERATOR_INIT_ACTION_FAILED);
        }
    }

    /**
     * Generates an irreversible hash for the given input
     *
     * @param input Hash will be generated for this data
     * @return Hash string
     */
    public String generateHash(String input) {
        try {
            final byte[] salt = initSalt();
            final PBEKeySpec spec = new PBEKeySpec(input.toCharArray(), salt, ITERATIONS, HASH_LENGTH);
            final byte[] hash = secretKeyFactory.generateSecret(spec).getEncoded();

            return convertByteArrayToHexString(salt) + ":" + convertByteArrayToHexString(hash);
        } catch (final Exception exception) {
            LOGGER.error("Hash generation failed, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.HASH_GENERATION_FAILED);
        }
    }

    /**
     * <p>
     * Fetches the random unique id
     * </p>
     *
     * @param input     Represents the unique input
     * @param digit     Represents the digit
     * @param isNumeric A flag indicating whether the ID should be numeric or alphanumeric
     * @return The random unique id
     */
    public String getRandomUniqueId(final String input, final int digit, final boolean isNumeric) {
        try {
            final String hash = generateHash(input);
            final String[] parts = hash.split(":");
            final String hashPart = parts[1];
            final String subHash = hashPart.substring(0, Math.min(16, hashPart.length()));
            final BigInteger bigInt = new BigInteger(subHash, 16);

            if (isNumeric) {
                final BigInteger maxValue = BigInteger.TEN.pow(digit);
                final BigInteger uniqueId = bigInt.mod(maxValue);

                return String.format("%0" + digit + "d", uniqueId);
            } else {
                final String id = bigInt.toString(36);

                return id.length() > digit ? id.substring(0, digit)
                        : String.format("%1$" + digit + "s", id).replace(' ', '0');
            }
        } catch (final Exception e) {
            LOGGER.error("Unique ID generation failed, Reason: {}", e.getMessage());
            throw ErrorCode.get(ErrorCodes.RANDOM_UNIQUE_ID_GENERATION_FAILED);
        }
    }

    /**
     * <p>
     * Gets the Hash based Message Authentication Code(H-MAC)
     * </p>
     *
     * @return the hmac
     */
    public String getHmac(final byte[] bytes, final String secretKey) {
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA_256);

            mac.init(new SecretKeySpec(secretKey.getBytes(), HMAC_SHA_256));
            final byte[] hash = mac.doFinal(bytes);

            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            LOGGER.error("H-MAC generation failed. Reason:{}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.HASH_GENERATION_FAILED);
        }
    }

    /**
     * Initializes the salt data
     *
     * @return Salt bytes
     */
    private byte[] initSalt() {
        final byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Converts byte array to hex string
     *
     * @param hashBytes Byte information
     * @return Converted hex string
     */
    private String convertByteArrayToHexString(byte[] hashBytes) {
        final StringBuilder hexBuilder = new StringBuilder();

        for (byte hashByte : hashBytes) {
            hexBuilder.append(String.format("%02x", hashByte));
        }

        return hexBuilder.toString();
    }
}
