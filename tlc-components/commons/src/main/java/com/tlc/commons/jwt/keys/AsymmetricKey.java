package com.tlc.commons.jwt.keys;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Holds the asymmetric key pair that is used to sign and verify the JWT token.
 *
 * @author Selvakumar
 * @version 1.0
 */
public class AsymmetricKey implements SigningKey {

    private final static Logger LOGGER = LoggerFactory.getLogger(AsymmetricKey.class);

    private final KeyGroup group;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AsymmetricKey(final String groupName, final String publicKeyLocation, final String privateKeyLocation) {
        this.group = KeyGroupRegistry.getInstance().get(groupName);
        this.privateKey = readRsaPrivateKey(privateKeyLocation);
        this.publicKey = readRsaPublicKey(publicKeyLocation);
    }

    public AsymmetricKey(final KeyGroup group, final KeyPair keyPair) {
        this.group = group;
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    /**
     * Reads the private key from the given file and returns it
     *
     * @param privateKeyLocation Location of the private key file
     * @return {@link  PrivateKey}
     */
    private PrivateKey readRsaPrivateKey(final String privateKeyLocation) {
        try {
            final String key = Files.readString(new File(privateKeyLocation).toPath(), Charset.defaultCharset());
            final String privateKeyPEM = key
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PRIVATE KEY-----", "");
            final byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);

            return keyFactory.generatePrivate(keySpec);
        } catch (Exception exception) {
            LOGGER.error("Invalid private key, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.INVALID_PRIVATE_KEY);
        }

    }

    /**
     * Reads the public key from the specified file and returns it
     *
     * @param publicKeyLocation Location of the public key file
     * @return {@link PublicKey}
     */
    private PublicKey readRsaPublicKey(final String publicKeyLocation) {
        try {
            final String key = Files.readString(new File(publicKeyLocation).toPath(), Charset.defaultCharset());
            final String publicKeyPEM = key
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll(System.lineSeparator(), "")
                    .replace("-----END PUBLIC KEY-----", "");
            final byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);

            return keyFactory.generatePublic(keySpec);
        } catch (Exception exception) {
            LOGGER.error("Invalid public key, Reason : {}", exception.getMessage());
            throw ErrorCode.get(ErrorCodes.INVALID_PUBLIC_KEY);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyGroup group() {
        return group;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Key getSigningKey() {
        return privateKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Key getVerificationKey() {
        return publicKey;
    }

}
