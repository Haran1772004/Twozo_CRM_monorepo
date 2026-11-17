package com.tlc.cluster;

import com.tlc.cluster.internal.registry.ClusterServiceRegistry;
import com.tlc.cluster.internal.zookeeper.ZookeeperService;
import com.tlc.cluster.service.ClusterService;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.util.Configurations;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Abishek
 * @version 1.0
 */
public class ClusterAccess implements BundleActivator {

    private final ServiceRegistry<ClusterService> clusterServiceRegistry;

    public ClusterAccess() {
        clusterServiceRegistry = ClusterServiceRegistry.getInstance();
    }

    @Override
    public void start(BundleContext bundleContext) {
        final ClusterService clusterService = new ZookeeperService();
        final Configurations configurations = ConfHolder.getInstance().getProperties("zookeeper");

        clusterService.start(configurations);
        clusterServiceRegistry.register(clusterService);
    }

    @Override
    public void stop(BundleContext bundleContext) {
        final ClusterService zookeeperService = clusterServiceRegistry.unregister();

		if (null != zookeeperService) {
			zookeeperService.stop();
		}
    }
}
