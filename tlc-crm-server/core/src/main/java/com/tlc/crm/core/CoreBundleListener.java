package com.tlc.crm.core;

import com.tlc.commons.service.listener.BundleListener;

import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.core.registrar.CoreResourceRegistrar;

import org.osgi.framework.Bundle;

/**
 * Listener is responsible for handling {@link Bundle} events, including activation and deactivation,
 * for instances within the core module.
 *
 * @author Kishore J
 * @version 1.0
 */
class CoreBundleListener extends BundleListener {

    private final CoreResourceRegistrar coreRegistrar;

    CoreBundleListener() {
        coreRegistrar = CoreResourceRegistrar.getInstance();
    }

    /**
     * <p>
     * Registers the required actions which are present in the bundle
     * </p>
     *
     * @param bundle started bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        registrarData.setClasses(getClasses(bundle));
        coreRegistrar.register(registrarData);
    }

    /**
     * <p>
     * Clears the actions which have been registered by the bundle
     * </p>
     *
     * @param bundle stopped bundle
     */
    @Override
    public void bundleStopped(final Bundle bundle) {
        final CrmServerRegistrarData registrarData = new CrmServerRegistrarData();

        registrarData.setModuleName(bundle.getSymbolicName());
        coreRegistrar.unregister(registrarData);
    }
}
