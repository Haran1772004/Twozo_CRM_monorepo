package com.tlc.cluster.service.provider;

import com.tlc.cluster.internal.registry.ClusterServiceRegistry;
import com.tlc.cluster.service.ClusterService;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the instance of the {@link ClusterService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ClusterServiceProvider {

    private static final ServiceRegistry<ClusterService> CLUSTER_SERVICE_REGISTRY;

    static {
        CLUSTER_SERVICE_REGISTRY = ClusterServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Retrieves the registered {@link ClusterService} instance.
     * </p>
     *
     * @return The {@link ClusterService} instance.
     */
    public static ClusterService get() {
        final ClusterService service = CLUSTER_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
