package com.tlc.email.server.internal.webhook.receiver.provider.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.email.server.internal.status.EmailWebhookErrorCodes;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.model.EncryptedContent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * Decrypts the notification of encrypted content via webhook event.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class OutlookWebhookPayloadDecryptor { //TODO will update this class based on the existing methods

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookWebhookPayloadDecryptor.class);

    private static final String ALGORITHM = "HMACSHA256";

    private final String privateKeyPath;
    private final PrivateKey privateKey;

    private OutlookWebhookPayloadDecryptor() {
        final Configurations configurations = ConfHolder.getInstance().getProperties("sync-service").getNestedConfigs("outlook")
                .getNestedConfigs("webhook");
        this.privateKeyPath = configurations.getNestedConfigs("certificate").getConfig("key.private");
        this.privateKey = loadPrivateKeyFromPem();
    }

    /**
     * <p>
     * Holds the single instance of {@link OutlookWebhookPayloadDecryptor}.
     * </p>
     */
    private static class Instance {
        private static final OutlookWebhookPayloadDecryptor INSTANCE = new OutlookWebhookPayloadDecryptor();
    }

    /**
     * Gets the instance of {@link OutlookWebhookPayloadDecryptor}
     *
     * @return {@link OutlookWebhookPayloadDecryptor}
     */
    public static OutlookWebhookPayloadDecryptor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Decrypts the encrypted content.
     *
     * @param encryptedContent Represents the {@link EncryptedContent}
     * @return decrypted data
     */
    public String decrypt(final EncryptedContent encryptedContent) {

        try {
            final byte[] decryptedSymmetricKey = decryptDataKey(encryptedContent.getKey());
            final String generatedSignature = getGeneratedSignature(encryptedContent.getData(), decryptedSymmetricKey);

            if (encryptedContent.getSignature().equals(generatedSignature)) {
                final SecretKey aesKey = new SecretKeySpec(decryptedSymmetricKey, "AES");
                final IvParameterSpec symmetricKey = new IvParameterSpec(Arrays.copyOf(decryptedSymmetricKey, 16));
                final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");

                cipher.init(Cipher.DECRYPT_MODE, aesKey, symmetricKey);

                return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedContent.getData())));
            } else {
                throw ErrorCode.get(EmailWebhookErrorCodes.SIGNATURE_VERIFICATION_FAILED);
            }
        } catch (Exception e) {
            throw ErrorCode.get(EmailWebhookErrorCodes.DECRYPT_ACTION_FAILED, e);
        }
    }

    /**
     * Loads the private key fro pem file.
     *
     * @return {@link PrivateKey}
     */
    private PrivateKey loadPrivateKeyFromPem() {

        try (InputStream inputStream = getClass().getResourceAsStream(privateKeyPath)) {

            if (Objects.isNull(inputStream)) {
                throw ErrorCode.get(EmailWebhookErrorCodes.PRIVATE_KEY_NOT_FOUND);
            }

            final String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");

            final byte[] der = Base64.getDecoder().decode(pem);

            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw ErrorCode.get(EmailWebhookErrorCodes.FAILED_TO_LOAD_PRIVATE_KEY, e);
        }
    }

    /**
     * Decrypts the data key by using the encrypted base64.
     *
     * @param encryptedKeyBase64 Represents the encrypted base64.
     * @return bytes
     */
    private byte[] decryptDataKey(String encryptedKeyBase64) {

        try {
            final byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);
            final Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");

            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            return cipher.doFinal(encryptedKey);
        } catch (Exception e) {
            throw ErrorCode.get(EmailWebhookErrorCodes.FAILED_TO_DECRYPT_DATA_KEY, e);
        }
    }

    /**
     * Gets the signature of decrypted data.
     *
     * @param encryptedData         Represents the encrypted data
     * @param decryptedSymmetricKey Represents the bytes of symmetric key
     * @return signature
     */
    private String getGeneratedSignature(final String encryptedData, final byte[] decryptedSymmetricKey) {
        final byte[] decodedEncryptedData = Base64.getDecoder().decode(encryptedData);

        try {
            final Mac mac = Mac.getInstance(ALGORITHM);
            final SecretKeySpec dataKey = new SecretKeySpec(decryptedSymmetricKey, ALGORITHM);

            mac.init(dataKey);

            return new String(Base64.getEncoder().encode(mac.doFinal(decodedEncryptedData)));
        } catch (Exception e) {
            throw ErrorCode.get(EmailWebhookErrorCodes.FAILED_TO_GENERATE_SIGNATURE, e);
        }
    }
}
