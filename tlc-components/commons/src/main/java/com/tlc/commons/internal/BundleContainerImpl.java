package com.tlc.commons.internal;

import com.tlc.commons.service.BundleContainer;
import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.service.TlcBundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Stores the sqlListened bundles and Tlc activator bundles.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class BundleContainerImpl implements BundleContainer {

    private final Map<Bundle, TlcBundleActivator> bundleStore;
    private final Set<Bundle> bundlesListenedBySqlListener;

    private static class Instance {
        private static final BundleContainerImpl INSTANCE = new BundleContainerImpl();
    }

    private BundleContainerImpl() {
        bundleStore = new LinkedHashMap<>();
        bundlesListenedBySqlListener = new HashSet<>();
    }

    public static BundleContainerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Stores the tlc-activator bundles and its activator instances in a {@link Map}.
     *
     * @param bundleContext The execution context of the bundle being started.
     * @param activator TlcBundleActivator.
     */
    public void setBundleStore(final BundleContext bundleContext, final TlcActivator activator) {
        bundleStore.put(bundleContext.getBundle(), activator);
    }

    /**
     * {@inheritDoc}
     *
     * @param bundle {@link Bundle}
     */
    @Override
    public void addBundleListenedBySqlListener(final Bundle bundle) {
        bundlesListenedBySqlListener.add(bundle);
    }

    /**
     * Get the bundles from the bundle store.
     *
     * @return Bundle store.
     */
    public Map<Bundle, TlcBundleActivator> getBundleStore() {
        return bundleStore;
    }

    /**
     * Get the {@link Set} of sql-listened bundles.
     *
     * @return sql-listened bundles.
     */
    public Set<Bundle> getBundlesListenedBySqlListener() {
        return bundlesListenedBySqlListener;
    }
}
