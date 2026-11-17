package com.tlc.commons.jwt.keys;

import com.tlc.commons.jwt.meta.KeyGroup;
import com.tlc.commons.jwt.registry.KeyGroupRegistry;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

/**
 * Holds the symmetric key that is used to sign and verify the JWT token.
 *
 * @author Selvakumar
 * @version 1.0
 */
public class SymmetricKey implements SigningKey {

    private KeyGroup group;
    private Key symmetricKey;

    public SymmetricKey(final String groupName, final String secret) {
        this.group = KeyGroupRegistry.getInstance().get(groupName);
        symmetricKey = new SecretKeySpec(secret.getBytes(),
                SignatureAlgorithm.HS256.getJcaName());
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
        return symmetricKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Key getVerificationKey() {
        return symmetricKey;
    }

}
