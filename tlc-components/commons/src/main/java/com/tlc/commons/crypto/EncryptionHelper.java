package com.tlc.commons.crypto;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Helper class which can be used to encrypt the data
 *
 * @author Selvakumar
 * @version 1.0
 */
public class EncryptionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionHelper.class);

    private static final String DATA_ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ENCRYPTION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String HASH_DELIMITER = "--";
    private static final int KEY_LENGTH = 128;
    private static final int ITERATION_COUNT = 500;

    private final Base64.Decoder binaryDecoder;
    private final Base64.Encoder binaryEncoder;
    private final RandomDataGenerator randomDataGenerator;

    private static class Instance {
        private static final EncryptionHelper INSTANCE = new EncryptionHelper();
    }

    private EncryptionHelper() {
        binaryDecoder = Base64.getDecoder();
        binaryEncoder = Base64.getEncoder().withoutPadding();
        randomDataGenerator = RandomDataGenerator.getInstance();
    }

    public static EncryptionHelper getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Encrypts data by using the given secret key
     *
     * @param secretKey         Secret key
     * @param dataToBeEncrypted Data that has to be encrypted
     * @return Encrypted data
     */
    public String encrypt(String secretKey, String dataToBeEncrypted) {
        try {
            final byte[] initVector = randomDataGenerator.generateRandomBytes(16);
            final byte[] salt = randomDataGenerator.generateRandomBytes(16);
            final Cipher cipher = getCipher(secretKey, salt, initVector, Cipher.ENCRYPT_MODE);

            return String.format("%s%s%s%s%s", binaryEncoder.encodeToString(salt), HASH_DELIMITER,
                    binaryEncoder.encodeToString(initVector), HASH_DELIMITER,
                    binaryEncoder.encodeToString(cipher.doFinal(dataToBeEncrypted.getBytes())));
        } catch (Exception exception) {
            LOGGER.error("Data encryption failed, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.DATA_ENCRYPTION_FAILED);
        }

    }

    /**
     * Decrypts the data by using the given secret key
     *
     * @param secretKey       Secret key
     * @param encryptedString Data that has to be decrypted
     * @return Decrypted data
     */
    public String decrypt(final String secretKey, final String encryptedString) {
        try {
            final String encryptionDetails[] = encryptedString.split(HASH_DELIMITER, 3);

            if (encryptionDetails.length == 3) {
                final byte[] salt = binaryDecoder.decode(encryptionDetails[0]);
                final byte[] initVector = binaryDecoder.decode(encryptionDetails[1]);
                final byte[] cipherBytes = binaryDecoder.decode(encryptionDetails[2]);
                final Cipher cipher = getCipher(secretKey, salt, initVector, Cipher.DECRYPT_MODE);

                return new String(cipher.doFinal(cipherBytes));
            } else {
                throw ErrorCode.get(ErrorCodes.CORRUPTED_STRING_CAN_NOT_BE_DECRYPTED);
            }

        } catch (Exception exception) {
            LOGGER.error("Data decryption failed, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.DATA_DECRYPTION_FAILED);
        }
    }

    /**
     * Initializes the {@link Cipher} instance
     *
     * @param secret     Secret key that can be used to encrypt/decrypt the data
     * @param salt       Salt that has to be used to produce strong encryption
     * @param initVector Initialization vector
     * @param mode       Encryption/Decryption mode
     * @return {@link Cipher} instance
     * @throws Exception In case {@link Cipher} initialization failed
     */
    private Cipher getCipher(final String secret, final byte[] salt, byte[] initVector, int mode) throws Exception {
        final Cipher cipher = Cipher.getInstance(DATA_ENCRYPTION_ALGORITHM);
        final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ENCRYPTION_ALGORITHM);
        final KeySpec keySpec = new PBEKeySpec(secret.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        final Key secretKey = new SecretKeySpec(factory.generateSecret(keySpec).getEncoded(), "AES");

        cipher.init(mode, secretKey, new IvParameterSpec(initVector));
        return cipher;
    }
}
