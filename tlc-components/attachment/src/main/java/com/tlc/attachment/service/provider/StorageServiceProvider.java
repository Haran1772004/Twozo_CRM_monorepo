package com.tlc.attachment.service.provider;

import com.tlc.attachment.internal.registry.StorageServiceRegistry;
import com.tlc.attachment.service.StorageService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the instance of the {@link StorageService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class StorageServiceProvider {

    private static final ServiceRegistry<StorageService> STORAGE_SERVICE_REGISTRY;

    static {
        STORAGE_SERVICE_REGISTRY = StorageServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Retrieves the registered {@link StorageService} instance.
     * </p>
     *
     * @return The {@link StorageService} instance.
     */
    public static StorageService get() {
        final StorageService service = STORAGE_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
