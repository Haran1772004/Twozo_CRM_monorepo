package com.tlc.sync.service.oauth.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.sync.service.oauth.service.SyncInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Representing a registry for {@link SyncInitializationService} service.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SyncServiceRegistry implements ServiceRegistry<SyncInitializationService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncServiceRegistry.class);

    private final AtomicReference<SyncInitializationService> syncService;

    private static final class Instance {
        private static final SyncServiceRegistry SYNC_SERVICE_REGISTRY = new SyncServiceRegistry();
    }

    private SyncServiceRegistry() {
        syncService = new AtomicReference<>();
    }

    public static ServiceRegistry<SyncInitializationService> getInstance() {
        return Instance.SYNC_SERVICE_REGISTRY;
    }

    /**
     * <p>
     * Registers a {@link SyncInitializationService} with the registry.
     * </p>
     *
     * @param service The {@link SyncInitializationService} to register.
     */
    @Override
    public void register(final SyncInitializationService service) {
        if (!syncService.compareAndSet(null, service)) {
            LOGGER.debug("Sync service: Service has already been initialized");
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link SyncInitializationService} instance.
     * </p>
     *
     * @return The {@link SyncInitializationService} instance.
     */
    @Override
    public SyncInitializationService get() {
        return syncService.get();
    }

    /**
     * <p>
     * Unregisters a {@link SyncInitializationService} from the registry.
     * </p>
     *
     * @return The unregistered {@link SyncInitializationService}.
     */
    @Override
    public SyncInitializationService unregister() {
        return syncService.getAndSet(null);
    }
}

