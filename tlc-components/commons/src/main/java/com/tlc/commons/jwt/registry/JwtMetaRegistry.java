package com.tlc.commons.jwt.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.jwt.meta.JwtMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry for managing and resolving {@link JwtMeta}
 *
 * @param <T> The type of metadata extending {@link JwtMeta}
 * @author sundar
 * @version 1.0
 */
abstract class JwtMetaRegistry<T extends JwtMeta> {

    private final Map<String, T> jwtModelMap;

    JwtMetaRegistry() {
        this.jwtModelMap = new HashMap<>();
    }

    /**
     * <p>
     * Fetches a {@link JwtMeta} model by its name.
     * </p>
     *
     * @param name Represents the name
     * @return the {@link JwtMeta}
     */
    public final T get(final String name) {
        Objects.requireNonNull(name, "Token group cannot be null");

        if (jwtModelMap.containsKey(name)) {
            return jwtModelMap.get(name);
        }
        throw ErrorCode.get(ErrorCodes.INVALID_JWT_GROUP);
    }

    /**
     * <p>
     * Removes a {@link JwtMeta} model from the registry by its name
     * </p>
     *
     * @param name Represents the name
     */
    public final void remove(final String name) {
        jwtModelMap.remove(name);
    }

    /**
     * <p>
     * Registers a new {@link JwtMeta} model into the registry
     * </p>
     *
     * @param model Represents the {@link JwtMeta} model
     */
    public abstract void register(final T model);

    /**
     * Adds a model to the jwt model map
     *
     * @param key   The unique key associated with the model
     * @param model Represents the {@link JwtMeta}
     */
    void put(final String key, final T model) {
        jwtModelMap.putIfAbsent(key, model);
    }
}