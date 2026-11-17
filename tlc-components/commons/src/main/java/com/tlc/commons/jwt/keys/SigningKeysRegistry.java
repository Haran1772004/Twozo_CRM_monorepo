package com.tlc.commons.jwt.keys;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.jwt.meta.KeyGroup;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A holder that holds the signing keys information
 *
 * @author sundar
 * @version 1.0
 */
public final class SigningKeysRegistry {

    private Map<KeyGroup, SigningKey> signingKeys;

    private SigningKeysRegistry() {
        signingKeys = new ConcurrentHashMap<>();
    }

    private static final class Instance {
        private static final SigningKeysRegistry SIGNING_KEYS_REGISTRY = new SigningKeysRegistry();
    }

    /**
     * <p>
     * Gets the {@link SigningKeysRegistry} instance
     * </p>
     *
     * @return the {@link SigningKeysRegistry} instance
     */
    public static SigningKeysRegistry getInstance() {
        return Instance.SIGNING_KEYS_REGISTRY;
    }

    /**
     * Fetches the signing key for the given key group
     *
     * @param keyGroup Key group
     * @return The {@link SigningKey}
     */
    public SigningKey get(final KeyGroup keyGroup) {
        final SigningKey signingKey = signingKeys.get(keyGroup);

        if (Objects.isNull(signingKey)) {
            throw ErrorCode.get(ErrorCodes.NO_SIGNING_KEY_FOUND, "Group " + keyGroup);
        }

        return signingKey;
    }

    /**
     * Checks whether any signing keys are registered or not
     *
     * @return If any signing keys are registered, it returns true otherwise it returns false
     */
    public boolean isEmpty() {
        return signingKeys.isEmpty();
    }

    /**
     * Identifies the number of signing keys registered
     *
     * @return number of signing keys registered
     */
    public int size() {
        return signingKeys.size();
    }

    /**
     * Registers a signing key
     *
     * @param signingKey Signing key
     */
    public void add(final SigningKey signingKey) {
        signingKeys.put(signingKey.group(), signingKey);
    }

    /**
     * Checks whether any signing key registered for the given key group
     *
     * @param group Key group
     * @return true, if any signing key registered for the given key group
     */
    public boolean contains(final KeyGroup group) {
        return signingKeys.containsKey(group);
    }

    /**
     * Removes the signing key registered for the given key group
     *
     * @param keyGroup Key group
     * @return Signing key
     */
    public SigningKey remove(final KeyGroup keyGroup) {
        return signingKeys.remove(keyGroup);
    }

    /**
     * Clears all the signing keys registered
     */
    public void clear() {
        signingKeys.clear();
    }

}
