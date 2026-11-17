package com.tlc.cluster.internal.registry;

import com.tlc.cluster.service.ClusterService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Representing a registry for {@link ClusterService} service.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ClusterServiceRegistry implements ServiceRegistry<ClusterService> {

    private final AtomicReference<ClusterService> clusterService;

    private static final class Instance {
        private static final ClusterServiceRegistry CLUSTER_SERVICE_REGISTRY = new ClusterServiceRegistry();
    }

    private ClusterServiceRegistry() {
        clusterService = new AtomicReference<>();
    }

    public static ServiceRegistry<ClusterService> getInstance() {
        return Instance.CLUSTER_SERVICE_REGISTRY;
    }

    /**
     * <p>
     * Registers a {@link ClusterService} with the registry.
     * </p>
     *
     * @param service The {@link ClusterService} to register.
     */
    @Override
    public void register(final ClusterService service) {
        if (!clusterService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * <p>
     * Retrieves the registered {@link ClusterService} instance.
     * </p>
     *
     * @return The {@link ClusterService} instance.
     */
    @Override
    public ClusterService get() {
        return clusterService.get();
    }

    /**
     * <p>
     * Unregisters a {@link ClusterService} from the registry.
     * </p>
     *
     * @return The unregistered {@link ClusterService}.
     */
    @Override
    public ClusterService unregister() {
        return clusterService.getAndSet(null);
    }
}
