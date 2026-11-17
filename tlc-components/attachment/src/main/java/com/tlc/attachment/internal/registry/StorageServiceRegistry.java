package com.tlc.attachment.internal.registry;

import com.tlc.attachment.service.StorageService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Representing a registry for {@link StorageService} service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class StorageServiceRegistry implements ServiceRegistry<StorageService> {

    private final AtomicReference<StorageService> storageService;

    private static final class Instance {
        private static final StorageServiceRegistry STORAGE_SERVICE_REGISTRY = new StorageServiceRegistry();
    }

    private StorageServiceRegistry() {
        storageService = new AtomicReference<>();
    }

    public static ServiceRegistry<StorageService> getInstance() {
        return Instance.STORAGE_SERVICE_REGISTRY;
    }

    /**
     * <p>
     * Registers a {@link StorageService} with the registry.
     * </p>
     *
     * @param service The {@link StorageService} to register.
     */
    @Override
    public void register(final StorageService service) {
        if (!storageService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link StorageService} instance.
     * </p>
     *
     * @return The {@link StorageService} instance.
     */
    @Override
    public StorageService get() {
        return storageService.get();
    }

    /**
     * <p>
     * Unregisters a {@link StorageService} from the registry.
     * </p>
     *
     * @return The unregistered {@link StorageService}.
     */
    @Override
    public StorageService unregister() {
        return storageService.getAndSet(null);
    }
}
