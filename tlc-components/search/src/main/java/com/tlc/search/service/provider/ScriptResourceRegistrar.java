package com.tlc.search.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.search.service.ScriptResourceService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Enables scripts registration.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class ScriptResourceRegistrar {

    private static final AtomicReference<ScriptResourceService> INSTANCE = new AtomicReference<>();

    private ScriptResourceRegistrar() {
    }

    /**
     * <p>
     * Registers the {@link ScriptResourceService} implementation.
     * </p>
     *
     * @param discovery The implementation to register.
     * @throws ErrorCode If an implementation is already registered.
     */
    public static void register(final ScriptResourceService discovery) {
        if (!INSTANCE.compareAndSet(null, discovery)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "ScriptResourceRegistrar already registered");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link ScriptResourceService} instance.
     * </p>
     *
     * @return The registered {@link ScriptResourceService}.
     * @throws ErrorCode If no implementation has been registered.
     */
    public static ScriptResourceService get() {
        final ScriptResourceService discovery = INSTANCE.get();

        if (Objects.isNull(discovery)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "ScriptResourceRegistrar not initialized");
        }

        return discovery;
    }
}

