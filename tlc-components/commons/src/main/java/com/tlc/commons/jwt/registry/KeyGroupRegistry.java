package com.tlc.commons.jwt.registry;

import com.tlc.commons.jwt.meta.KeyGroup;

import java.util.Objects;

/**
 * Registry for managing and resolving {@link KeyGroup}
 *
 * @author sundar
 * @version 1.0
 */
public final class KeyGroupRegistry extends JwtMetaRegistry<KeyGroup> {

    private KeyGroupRegistry() {
    }

    private static class Instance {
        private static final KeyGroupRegistry KEY_GROUP_REGISTRY = new KeyGroupRegistry();
    }

    /**
     * <p>
     * Gets the {@link KeyGroupRegistry} instance
     * </p>
     *
     * @return The {@link KeyGroupRegistry}
     */
    public static KeyGroupRegistry getInstance() {
        return Instance.KEY_GROUP_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param model Represents the {@link KeyGroup} model
     */
    @Override
    public void register(final KeyGroup model) {
        Objects.requireNonNull(model);
        put(model.name(), model);
    }
}
