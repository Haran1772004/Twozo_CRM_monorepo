package com.tlc.commons.service;

import com.tlc.commons.internal.BundleContainerImpl;
import org.osgi.framework.Bundle;

import java.util.Set;

/**
 * An Interface that provides a way to add the sql-listened bundles.
 */
public interface BundleContainer {

    /**
     * Returns the instance of {@link BundleContainer}.
     *
     * @return {@link BundleContainer} instance.
     */
    static BundleContainer getInstance() {
        return BundleContainerImpl.getInstance();
    }

    /**
     * Stores the sql-listener bundles in a {@link Set}.
     *
     * @param bundle {@link Bundle}.
     */
    void addBundleListenedBySqlListener(final Bundle bundle);

}
