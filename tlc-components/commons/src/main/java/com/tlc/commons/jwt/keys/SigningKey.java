package com.tlc.commons.jwt.keys;

import com.tlc.commons.jwt.meta.KeyGroup;

import java.security.Key;


/**
 * Holds the keys that is used to sign the JWT token and to verify JWT signature
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface SigningKey {

    /**
     * Represents the group of the key
     *
     * @return Key group
     */
    KeyGroup group();

    /**
     * Fetches the key which is used to sign the JWT token
     *
     * @return Signature key
     */
    Key getSigningKey();

    /**
     * Fetches the key which is used to verify the JWT signature
     *
     * @return Verification key
     */
    Key getVerificationKey();

}
