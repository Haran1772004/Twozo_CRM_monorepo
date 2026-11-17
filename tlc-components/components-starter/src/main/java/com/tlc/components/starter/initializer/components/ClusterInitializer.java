package com.tlc.components.starter.initializer.components;

import com.tlc.cluster.internal.registry.ClusterServiceRegistry;
import com.tlc.cluster.internal.zookeeper.ZookeeperService;
import com.tlc.cluster.service.ClusterService;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the cluster services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class ClusterInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<ClusterService> clusterServiceRegistry;

    private ClusterInitializer() {
        this.clusterServiceRegistry = ClusterServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final ClusterService clusterService = new ZookeeperService();
            final Configurations configurations = getProperties(ComponentsStarterFields.ZOOKEEPER);

            clusterService.start(configurations);
            clusterServiceRegistry.register(clusterService);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.ZOOKEEPER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return clusterServiceRegistry;
    }
}
